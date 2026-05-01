package ke.co.chamaledger.chamalegder.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fund_ledger")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FundLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionType; // e.g., "CONTRIBUTION", "LOAN_DISBURSEMENT", "FINE"

    private BigDecimal debit;  // Money out
    private BigDecimal credit; // Money in

    private BigDecimal runningBalance; // Snapshot of balance after this entry

    private String description;
    private String referenceId; // Link to Mpesa Receipt Number or Loan ID

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}