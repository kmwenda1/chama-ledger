package ke.co.chamaledger.chamalegder.mpesa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String phoneNumber;
    private String fullName;
}