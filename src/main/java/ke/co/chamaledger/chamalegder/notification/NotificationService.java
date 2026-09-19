package ke.co.chamaledger.chamalegder.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    @Value("${app.brevo.api-key}")
    private String apiKey;

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Sends a transactional email via Brevo.
     *
     * @param toEmail   recipient email address
     * @param toName    recipient display name (may be null)
     * @param subject   email subject
     * @param htmlContent email body (HTML)
     */
    public void sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            Map<String, Object> body = Map.of(
                    "sender", Map.of("name", "ChamaLedger", "email", "noreply@chamaledger.co.ke"),
                    "to", List.of(Map.of("email", toEmail, "name", toName != null ? toName : toEmail)),
                    "subject", subject,
                    "htmlContent", htmlContent
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent to {} via Brevo. Response: {}", toEmail, response.getBody());
            } else {
                log.error("Failed to send email to {}. Status: {} Response: {}", toEmail, response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error calling Brevo API for {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Convenience method: sends a plain-text notification email.
     */
    public void sendNotification(String toEmail, String toName, String subject, String message) {
        String html = "<p>" + message.replace("\n", "<br/>") + "</p>";
        sendEmail(toEmail, toName, subject, html);
    }
}
