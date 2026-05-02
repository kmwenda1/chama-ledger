package ke.co.chamaledger.chamalegder.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionHistoryDTO {
    private String referenceId;
    private String type;
    private BigDecimal amount;
    private BigDecimal runningBalance;
    private String description;
    private LocalDateTime date;
}
