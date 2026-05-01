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
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", apiKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/json");

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("username", username);
            body.add("to", phoneNumber);
            body.add("message", message);
            // body.add("from", "CHAMA_LDGR"); // Uncomment once you register a Sender ID

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(AT_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS sent successfully to {}", phoneNumber);
            } else {
                log.error("Failed to send SMS to {}. Status: {}", phoneNumber, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling Africa's Talking API: {}", e.getMessage());
        }
    }
}