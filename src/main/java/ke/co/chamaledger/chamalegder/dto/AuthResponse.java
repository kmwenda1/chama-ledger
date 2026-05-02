package ke.co.chamaledger.chamalegder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String token;
    private String message;
    private String fullName;
    private String phoneNumber;
    private String role; // MEMBER, TREASURER, MANAGER
}