package ke.co.chamaledger.chamalegder.mpesa.repository;

import ke.co.chamaledger.chamalegder.model.FundLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FundLedgerRepository extends JpaRepository<FundLedger, Long> {

    /**
     * Gets the most recent ledger entry to retrieve the current running balance.
     */
    Optional<FundLedger> findTopByOrderByIdDesc();
}