package ke.co.chamaledger.chamalegder.mpesa.service;

import ke.co.chamaledger.chamalegder.domain.Loan;
import ke.co.chamaledger.chamalegder.model.FundLedger;
import ke.co.chamaledger.chamalegder.dto.TransactionHistoryDTO;
import ke.co.chamaledger.chamalegder.mpesa.model.MpesaTransaction;
import ke.co.chamaledger.chamalegder.mpesa.repository.FundLedgerRepository;
import ke.co.chamaledger.chamalegder.repository.ChamaMemberRepository;
import ke.co.chamaledger.chamalegder.entity.ChamaMember;
import ke.co.chamaledger.chamalegder.notification.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private static final int MONEY_SCALE = 2;

    private final FundLedgerRepository ledgerRepository;
    private final ChamaMemberRepository chamaMemberRepository;
    private final SmsService smsService;

    @Transactional
    public void recordContribution(MpesaTransaction tx) {
        String phone = normalizePhone(tx.getPhoneNumber());
        BigDecimal amount = BigDecimal.valueOf(tx.getAmount());
        String receipt = tx.getMpesaReceiptNumber();
        String referenceId = resolveContributionReference(tx);

        if (isBlank(referenceId)) {
            throw new RuntimeException("Cannot record contribution without an M-Pesa receipt or checkout request ID");
        }
        if (hasExistingContribution(tx)) {
            log.info("Skipping duplicate contribution ledger entry for checkoutRequestID={} receipt={}",
                    tx.getCheckoutRequestID(), tx.getMpesaReceiptNumber());
            return;
        }

        String memberName = findMemberNameByPhone(phone)
                .map(member -> member.getUser().getFullName())
                .orElse("Guest Contributor (" + phone + ")");

        BigDecimal lastBalance = ledgerRepository.findTopByOrderByIdDesc()
                .map(FundLedger::getRunningBalance)
                .orElse(BigDecimal.ZERO);

        FundLedger ledgerEntry = FundLedger.builder()
                .transactionType("CONTRIBUTION")
                .credit(amount)
                .debit(BigDecimal.ZERO)
                .runningBalance(lastBalance.add(amount))
                .description("M-Pesa payment from " + memberName)
                .referenceId(referenceId)
                .build();

        ledgerRepository.save(ledgerEntry);

        System.out.println("\n[CHAMA SYSTEM] >>> SUCCESS: Recorded KES " + amount + " from " + memberName + ". Receipt: " + referenceId + ". New Balance: " + lastBalance.add(amount));

        String message = String.format(
                "Hello %s, we have received your Chama contribution of KES %s. Receipt: %s. Your group balance has been updated.",
                memberName,
                amount,
                referenceId
        );

        System.out.println("[SMS DEBUG] Attempting to send SMS to: " + phone);
        smsService.sendSms(phone, message);
    }

    private boolean hasExistingContribution(MpesaTransaction tx) {
        String receipt = tx.getMpesaReceiptNumber();
        String checkoutRequestId = tx.getCheckoutRequestID();

        if (!isBlank(receipt) && ledgerRepository.findByReferenceId(receipt).isPresent()) {
            return true;
        }

        Optional<FundLedger> checkoutLedger = isBlank(checkoutRequestId)
                ? Optional.empty()
                : ledgerRepository.findByReferenceId(checkoutRequestId);
        if (checkoutLedger.isPresent()) {
            if (!isBlank(receipt)) {
                FundLedger existingEntry = checkoutLedger.get();
                existingEntry.setReferenceId(receipt);
                ledgerRepository.save(existingEntry);
                log.info("Updated reconciled ledger reference from checkoutRequestID={} to receipt={}",
                        checkoutRequestId, receipt);
            }
            return true;
        }

        return false;
    }

    private String resolveContributionReference(MpesaTransaction tx) {
        if (!isBlank(tx.getMpesaReceiptNumber())) {
            return tx.getMpesaReceiptNumber();
        }
        return tx.getCheckoutRequestID();
    }

    @Transactional
    public void recordLoanDisbursement(Loan loan) {
        BigDecimal amount = money(loan.getAmountRequested());
        BigDecimal lastBalance = ledgerRepository.findTopByOrderByIdDesc()
                .map(FundLedger::getRunningBalance)
                .orElse(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        BigDecimal newBalance = money(lastBalance.subtract(amount));

        FundLedger ledgerEntry = FundLedger.builder()
                .transactionType("LOAN_DISBURSEMENT")
                .credit(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .debit(amount)
                .runningBalance(newBalance)
                .description("Loan disbursement to " + loan.getBorrower().getFullName())
                .referenceId(loan.getLoanNumber())
                .build();

        ledgerRepository.save(ledgerEntry);
    }

    @Transactional(readOnly = true)
    public List<TransactionHistoryDTO> getMemberHistory(String phone) {
        String normalizedPhone = normalizePhone(phone);
        return ledgerRepository.findMemberHistoryByPhone(normalizedPhone)
                .stream()
                .map(this::toHistoryDTO)
                .toList();
    }

    private TransactionHistoryDTO toHistoryDTO(FundLedger entry) {
        BigDecimal amount = entry.getCredit() != null && entry.getCredit().compareTo(BigDecimal.ZERO) > 0
                ? entry.getCredit()
                : entry.getDebit();

        return TransactionHistoryDTO.builder()
                .referenceId(entry.getReferenceId())
                .type(entry.getTransactionType())
                .amount(amount)
                .runningBalance(entry.getRunningBalance())
                .description(entry.getDescription())
                .date(entry.getCreatedAt())
                .build();
    }

    private Optional<ChamaMember> findMemberNameByPhone(String phone) {
        String digits = phone == null ? "" : phone.replace("+", "").trim();
        String withPlus = digits.isBlank() ? digits : "+" + digits;

        Optional<ChamaMember> direct = chamaMemberRepository.findFirstByUser_PhoneNumberAndIsActiveTrue(phone);
        if (direct.isPresent()) return direct;

        Optional<ChamaMember> noPlus = chamaMemberRepository.findFirstByUser_PhoneNumberAndIsActiveTrue(digits);
        if (noPlus.isPresent()) return noPlus;

        return chamaMemberRepository.findFirstByUser_PhoneNumberAndIsActiveTrue(withPlus);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replace("+", "").trim();
        return digits.isBlank() ? phone.trim() : "+" + digits;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
