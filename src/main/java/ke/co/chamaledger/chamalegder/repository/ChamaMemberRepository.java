package ke.co.chamaledger.chamalegder.repository;

import ke.co.chamaledger.chamalegder.entity.ChamaMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChamaMemberRepository extends JpaRepository<ChamaMember, UUID> {

    // Get all members in a specific chama
    List<ChamaMember> findByChama_Id(UUID chamaId);

    // Get all chamas a specific user belongs to
    List<ChamaMember> findByUser_Id(UUID userId);

    // Find a specific member's record in a specific chama
    Optional<ChamaMember> findByChama_IdAndUser_Id(UUID chamaId, UUID userId);
}