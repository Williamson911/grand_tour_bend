package be.technifutur.grandtourbend.repositories;

import be.technifutur.grandtourbend.entities.CollectionCard;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionCardRepository extends JpaRepository<CollectionCard, UUID> {
    @EntityGraph(attributePaths = {"card", "variant"})
    List<CollectionCard> findByCollection_Id(UUID collectionId);

    @EntityGraph(attributePaths = {"card", "variant"})
    Optional<CollectionCard> findTopByCollection_IdOrderByPriceDesc(UUID collectionId);

    @Query("SELECT COALESCE(SUM(cc.quantity), 0) FROM CollectionCard cc WHERE cc.collection.id = :collectionId")
    long sumQuantityByCollection_Id(@Param("collectionId") UUID collectionId);

    @Query("SELECT COALESCE(SUM(cc.quantity * cc.price), 0.0) FROM CollectionCard cc WHERE cc.collection.id = :collectionId")
    BigDecimal sumTotalPriceByCollection_Id(@Param("collectionId") UUID collectionId);

    void deleteAllByCollection_Id(UUID collectionId);
}
