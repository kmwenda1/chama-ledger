package ke.co.chamaledger.chamalegder.mpesa.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mpesa_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MpesaTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String checkoutRequestID; // Link to the specific STK push

    private String merchantRequestID;

    @Column(unique = true)
    private String mpesaReceiptNumber; // The "UE1HC2O1B3" code

    private Double amount;
    private String phoneNumber;
    private String status; // PENDING, SUCCESS, FAILED
    private String resultDesc;
    private LocalDateTime transactionDate;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}