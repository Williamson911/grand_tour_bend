package be.technifutur.grandtourbend.repositories;

import be.technifutur.grandtourbend.entities.CardVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardVariantRepository extends JpaRepository<CardVariant, UUID> {
    Optional<CardVariant> findByCardNumber(String cardNumber);

    Optional<CardVariant> findByImgLink(String imgLink);
}
