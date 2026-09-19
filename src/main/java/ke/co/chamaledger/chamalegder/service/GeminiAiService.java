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
public class GeminiAiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.model-url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent}")
    private String modelUrl;

    // ─── Meeting Notes ────────────────────────────────────────────────────────

    public JsonNode analyzeMeetingNotes(String rawContent) {
        try {
            String prompt = """
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

                Here is the raw WhatsApp chat:

                """ + rawContent;

            String responseText = callGemini(prompt);

            // Strip markdown code fences if present
            String json = responseText.strip();
            if (json.startsWith("```")) {
                json = json.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").strip();
            }

            return objectMapper.readTree(json);

        } catch (Exception e) {
            log.error("Error calling Gemini API for meeting notes", e);
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

            String content = callGemini(prompt);
            return content != null ? content.trim() : fallbackSummary(totalContributions, currentBalance, defaulterCount, totalMembers);

        } catch (Exception e) {
            log.error("Error generating executive summary via Gemini", e);
            return fallbackSummary(totalContributions, currentBalance, defaulterCount, totalMembers);
        }
    }

    // ─── Shared helpers ───────────────────────────────────────────────────────

    private String callGemini(String prompt) {
        String url = modelUrl + "?key=" + apiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("candidates").get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText();
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage(), e);
            }
        }

        throw new RuntimeException("Gemini API returned status: " + response.getStatusCode());
    }

    private String fallbackSummary(BigDecimal contributions, BigDecimal balance,
                                   int defaulters, int total) {
        return "Weekly contributions: KES %s, balance: KES %s. %d/%d members contributed."
                .formatted(contributions, balance, total - defaulters, total);
    }
}
