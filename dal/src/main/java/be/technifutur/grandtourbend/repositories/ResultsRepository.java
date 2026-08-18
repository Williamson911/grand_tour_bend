package be.technifutur.grandtourbend.repositories;

import be.technifutur.grandtourbend.entities.Expenses;
import be.technifutur.grandtourbend.entities.Results;
import io.micrometer.common.KeyValues;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResultsRepository extends JpaRepository<Results, UUID> {
    @EntityGraph(attributePaths = "leaderCard")
    List<Results> findByUser_Id(UUID userId);

    @EntityGraph(attributePaths = "leaderCard")
    Optional<Results> findByUser_IdAndEvent_Id(UUID userId, UUID eventId);

    void deleteAllByUser_Id(UUID userId);
}
