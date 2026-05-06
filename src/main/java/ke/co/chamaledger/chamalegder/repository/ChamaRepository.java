package ke.co.chamaledger.chamalegder.repository;

import ke.co.chamaledger.chamalegder.entity.Chama;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChamaRepository extends JpaRepository<Chama, UUID> {

    List<Chama> findByCreatedBy_Id(UUID userId);

    boolean existsByRegistrationNumber(String registrationNumber);
}
