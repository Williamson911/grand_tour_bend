package be.technifutur.grandtourbend.repositories;

import be.technifutur.grandtourbend.entities.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {
    Page<Card> findByCardType(String cardType, Pageable pageable);

    @Query("SELECT c FROM Card c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(c.backName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Card> searchByNameOrBackName(@Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM Card c WHERE c.cardType = :cardType AND "
            + "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(c.backName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Card> searchByCardTypeAndNameOrBackName(
            @Param("cardType") String cardType, @Param("search") String search, Pageable pageable);

    @Query(
            value = """
                    SELECT c.id AS card_id, NULL::uuid AS variant_id, c.name, c.back_name, c.card_type, c.color, c.card_number, c.series, c.rarity, c.img_link
                    FROM cards c
                    WHERE (:type IS NULL OR c.card_type = :type)
                      AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:color IS NULL OR c.color = :color)
                      AND (:series IS NULL OR c.series = :series)
                      AND (:rarity IS NULL OR c.rarity = :rarity)
                    UNION ALL
                    SELECT c.id AS card_id, v.id AS variant_id, c.name, c.back_name, c.card_type, c.color, v.card_number, v.series, v.rarity, v.img_link
                    FROM card_variants v
                    JOIN cards c ON c.id = v.card_id
                    WHERE (:type IS NULL OR c.card_type = :type)
                      AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:color IS NULL OR c.color = :color)
                      AND (:series IS NULL OR v.series = :series)
                      AND (:rarity IS NULL OR v.rarity = :rarity)
                    ORDER BY name, card_number
                    """,
            countQuery = """
                    SELECT count(*) FROM (
                        SELECT c.id FROM cards c
                        WHERE (:type IS NULL OR c.card_type = :type)
                          AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                          AND (:color IS NULL OR c.color = :color)
                          AND (:series IS NULL OR c.series = :series)
                          AND (:rarity IS NULL OR c.rarity = :rarity)
                        UNION ALL
                        SELECT v.id FROM card_variants v JOIN cards c ON c.id = v.card_id
                        WHERE (:type IS NULL OR c.card_type = :type)
                          AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                          AND (:color IS NULL OR c.color = :color)
                          AND (:series IS NULL OR v.series = :series)
                          AND (:rarity IS NULL OR v.rarity = :rarity)
                    ) sub
                    """,
            nativeQuery = true
    )
    Page<CardPrintingProjection> findPrintings(
            @Param("type") String type,
            @Param("search") String search,
            @Param("color") String color,
            @Param("series") String series,
            @Param("rarity") String rarity,
            Pageable pageable
    );

    @Query(value = "SELECT DISTINCT color FROM cards WHERE color IS NOT NULL ORDER BY color", nativeQuery = true)
    List<String> findDistinctColors();

    @Query(value = """
            SELECT series FROM cards
            UNION
            SELECT series FROM card_variants
            ORDER BY series
            """, nativeQuery = true)
    List<String> findDistinctSeries();

    @Query(value = """
            SELECT rarity FROM cards
            UNION
            SELECT rarity FROM card_variants
            ORDER BY rarity
            """, nativeQuery = true)
    List<String> findDistinctRarities();
}
