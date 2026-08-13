package be.technifutur.grandtourbend.repositories;

import be.technifutur.grandtourbend.entities.Expenses;
import be.technifutur.grandtourbend.entities.Results;
import io.micrometer.common.KeyValues;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResultsRepository extends JpaRepository<Results, UUID> {
    List<Results> findByUser_Id(UUID userId);

    Optional<Results> findByUser_IdAndEvent_Id(UUID userId, UUID eventId);
}
