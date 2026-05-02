package ke.co.chamaledger.chamalegder.controller;

import jakarta.validation.Valid;
import ke.co.chamaledger.chamalegder.dto.MeetingResponse;
import ke.co.chamaledger.chamalegder.dto.MeetingUploadRequest;
import ke.co.chamaledger.chamalegder.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<MeetingResponse> uploadMeetingNotes(
            @Valid @RequestBody MeetingUploadRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(meetingService.uploadMeetingNotes(authentication.getName(), request));
    }
}
