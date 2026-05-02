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
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAiService {

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private static final String SECRETARY_PROMPT = "Act as a Chama Secretary. Extract the summary, decisions, and action items from these notes in JSON format.";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    public JsonNode analyzeMeetingNotes(String rawContent) {
        String prompt = SECRETARY_PROMPT + "\nReturn only valid JSON with keys: summary, decisions, actionItems.\n\nNotes:\n" + rawContent;
        String text = generateText(prompt, "application/json");

        try {
            return objectMapper.readTree(text);
        } catch (Exception e) {
            throw new RuntimeException("Gemini returned invalid JSON", e);
        }
    }

    public String generateExecutiveSummary(BigDecimal weeklyContributions,
                                           BigDecimal currentBalance,
                                           int defaulterCount,
                                           int activeMemberCount) {
        String fallback = fallbackExecutiveSummary(weeklyContributions, currentBalance, defaulterCount, activeMemberCount);
        String prompt = """
                Act as a Chama financial advisor. Write exactly two short sentences for leaders.
                Keep the whole response under 180 characters. Mention whether collections are healthy or need follow-up.
                Weekly contributions: KES %s
                Current group balance: KES %s
                Defaulters this week: %d of %d active members
                """.formatted(weeklyContributions, currentBalance, defaulterCount, activeMemberCount);

        try {
            return enforceTwoSentences(generateText(prompt, null), fallback);
        } catch (Exception e) {
            log.warn("Falling back to local executive summary because Gemini failed: {}", e.getMessage());
            return fallback;
        }
    }

    private String generateText(String prompt, String responseMimeType) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new RuntimeException("Gemini API key is not configured");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        );

        if (responseMimeType != null && !responseMimeType.isBlank()) {
            body = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt))
                    )),
                    "generationConfig", Map.of("responseMimeType", responseMimeType)
            );
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String url = UriComponentsBuilder.fromHttpUrl(GEMINI_URL)
                .queryParam("key", geminiApiKey)
                .toUriString();

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
        JsonNode responseBody = response.getBody();
        if (responseBody == null) {
            throw new RuntimeException("Gemini returned an empty response body");
        }

        JsonNode textNode = responseBody
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");

        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            throw new RuntimeException("Gemini returned an empty response");
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
        if (cleaned.isBlank()) {
            return fallback;
        }

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
        if (sentences.length >= 2) {
            return sentences[1].trim();
        }
        return "Leaders should review member follow-up.";
    }

    private boolean endsWithSentencePunctuation(String value) {
        return value.endsWith(".") || value.endsWith("!") || value.endsWith("?");
    }

    private String cleanText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }
}
