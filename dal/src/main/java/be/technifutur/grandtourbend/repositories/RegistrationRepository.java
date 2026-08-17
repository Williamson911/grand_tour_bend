package be.technifutur.grandtourbend.repositories;

import be.technifutur.grandtourbend.entities.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    List<Registration> findByUser_Id(UUID userId);

    Optional<Registration> findByUser_IdAndEvent_Id(UUID userId, UUID eventId);

    void deleteAllByUser_Id(UUID userId);
}
