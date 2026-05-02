package ke.co.chamaledger.chamalegder.service;

import ke.co.chamaledger.chamalegder.domain.Loan;
import ke.co.chamaledger.chamalegder.domain.LoanStatus;
import ke.co.chamaledger.chamalegder.dto.LoanApplicationRequest;
import ke.co.chamaledger.chamalegder.dto.LoanDetailResponse;
import ke.co.chamaledger.chamalegder.dto.LoanReviewRequest;
import ke.co.chamaledger.chamalegder.entity.ChamaMember;
import ke.co.chamaledger.chamalegder.entity.User;
import ke.co.chamaledger.chamalegder.model.FundLedger;
import ke.co.chamaledger.chamalegder.mpesa.repository.FundLedgerRepository;
import ke.co.chamaledger.chamalegder.mpesa.service.LedgerService;
import ke.co.chamaledger.chamalegder.notification.SmsService;
import ke.co.chamaledger.chamalegder.repository.ChamaMemberRepository;
import ke.co.chamaledger.chamalegder.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final List<LoanStatus> ACTIVE_STATUSES = List.of(
            LoanStatus.PENDING,
            LoanStatus.APPROVED,
            LoanStatus.DISBURSED
    );
    private static final int MIN_TRUST_SCORE = 60;
    private static final int SCORE_PER_CONTRIBUTION = 10;

    private final LoanRepository loanRepository;
    private final ChamaMemberRepository chamaMemberRepository;
    private final LedgerService ledgerService;
    private final SmsService smsService;
    private final FundLedgerRepository fundLedgerRepository;

    public int calculateTrustScore(String phone) {
        String normalizedPhone = normalizePhone(phone);
        long contributions = fundLedgerRepository.findMemberHistoryByPhone(normalizedPhone)
                .stream()
                .filter(e -> "CONTRIBUTION".equalsIgnoreCase(e.getTransactionType()))
                .count();
        return (int) Math.min(100, contributions * SCORE_PER_CONTRIBUTION);
    }

    public String getLoanIneligibilityReason(String phone) {
        ChamaMember membership = findActiveMembership(phone).orElse(null);
        if (membership == null) return "No active chama membership found.";

        if (membership.getJoinedAt() != null) {
            long daysSinceJoined = java.time.temporal.ChronoUnit.DAYS.between(
                    membership.getJoinedAt().toLocalDate(),
                    java.time.LocalDate.now()
            );
            if (daysSinceJoined < 30) {
                return "Must be an active member for at least 30 days. " + (30 - daysSinceJoined) + " days remaining.";
            }
        }

        int score = calculateTrustScore(phone);
        if (score < MIN_TRUST_SCORE) {
            int needed = (MIN_TRUST_SCORE - score) / SCORE_PER_CONTRIBUTION;
            return "Trust score too low (" + score + "/100). Make " + needed + " more contribution(s) to qualify.";
        }
        return null;
    }

    @Transactional
    public LoanDetailResponse applyForLoan(String phone, LoanApplicationRequest request) {
        ChamaMember membership = findActiveMembership(phone)
                .orElseThrow(() -> new RuntimeException("Active chama membership not found for this user"));

        String ineligibilityReason = getLoanIneligibilityReason(phone);
        if (ineligibilityReason != null) {
            throw new RuntimeException(ineligibilityReason);
        }

        User borrower = membership.getUser();
        if (loanRepository.existsByBorrowerAndStatusIn(borrower, ACTIVE_STATUSES)) {
            throw new RuntimeException("You already have an active loan application or loan");
        }

        BigDecimal amountRequested = money(request.getAmountRequested());
        BigDecimal interestRate = money(membership.getChama().getLoanInterestRate());
        BigDecimal interestAmount = amountRequested.multiply(interestRate)
                .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalRepayable = money(amountRequested.add(interestAmount));
        BigDecimal monthlyRepayment = totalRepayable.divide(
                BigDecimal.valueOf(request.getDurationMonths()),
                MONEY_SCALE,
                RoundingMode.HALF_UP
        );

        Loan loan = Loan.builder()
                .loanNumber(generateLoanNumber())
                .amountRequested(amountRequested)
                .interestRate(interestRate)
                .durationMonths(request.getDurationMonths())
                .totalRepayable(totalRepayable)
                .monthlyRepayment(monthlyRepayment)
                .status(LoanStatus.PENDING)
                .borrower(borrower)
                .purpose(request.getPurpose())
                .build();

        return toResponse(loanRepository.save(loan));
    }

    @Transactional(readOnly = true)
    public List<LoanDetailResponse> getMyLoans(String phone) {
        return findLoansByPhone(phone).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LoanDetailResponse reviewLoan(UUID loanId, LoanReviewRequest request, String reviewerPhone) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new RuntimeException("Only pending loans can be reviewed");
        }
        if (request.getStatus() != LoanStatus.APPROVED && request.getStatus() != LoanStatus.REJECTED) {
            throw new RuntimeException("Loan review status must be APPROVED or REJECTED");
        }

        ChamaMember borrowerMembership = findActiveMembership(loan.getBorrower().getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("Borrower's active chama membership was not found"));
        ChamaMember reviewerMembership = findActiveMembershipInChama(
                borrowerMembership.getChama().getId(),
                reviewerPhone
        ).orElseThrow(() -> new RuntimeException("Reviewer is not an active member of this chama"));

        if (!canReviewLoans(reviewerMembership)) {
            throw new RuntimeException("Only TREASURER or ADMIN members can review loans");
        }

        loan.setReviewNotes(request.getNotes());
        loan.setStatus(request.getStatus());

        if (request.getStatus() == LoanStatus.APPROVED) {
            ledgerService.recordLoanDisbursement(loan);
        }

        Loan savedLoan = loanRepository.save(loan);
        notifyBorrower(savedLoan);
        return toResponse(savedLoan);
    }

    private List<Loan> findLoansByPhone(String phone) {
        String normalizedPhone = normalizePhone(phone);
        List<Loan> loans = loanRepository.findByBorrower_PhoneNumberOrderByCreatedAtDesc(normalizedPhone);
        if (!loans.isEmpty()) return loans;

        String digits = normalizedPhone.replace("+", "");
        loans = loanRepository.findByBorrower_PhoneNumberOrderByCreatedAtDesc(digits);
        if (!loans.isEmpty()) return loans;

        return loanRepository.findByBorrower_PhoneNumberOrderByCreatedAtDesc(phone);
    }

    private Optional<ChamaMember> findActiveMembership(String phone) {
        String normalizedPhone = normalizePhone(phone);

        Optional<ChamaMember> membership = chamaMemberRepository.findFirstByUser_PhoneNumberAndIsActiveTrue(normalizedPhone);
        if (membership.isPresent()) return membership;

        String digits = normalizedPhone.replace("+", "");
        membership = chamaMemberRepository.findFirstByUser_PhoneNumberAndIsActiveTrue(digits);
        if (membership.isPresent()) return membership;

        return chamaMemberRepository.findFirstByUser_PhoneNumberAndIsActiveTrue(phone);
    }

    private Optional<ChamaMember> findActiveMembershipInChama(UUID chamaId, String phone) {
        String normalizedPhone = normalizePhone(phone);

        Optional<ChamaMember> membership = chamaMemberRepository.findFirstByChama_IdAndUser_PhoneNumberAndIsActiveTrue(chamaId, normalizedPhone);
        if (membership.isPresent()) return membership;

        String digits = normalizedPhone.replace("+", "");
        membership = chamaMemberRepository.findFirstByChama_IdAndUser_PhoneNumberAndIsActiveTrue(chamaId, digits);
        if (membership.isPresent()) return membership;

        return chamaMemberRepository.findFirstByChama_IdAndUser_PhoneNumberAndIsActiveTrue(chamaId, phone);
    }

    private boolean canReviewLoans(ChamaMember member) {
        String role = member.getRole();
        return "TREASURER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    private void notifyBorrower(Loan loan) {
        String message = switch (loan.getStatus()) {
            case APPROVED -> String.format(
                    "Hello %s, your loan %s has been approved. Amount: KES %s. Monthly repayment: KES %s.",
                    loan.getBorrower().getFullName(),
                    loan.getLoanNumber(),
                    money(loan.getAmountRequested()),
                    money(loan.getMonthlyRepayment())
            );
            case REJECTED -> String.format(
                    "Hello %s, your loan %s has been rejected. Notes: %s",
                    loan.getBorrower().getFullName(),
                    loan.getLoanNumber(),
                    loan.getReviewNotes() == null || loan.getReviewNotes().isBlank() ? "No notes provided." : loan.getReviewNotes()
            );
            default -> null;
        };

        if (message != null) {
            smsService.sendSms(loan.getBorrower().getPhoneNumber(), message);
        }
    }

    private String generateLoanNumber() {
        String prefix = "CL-" + Year.now().getValue() + "-";
        long nextSequence = loanRepository.countByLoanNumberStartingWith(prefix) + 1;
        return prefix + String.format("%05d", nextSequence);
    }

    private LoanDetailResponse toResponse(Loan loan) {
        User borrower = loan.getBorrower();
        return LoanDetailResponse.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .amountRequested(money(loan.getAmountRequested()))
                .interestRate(money(loan.getInterestRate()))
                .durationMonths(loan.getDurationMonths())
                .totalRepayable(money(loan.getTotalRepayable()))
                .monthlyRepayment(money(loan.getMonthlyRepayment()))
                .status(loan.getStatus())
                .purpose(loan.getPurpose())
                .reviewNotes(loan.getReviewNotes())
                .borrowerName(borrower.getFullName())
                .borrowerPhoneNumber(borrower.getPhoneNumber())
                .createdAt(loan.getCreatedAt())
                .build();
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) {
            throw new RuntimeException("Required financial value is missing");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replace("+", "").trim();
        return digits.isBlank() ? phone.trim() : "+" + digits;
    }
}
