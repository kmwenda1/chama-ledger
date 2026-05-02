package ke.co.chamaledger.chamalegder.dto;

import ke.co.chamaledger.chamalegder.domain.LoanStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LoanDetailResponse {
    private UUID id;
    private String loanNumber;
    private BigDecimal amountRequested;
    private BigDecimal interestRate;
    private Integer durationMonths;
    private BigDecimal totalRepayable;
    private BigDecimal monthlyRepayment;
    private LoanStatus status;
    private String purpose;
    private String borrowerName;
    private String borrowerPhoneNumber;
    private LocalDateTime createdAt;
}
