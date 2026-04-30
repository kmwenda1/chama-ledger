package ke.co.chamaledger.chamalegder.service;

import ke.co.chamaledger.chamalegder.dto.AuthResponse;
import ke.co.chamaledger.chamalegder.dto.LoginRequest;
import ke.co.chamaledger.chamalegder.dto.RegisterRequest;
import ke.co.chamaledger.chamalegder.entity.User;
import ke.co.chamaledger.chamalegder.repository.UserRepository;
import ke.co.chamaledger.chamalegder.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        // 1. Check if user already exists
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number already registered");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // 2. Build the new user object
        User user = User.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .nationalId(request.getNationalId())
                .passwordHash(passwordEncoder.encode(request.getPassword())) // Hash the password!
                .isActive(true)
                .isPhoneVerified(false) // We'll verify this later with OTP
                .build();

        // 3. Save to database
        userRepository.save(user);

        // 4. Generate JWT Token
        String jwtToken = jwtService.generateToken(user);

        // 5. Return the response
        return AuthResponse.builder()
                .token(jwtToken)
                .message("User registered successfully")
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate the user (Spring checks the hashed password automatically here)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getPhoneNumber(),
                        request.getPassword()
                )
        );

        // 2. If we reach here, password is correct. Fetch the user.
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Generate a fresh JWT Token
        String jwtToken = jwtService.generateToken(user);

        // 4. Return the response
        return AuthResponse.builder()
                .token(jwtToken)
                .message("Login successful")
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}