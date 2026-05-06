package ke.co.chamaledger.chamalegder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
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

    // ─── Meeting Notes ────────────────────────────────────────────────────────

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

            return callGroqForJson(requestBody);

        } catch (Exception e) {
            log.error("Error calling Groq API for meeting notes", e);
            throw new RuntimeException("AI processing failed: " + e.getMessage(), e);
        }
    }

    // ─── Weekly Report Executive Summary ─────────────────────────────────────

    public String generateExecutiveSummary(BigDecimal totalContributions,
                                           BigDecimal currentBalance,
                                           int defaulterCount,
                                           int totalMembers) {
        try {
            String prompt = """
                You are a financial analyst for a Kenyan Chama (savings group).
                Write a concise 1-2 sentence executive summary (max 160 characters) based on:
                - Total contributions this week: KES %s
                - Current balance: KES %s
                - Members who did not contribute: %d out of %d

                Be direct and professional. Do not use bullet points or headers.
                """.formatted(totalContributions, currentBalance, defaulterCount, totalMembers);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "temperature", 0.3,
                    "max_tokens", 100,
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "You write short, clear financial summaries for Chama groups."),
                            Map.of("role", "user", "content", prompt)
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_API_URL, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("choices").get(0).path("message").path("content").asText("").trim();
            }

            log.warn("Groq returned non-200 for executive summary: {}", response.getStatusCode());
            return fallbackSummary(totalContributions, currentBalance, defaulterCount, totalMembers);

        } catch (Exception e) {
            log.error("Error generating executive summary via Groq", e);
            return fallbackSummary(totalContributions, currentBalance, defaulterCount, totalMembers);
        }
    }

    // ─── Shared helpers ───────────────────────────────────────────────────────

    private JsonNode callGroqForJson(Map<String, Object> requestBody) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(GROQ_API_URL, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            return objectMapper.readTree(content);
        }

        throw new RuntimeException("Groq API returned status: " + response.getStatusCode());
    }

    private String fallbackSummary(BigDecimal contributions, BigDecimal balance,
                                   int defaulters, int total) {
        return "Weekly contributions: KES %s, balance: KES %s. %d/%d members contributed."
                .formatted(contributions, balance, total - defaulters, total);
    }
}