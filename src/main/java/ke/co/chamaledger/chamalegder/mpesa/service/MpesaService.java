package ke.co.chamaledger.chamalegder.mpesa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.chamaledger.chamalegder.mpesa.config.MpesaConfig;
import ke.co.chamaledger.chamalegder.mpesa.dto.AccessTokenResponse;
import ke.co.chamaledger.chamalegder.mpesa.dto.StkPushRequest;
import ke.co.chamaledger.chamalegder.mpesa.model.MpesaTransaction;
import ke.co.chamaledger.chamalegder.mpesa.repository.MpesaTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpesaService {

    private final MpesaConfig mpesaConfig;
    private final RestTemplate restTemplate;
    private final MpesaTransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    public String getAccessToken() {
        String auth = mpesaConfig.getConsumerKey() + ":" + mpesaConfig.getConsumerSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + encodedAuth);

        ResponseEntity<AccessTokenResponse> response = restTemplate.exchange(
                mpesaConfig.getAuthUrl(), HttpMethod.GET, new HttpEntity<>(headers), AccessTokenResponse.class
        );
        return response.getBody().getAccessToken();
    }

    public String sendStkPush(String phone, String amount, String reference) {
        if (phone.startsWith("+")) phone = phone.substring(1);
        if (phone.startsWith("0")) phone = "254" + phone.substring(1);
        String timestamp = MpesaUtils.getTimestamp();
        String password = MpesaUtils.getPassword(mpesaConfig.getShortcode(), mpesaConfig.getPasskey(), timestamp);

        StkPushRequest body = StkPushRequest.builder()
                .businessShortCode(mpesaConfig.getShortcode())
                .password(password)
                .timestamp(timestamp)
                .transactionType("CustomerPayBillOnline")
                .amount(amount)
                .partyA(phone)
                .partyB(mpesaConfig.getShortcode())
                .phoneNumber(phone)
                .callBackURL(mpesaConfig.getCallbackUrl())
                .accountReference(reference)
                .transactionDesc("Chama Payment")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String response = restTemplate.postForObject(mpesaConfig.getStkPushUrl(), new HttpEntity<>(body, headers), String.class);
        savePendingTransaction(phone, amount, response);
        return response;
    }

    @Transactional
    public void processCallback(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode body = root.path("Body").path("stkCallback");

            String checkoutRequestID = body.path("CheckoutRequestID").asText();
            int resultCode = body.path("ResultCode").asInt();
            String resultDesc = body.path("ResultDesc").asText();

            System.out.println("[CALLBACK DEBUG] ResultCode: " + resultCode + ", ResultDesc: " + resultDesc);

            MpesaTransaction transaction = transactionRepository.findByCheckoutRequestID(checkoutRequestID)
                    .orElse(new MpesaTransaction());

            transaction.setCheckoutRequestID(checkoutRequestID);
            transaction.setMerchantRequestID(body.path("MerchantRequestID").asText());
            transaction.setResultDesc(resultDesc);

            if (resultCode == 0) {
                transaction.setStatus("SUCCESS");
                JsonNode metadataItems = body.path("CallbackMetadata").path("Item");
                for (JsonNode item : metadataItems) {
                    String name = item.path("Name").asText();
                    JsonNode value = item.path("Value");

                    switch (name) {
                        case "Amount" -> transaction.setAmount(value.asDouble());
                        case "MpesaReceiptNumber" -> transaction.setMpesaReceiptNumber(value.asText());
                        case "PhoneNumber" -> transaction.setPhoneNumber(value.asText());
                        case "TransactionDate" -> transaction.setTransactionDate(MpesaUtils.parseMpesaTimestamp(value.asText()));
                    }
                }

                System.out.println("[CALLBACK DEBUG] ResultCode is 0. Recording contribution now.");
                ledgerService.recordContribution(transaction);
            } else {
                transaction.setStatus("FAILED");
                System.out.println("[CALLBACK DEBUG] Payment was not successful, so ledger was not updated.");
            }

            transactionRepository.save(transaction);

        } catch (Exception e) {
            log.error("Failed to parse M-Pesa callback", e);
            throw new RuntimeException("Failed to process M-Pesa callback", e);
        }
    }

    public void reconcileStaleTransactions() {
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(15);
        var pendingTransactions = transactionRepository.findByStatusAndCreatedAtBefore("PENDING", staleBefore);

        log.info("M-Pesa reconciliation started. Found {} stale pending transaction(s) older than {}",
                pendingTransactions.size(), staleBefore);

        for (MpesaTransaction transaction : pendingTransactions) {
            reconcileTransaction(transaction);
        }

        log.info("M-Pesa reconciliation finished. Checked {} transaction(s)", pendingTransactions.size());
    }

    private void savePendingTransaction(String phone, String amount, String responseBody) {
        try {
            if (responseBody == null || responseBody.isBlank()) {
                log.warn("STK Push response was empty; no pending transaction was saved");
                return;
            }

            JsonNode response = objectMapper.readTree(responseBody);
            String checkoutRequestId = textOrNull(response.path("CheckoutRequestID"));
            if (checkoutRequestId == null) {
                log.warn("STK Push response did not contain CheckoutRequestID; no pending transaction was saved");
                return;
            }

            MpesaTransaction transaction = transactionRepository.findByCheckoutRequestID(checkoutRequestId)
                    .orElse(new MpesaTransaction());
            transaction.setCheckoutRequestID(checkoutRequestId);
            transaction.setMerchantRequestID(textOrNull(response.path("MerchantRequestID")));
            transaction.setPhoneNumber(phone);
            transaction.setAmount(Double.valueOf(amount));
            transaction.setStatus("PENDING");
            transaction.setResultDesc(response.path("ResponseDescription").asText("STK Push accepted"));

            transactionRepository.save(transaction);
            log.info("Saved pending M-Pesa transaction checkoutRequestID={} phone={} amount={}",
                    checkoutRequestId, phone, amount);
        } catch (Exception e) {
            log.error("Failed to save pending M-Pesa transaction from STK Push response", e);
        }
    }

    private void reconcileTransaction(MpesaTransaction pendingTransaction) {
        String checkoutRequestId = pendingTransaction.getCheckoutRequestID();
        if (checkoutRequestId == null || checkoutRequestId.isBlank()) {
            log.warn("Skipping pending M-Pesa transaction id={} because CheckoutRequestID is missing",
                    pendingTransaction.getId());
            return;
        }

        try {
            log.info("Reconciling pending M-Pesa transaction id={} checkoutRequestID={}",
                    pendingTransaction.getId(), checkoutRequestId);

            JsonNode response = queryStkStatus(checkoutRequestId);
            JsonNode resultCodeNode = response.path("ResultCode");
            String resultDesc = response.path("ResultDesc")
                    .asText(response.path("ResponseDescription").asText("No result description"));

            if (resultCodeNode.isMissingNode() || resultCodeNode.asText().isBlank()) {
                log.info("M-Pesa reconciliation still pending for checkoutRequestID={}. ResponseDesc={}",
                        checkoutRequestId, resultDesc);
                return;
            }

            int resultCode = resultCodeNode.asInt();
            log.info("M-Pesa reconciliation response checkoutRequestID={} resultCode={} resultDesc={}",
                    checkoutRequestId, resultCode, resultDesc);

            if (resultCode == 0) {
                markReconciledSuccess(pendingTransaction.getId(), response, resultDesc);
            } else {
                markReconciledFailed(pendingTransaction.getId(), resultDesc);
            }
        } catch (Exception e) {
            log.error("M-Pesa reconciliation failed for checkoutRequestID={}", checkoutRequestId, e);
        }
    }

    private JsonNode queryStkStatus(String checkoutRequestId) {
        String timestamp = MpesaUtils.getTimestamp();
        String password = MpesaUtils.getPassword(mpesaConfig.getShortcode(), mpesaConfig.getPasskey(), timestamp);

        Map<String, String> body = Map.of(
                "BusinessShortCode", mpesaConfig.getShortcode(),
                "Password", password,
                "Timestamp", timestamp,
                "CheckoutRequestID", checkoutRequestId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                mpesaConfig.getStkQueryUrl(),
                new HttpEntity<>(body, headers),
                JsonNode.class
        );

        if (response.getBody() == null) {
            throw new RuntimeException("Safaricom STK Query returned an empty response");
        }
        return response.getBody();
    }

    private void markReconciledSuccess(Long transactionId, JsonNode response, String resultDesc) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            MpesaTransaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Pending transaction not found: " + transactionId));
            if (!"PENDING".equalsIgnoreCase(transaction.getStatus())) {
                log.info("Skipping reconciliation success for transaction id={} because status is already {}",
                        transactionId, transaction.getStatus());
                return;
            }

            String receiptNumber = extractReceiptNumber(response);
            if (receiptNumber != null) {
                transaction.setMpesaReceiptNumber(receiptNumber);
            } else {
                log.warn("STK Query success for checkoutRequestID={} did not include MpesaReceiptNumber; using checkoutRequestID as ledger reference",
                        transaction.getCheckoutRequestID());
            }

            transaction.setStatus("SUCCESS");
            transaction.setResultDesc(resultDesc);
            ledgerService.recordContribution(transaction);
            transactionRepository.save(transaction);
            log.info("Reconciled M-Pesa transaction SUCCESS id={} checkoutRequestID={} receipt={}",
                    transaction.getId(), transaction.getCheckoutRequestID(), transaction.getMpesaReceiptNumber());
        });
    }

    private void markReconciledFailed(Long transactionId, String resultDesc) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            MpesaTransaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Pending transaction not found: " + transactionId));
            if (!"PENDING".equalsIgnoreCase(transaction.getStatus())) {
                log.info("Skipping reconciliation failure for transaction id={} because status is already {}",
                        transactionId, transaction.getStatus());
                return;
            }

            transaction.setStatus("FAILED");
            transaction.setResultDesc(resultDesc);
            transactionRepository.save(transaction);
            log.info("Reconciled M-Pesa transaction FAILED id={} checkoutRequestID={} resultDesc={}",
                    transaction.getId(), transaction.getCheckoutRequestID(), resultDesc);
        });
    }

    private String extractReceiptNumber(JsonNode response) {
        JsonNode receipt = response.findValue("MpesaReceiptNumber");
        if (receipt == null || receipt.asText().isBlank()) {
            receipt = response.findValue("ReceiptNumber");
        }
        if (receipt == null || receipt.asText().isBlank()) {
            receipt = response.findValue("MpesaReceipt");
        }
        return receipt == null || receipt.asText().isBlank() ? null : receipt.asText();
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.asText().isBlank() ? null : node.asText();
    }
}
