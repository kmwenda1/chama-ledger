package ke.co.chamaledger.chamalegder.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ChamaRequest {
    private String name;
    private String description;
    private String registrationNumber;
    private BigDecimal monthlyContribution;
    private Integer contributionDay;
    private BigDecimal loanInterestRate;
    private BigDecimal maxLoanMultiplier;
}