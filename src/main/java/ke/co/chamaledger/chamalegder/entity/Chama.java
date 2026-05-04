package ke.co.chamaledger.chamalegder.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chamas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chama {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Column(name = "paybill_number", length = 20)
    private String paybillNumber;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "monthly_contribution", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyContribution; // Using BigDecimal for accurate money math!

    @Column(name = "contribution_day", nullable = false)
    private Integer contributionDay;

    @Column(name = "meeting_frequency", nullable = false, length = 20)
    @Builder.Default
    private String meetingFrequency = "MONTHLY";

    @Column(name = "loan_interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal loanInterestRate;

    @Column(name = "max_loan_multiplier", nullable = false, precision = 4, scale = 1)
    private BigDecimal maxLoanMultiplier;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // This creates the foreign key relationship to the User table!
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
