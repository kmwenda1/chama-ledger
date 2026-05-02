package ke.co.chamaledger.chamalegder.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MeetingResponse {
    private UUID id;
    private String title;
    private LocalDateTime meetingDate;
    private String rawContent;
    private String summary;
    private JsonNode decisions;
    private JsonNode actionItems;
    private LocalDateTime createdAt;
}
