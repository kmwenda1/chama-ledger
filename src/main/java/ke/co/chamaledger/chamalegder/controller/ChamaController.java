package ke.co.chamaledger.chamalegder.controller;

import ke.co.chamaledger.chamalegder.dto.ChamaRequest;
import ke.co.chamaledger.chamalegder.entity.Chama;
import ke.co.chamaledger.chamalegder.entity.User;
import ke.co.chamaledger.chamalegder.service.ChamaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chamas")
@RequiredArgsConstructor
public class ChamaController {

    private final ChamaService chamaService;

    @PostMapping
    public ResponseEntity<Chama> createChama(
            @RequestBody ChamaRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(chamaService.createChama(request, currentUser));
    }
}