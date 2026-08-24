package be.technifutur.grandtourbend.repositories;

import be.technifutur.grandtourbend.entities.CardVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CardVariantRepository extends JpaRepository<CardVariant, UUID> {
}
