package ke.co.chamaledger.chamalegder.repository;

import ke.co.chamaledger.chamalegder.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    // Finds the most recent matching OTP code for verification
    Optional<OtpCode> findFirstByPhoneNumberAndCodeAndPurposeOrderByCreatedAtDesc(
            String phoneNumber, String code, String purpose);
}