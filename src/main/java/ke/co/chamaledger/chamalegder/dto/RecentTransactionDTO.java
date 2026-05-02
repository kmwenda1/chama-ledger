package ke.co.chamaledger.chamalegder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentTransactionDTO {
    private LocalDateTime date;
    private BigDecimal amount;
    private String type;
    private String reference;
    private String status;
}
