# Card Collections — Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the backend half of card collections — a `/cards/printings` endpoint that flattens the card catalog (cards + variants) into one browsable, filterable, paginated list, and full CRUD for user-owned named `Collection`s of `CollectionCard` line items (card + optional variant + quantity + unit price).

**Architecture:** Standard 4-module layering already used throughout this codebase (`dl` entities → `dal` repositories → `bll` services → `cl` DTOs → `api` controllers). `Collection`/`CollectionCard` are new entities following the `UuidBaseEntity` + Lombok pattern used by `Results`. The printings list is not a new entity — it's a native SQL `UNION ALL` between `cards` and `card_variants`, mapped through a Spring Data interface projection, because pagination/filtering has to be correct across the combined ~10k-row set (see `docs/superpowers/specs/2026-08-19-card-collections-design.md` for why `/cards` itself can't be reused for this).

**Tech Stack:** Spring Boot 4 / Java, Spring Data JPA (incl. a native `@Query`), Hibernate `ddl-auto: update` (no manual migration needed — new tables are created on next app boot, same as every other entity in this codebase), JUnit 5 + Mockito + AssertJ.

**Spec:** `docs/superpowers/specs/2026-08-19-card-collections-design.md`

---

## Task 1: `Collection` and `CollectionCard` entities

**Files:**
- Create: `dl/src/main/java/be/technifutur/grandtourbend/entities/Collection.java`
- Create: `dl/src/main/java/be/technifutur/grandtourbend/entities/CollectionCard.java`

No test for these — this codebase doesn't unit-test plain entity classes (confirmed: no `@DataJpaTest`/entity tests exist anywhere in the repo); they're exercised indirectly through the service tests in Task 8.

- [ ] **Step 1: Create the `Collection` entity**

```java
package be.technifutur.grandtourbend.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "collections")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Collection extends UuidBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "user_id", nullable = false)
    @Getter
    private User user;

    @Column(nullable = false)
    @Getter
    @Setter
    private String name;
}
```

No `@OneToMany` back to `CollectionCard` here on purpose — every read/write of a collection's items goes through `CollectionCardRepository` directly (Task 3), the same way `Results.matches` is queried on its own rather than through a bidirectional graph. Keeps the entity minimal and avoids Hibernate orphan-removal/dirty-checking complexity for what's really an explicit "replace all items" operation (Task 8).

- [ ] **Step 2: Create the `CollectionCard` entity**

```java
package be.technifutur.grandtourbend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "collection_cards")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CollectionCard extends UuidBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "collection_id", nullable = false)
    @Getter
    @Setter
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "card_id", nullable = false)
    @Getter
    @Setter
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "variant_id")
    @Getter
    @Setter
    private CardVariant variant;

    @Column(nullable = false)
    @Getter
    @Setter
    private Integer quantity;

    @Column(nullable = false)
    @Getter
    @Setter
    private BigDecimal price;
}
```

`variant` has no `nullable = false` — `null` means "the base printing, not an alt-art", per the spec.

- [ ] **Step 3: Compile check**

Run: `./mvnw -q -pl dl -am compile`
Expected: `BUILD SUCCESS`, no output (the `-q` flag suppresses success output).

- [ ] **Step 4: Commit**

```bash
git add dl/src/main/java/be/technifutur/grandtourbend/entities/Collection.java dl/src/main/java/be/technifutur/grandtourbend/entities/CollectionCard.java
git commit -m "feat: add Collection and CollectionCard entities"
```

---

## Task 2: `CollectionRepository` and `CollectionCardRepository`

