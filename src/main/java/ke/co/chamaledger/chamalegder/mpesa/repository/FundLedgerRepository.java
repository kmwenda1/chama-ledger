package ke.co.chamaledger.chamalegder.mpesa.repository;

import ke.co.chamaledger.chamalegder.model.FundLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FundLedgerRepository extends JpaRepository<FundLedger, Long> {

    Optional<FundLedger> findTopByOrderByIdDesc();

    Optional<FundLedger> findByReferenceId(String referenceId);

    @Query("""
            SELECT SUM(fl.credit)
            FROM FundLedger fl
            WHERE UPPER(fl.transactionType) = 'CONTRIBUTION'
              AND fl.createdAt >= :start
              AND fl.createdAt < :end
            """)
    Optional<BigDecimal> sumContributionCreditsBetween(@Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    @Query("""
            SELECT DISTINCT mt.phoneNumber
            FROM FundLedger fl
            JOIN MpesaTransaction mt
              ON fl.referenceId = mt.mpesaReceiptNumber
              OR fl.referenceId = mt.checkoutRequestID
            WHERE UPPER(fl.transactionType) = 'CONTRIBUTION'
              AND fl.createdAt >= :start
              AND fl.createdAt < :end
            """)
    List<String> findContributionPhoneNumbersBetween(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

    @Query("""
            SELECT fl
            FROM FundLedger fl
            JOIN MpesaTransaction mt ON fl.referenceId = mt.mpesaReceiptNumber
            WHERE mt.phoneNumber = :phone
               OR mt.phoneNumber = CONCAT('+', :phone)
               OR CONCAT('+', mt.phoneNumber) = :phone
            ORDER BY fl.id DESC
            """)
    List<FundLedger> findMemberHistoryByPhone(@Param("phone") String phone);
}
