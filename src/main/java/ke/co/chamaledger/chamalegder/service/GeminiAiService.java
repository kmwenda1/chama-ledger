package ke.co.chamaledger.chamalegder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAiService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL = "llama3-8b-8192";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.groq.api-key:}")
    private String groqApiKey;

    public JsonNode analyzeMeetingNotes(String rawContent) {
        String prompt = """
                Act as a Chama Secretary. Extract the summary, decisions, and action items from these meeting notes.
                Return ONLY valid JSON with exactly these keys: summary, decisions, actionItems.
                - summary: a short paragraph summarizing the meeting
                - decisions: an array of strings, each being a decision made
                - actionItems: an array of objects with keys: task and assignee
                Do not include any text outside the JSON.
                
                Meeting Notes:
                """ + rawContent;

        String text = generateText(prompt);

        try {
            // Clean up markdown code blocks if present
            String cleaned = text
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.error("Groq returned invalid JSON: {}", text);
            throw new RuntimeException("Groq returned invalid JSON: " + e.getMessage());
        }
    }

    public String generateExecutiveSummary(BigDecimal weeklyContributions,
                                           BigDecimal currentBalance,
                                           int defaulterCount,
                                           int activeMemberCount) {
        String fallback = fallbackExecutiveSummary(
                weeklyContributions, currentBalance, defaulterCount, activeMemberCount);

        String prompt = """
                Act as a Chama financial advisor. Write exactly two short sentences for leaders.
                Keep the whole response under 180 characters. Mention whether collections are healthy or need follow-up.
                Weekly contributions: KES %s
                Current group balance: KES %s
                Defaulters this week: %d of %d active members
                """.formatted(weeklyContributions, currentBalance, defaulterCount, activeMemberCount);

        try {
            return enforceTwoSentences(generateText(prompt), fallback);
        } catch (Exception e) {
            log.warn("Falling back to local executive summary because Groq failed: {}", e.getMessage());
            return fallback;
        }
    }

    private String generateText(String prompt) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            throw new RuntimeException("Groq API key is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
                "model", GROQ_MODEL,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                GROQ_URL, request, JsonNode.class);

        JsonNode responseBody = response.getBody();
        if (responseBody == null) {
            throw new RuntimeException("Groq returned an empty response body");
        }

        JsonNode textNode = responseBody
                .path("choices")
                .path(0)
                .path("message")
                .path("content");

        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            throw new RuntimeException("Groq returned an empty response");
        }

        return textNode.asText().trim();
    }

    private String fallbackExecutiveSummary(BigDecimal weeklyContributions,
                                            BigDecimal currentBalance,
                                            int defaulterCount,
                                            int activeMemberCount) {
        String collectionHealth = weeklyContributions.compareTo(BigDecimal.ZERO) > 0
                ? "Collections are active this week."
                : "Collections are quiet this week.";
        String followUp = defaulterCount == 0
                ? "All active members have contributed."
                : "%d of %d active members need follow-up.".formatted(defaulterCount, activeMemberCount);

        return collectionHealth + " Balance is KES " + currentBalance + "; " + followUp;
    }

    private String enforceTwoSentences(String summary, String fallback) {
        String cleaned = cleanText(summary);
        if (cleaned.isBlank()) return fallback;

        String[] sentences = cleaned.split("(?<=[.!?])\\s+");
        if (sentences.length >= 2) {
            return sentences[0].trim() + " " + sentences[1].trim();
        }

        if (endsWithSentencePunctuation(cleaned)) {
            return cleaned + " " + secondFallbackSentence(fallback);
        }

        return cleaned + ". " + secondFallbackSentence(fallback);
    }

    private String secondFallbackSentence(String fallback) {
        String[] sentences = fallback.split("(?<=[.!?])\\s+");
        if (sentences.length >= 2) return sentences[1].trim();
        return "Leaders should review member follow-up.";
    }

    private boolean endsWithSentencePunctuation(String value) {
        return value.endsWith(".") || value.endsWith("!") || value.endsWith("?");
    }

    private String cleanText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }
}