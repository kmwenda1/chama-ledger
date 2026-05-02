package ke.co.chamaledger.chamalegder.dto;

import jakarta.validation.constraints.NotNull;
import ke.co.chamaledger.chamalegder.domain.LoanStatus;
import lombok.Data;

@Data
public class LoanReviewRequest {

    @NotNull(message = "Review status is required")
    private LoanStatus status;

    private String notes;
}
