package be.technifutur.grandtourbend.repositories;

import be.technifutur.grandtourbend.entities.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, UUID> {
    List<Collection> findByUser_Id(UUID userId);

    Optional<Collection> findByIdAndUser_Id(UUID id, UUID userId);
}
