package ke.co.chamaledger.chamalegder.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class SmsService {

    @Value("${app.africas-talking.api-key}")
    private String apiKey;

    @Value("${app.africas-talking.username}")
    private String username;

    private final String AT_URL = "https://api.africastalking.com/version1/messaging";
    private final RestTemplate restTemplate = new RestTemplate();

    public void sendSms(String phoneNumber, String message) {
        String normalizedPhone = normalizeForAfricasTalking(phoneNumber);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", apiKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/json");

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("username", username);
            body.add("to", normalizedPhone);
            body.add("message", message);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(AT_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS API accepted for {}. Response: {}", normalizedPhone, response.getBody());
            } else {
                log.error("Failed to send SMS to {}. Status: {} Response: {}", normalizedPhone, response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error calling Africa's Talking API for {}: {}", normalizedPhone, e.getMessage(), e);
        }
    }

    private String normalizeForAfricasTalking(String phoneNumber) {
        if (phoneNumber == null) return "";
        String trimmed = phoneNumber.trim();
        if (trimmed.startsWith("+")) return trimmed;

        String digits = trimmed.replaceAll("\\s+", "");
        if (digits.startsWith("254")) return "+" + digits;

        return digits;
    }
}
