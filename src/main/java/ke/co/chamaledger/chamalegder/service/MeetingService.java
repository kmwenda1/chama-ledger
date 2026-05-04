package ke.co.chamaledger.chamalegder.service;

import com.fasterxml.jackson.databind.JsonNode;
import ke.co.chamaledger.chamalegder.domain.Meeting;
import ke.co.chamaledger.chamalegder.dto.MeetingResponse;
import ke.co.chamaledger.chamalegder.dto.MeetingUploadRequest;
import ke.co.chamaledger.chamalegder.entity.ChamaMember;
import ke.co.chamaledger.chamalegder.repository.ChamaMemberRepository;
import ke.co.chamaledger.chamalegder.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final ChamaMemberRepository chamaMemberRepository;
    private final GeminiAiService geminiAiService;

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
        return geminiAiService.analyzeMeetingNotes(rawContent);
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
