package ke.co.chamaledger.chamalegder.repository;

import ke.co.chamaledger.chamalegder.entity.ChamaMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChamaMemberRepository extends JpaRepository<ChamaMember, UUID> {

    List<ChamaMember> findByChama_Id(UUID chamaId);

    List<ChamaMember> findByUser_Id(UUID userId);

    Optional<ChamaMember> findByChama_IdAndUser_Id(UUID chamaId, UUID userId);

    // NEW: Find member by the phone number attached to the User entity
    // We use First because a user might be in multiple chamas;
    // for now, we find the first active one.
    Optional<ChamaMember> findFirstByUser_PhoneNumberAndIsActiveTrue(String phoneNumber);
}