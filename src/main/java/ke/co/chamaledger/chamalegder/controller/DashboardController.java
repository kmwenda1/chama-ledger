package ke.co.chamaledger.chamalegder.controller;

import ke.co.chamaledger.chamalegder.domain.Loan;
import ke.co.chamaledger.chamalegder.domain.LoanStatus;
import ke.co.chamaledger.chamalegder.dto.DashboardResponse;
import ke.co.chamaledger.chamalegder.dto.LoanDetailResponse;
import ke.co.chamaledger.chamalegder.dto.MpesaLogDTO;
import ke.co.chamaledger.chamalegder.dto.RecentTransactionDTO;
import ke.co.chamaledger.chamalegder.entity.ChamaMember;
import ke.co.chamaledger.chamalegder.entity.User;
import ke.co.chamaledger.chamalegder.model.FundLedger;
import ke.co.chamaledger.chamalegder.mpesa.model.MpesaTransaction;
import ke.co.chamaledger.chamalegder.mpesa.repository.FundLedgerRepository;
import ke.co.chamaledger.chamalegder.mpesa.repository.MpesaTransactionRepository;
import ke.co.chamaledger.chamalegder.repository.ChamaMemberRepository;
import ke.co.chamaledger.chamalegder.repository.LoanRepository;
import ke.co.chamaledger.chamalegder.service.LoanService;
import ke.co.chamaledger.chamalegder.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/dashboard")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class DashboardController {

    private static final int MONEY_SCALE = 2;
    private static final List<LoanStatus> ACTIVE_LOAN_STATUSES = List.of(
            LoanStatus.PENDING,
            LoanStatus.APPROVED,
            LoanStatus.DISBURSED
    );

    private final FundLedgerRepository fundLedgerRepository;
    private final LoanRepository loanRepository;
    private final ChamaMemberRepository chamaMemberRepository;
    private final MpesaTransactionRepository mpesaTransactionRepository;
    private final LoanService loanService;
    private final ReportService reportService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardResponse> getDashboardSummary(Principal principal) {
        String phone = (principal == null || principal.getName() == null) ? "" : principal.getName();

        List<FundLedger> memberHistory = fundLedgerRepository.findMemberHistoryByPhone(phone);

        BigDecimal personalSavings = memberHistory.stream()
                .map(FundLedger::getCredit)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal groupBalance = fundLedgerRepository.findTopByOrderByIdDesc()
                .map(FundLedger::getRunningBalance)
                .orElse(BigDecimal.ZERO);

        String aiInsight = "Your Chama is active. Keep contributing to maintain high liquidity!";
        try {
            aiInsight = reportService.generateWeeklyHealthReport().executiveSummary();
        } catch (Exception ignored) {}

        List<RecentTransactionDTO> recentTransactions = memberHistory.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(this::toRecentTransaction)
                .toList();

        // Resolve role and member info
        Optional<ChamaMember> membership = phone.isEmpty()
                ? Optional.empty()
                : chamaMemberRepository.findFirstByUser_PhoneNumberAndIsActiveTrue(phone);

        String role = membership.map(m -> mapChamaRole(m.getRole())).orElse("MEMBER");
        String fullName = membership.map(m -> m.getUser().getFullName()).orElse("");

        // Trust score + loan eligibility
        int trustScore = phone.isEmpty() ? 0 : loanService.calculateTrustScore(phone);
        String ineligibilityReason = phone.isEmpty() ? null : loanService.getLoanIneligibilityReason(phone);
        User memberUser = membership.map(ChamaMember::getUser).orElse(null);
        boolean hasActiveLoan = memberUser != null
                && loanRepository.existsByBorrowerAndStatusIn(memberUser, ACTIVE_LOAN_STATUSES);
        boolean loanEligible = ineligibilityReason == null && !hasActiveLoan;

        // Role-specific data
        List<LoanDetailResponse> pendingLoans = null;
        List<MpesaLogDTO> mpesaLogs = null;

        if ("MANAGER".equals(role)) {
            pendingLoans = loanRepository.findByStatusOrderByCreatedAtDesc(LoanStatus.PENDING)
                    .stream()
                    .map(this::toLoanResponse)
                    .toList();
        }

        if ("TREASURER".equals(role)) {
            mpesaLogs = mpesaTransactionRepository.findTop20ByOrderByCreatedAtDesc()
                    .stream()
                    .map(this::toMpesaLog)
                    .toList();
        }

        DashboardResponse response = DashboardResponse.builder()
                .personalSavings(money(personalSavings))
                .groupBalance(money(groupBalance))
                .activeLoansCount(countActiveLoans(phone))
                .aiInsight(aiInsight)
                .recentTransactions(recentTransactions)
                .role(role)
                .fullName(fullName)
                .trustScore(trustScore)
                .loanEligible(loanEligible)
                .loanIneligibilityReason(ineligibilityReason)
                .pendingLoans(pendingLoans)
                .mpesaLogs(mpesaLogs)
                .build();

        return ResponseEntity.ok(response);
    }

    private String mapChamaRole(String chamaRole) {
        if (chamaRole == null) return "MEMBER";
        return switch (chamaRole.toUpperCase()) {
            case "CHAIRPERSON", "ADMIN" -> "MANAGER";
            case "TREASURER" -> "TREASURER";
            default -> "MEMBER";
        };
    }

    private RecentTransactionDTO toRecentTransaction(FundLedger entry) {
        BigDecimal credit = zeroIfNull(entry.getCredit());
        BigDecimal debit = zeroIfNull(entry.getDebit());
        boolean isCredit = credit.compareTo(BigDecimal.ZERO) > 0;
        return RecentTransactionDTO.builder()
                .date(entry.getCreatedAt())
                .amount(money(isCredit ? credit : debit))
                .type(isCredit ? "CREDIT" : "DEBIT")
                .reference(entry.getReferenceId())
                .status("COMPLETED")
                .build();
    }

    private LoanDetailResponse toLoanResponse(Loan loan) {
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

    private MpesaLogDTO toMpesaLog(MpesaTransaction tx) {
        return MpesaLogDTO.builder()
                .id(tx.getId())
                .phoneNumber(tx.getPhoneNumber())
                .amount(tx.getAmount())
                .status(tx.getStatus())
                .mpesaReceiptNumber(tx.getMpesaReceiptNumber())
                .checkoutRequestID(tx.getCheckoutRequestID())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private int countActiveLoans(String phone) {
        if (phone.isEmpty()) return 0;
        long count = countActiveLoansByExactPhone(phone);
        if (count > 0) return (int) count;
        String normalized = normalizePhone(phone);
        count = countActiveLoansByExactPhone(normalized);
        if (count > 0) return (int) count;
        return (int) countActiveLoansByExactPhone(normalized.replace("+", ""));
    }

    private long countActiveLoansByExactPhone(String phone) {
        return loanRepository.countByBorrower_PhoneNumberAndStatusIn(phone, ACTIVE_LOAN_STATUSES);
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isEmpty()) return "";
        String digits = phone.replaceAll("\\D", "");
        return digits.isEmpty() ? "" : "+" + digits;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return zeroIfNull(value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
