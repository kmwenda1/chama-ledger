package ke.co.chamaledger.chamalegder.mpesa.service;

import ke.co.chamaledger.chamalegder.model.FundLedger;
import ke.co.chamaledger.chamalegder.mpesa.model.MpesaTransaction;
import ke.co.chamaledger.chamalegder.mpesa.repository.FundLedgerRepository;
import ke.co.chamaledger.chamalegder.mpesa.repository.MpesaTransactionRepository;
import ke.co.chamaledger.chamalegder.repository.ChamaMemberRepository;
import ke.co.chamaledger.chamalegder.entity.ChamaMember;
import ke.co.chamaledger.chamalegder.notification.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final FundLedgerRepository ledgerRepository;
    private final ChamaMemberRepository chamaMemberRepository;
    private final MpesaTransactionRepository mpesaTransactionRepository;
    private final SmsService smsService;

    @Transactional
    public void recordContribution(MpesaTransaction tx) {
        String phone = tx.getPhoneNumber();
        BigDecimal amount = BigDecimal.valueOf(tx.getAmount());
        String receipt = tx.getMpesaReceiptNumber();

        // 1. Identify the member by phone number
        String memberName = chamaMemberRepository.findFirstByUser_PhoneNumberAndIsActiveTrue(phone)
                .map(member -> member.getUser().getFullName())
                .orElse("Guest Contributor (" + phone + ")");

        // 2. Update Group Running Balance (Double-Entry)
        BigDecimal lastBalance = ledgerRepository.findTopByOrderByIdDesc()
                .map(FundLedger::getRunningBalance)
                .orElse(BigDecimal.ZERO);

        FundLedger ledgerEntry = FundLedger.builder()
                .transactionType("CONTRIBUTION")
                .credit(amount)
                .debit(BigDecimal.ZERO)
                .runningBalance(lastBalance.add(amount))
                .description("M-Pesa payment from " + memberName)
                .referenceId(receipt)
                .build();

        ledgerRepository.save(ledgerEntry);

        // 3. Send the REAL SMS notification to the member's phone
        String message = String.format(
                "Hello %s, we have received your Chama contribution of KES %s. Receipt: %s. Your group balance has been updated.",
                memberName,
                amount,
                receipt
        );

        smsService.sendSms(phone, message);

        log.info("Full Loop Complete: Ledger entry created and SMS sent to {}", phone);
    }
}