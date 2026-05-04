package ke.co.chamaledger.chamalegder.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chama_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChamaMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Links to the Chama table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chama_id", nullable = false)
    private Chama chama;

    // Links to the User table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String role; // Values: CHAIRPERSON, TREASURER, SECRETARY, or MEMBER

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