**Files:**
- Create: `dal/src/main/java/be/technifutur/grandtourbend/repositories/CollectionRepository.java`
- Create: `dal/src/main/java/be/technifutur/grandtourbend/repositories/CollectionCardRepository.java`
- Create: `dal/src/main/java/be/technifutur/grandtourbend/repositories/CardVariantRepository.java` (didn't exist yet — needed to validate a `variantId` belongs to its `cardId` in Task 8)

No test for these either, same reasoning as Task 1 — plain Spring Data derived-query interfaces, no custom logic to unit test.

- [ ] **Step 1: Create `CollectionRepository`**

```java
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
```

- [ ] **Step 2: Create `CollectionCardRepository`**

```java
package be.technifutur.grandtourbend.repositories;

import be.technifutur.grandtourbend.entities.CollectionCard;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface CollectionCardRepository extends JpaRepository<CollectionCard, UUID> {
    @EntityGraph(attributePaths = {"card", "variant"})
    List<CollectionCard> findByCollection_Id(UUID collectionId);

    @Query("SELECT COALESCE(SUM(cc.quantity), 0) FROM CollectionCard cc WHERE cc.collection.id = :collectionId")
    long sumQuantityByCollection_Id(@Param("collectionId") UUID collectionId);

    @Query("SELECT COALESCE(SUM(cc.quantity * cc.price), 0) FROM CollectionCard cc WHERE cc.collection.id = :collectionId")
    BigDecimal sumTotalPriceByCollection_Id(@Param("collectionId") UUID collectionId);

    void deleteAllByCollection_Id(UUID collectionId);
}
```

- [ ] **Step 3: Create `CardVariantRepository`**

```java
package be.technifutur.grandtourbend.repositories;

import be.technifutur.grandtourbend.entities.CardVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CardVariantRepository extends JpaRepository<CardVariant, UUID> {
}
```

- [ ] **Step 4: Compile check**

Run: `./mvnw -q -pl dal -am compile`
Expected: `BUILD SUCCESS`, no output.

- [ ] **Step 5: Commit**

```bash
git add dal/src/main/java/be/technifutur/grandtourbend/repositories/CollectionRepository.java dal/src/main/java/be/technifutur/grandtourbend/repositories/CollectionCardRepository.java dal/src/main/java/be/technifutur/grandtourbend/repositories/CardVariantRepository.java
git commit -m "feat: add Collection/CollectionCard/CardVariant repositories"
```

---

## Task 3: `CollectionNotFoundException` and `VariantNotOwnedByCardException`

**Files:**
- Create: `bll/src/main/java/be/technifutur/grandtourbend/exceptions/CollectionNotFoundException.java`
- Create: `bll/src/main/java/be/technifutur/grandtourbend/exceptions/VariantNotOwnedByCardException.java`

- [ ] **Step 1: Create `CollectionNotFoundException`**

```java
package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class CollectionNotFoundException extends GrandTourBendException {

    public CollectionNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
```

- [ ] **Step 2: Create `VariantNotOwnedByCardException`**

```java
package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class VariantNotOwnedByCardException extends GrandTourBendException {

    public VariantNotOwnedByCardException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
```

Covers both "no such variant" and "variant exists but belongs to a different card" — from the client's perspective both are the same class of invalid reference, so one exception type is enough (mirrors how `InvalidLeaderCardTypeException` already covers a single validation concern with one exception).

- [ ] **Step 3: Compile check**

Run: `./mvnw -q -pl bll -am compile`
Expected: `BUILD SUCCESS`, no output.

- [ ] **Step 4: Commit**

```bash
git add bll/src/main/java/be/technifutur/grandtourbend/exceptions/CollectionNotFoundException.java bll/src/main/java/be/technifutur/grandtourbend/exceptions/VariantNotOwnedByCardException.java
git commit -m "feat: add Collection-related exceptions"
```

---

## Task 4: `/cards/printings` — repository query

**Files:**
- Create: `dal/src/main/java/be/technifutur/grandtourbend/repositories/CardPrintingProjection.java`
- Modify: `dal/src/main/java/be/technifutur/grandtourbend/repositories/CardRepository.java`

This is the query already hand-verified against the live local database (both the data query and the count subquery return correct rows — a card with variants shows one row per variant plus one row for the base card, e.g. `Tien Shinhan BT28-056` (base) and `Tien Shinhan BT28-056_PR` (variant) as two separate printings). No unit test here — same "no repository-level DB tests" convention as `searchByNameOrBackName` (Task 5 tests the service layer's delegation instead).

- [ ] **Step 1: Create the projection interface**

```java
package be.technifutur.grandtourbend.repositories;

import java.util.UUID;

public interface CardPrintingProjection {
    UUID getCardId();
    UUID getVariantId();
    String getName();
    String getBackName();
    String getCardType();
    String getColor();
    String getCardNumber();
    String getSeries();
    String getRarity();
    String getImgLink();
}
```

- [ ] **Step 2: Add the native query to `CardRepository`**

Add these imports to the existing `CardRepository.java`:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

(Both are likely already imported from the earlier `searchByNameOrBackName` work — check before adding duplicates.)

Add this method to the `CardRepository` interface:

```java
    @Query(
            value = """
                    SELECT c.id AS card_id, NULL::uuid AS variant_id, c.name, c.back_name, c.card_type, c.color, c.card_number, c.series, c.rarity, c.img_link
                    FROM cards c
                    WHERE (:type IS NULL OR c.card_type = :type)
                      AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:color IS NULL OR c.color = :color)
                      AND (:series IS NULL OR c.series = :series)
                    UNION ALL
                    SELECT c.id AS card_id, v.id AS variant_id, c.name, c.back_name, c.card_type, c.color, v.card_number, v.series, v.rarity, v.img_link
                    FROM card_variants v
                    JOIN cards c ON c.id = v.card_id
                    WHERE (:type IS NULL OR c.card_type = :type)
                      AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:color IS NULL OR c.color = :color)
                      AND (:series IS NULL OR v.series = :series)
                    ORDER BY name, card_number
                    """,
            countQuery = """
                    SELECT count(*) FROM (
                        SELECT c.id FROM cards c
                        WHERE (:type IS NULL OR c.card_type = :type)
                          AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                          AND (:color IS NULL OR c.color = :color)
                          AND (:series IS NULL OR c.series = :series)
                        UNION ALL
                        SELECT v.id FROM card_variants v JOIN cards c ON c.id = v.card_id
                        WHERE (:type IS NULL OR c.card_type = :type)
                          AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                          AND (:color IS NULL OR c.color = :color)
                          AND (:series IS NULL OR v.series = :series)
                    ) sub
                    """,
            nativeQuery = true
    )
    Page<CardPrintingProjection> findPrintings(
            @Param("type") String type,
            @Param("search") String search,
            @Param("color") String color,
            @Param("series") String series,
            Pageable pageable
    );
```

- [ ] **Step 3: Compile check**

Run: `./mvnw -q -pl dal -am compile`
Expected: `BUILD SUCCESS`, no output.

- [ ] **Step 4: Manually verify the query against the real local database**

Run: `"C:\Program Files\PostgreSQL\18\bin\psql.exe" "postgresql://postgres:postgres@localhost:5432/grand_tour_bend" -c "SELECT count(*) FROM (SELECT c.id FROM cards c WHERE c.card_type = 'LEADER' UNION ALL SELECT v.id FROM card_variants v JOIN cards c ON c.id = v.card_id WHERE c.card_type = 'LEADER') sub;"`
Expected: a single row with `count` around 740 (740 was the verified value for `type=LEADER` at the time this plan was written — re-check it's in that ballpark, not exactly 740, since the catalog could be re-seeded).

- [ ] **Step 5: Commit**

```bash
git add dal/src/main/java/be/technifutur/grandtourbend/repositories/CardPrintingProjection.java dal/src/main/java/be/technifutur/grandtourbend/repositories/CardRepository.java
git commit -m "feat: add flattened card+variant printings query"
```

---

## Task 5: `/cards/printings` — service and controller

**Files:**
- Create: `cl/src/main/java/be/technifutur/grandtourbend/models/card/responses/CardPrintingResponse.java`
- Modify: `bll/src/main/java/be/technifutur/grandtourbend/CardService.java`
- Modify: `bll/src/main/java/be/technifutur/grandtourbend/services/impls/CardServiceImpl.java`
- Modify: `bll/src/test/java/be/technifutur/grandtourbend/services/impls/CardServiceImplTest.java`
- Modify: `api/src/main/java/be/technifutur/grandtourbend/models/controller/CardController.java`

- [ ] **Step 1: Create the `CardPrintingResponse` DTO**

```java
package be.technifutur.grandtourbend.models.card.responses;

import java.util.UUID;

public record CardPrintingResponse(
        UUID cardId,
        UUID variantId,
        String name,
        String backName,
        String cardType,
        String color,
        String cardNumber,
        String series,
        String rarity,
        String imgLink
) {
}
```

No `fromProjection` factory on the DTO itself, deliberately — `CardPrintingProjection` (Task 4) lives in `dal` and is a Spring Data JPA projection interface tied to one native query's column list, not a domain concept. Giving `cl` a dependency on `dal` just to reference it would cross a layering line that doesn't need crossing: `bll` already depends on both `dal` and `cl`, so it's the natural place to map between them (mirrors the existing precedent of `CardResponse.fromCard(Card)`, except that one's source type — a `dl` entity — is a stable domain type `cl` already legitimately depends on, unlike a query-shaped projection interface). The mapping is a private method on `CardServiceImpl` instead — see Step 5.

- [ ] **Step 2: Write the failing test for `CardServiceImpl.getPrintings`**

Add to `CardServiceImplTest.java` (new imports: `CardPrintingProjection` from `be.technifutur.grandtourbend.repositories`, `CardPrintingResponse` from `be.technifutur.grandtourbend.models.card.responses`):

```java
    @Test
    void getPrintings_delegatesToFindPrintingsWithAllFilters() {
        CardPrintingProjection projection = new CardPrintingProjection() {
            public UUID getCardId() { return UUID.fromString("00000000-0000-0000-0000-000000000001"); }
            public UUID getVariantId() { return null; }
            public String getName() { return "Son Goku"; }
            public String getBackName() { return null; }
            public String getCardType() { return "LEADER"; }
            public String getColor() { return "Red"; }
            public String getCardNumber() { return "BT18-030"; }
            public String getSeries() { return "BT18"; }
            public String getRarity() { return "Common[C]"; }
            public String getImgLink() { return "BT18-030"; }
        };
        when(cardRepository.findPrintings(eq("LEADER"), eq("Goku"), eq("Red"), eq("BT18"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(projection)));

        Page<CardPrintingResponse> result = cardService.getPrintings("LEADER", "Goku", "Red", "BT18", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().cardNumber()).isEqualTo("BT18-030");
        assertThat(result.getContent().getFirst().variantId()).isNull();
        verify(cardRepository).findPrintings("LEADER", "Goku", "Red", "BT18", pageable);
    }
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./mvnw -q -pl bll -am test -Dtest=CardServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `FAIL` — compile error, `getPrintings` not defined on `CardService`/`CardServiceImpl`.

- [ ] **Step 4: Add `getPrintings` to the `CardService` interface**

```java
package be.technifutur.grandtourbend;

import be.technifutur.grandtourbend.models.card.responses.CardDetailResponse;
import be.technifutur.grandtourbend.models.card.responses.CardPrintingResponse;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CardService {
    Page<CardResponse> getAll(String type, String search, Pageable pageable);
    Page<CardPrintingResponse> getPrintings(String type, String search, String color, String series, Pageable pageable);
    CardDetailResponse getById(UUID id);
}
```

- [ ] **Step 5: Implement `getPrintings` in `CardServiceImpl`**

Add this method to `CardServiceImpl` (add imports `be.technifutur.grandtourbend.models.card.responses.CardPrintingResponse;` and `be.technifutur.grandtourbend.repositories.CardPrintingProjection;`):

```java
    @Override
    public Page<CardPrintingResponse> getPrintings(String type, String search, String color, String series, Pageable pageable) {
        String normalizedType = blankToNull(type);
        String normalizedSearch = blankToNull(search);
        String normalizedColor = blankToNull(color);
        String normalizedSeries = blankToNull(series);
        return cardRepository
                .findPrintings(normalizedType, normalizedSearch, normalizedColor, normalizedSeries, pageable)
                .map(this::toPrintingResponse);
    }

    private CardPrintingResponse toPrintingResponse(CardPrintingProjection p) {
        return new CardPrintingResponse(
                p.getCardId(),
                p.getVariantId(),
                p.getName(),
                p.getBackName(),
                p.getCardType(),
                p.getColor(),
                p.getCardNumber(),
                p.getSeries(),
                p.getRarity(),
                p.getImgLink()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
```

Native `:param IS NULL OR ...` bindings need an actual SQL `NULL`, not an empty string — that's what `blankToNull` guards against (an empty-string `search=""` from a query param would otherwise never match `IS NULL` and silently return zero rows).

- [ ] **Step 6: Run test to verify it passes**

Run: `./mvnw -q -pl bll -am test -Dtest=CardServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `PASS`, all `CardServiceImplTest` tests green (check `bll/target/surefire-reports/be.technifutur.grandtourbend.services.impls.CardServiceImplTest.txt` for `Failures: 0, Errors: 0`).

- [ ] **Step 7: Add the controller endpoint**

Add to `CardController.java` (below the existing `getAll` method):

```java
    @GetMapping("/printings")
    @Operation(summary = "Lister les impressions de cartes", description = "Une ligne par impression r\u00e9elle (carte de base + chaque variante), filtrable par type, recherche de nom, couleur et s\u00e9rie.")
    @ApiResponse(responseCode = "200", description = "Page d'impressions")
    public ResponseEntity<Page<CardPrintingResponse>> getPrintings(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String series,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(cardService.getPrintings(type, search, color, series, PageRequest.of(page, size)));
    }
```

Add `import be.technifutur.grandtourbend.models.card.responses.CardPrintingResponse;` to `CardController.java`.

- [ ] **Step 8: Full backend build check**

Run: `./mvnw -q test`
Expected: no `[ERROR]` output; check `api/target/surefire-reports/*.txt` and `bll/target/surefire-reports/*.txt` all show `Failures: 0, Errors: 0`.

- [ ] **Step 9: Commit**

```bash
git add cl/src/main/java/be/technifutur/grandtourbend/models/card/responses/CardPrintingResponse.java bll/src/main/java/be/technifutur/grandtourbend/CardService.java bll/src/main/java/be/technifutur/grandtourbend/services/impls/CardServiceImpl.java bll/src/test/java/be/technifutur/grandtourbend/services/impls/CardServiceImplTest.java api/src/main/java/be/technifutur/grandtourbend/models/controller/CardController.java
git commit -m "feat: add GET /cards/printings endpoint"
```

---

## Task 6: Collection request/response DTOs

**Files:**
- Create: `cl/src/main/java/be/technifutur/grandtourbend/models/collection/requests/CollectionItemRequest.java`
- Create: `cl/src/main/java/be/technifutur/grandtourbend/models/collection/requests/CollectionRequest.java`
- Create: `cl/src/main/java/be/technifutur/grandtourbend/models/collection/responses/CollectionItemResponse.java`
- Create: `cl/src/main/java/be/technifutur/grandtourbend/models/collection/responses/CollectionResponse.java`
- Create: `cl/src/main/java/be/technifutur/grandtourbend/models/collection/responses/CollectionSummaryResponse.java`

No test — plain DTOs, same convention as `ResultsRequest`/`ResultsResponse` (those aren't unit-tested either; they're exercised through `ResultsServiceImplTest`).

- [ ] **Step 1: Create `CollectionItemRequest`**

```java
package be.technifutur.grandtourbend.models.collection.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CollectionItemRequest(
        @NotNull UUID cardId,
        UUID variantId,
        @NotNull @Min(1) Integer quantity,
        @NotNull @DecimalMin("0") BigDecimal price
) {
}
```

- [ ] **Step 2: Create `CollectionRequest`**

```java
package be.technifutur.grandtourbend.models.collection.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CollectionRequest(
        @NotBlank String name,
        @NotNull List<@Valid CollectionItemRequest> items
) {
}
```

- [ ] **Step 3: Create `CollectionItemResponse`**

```java
package be.technifutur.grandtourbend.models.collection.responses;

import be.technifutur.grandtourbend.entities.CollectionCard;
import be.technifutur.grandtourbend.models.card.responses.CardPrintingResponse;

import java.math.BigDecimal;
import java.util.UUID;

public record CollectionItemResponse(
        UUID cardId,
        UUID variantId,
        Integer quantity,
        BigDecimal price,
        CardPrintingResponse card
) {
    public static CollectionItemResponse fromCollectionCard(CollectionCard cc) {
        var variant = cc.getVariant();
        var card = cc.getCard();
        UUID variantId = variant != null ? variant.getId() : null;
        CardPrintingResponse printing = new CardPrintingResponse(
                card.getId(),
                variantId,
                card.getName(),
                card.getBackName(),
                card.getCardType(),
                card.getColor(),
                variant != null ? variant.getCardNumber() : card.getCardNumber(),
                variant != null ? variant.getSeries() : card.getSeries(),
                variant != null ? variant.getRarity() : card.getRarity(),
                variant != null ? variant.getImgLink() : card.getImgLink()
        );
        return new CollectionItemResponse(card.getId(), variantId, cc.getQuantity(), cc.getPrice(), printing);
    }
}
```

- [ ] **Step 4: Create `CollectionResponse`**

```java
package be.technifutur.grandtourbend.models.collection.responses;

import java.util.List;
import java.util.UUID;

public record CollectionResponse(
        UUID id,
        String name,
        List<CollectionItemResponse> items
) {
}
```

- [ ] **Step 5: Create `CollectionSummaryResponse`**

```java
package be.technifutur.grandtourbend.models.collection.responses;

import java.math.BigDecimal;
import java.util.UUID;

public record CollectionSummaryResponse(
        UUID id,
        String name,
        long cardCount,
        BigDecimal totalPrice
) {
}
```

- [ ] **Step 6: Compile check**

Run: `./mvnw -q -pl cl -am compile`
Expected: `BUILD SUCCESS`, no output.

- [ ] **Step 7: Commit**

```bash
git add cl/src/main/java/be/technifutur/grandtourbend/models/collection/
git commit -m "feat: add Collection request/response DTOs"
```

---

## Task 7: `CollectionService` interface

**Files:**
- Create: `bll/src/main/java/be/technifutur/grandtourbend/CollectionService.java`

- [ ] **Step 1: Create the interface**

```java
package be.technifutur.grandtourbend;

import be.technifutur.grandtourbend.models.collection.requests.CollectionRequest;
import be.technifutur.grandtourbend.models.collection.responses.CollectionResponse;
import be.technifutur.grandtourbend.models.collection.responses.CollectionSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface CollectionService {
    List<CollectionSummaryResponse> getAll(UUID userId);
    CollectionResponse getById(UUID userId, UUID collectionId);
    CollectionResponse create(UUID userId, CollectionRequest request);
    CollectionResponse update(UUID userId, UUID collectionId, CollectionRequest request);
    void delete(UUID userId, UUID collectionId);
}
```

- [ ] **Step 2: Commit**

```bash
git add bll/src/main/java/be/technifutur/grandtourbend/CollectionService.java
git commit -m "feat: add CollectionService interface"
```

---

## Task 8: `CollectionServiceImpl` — create

**Files:**
- Create: `bll/src/main/java/be/technifutur/grandtourbend/services/impls/CollectionServiceImpl.java`
- Create: `bll/src/test/java/be/technifutur/grandtourbend/services/impls/CollectionServiceImplTest.java`

This task covers `create`; Tasks 9–10 add `update`/`delete`/`getAll`/`getById` to the same two files incrementally, TDD-style.

- [ ] **Step 1: Write the failing test for `create`**

```java
package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.CardVariant;
import be.technifutur.grandtourbend.entities.Collection;
import be.technifutur.grandtourbend.entities.CollectionCard;
import be.technifutur.grandtourbend.entities.User;
import be.technifutur.grandtourbend.exceptions.CardNotFoundException;
import be.technifutur.grandtourbend.exceptions.VariantNotOwnedByCardException;
import be.technifutur.grandtourbend.models.collection.requests.CollectionItemRequest;
import be.technifutur.grandtourbend.models.collection.requests.CollectionRequest;
import be.technifutur.grandtourbend.models.collection.responses.CollectionResponse;
import be.technifutur.grandtourbend.repositories.CardRepository;
import be.technifutur.grandtourbend.repositories.CardVariantRepository;
import be.technifutur.grandtourbend.repositories.CollectionCardRepository;
import be.technifutur.grandtourbend.repositories.CollectionRepository;
import be.technifutur.grandtourbend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceImplTest {

    @Mock private CollectionRepository collectionRepository;
    @Mock private CollectionCardRepository collectionCardRepository;
    @Mock private UserRepository userRepository;
    @Mock private CardRepository cardRepository;
    @Mock private CardVariantRepository cardVariantRepository;

    @InjectMocks
    private CollectionServiceImpl collectionService;

    private final UUID userId = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID();

    private Card card() {
        Card card = new Card();
        card.setCardNumber("BT18-030");
        card.setName("Son Goku");
        return card;
    }

    @Test
    void create_withValidItems_savesCollectionAndItems() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card()));
        when(collectionRepository.save(any(Collection.class))).thenAnswer(inv -> {
            Collection c = inv.getArgument(0);
            return c;
        });
        when(collectionCardRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(collectionCardRepository.findByCollection_Id(any())).thenReturn(List.of());

        CollectionRequest request = new CollectionRequest(
                "Ma collection",
                List.of(new CollectionItemRequest(cardId, null, 3, BigDecimal.valueOf(12.5)))
        );

        CollectionResponse response = collectionService.create(userId, request);

        assertThat(response.name()).isEqualTo("Ma collection");
        ArgumentCaptor<List<CollectionCard>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(collectionCardRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getQuantity()).isEqualTo(3);
        assertThat(captor.getValue().getFirst().getPrice()).isEqualByComparingTo("12.5");
    }

    @Test
    void create_whenCardDoesNotExist_throwsCardNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        CollectionRequest request = new CollectionRequest(
                "Ma collection",
                List.of(new CollectionItemRequest(cardId, null, 1, BigDecimal.ONE))
        );

        assertThatThrownBy(() -> collectionService.create(userId, request))
                .isInstanceOf(CardNotFoundException.class);
    }

    @Test
    void create_whenVariantDoesNotBelongToCard_throwsVariantNotOwnedByCardException() {
        UUID otherCardId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        // UuidBaseEntity.id has no public setter (only @Getter, populated via
        // @GeneratedValue in real use), so a mock is the only way to give this
        // Card a specific id in a unit test.
        Card otherCard = org.mockito.Mockito.mock(Card.class);
        when(otherCard.getId()).thenReturn(otherCardId);
        CardVariant variant = new CardVariant();
        variant.setCard(otherCard);

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card()));
        when(cardVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));

        CollectionRequest request = new CollectionRequest(
                "Ma collection",
                List.of(new CollectionItemRequest(cardId, variantId, 1, BigDecimal.ONE))
        );

        assertThatThrownBy(() -> collectionService.create(userId, request))
                .isInstanceOf(VariantNotOwnedByCardException.class);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -q -pl bll -am test -Dtest=CollectionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `FAIL` — compile error, `CollectionServiceImpl` doesn't exist yet.

- [ ] **Step 3: Implement `CollectionServiceImpl.create`**

```java
package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.CollectionService;
import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.CardVariant;
import be.technifutur.grandtourbend.entities.Collection;
import be.technifutur.grandtourbend.entities.CollectionCard;
import be.technifutur.grandtourbend.entities.User;
import be.technifutur.grandtourbend.exceptions.CardNotFoundException;
import be.technifutur.grandtourbend.exceptions.CollectionNotFoundException;
import be.technifutur.grandtourbend.exceptions.VariantNotOwnedByCardException;
import be.technifutur.grandtourbend.models.collection.requests.CollectionItemRequest;
import be.technifutur.grandtourbend.models.collection.requests.CollectionRequest;
import be.technifutur.grandtourbend.models.collection.responses.CollectionItemResponse;
import be.technifutur.grandtourbend.models.collection.responses.CollectionResponse;
import be.technifutur.grandtourbend.models.collection.responses.CollectionSummaryResponse;
import be.technifutur.grandtourbend.repositories.CardRepository;
import be.technifutur.grandtourbend.repositories.CardVariantRepository;
import be.technifutur.grandtourbend.repositories.CollectionCardRepository;
import be.technifutur.grandtourbend.repositories.CollectionRepository;
import be.technifutur.grandtourbend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionCardRepository collectionCardRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final CardVariantRepository cardVariantRepository;

    @Override
    public List<CollectionSummaryResponse> getAll(UUID userId) {
        throw new UnsupportedOperationException("implemented in Task 10");
    }

    @Override
    public CollectionResponse getById(UUID userId, UUID collectionId) {
        throw new UnsupportedOperationException("implemented in Task 10");
    }

    @Override
    @Transactional
    public CollectionResponse create(UUID userId, CollectionRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new IllegalStateException("Authenticated user " + userId + " not found")
        );

        Collection collection = new Collection(user, request.name());
        collection = collectionRepository.save(collection);

        List<CollectionCard> items = toCollectionCards(collection, request.items());
        collectionCardRepository.saveAll(items);

        return toResponse(collection);
    }

    @Override
    public CollectionResponse update(UUID userId, UUID collectionId, CollectionRequest request) {
        throw new UnsupportedOperationException("implemented in Task 9");
    }

    @Override
    public void delete(UUID userId, UUID collectionId) {
        throw new UnsupportedOperationException("implemented in Task 9");
    }

    private List<CollectionCard> toCollectionCards(Collection collection, List<CollectionItemRequest> items) {
        return items.stream().map(item -> {
            Card card = cardRepository.findById(item.cardId()).orElseThrow(() ->
                    new CardNotFoundException("Card with id " + item.cardId() + " not found")
            );
            CardVariant variant = resolveVariant(item.cardId(), item.variantId());

            CollectionCard cc = new CollectionCard();
            cc.setCollection(collection);
            cc.setCard(card);
            cc.setVariant(variant);
            cc.setQuantity(item.quantity());
            cc.setPrice(item.price());
            return cc;
        }).toList();
    }

    private CardVariant resolveVariant(UUID cardId, UUID variantId) {
        if (variantId == null) return null;
        CardVariant variant = cardVariantRepository.findById(variantId).orElseThrow(() ->
                new VariantNotOwnedByCardException("Variant " + variantId + " not found")
        );
        if (!variant.getCard().getId().equals(cardId)) {
            throw new VariantNotOwnedByCardException("Variant " + variantId + " does not belong to card " + cardId);
        }
        return variant;
    }

    private CollectionResponse toResponse(Collection collection) {
        List<CollectionItemResponse> items = collectionCardRepository.findByCollection_Id(collection.getId())
                .stream()
                .map(CollectionItemResponse::fromCollectionCard)
                .toList();
        return new CollectionResponse(collection.getId(), collection.getName(), items);
    }
}
```

`Collection` needs an `(User, String)` constructor for `new Collection(user, request.name())` to compile — `@AllArgsConstructor` from Task 1 generates `Collection(User user, String name)` (in field-declaration order), so this already works; no change needed to the entity.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw -q -pl bll -am test -Dtest=CollectionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `PASS` for the 3 tests written in Step 1. Check `bll/target/surefire-reports/be.technifutur.grandtourbend.services.impls.CollectionServiceImplTest.txt` for `Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add bll/src/main/java/be/technifutur/grandtourbend/services/impls/CollectionServiceImpl.java bll/src/test/java/be/technifutur/grandtourbend/services/impls/CollectionServiceImplTest.java
git commit -m "feat: implement CollectionServiceImpl.create"
```

---

## Task 9: `CollectionServiceImpl` — update and delete

**Files:**
- Modify: `bll/src/main/java/be/technifutur/grandtourbend/services/impls/CollectionServiceImpl.java`
- Modify: `bll/src/test/java/be/technifutur/grandtourbend/services/impls/CollectionServiceImplTest.java`

- [ ] **Step 1: Write the failing tests for `update` and `delete`**

Add to `CollectionServiceImplTest.java`:

```java
    @Test
    void update_replacesExistingItemsWithTheNewSet() {
        UUID collectionId = UUID.randomUUID();
        Collection existing = new Collection(new User(), "Old name");
        when(collectionRepository.findByIdAndUser_Id(collectionId, userId)).thenReturn(Optional.of(existing));
        when(collectionRepository.save(any(Collection.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card()));
        when(collectionCardRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(collectionCardRepository.findByCollection_Id(any())).thenReturn(List.of());

        CollectionRequest request = new CollectionRequest(
                "New name",
                List.of(new CollectionItemRequest(cardId, null, 2, BigDecimal.TEN))
        );

        CollectionResponse response = collectionService.update(userId, collectionId, request);

        assertThat(response.name()).isEqualTo("New name");
        org.mockito.Mockito.verify(collectionCardRepository).deleteAllByCollection_Id(collectionId);
        assertThat(existing.getName()).isEqualTo("New name");

        ArgumentCaptor<List<CollectionCard>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(collectionCardRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getQuantity()).isEqualTo(2);
        assertThat(captor.getValue().getFirst().getPrice()).isEqualByComparingTo("10");
    }

    @Test
    void update_whenCollectionNotOwnedByUser_throwsCollectionNotFoundException() {
        UUID collectionId = UUID.randomUUID();
        when(collectionRepository.findByIdAndUser_Id(collectionId, userId)).thenReturn(Optional.empty());

        CollectionRequest request = new CollectionRequest("Name", List.of());

        assertThatThrownBy(() -> collectionService.update(userId, collectionId, request))
                .isInstanceOf(be.technifutur.grandtourbend.exceptions.CollectionNotFoundException.class);
    }

    @Test
    void delete_removesCollectionAndItsItems() {
        UUID collectionId = UUID.randomUUID();
        Collection existing = new Collection(new User(), "Name");
        when(collectionRepository.findByIdAndUser_Id(collectionId, userId)).thenReturn(Optional.of(existing));

        collectionService.delete(userId, collectionId);

        org.mockito.Mockito.verify(collectionCardRepository).deleteAllByCollection_Id(collectionId);
        org.mockito.Mockito.verify(collectionRepository).delete(existing);
    }

    @Test
    void delete_whenCollectionNotOwnedByUser_throwsCollectionNotFoundException() {
        UUID collectionId = UUID.randomUUID();
        when(collectionRepository.findByIdAndUser_Id(collectionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.delete(userId, collectionId))
                .isInstanceOf(be.technifutur.grandtourbend.exceptions.CollectionNotFoundException.class);
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -q -pl bll -am test -Dtest=CollectionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `FAIL` — the new tests hit `UnsupportedOperationException` from the Task 8 stubs.

- [ ] **Step 3: Implement `update` and `delete`**

Replace the `update` and `delete` method bodies in `CollectionServiceImpl`:

```java
    @Override
    @Transactional
    public CollectionResponse update(UUID userId, UUID collectionId, CollectionRequest request) {
        Collection collection = findOwnedCollection(userId, collectionId);
        collection.setName(request.name());
        collection = collectionRepository.save(collection);

        collectionCardRepository.deleteAllByCollection_Id(collectionId);
        List<CollectionCard> items = toCollectionCards(collection, request.items());
        collectionCardRepository.saveAll(items);

        return toResponse(collection);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID collectionId) {
        Collection collection = findOwnedCollection(userId, collectionId);
        collectionCardRepository.deleteAllByCollection_Id(collectionId);
        collectionRepository.delete(collection);
    }

    private Collection findOwnedCollection(UUID userId, UUID collectionId) {
        return collectionRepository.findByIdAndUser_Id(collectionId, userId).orElseThrow(() ->
                new CollectionNotFoundException("Collection with id " + collectionId + " not found")
        );
    }
```

`@Transactional` on `create`/`update`/`delete` (added to `create` back in Task 8, per that task's code review): without it, each repository call (`collectionRepository.save`, `collectionCardRepository.deleteAllByCollection_Id`, `.saveAll`) runs in its own independent transaction (Spring Data JPA repositories are individually `@Transactional` per call by default), so a validation failure partway through `update` — e.g. item 3 of 5 references an invalid `variantId` — would leave a collection that's already had its old items deleted and its new name saved, but gained none of the new items: silent data loss, not just an orphan row like the `create` case. Wrapping the whole method in one transaction makes the failure roll back atomically instead.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw -q -pl bll -am test -Dtest=CollectionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `PASS`, all 7 tests green.

- [ ] **Step 5: Commit**

```bash
git add bll/src/main/java/be/technifutur/grandtourbend/services/impls/CollectionServiceImpl.java bll/src/test/java/be/technifutur/grandtourbend/services/impls/CollectionServiceImplTest.java
git commit -m "feat: implement CollectionServiceImpl.update and .delete"
```

---

## Task 10: `CollectionServiceImpl` — getAll and getById

**Files:**
- Modify: `bll/src/main/java/be/technifutur/grandtourbend/services/impls/CollectionServiceImpl.java`
- Modify: `bll/src/test/java/be/technifutur/grandtourbend/services/impls/CollectionServiceImplTest.java`

- [ ] **Step 1: Write the failing tests**

Add to `CollectionServiceImplTest.java`:

```java
    @Test
    void getAll_returnsSummariesWithCardCountAndTotalPrice() {
        Collection collection = new Collection(new User(), "Ma collection");
        when(collectionRepository.findByUser_Id(userId)).thenReturn(List.of(collection));
        when(collectionCardRepository.sumQuantityByCollection_Id(collection.getId())).thenReturn(5L);
        when(collectionCardRepository.sumTotalPriceByCollection_Id(collection.getId())).thenReturn(BigDecimal.valueOf(42.5));

        List<CollectionSummaryResponse> result = collectionService.getAll(userId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Ma collection");
        assertThat(result.getFirst().cardCount()).isEqualTo(5L);
        assertThat(result.getFirst().totalPrice()).isEqualByComparingTo("42.5");
    }

    @Test
    void getById_whenOwnedByUser_returnsFullDetail() {
        UUID collectionId = UUID.randomUUID();
        Collection collection = new Collection(new User(), "Ma collection");
        when(collectionRepository.findByIdAndUser_Id(collectionId, userId)).thenReturn(Optional.of(collection));
        when(collectionCardRepository.findByCollection_Id(collection.getId())).thenReturn(List.of());

        CollectionResponse result = collectionService.getById(userId, collectionId);

        assertThat(result.name()).isEqualTo("Ma collection");
        assertThat(result.items()).isEmpty();
    }

    @Test
    void getById_whenNotOwnedByUser_throwsCollectionNotFoundException() {
        UUID collectionId = UUID.randomUUID();
        when(collectionRepository.findByIdAndUser_Id(collectionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.getById(userId, collectionId))
                .isInstanceOf(be.technifutur.grandtourbend.exceptions.CollectionNotFoundException.class);
    }
```

Add `import be.technifutur.grandtourbend.models.collection.responses.CollectionSummaryResponse;` to the test file.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -q -pl bll -am test -Dtest=CollectionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `FAIL` — hits the `UnsupportedOperationException` stubs from Task 8.

- [ ] **Step 3: Implement `getAll` and `getById`**

Replace the `getAll` and `getById` method bodies in `CollectionServiceImpl`:

```java
    @Override
    @Transactional(readOnly = true)
    public List<CollectionSummaryResponse> getAll(UUID userId) {
        return collectionRepository.findByUser_Id(userId).stream()
                .map(c -> new CollectionSummaryResponse(
                        c.getId(),
                        c.getName(),
                        collectionCardRepository.sumQuantityByCollection_Id(c.getId()),
                        collectionCardRepository.sumTotalPriceByCollection_Id(c.getId())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionResponse getById(UUID userId, UUID collectionId) {
        Collection collection = findOwnedCollection(userId, collectionId);
        return toResponse(collection);
    }
```

`@Transactional(readOnly = true)` on both — without it, `getAll`'s loop runs `1 + 2N` independent implicit transactions (one per repository call) instead of one; wrapping the method collapses that to a single transaction and lets Hibernate skip flush/dirty-checking overhead for a read. Added per code review during implementation; the `getAll` N+1 query pattern itself (2 aggregate queries per collection) was reviewed and explicitly accepted as a follow-up rather than fixed now — unlike `create`'s per-item lookups (bounded by one request's payload size), this N grows with a user's lifetime collection count, so it's a real if currently-harmless-at-this-app's-scale gap, worth a batched `GROUP BY` query later if collection counts ever become large.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw -q -pl bll -am test -Dtest=CollectionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `PASS`, all 10 tests green.

- [ ] **Step 5: Commit**

```bash
git add bll/src/main/java/be/technifutur/grandtourbend/services/impls/CollectionServiceImpl.java bll/src/test/java/be/technifutur/grandtourbend/services/impls/CollectionServiceImplTest.java
git commit -m "feat: implement CollectionServiceImpl.getAll and .getById"
```

---

## Task 11: `CollectionController`

**Files:**
- Create: `api/src/main/java/be/technifutur/grandtourbend/models/controller/CollectionController.java`

No test — controllers in this codebase are thin pass-throughs with no logic of their own (see `ResultsController`), not unit-tested directly; correctness is covered by `CollectionServiceImplTest` plus the manual smoke test in Step 3.

- [ ] **Step 1: Create the controller**

```java
package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.CollectionService;
import be.technifutur.grandtourbend.models.collection.requests.CollectionRequest;
import be.technifutur.grandtourbend.models.collection.responses.CollectionResponse;
import be.technifutur.grandtourbend.models.collection.responses.CollectionSummaryResponse;
import be.technifutur.grandtourbend.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/collections")
@CrossOrigin("*")
@Tag(name = "Collections", description = "Collections de cartes poss\u00e9d\u00e9es par l'utilisateur connect\u00e9")
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    @Operation(summary = "Lister mes collections")
    @ApiResponse(responseCode = "200", description = "Liste des collections")
    public ResponseEntity<List<CollectionSummaryResponse>> getAll(
            @AuthenticationPrincipal JwtUtils.UserSession session
    ) {
        return ResponseEntity.ok(collectionService.getAll(session.id()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "D\u00e9tail d'une collection")
    @ApiResponse(responseCode = "200", description = "D\u00e9tail de la collection")
    @ApiResponse(responseCode = "404", description = "Collection introuvable")
    public ResponseEntity<CollectionResponse> getById(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(collectionService.getById(session.id(), id));
    }

    @PostMapping
    @Operation(summary = "Cr\u00e9er une collection")
    @ApiResponse(responseCode = "201", description = "Collection cr\u00e9\u00e9e")
    public ResponseEntity<CollectionResponse> create(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @Valid @RequestBody CollectionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(collectionService.create(session.id(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Remplacer le nom et le contenu d'une collection")
    @ApiResponse(responseCode = "200", description = "Collection mise \u00e0 jour")
    @ApiResponse(responseCode = "404", description = "Collection introuvable")
    public ResponseEntity<CollectionResponse> update(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID id,
            @Valid @RequestBody CollectionRequest request
    ) {
        return ResponseEntity.ok(collectionService.update(session.id(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une collection")
    @ApiResponse(responseCode = "204", description = "Collection supprim\u00e9e")
    @ApiResponse(responseCode = "404", description = "Collection introuvable")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID id
    ) {
        collectionService.delete(session.id(), id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Full backend build and test run**

Run: `./mvnw -q test`
Expected: no `[ERROR]` output. Check every `target/surefire-reports/*.txt` across `api`/`bll`/`dal`/`dl`/`cl` shows `Failures: 0, Errors: 0`.

- [ ] **Step 3: Manual smoke test against the running app**

Start the app (`./mvnw -pl api spring-boot:run`, or however it's normally run locally), then, authenticated as an existing user (reuse a JWT from a normal login), exercise the new endpoints:

```bash
curl -s "http://localhost:8080/cards/printings?type=LEADER&search=goku&size=5" | jq .
curl -s -X POST "http://localhost:8080/collections" -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"name\":\"Test\",\"items\":[]}" | jq .
```
Expected: the printings call returns a paginated JSON page of Goku leader printings; the collections call returns `{"id": "...", "name": "Test", "items": []}`. Stop the app afterward.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/be/technifutur/grandtourbend/models/controller/CollectionController.java
git commit -m "feat: add CollectionController"
```

---

## Self-Review Notes

- **Spec coverage:** every backend item from the design spec is covered — printings browse endpoint (Tasks 4–5), Collection CRUD with full-replace update semantics (Tasks 6–11), `cardCount`/`totalPrice` aggregation (Task 10), card/variant validation (Task 8).
- **Type consistency:** `CollectionCard.quantity`/`.price`, `CollectionItemRequest.quantity`/`.price`, and the aggregation queries all agree on `Integer quantity` / `BigDecimal price`. `CardPrintingResponse`'s field order/names are identical between `CardServiceImpl.toPrintingResponse` (Task 5) and the hand-built construction in `CollectionItemResponse.fromCollectionCard` (Task 6) — both build the same 10-field record positionally, so a mismatch in one would need to be checked against the other if either changes.
