package ke.co.chamaledger.chamalegder.service;

import ke.co.chamaledger.chamalegder.dto.AuthResponse;
import ke.co.chamaledger.chamalegder.dto.LoginRequest;
import ke.co.chamaledger.chamalegder.dto.RegisterRequest;
import ke.co.chamaledger.chamalegder.entity.Chama;
import ke.co.chamaledger.chamalegder.entity.ChamaMember;
import ke.co.chamaledger.chamalegder.entity.User;
import ke.co.chamaledger.chamalegder.repository.ChamaMemberRepository;
import ke.co.chamaledger.chamalegder.repository.ChamaRepository;
import ke.co.chamaledger.chamalegder.repository.UserRepository;
import ke.co.chamaledger.chamalegder.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String DEFAULT_CHAMA_REG = "CL-SEED-001";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ChamaMemberRepository chamaMemberRepository;
    private final ChamaRepository chamaRepository;

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

        // Auto-add new user as an active MEMBER of the default chama
        addToDefaultChama(savedUser);

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

    private void addToDefaultChama(User user) {
        try {
            Optional<Chama> defaultChama = chamaRepository.findAll().stream()
                    .filter(c -> DEFAULT_CHAMA_REG.equals(c.getRegistrationNumber()))
                    .findFirst();

            if (defaultChama.isEmpty()) {
                log.warn("[Auth] Default chama '{}' not found. Skipping auto-membership.", DEFAULT_CHAMA_REG);
                return;
            }

            Chama chama = defaultChama.get();

            // Avoid duplicate membership
            boolean alreadyMember = chamaMemberRepository
                    .findByChama_IdAndUser_Id(chama.getId(), user.getId())
                    .isPresent();

            if (alreadyMember) {
                log.info("[Auth] User {} is already a member of chama {}", user.getPhoneNumber(), chama.getName());
                return;
            }

            ChamaMember member = ChamaMember.builder()
                    .chama(chama)
                    .user(user)
                    .role("MEMBER")
                    .isActive(true)
                    .build();

            chamaMemberRepository.save(member);
            log.info("[Auth] User {} auto-added as MEMBER to chama '{}'", user.getPhoneNumber(), chama.getName());

        } catch (Exception e) {
            log.error("[Auth] Failed to auto-add user {} to default chama: {}", user.getPhoneNumber(), e.getMessage());
        }
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