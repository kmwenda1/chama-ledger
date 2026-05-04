package ke.co.chamaledger.chamalegder.repository;

import ke.co.chamaledger.chamalegder.entity.ChamaMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChamaMemberRepository extends JpaRepository<ChamaMember, UUID> {

    // Fix for DataInitializer
    Optional<ChamaMember> findByChama_IdAndUser_Id(UUID chamaId, UUID userId);

    // Fix for MeetingService
    List<ChamaMember> findByUser_PhoneNumberAndIsActiveTrue(String phoneNumber);

    // FIX FOR LoanService & LedgerService (The "cannot find symbol" error you just sent)
    Optional<ChamaMember> findFirstByUser_PhoneNumberAndIsActiveTrue(String phoneNumber);

    // Used by other parts of LoanService
    Optional<ChamaMember> findFirstByChama_IdAndUser_PhoneNumberAndIsActiveTrue(UUID chamaId, String phoneNumber);

    @Query("SELECT cm FROM ChamaMember cm JOIN FETCH cm.user WHERE cm.isActive = true")
    List<ChamaMember> findActiveMembersWithUsers();

    @Query("SELECT cm FROM ChamaMember cm WHERE cm.isActive = true AND cm.role IN :roles")
    List<ChamaMember> findActiveMembersByRoles(List<String> roles);

    List<ChamaMember> findByChama_Id(UUID chamaId);

    List<ChamaMember> findByUser_Id(UUID userId);
}