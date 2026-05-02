package ke.co.chamaledger.chamalegder.mpesa.repository;

import ke.co.chamaledger.chamalegder.mpesa.model.MpesaTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MpesaTransactionRepository extends JpaRepository<MpesaTransaction, Long> {
    Optional<MpesaTransaction> findByCheckoutRequestID(String checkoutRequestID);

    List<MpesaTransaction> findByStatusAndCreatedAtBefore(String status, LocalDateTime createdBefore);

    List<MpesaTransaction> findTop20ByOrderByCreatedAtDesc();
}
