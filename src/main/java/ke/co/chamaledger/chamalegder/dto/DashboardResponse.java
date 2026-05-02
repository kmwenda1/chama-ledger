package ke.co.chamaledger.chamalegder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private BigDecimal personalSavings;
    private BigDecimal groupBalance;
    private int activeLoansCount;
    private String aiInsight;
    private List<RecentTransactionDTO> recentTransactions;

    // Auth & role
    private String role;           // MEMBER, TREASURER, MANAGER
    private String fullName;

    // Loan eligibility
    private int trustScore;
    private boolean loanEligible;
    private String loanIneligibilityReason;

    // MANAGER view
    private List<LoanDetailResponse> pendingLoans;

    // TREASURER view
    private List<MpesaLogDTO> mpesaLogs;
}
