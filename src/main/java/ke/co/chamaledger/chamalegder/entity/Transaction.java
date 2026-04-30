package ke.co.chamaledger.chamalegder.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chama_id", nullable = false)
    private Chama chama;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String type; // CONTRIBUTION, LOAN_REPAYMENT, DISBURSEMENT

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, FAILED

    private String mpesaReceiptNumber;

    private String phoneNumber; // The number that paid

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}