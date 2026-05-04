package ke.co.chamaledger.chamalegder.domain;

import jakarta.persistence.*;
import ke.co.chamaledger.chamalegder.entity.User;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "loan_number", nullable = false, unique = true, length = 30)
    private String loanNumber;

    @Column(name = "amount_requested", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountRequested;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "total_repayable", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRepayable;

    @Column(name = "monthly_repayment", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRepayment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private User borrower;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
