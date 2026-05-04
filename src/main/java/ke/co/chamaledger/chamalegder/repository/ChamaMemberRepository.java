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

    // 1. Used by LoanService to find a specific active member by Chama ID and Phone
    Optional<ChamaMember> findFirstByChama_IdAndUser_PhoneNumberAndIsActiveTrue(UUID chamaId, String phoneNumber);

    // 2. Used by LoanService / MeetingService to find an active member globally by phone
    Optional<ChamaMember> findFirstByUser_PhoneNumberAndIsActiveTrue(String phoneNumber);

    Optional<ChamaMember> findByUser_PhoneNumberAndIsActiveTrue(String phoneNumber);

    // 3. Used by ReportService to get members with user details
    // We use @Query to ensure "WithUsers" logic is explicit (Active members)
    @Query("SELECT cm FROM ChamaMember cm JOIN FETCH cm.user WHERE cm.isActive = true")
    List<ChamaMember> findActiveMembersWithUsers();

    // 4. Used by ReportService to filter active members by specific roles (e.g., 'CHAIRMAN', 'TREASURER')
    @Query("SELECT cm FROM ChamaMember cm WHERE cm.isActive = true AND cm.role IN :roles")
    List<ChamaMember> findActiveMembersByRoles(List<String> roles);

    // Standard lookup by Chama
    List<ChamaMember> findByChama_Id(UUID chamaId);

    // Standard lookup by User
    List<ChamaMember> findByUser_Id(UUID userId);
}