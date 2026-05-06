package ke.co.chamaledger.chamalegder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroqAiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.groq.api-key}")
    private String apiKey;

    @Value("${app.groq.model:llama-3.1-8b-instant}")
    private String model;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public JsonNode analyzeMeetingNotes(String rawContent) {
        try {
            String systemPrompt = """
                You are an expert assistant that processes WhatsApp Chama group meeting notes.
                Extract and return **only** valid JSON with this exact structure:

                {
                  "summary": "One paragraph summary of the meeting",
                  "decisions": ["Decision one", "Decision two"],
                  "actionItems": [
                    {
                      "person": "Full Name",
                      "task": "What they are supposed to do",
                      "deadline": "10th May 2024 or null"
                    }
                  ]
                }
                """;

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "temperature", 0.2,
                    "max_tokens", 2048,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", "Here is the raw WhatsApp chat:\n\n" + rawContent)
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_API_URL, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices")
                        .get(0)
                        .path("message")
                        .path("content")
                        .asText();

                return objectMapper.readTree(content);
            }

            throw new RuntimeException("Groq API returned status: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("Error calling Groq API", e);
            throw new RuntimeException("AI processing failed: " + e.getMessage(), e);
        }
    }
}