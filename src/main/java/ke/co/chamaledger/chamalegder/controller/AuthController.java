package ke.co.chamaledger.chamalegder.controller;

import jakarta.validation.Valid;
import ke.co.chamaledger.chamalegder.dto.AuthResponse;
import ke.co.chamaledger.chamalegder.dto.LoginRequest;
import ke.co.chamaledger.chamalegder.dto.RegisterRequest;
import ke.co.chamaledger.chamalegder.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request // @Valid triggers our DTO rules!
    ) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }
}