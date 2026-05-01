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
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpesaService {

    private final MpesaConfig mpesaConfig;
    private final RestTemplate restTemplate;
    private final MpesaTransactionRepository transactionRepository; // Correct repository
    private final LedgerService ledgerService;

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

        return restTemplate.postForObject(mpesaConfig.getStkPushUrl(), new HttpEntity<>(body, headers), String.class);
    }

    public void processCallback(String jsonResponse) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode body = root.path("Body").path("stkCallback");

            String checkoutRequestID = body.path("CheckoutRequestID").asText();
            int resultCode = body.path("ResultCode").asInt();
            String resultDesc = body.path("ResultDesc").asText();

            // Use MpesaTransactionRepository to find the record
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
                    }
                }

                // Pass the MpesaTransaction object to LedgerService for business logic processing
                ledgerService.recordContribution(transaction);

                log.info("Payment SUCCESS for phone: {}", transaction.getPhoneNumber());
            } else {
                transaction.setStatus("FAILED");
                log.warn("Payment FAILED: {}", resultDesc);
            }

            transactionRepository.save(transaction);

        } catch (Exception e) {
            log.error("Failed to parse M-Pesa callback", e);
        }
    }
}