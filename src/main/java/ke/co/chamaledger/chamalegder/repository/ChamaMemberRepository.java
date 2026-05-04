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

    // FIX FOR ERROR 1: Used by DataInitializer.java (Line 104)
    Optional<ChamaMember> findByChama_IdAndUser_Id(UUID chamaId, UUID userId);

    // FIX FOR ERRORS 2, 3, & 4: Used by MeetingService.java
    // The service expects a List, so we change the return type from Optional to List
    List<ChamaMember> findByUser_PhoneNumberAndIsActiveTrue(String phoneNumber);

    // Used by LoanService
    Optional<ChamaMember> findFirstByChama_IdAndUser_PhoneNumberAndIsActiveTrue(UUID chamaId, String phoneNumber);

    // Used by ReportService
    @Query("SELECT cm FROM ChamaMember cm JOIN FETCH cm.user WHERE cm.isActive = true")
    List<ChamaMember> findActiveMembersWithUsers();

    @Query("SELECT cm FROM ChamaMember cm WHERE cm.isActive = true AND cm.role IN :roles")
    List<ChamaMember> findActiveMembersByRoles(List<String> roles);

    List<ChamaMember> findByChama_Id(UUID chamaId);

    List<ChamaMember> findByUser_Id(UUID userId);
}