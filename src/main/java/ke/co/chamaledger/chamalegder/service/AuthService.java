package ke.co.chamaledger.chamalegder.service;

import ke.co.chamaledger.chamalegder.dto.AuthResponse;
import ke.co.chamaledger.chamalegder.dto.LoginRequest;
import ke.co.chamaledger.chamalegder.dto.RegisterRequest;
import ke.co.chamaledger.chamalegder.entity.ChamaMember;
import ke.co.chamaledger.chamalegder.entity.User;
import ke.co.chamaledger.chamalegder.repository.ChamaMemberRepository;
import ke.co.chamaledger.chamalegder.repository.UserRepository;
import ke.co.chamaledger.chamalegder.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ChamaMemberRepository chamaMemberRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number already registered");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .nationalId(request.getNationalId())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .isPhoneVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        String jwtToken = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .token(jwtToken)
                .message("User registered successfully")
                .fullName(savedUser.getFullName())
                .phoneNumber(savedUser.getPhoneNumber())
                .role("MEMBER")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getPhoneNumber(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String jwtToken = jwtService.generateToken(user);
        String role = resolveRole(request.getPhoneNumber());

        return AuthResponse.builder()
                .token(jwtToken)
                .message("Login successful")
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(role)
                .build();
    }

    private String resolveRole(String phone) {
        return chamaMemberRepository.findFirstByUser_PhoneNumberAndIsActiveTrue(phone)
                .map(m -> mapChamaRole(m.getRole()))
                .orElse("MEMBER");
    }

    private String mapChamaRole(String chamaRole) {
        if (chamaRole == null) return "MEMBER";
        return switch (chamaRole.toUpperCase()) {
            case "CHAIRPERSON", "ADMIN" -> "MANAGER";
            case "TREASURER" -> "TREASURER";
            default -> "MEMBER";
        };
    }
}
