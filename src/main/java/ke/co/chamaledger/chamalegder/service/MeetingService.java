package ke.co.chamaledger.chamalegder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.chamaledger.chamalegder.domain.Meeting;
import ke.co.chamaledger.chamalegder.dto.MeetingResponse;
import ke.co.chamaledger.chamalegder.dto.MeetingUploadRequest;
import ke.co.chamaledger.chamalegder.entity.ChamaMember;
import ke.co.chamaledger.chamalegder.repository.ChamaMemberRepository;
import ke.co.chamaledger.chamalegder.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private static final String SECRETARY_PROMPT = "Act as a Chama Secretary. Extract the summary, decisions, and action items from these notes in JSON format.";

    private final MeetingRepository meetingRepository;
    private final ChamaMemberRepository chamaMemberRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    @Transactional
    public MeetingResponse uploadMeetingNotes(String phone, MeetingUploadRequest request) {
        if (!canUploadMeetingNotes(phone)) {
            throw new RuntimeException("Only CHAIRPERSON or SECRETARY members can upload meeting notes");
        }

        JsonNode processedNotes = processMeetingWithGemini(request.getRawContent());

        Meeting meeting = Meeting.builder()
                .title(request.getTitle())
                .meetingDate(request.getMeetingDate())
                .rawContent(request.getRawContent())
                .summary(processedNotes.path("summary").asText(""))
                .decisions(processedNotes.path("decisions"))
                .actionItems(processedNotes.path("actionItems"))
                .build();

        return toResponse(meetingRepository.save(meeting));
    }

    public JsonNode processMeetingWithGemini(String rawContent) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new RuntimeException("Gemini API key is not configured");
        }

        String prompt = SECRETARY_PROMPT + "\nReturn only valid JSON with keys: summary, decisions, actionItems.\n\nNotes:\n" + rawContent;
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of("responseMimeType", "application/json")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String url = UriComponentsBuilder.fromHttpUrl(GEMINI_URL)
                .queryParam("key", geminiApiKey)
                .toUriString();

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
        JsonNode textNode = response.getBody()
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");

        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            throw new RuntimeException("Gemini returned an empty meeting analysis");
        }

        try {
            return objectMapper.readTree(textNode.asText());
        } catch (Exception e) {
            throw new RuntimeException("Gemini returned invalid JSON", e);
        }
    }

    private boolean canUploadMeetingNotes(String phone) {
        return findMemberships(phone).stream()
                .map(ChamaMember::getRole)
                .anyMatch(role -> "CHAIRPERSON".equalsIgnoreCase(role) || "SECRETARY".equalsIgnoreCase(role));
    }

    private List<ChamaMember> findMemberships(String phone) {
        String normalizedPhone = normalizePhone(phone);
        List<ChamaMember> memberships = chamaMemberRepository.findByUser_PhoneNumberAndIsActiveTrue(normalizedPhone);
        if (!memberships.isEmpty()) return memberships;

        String digits = normalizedPhone.replace("+", "");
        memberships = chamaMemberRepository.findByUser_PhoneNumberAndIsActiveTrue(digits);
        if (!memberships.isEmpty()) return memberships;

        return chamaMemberRepository.findByUser_PhoneNumberAndIsActiveTrue(phone);
    }

    private MeetingResponse toResponse(Meeting meeting) {
        return MeetingResponse.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .meetingDate(meeting.getMeetingDate())
                .rawContent(meeting.getRawContent())
                .summary(meeting.getSummary())
                .decisions(meeting.getDecisions())
                .actionItems(meeting.getActionItems())
                .createdAt(meeting.getCreatedAt())
                .build();
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replace("+", "").trim();
        return digits.isBlank() ? phone.trim() : "+" + digits;
    }
}
