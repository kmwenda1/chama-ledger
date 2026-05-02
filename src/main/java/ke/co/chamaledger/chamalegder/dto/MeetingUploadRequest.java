package ke.co.chamaledger.chamalegder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MeetingUploadRequest {

    @NotBlank(message = "Meeting title is required")
    private String title;

    @NotNull(message = "Meeting date is required")
    private LocalDateTime meetingDate;

    @NotBlank(message = "Meeting notes are required")
    private String rawContent;
}
