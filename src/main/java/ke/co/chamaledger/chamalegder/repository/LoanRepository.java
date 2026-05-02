package ke.co.chamaledger.chamalegder.repository;

import ke.co.chamaledger.chamalegder.domain.Loan;
import ke.co.chamaledger.chamalegder.domain.LoanStatus;
import ke.co.chamaledger.chamalegder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    List<Loan> findByBorrower_PhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    boolean existsByBorrowerAndStatusIn(User borrower, Collection<LoanStatus> statuses);

    long countByBorrower_PhoneNumberAndStatusIn(String phoneNumber, Collection<LoanStatus> statuses);

    long countByLoanNumberStartingWith(String loanNumberPrefix);

    List<Loan> findByStatusOrderByCreatedAtDesc(LoanStatus status);
}
