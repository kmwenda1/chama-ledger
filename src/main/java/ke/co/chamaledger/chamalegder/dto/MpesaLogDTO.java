package ke.co.chamaledger.chamalegder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpesaLogDTO {
    private Long id;
    private String phoneNumber;
    private Double amount;
    private String status;
    private String mpesaReceiptNumber;
    private String checkoutRequestID;
    private LocalDateTime createdAt;
}
