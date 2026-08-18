package be.technifutur.grandtourbend.repositories;

import be.technifutur.grandtourbend.entities.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {
    Page<Card> findByCardType(String cardType, Pageable pageable);

    Page<Card> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Card> findByCardTypeAndNameContainingIgnoreCase(String cardType, String name, Pageable pageable);
}
