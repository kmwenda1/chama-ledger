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

    private String role;
    private String fullName;

    private int trustScore;
    private boolean loanEligible;
    private String loanIneligibilityReason;

    private List<LoanDetailResponse> pendingLoans;

    private List<MpesaLogDTO> mpesaLogs;
}
