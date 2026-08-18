# Cards Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a read-only `Cards` catalog (Card + CardVariant), seeded at startup from a trimmed DeckPlanet dataset, and replace `Results.leaderPlayed` (free String) with a real `leaderCard` relation to it.

**Architecture:** Standard N-tier flow already used by every other entity in this repo: `dl` (JPA entities) → `cl` (request/response DTOs) → `dal` (Spring Data repositories + startup seeder) → `bll` (service + validation + exceptions) → `api`/`il` (controller + security rule). Card list fields (`characters`, `traits`, `era`, `keywords`, `finishes`) are stored as JSONB via `@JdbcTypeCode(SqlTypes.JSON)`, the same pattern already used by `Results.matches`.

**Tech Stack:** Spring Boot 4.1 (Java 25), Spring Data JPA, PostgreSQL (local, `jdbc:postgresql://localhost:5432/grand_tour_bend`), Lombok, Jackson, JUnit 5 + Mockito + AssertJ (already on the classpath via `spring-boot-starter-test`, inherited by every module from the root `pom.xml`).

Full design rationale: `docs/superpowers/specs/2026-08-18-cards-catalog-design.md`.

---

## Reference: seed JSON shape

Task 6 generates `dal/src/main/resources/seed/dbs_cards.json`, an array of entries shaped exactly like this (matches the Jackson records in Task 7):

```json
[
  {
    "card": {
      "sourceId": 998,
      "cardNumber": "BT18-030",
      "name": "Son Goku",
      "cardType": "LEADER",
      "color": "Blue",
      "energyCost": null,
      "zEnergyCost": null,
      "power": 10000,
      "comboCost": null,
      "comboPower": null,
      "skill": "[Permanent] You can't include black Battle Cards in your deck...",
      "characters": ["Son Goku"],
      "traits": ["Saiyan", "Another World Budokai"],
      "era": ["Another World Budokai Saga"],
      "keywords": ["Permanent", "Activate: Main", "Once per turn", "Awaken"],
      "rarity": "Uncommon[UC]",
      "series": "BT18",
      "imgLink": "BT18-030",
      "isHorizontal": false,
      "isBanned": false,
      "isLimited": false,
      "hasErrata": false,
      "limitedTo": 4,
      "viewCount": 42966,
      "backName": "Son Goku, Another World Fighter",
      "backSkill": "[Auto] Discard 1 card from your hand...",
      "backPower": 15000
    },
    "variants": [
      {
        "sourceId": 9138,
        "cardNumber": "BT12-020_PR",
        "series": "BT12",
        "rarity": "Uncommon[UC]",
        "imgLink": "BT12-020_PR",
        "finishes": [],
        "isBanned": false,
        "isLimited": false,
        "hasErrata": false,
        "limitedTo": 4,
        "viewCount": 42038
      }
    ]
  }
]
```

---

### Task 1: `Card` and `CardVariant` entities

**Files:**
- Create: `dl/src/main/java/be/technifutur/grandtourbend/entities/Card.java`
- Create: `dl/src/main/java/be/technifutur/grandtourbend/entities/CardVariant.java`

- [ ] **Step 1: Create `Card.java`**

```java
package be.technifutur.grandtourbend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cards")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Card extends UuidBaseEntity {

    @Column(nullable = false, unique = true)
    @Getter @Setter
    private Integer sourceId;

    @Column(nullable = false, unique = true)
    @Getter @Setter
    private String cardNumber;

    @Column(nullable = false)
    @Getter @Setter
    private String name;

    @Column(nullable = false)
    @Getter @Setter
    private String cardType;

    @Column
    @Getter @Setter
    private String color;

    @Column
    @Getter @Setter
    private String energyCost;

    @Column
    @Getter @Setter
    private Integer zEnergyCost;

    @Column
    @Getter @Setter
    private Integer power;

    @Column
    @Getter @Setter
    private Integer comboCost;

    @Column
    @Getter @Setter
    private Integer comboPower;

    @Column(columnDefinition = "text")
    @Getter @Setter
    private String skill;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Getter @Setter
    private List<String> characters = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Getter @Setter
    private List<String> traits = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Getter @Setter
    private List<String> era = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Getter @Setter
    private List<String> keywords = new ArrayList<>();

    @Column(nullable = false)
    @Getter @Setter
    private String rarity;

    @Column(nullable = false)
    @Getter @Setter
    private String series;

    @Column
    @Getter @Setter
    private String imgLink;

    @Column(nullable = false)
    @Getter @Setter
    private boolean isHorizontal;

    @Column(nullable = false)
    @Getter @Setter
    private boolean isBanned;

    @Column(nullable = false)
    @Getter @Setter
    private boolean isLimited;

    @Column(nullable = false)
    @Getter @Setter
    private boolean hasErrata;

    @Column
    @Getter @Setter
    private Integer limitedTo;

    @Column
    @Getter @Setter
    private Integer viewCount;

    @Column
    @Getter @Setter
    private String backName;

    @Column(columnDefinition = "text")
    @Getter @Setter
    private String backSkill;

    @Column
    @Getter @Setter
    private Integer backPower;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter @Setter
    private List<CardVariant> variants = new ArrayList<>();
}
```

- [ ] **Step 2: Create `CardVariant.java`**

```java
package be.technifutur.grandtourbend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "card_variants")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CardVariant extends UuidBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    @Getter @Setter
    private Card card;

    @Column(nullable = false, unique = true)
    @Getter @Setter
    private Integer sourceId;

    @Column(nullable = false)
    @Getter @Setter
    private String cardNumber;

    @Column(nullable = false)
    @Getter @Setter
    private String series;

    @Column(nullable = false)
    @Getter @Setter
    private String rarity;

    @Column
    @Getter @Setter
    private String imgLink;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Getter @Setter
    private List<String> finishes = new ArrayList<>();

    @Column(nullable = false)
    @Getter @Setter
    private boolean isBanned;

    @Column(nullable = false)
    @Getter @Setter
    private boolean isLimited;

    @Column(nullable = false)
    @Getter @Setter
    private boolean hasErrata;

    @Column
    @Getter @Setter
    private Integer limitedTo;

    @Column
    @Getter @Setter
    private Integer viewCount;
}
```

Note: `CardVariant.cardNumber` has **no** unique constraint — 18 of the 3323 variant printings in the source dataset share a `card_number` with another variant, unlike top-level cards (`Card.cardNumber` and `Card.sourceId` are both confirmed globally unique across all 6491 records).

Note: Lombok's boolean getter naming depends on the field name. `isHorizontal`, `isBanned`, `isLimited` already start with `is`, so Lombok reuses them as-is (`isHorizontal()`, `isBanned()`, `isLimited()`) and strips the `is` for setters (`setHorizontal()`, `setBanned()`, `setLimited()`). `hasErrata` does **not** start with `is`, so Lombok prepends it: getter is `isHasErrata()`, setter stays `setHasErrata()`. Task 2's DTOs and Task 7's initializer already use the correct forms below — this note is just so the pattern doesn't look like a typo.

- [ ] **Step 3: Verify it compiles**

Run: `./mvnw -pl dl -am compile -q`
Expected: no output, exit code 0 (Maven is quiet on success with `-q`).

- [ ] **Step 4: Commit**

```bash
git add dl/src/main/java/be/technifutur/grandtourbend/entities/Card.java dl/src/main/java/be/technifutur/grandtourbend/entities/CardVariant.java
git commit -m "feat: add Card and CardVariant entities"
```

---

### Task 2: Card response DTOs

**Files:**
- Create: `cl/src/main/java/be/technifutur/grandtourbend/models/card/responses/CardResponse.java`
- Create: `cl/src/main/java/be/technifutur/grandtourbend/models/card/responses/CardVariantResponse.java`
- Create: `cl/src/main/java/be/technifutur/grandtourbend/models/card/responses/CardDetailResponse.java`

- [ ] **Step 1: Create `CardResponse.java`**

```java
package be.technifutur.grandtourbend.models.card.responses;

import be.technifutur.grandtourbend.entities.Card;

import java.util.List;
import java.util.UUID;

public record CardResponse(
        UUID id,
        String cardNumber,
        String name,
        String cardType,
        String color,
        String energyCost,
        Integer zEnergyCost,
        Integer power,
        Integer comboCost,
        Integer comboPower,
        String skill,
        List<String> characters,
        List<String> traits,
        List<String> era,
        List<String> keywords,
        String rarity,
        String series,
        String imgLink,
        boolean isHorizontal,
        boolean isBanned,
        boolean isLimited,
        boolean hasErrata,
        Integer limitedTo,
        Integer viewCount,
        String backName,
        String backSkill,
        Integer backPower
) {
    public static CardResponse fromCard(Card c) {
        return new CardResponse(
                c.getId(),
                c.getCardNumber(),
                c.getName(),
                c.getCardType(),
                c.getColor(),
                c.getEnergyCost(),
                c.getZEnergyCost(),
                c.getPower(),
                c.getComboCost(),
                c.getComboPower(),
                c.getSkill(),
                c.getCharacters(),
                c.getTraits(),
                c.getEra(),
                c.getKeywords(),
                c.getRarity(),
                c.getSeries(),
                c.getImgLink(),
                c.isHorizontal(),
                c.isBanned(),
                c.isLimited(),
                c.isHasErrata(),
                c.getLimitedTo(),
                c.getViewCount(),
                c.getBackName(),
                c.getBackSkill(),
                c.getBackPower()
        );
    }
}
```

- [ ] **Step 2: Create `CardVariantResponse.java`**

```java
package be.technifutur.grandtourbend.models.card.responses;

import be.technifutur.grandtourbend.entities.CardVariant;

import java.util.List;
import java.util.UUID;

public record CardVariantResponse(
        UUID id,
        String cardNumber,
        String series,
        String rarity,
        String imgLink,
        List<String> finishes,
        boolean isBanned,
        boolean isLimited,
        boolean hasErrata,
        Integer limitedTo,
        Integer viewCount
) {
    public static CardVariantResponse fromCardVariant(CardVariant v) {
        return new CardVariantResponse(
                v.getId(),
                v.getCardNumber(),
                v.getSeries(),
                v.getRarity(),
                v.getImgLink(),
                v.getFinishes(),
                v.isBanned(),
                v.isLimited(),
                v.isHasErrata(),
                v.getLimitedTo(),
                v.getViewCount()
        );
    }
}
```

- [ ] **Step 3: Create `CardDetailResponse.java`**

```java
package be.technifutur.grandtourbend.models.card.responses;

import be.technifutur.grandtourbend.entities.Card;

import java.util.List;

public record CardDetailResponse(
        CardResponse card,
        List<CardVariantResponse> variants
) {
    public static CardDetailResponse fromCard(Card c) {
        return new CardDetailResponse(
                CardResponse.fromCard(c),
                c.getVariants().stream().map(CardVariantResponse::fromCardVariant).toList()
        );
    }
}
```

- [ ] **Step 4: Verify it compiles**

Run: `./mvnw -pl cl -am compile -q`
Expected: no output, exit code 0.

- [ ] **Step 5: Commit**

```bash
git add cl/src/main/java/be/technifutur/grandtourbend/models/card
git commit -m "feat: add Card response DTOs"
```

---

### Task 3: `CardRepository`

**Files:**
- Create: `dal/src/main/java/be/technifutur/grandtourbend/repositories/CardRepository.java`

- [ ] **Step 1: Create the repository**

```java
package be.technifutur.grandtourbend.repositories;

import be.technifutur.grandtourbend.entities.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {
    List<Card> findByCardType(String cardType);

    List<Card> findByNameContainingIgnoreCase(String name);

    List<Card> findByCardTypeAndNameContainingIgnoreCase(String cardType, String name);
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./mvnw -pl dal -am compile -q`
Expected: no output, exit code 0.

- [ ] **Step 3: Commit**

```bash
git add dal/src/main/java/be/technifutur/grandtourbend/repositories/CardRepository.java
git commit -m "feat: add CardRepository"
```

---

### Task 4: `CardNotFoundException`, `CardService`, `CardServiceImpl`

**Files:**
- Create: `bll/src/main/java/be/technifutur/grandtourbend/exceptions/CardNotFoundException.java`
- Create: `bll/src/main/java/be/technifutur/grandtourbend/CardService.java`
- Create: `bll/src/main/java/be/technifutur/grandtourbend/services/impls/CardServiceImpl.java`
- Test: `bll/src/test/java/be/technifutur/grandtourbend/services/impls/CardServiceImplTest.java`

- [ ] **Step 1: Create `CardNotFoundException.java`**

```java
package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class CardNotFoundException extends GrandTourBendException {

    public CardNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
```

- [ ] **Step 2: Create `CardService.java` interface**

```java
package be.technifutur.grandtourbend;

import be.technifutur.grandtourbend.models.card.responses.CardDetailResponse;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;

import java.util.List;
import java.util.UUID;

public interface CardService {
    List<CardResponse> getAll(String type, String search);
    CardDetailResponse getById(UUID id);
}
```

- [ ] **Step 3: Write the failing test**

```java
package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.exceptions.CardNotFoundException;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;
import be.technifutur.grandtourbend.repositories.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardServiceImpl cardService;

    private Card leaderCard() {
        Card card = new Card();
        card.setSourceId(1);
        card.setCardNumber("BT18-030");
        card.setName("Son Goku");
        card.setCardType("LEADER");
        card.setRarity("Uncommon[UC]");
        card.setSeries("BT18");
        return card;
    }

    @Test
    void getAll_withTypeFilter_delegatesToFindByCardType() {
        when(cardRepository.findByCardType("LEADER")).thenReturn(List.of(leaderCard()));

        List<CardResponse> result = cardService.getAll("LEADER", null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().cardType()).isEqualTo("LEADER");
    }

    @Test
    void getAll_withNoFilters_delegatesToFindAll() {
        when(cardRepository.findAll()).thenReturn(List.of(leaderCard()));

        List<CardResponse> result = cardService.getAll(null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void getById_whenMissing_throwsCardNotFoundException() {
        UUID id = UUID.randomUUID();
        when(cardRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getById(id))
                .isInstanceOf(CardNotFoundException.class);
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `./mvnw -pl bll -am test -Dtest=CardServiceImplTest -q`
Expected: FAIL — compile error, `CardServiceImpl` does not exist yet.

- [ ] **Step 5: Create `CardServiceImpl.java`**

```java
package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.CardService;
import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.exceptions.CardNotFoundException;
import be.technifutur.grandtourbend.models.card.responses.CardDetailResponse;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;
import be.technifutur.grandtourbend.repositories.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;

    @Override
    public List<CardResponse> getAll(String type, String search) {
        boolean hasType = type != null && !type.isBlank();
        boolean hasSearch = search != null && !search.isBlank();

        List<Card> cards;
        if (hasType && hasSearch) {
            cards = cardRepository.findByCardTypeAndNameContainingIgnoreCase(type, search);
        } else if (hasType) {
            cards = cardRepository.findByCardType(type);
        } else if (hasSearch) {
            cards = cardRepository.findByNameContainingIgnoreCase(search);
        } else {
            cards = cardRepository.findAll();
        }

        return cards.stream().map(CardResponse::fromCard).toList();
    }

    @Override
    public CardDetailResponse getById(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card with id " + id + " not found"));

        return CardDetailResponse.fromCard(card);
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./mvnw -pl bll -am test -Dtest=CardServiceImplTest -q`
Expected: PASS, `Tests run: 3, Failures: 0, Errors: 0`.

- [ ] **Step 7: Commit**

```bash
git add bll/src/main/java/be/technifutur/grandtourbend/exceptions/CardNotFoundException.java bll/src/main/java/be/technifutur/grandtourbend/CardService.java bll/src/main/java/be/technifutur/grandtourbend/services/impls/CardServiceImpl.java bll/src/test/java/be/technifutur/grandtourbend/services/impls/CardServiceImplTest.java
git commit -m "feat: add CardService with type/name filtering"
```

---

### Task 5: `CardController` and public route

**Files:**
- Create: `api/src/main/java/be/technifutur/grandtourbend/models/controller/CardController.java`
- Modify: `il/src/main/java/be/technifutur/grandtourbend/configs/SecurityConfig.java:61`

- [ ] **Step 1: Create `CardController.java`**

```java
package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.CardService;
import be.technifutur.grandtourbend.models.card.responses.CardDetailResponse;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cards")
@CrossOrigin("*")
@Tag(name = "Cards", description = "Catalogue des cartes Dragon Ball Super Card Game")
public class CardController {

    private final CardService cardService;

    @GetMapping
    @Operation(summary = "Lister les cartes", description = "Filtrable par type (ex: type=LEADER) et par recherche de nom (search=...).")
    @ApiResponse(responseCode = "200", description = "Liste des cartes")
    public ResponseEntity<List<CardResponse>> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(cardService.getAll(type, search));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une carte", description = "Inclut ses variantes (alt-arts).")
    @ApiResponse(responseCode = "200", description = "Détail de la carte")
    @ApiResponse(responseCode = "404", description = "Carte introuvable")
    public ResponseEntity<CardDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.getById(id));
    }
}
```

- [ ] **Step 2: Make `GET /cards` and `GET /cards/{id}` public**

In `il/src/main/java/be/technifutur/grandtourbend/configs/SecurityConfig.java`, change:

```java
                        .requestMatchers(HttpMethod.GET, "/event", "/event/*", "/event-type").permitAll()
```

to:

```java
                        .requestMatchers(HttpMethod.GET, "/event", "/event/*", "/event-type").permitAll()
                        .requestMatchers(HttpMethod.GET, "/cards", "/cards/*").permitAll()
```

- [ ] **Step 3: Verify it compiles**

Run: `./mvnw -pl api -am compile -q`
Expected: no output, exit code 0.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/be/technifutur/grandtourbend/models/controller/CardController.java il/src/main/java/be/technifutur/grandtourbend/configs/SecurityConfig.java
git commit -m "feat: expose GET /cards and /cards/{id}"
```

---

### Task 6: Generate the trimmed card seed data

**Files:**
- Create: `scripts/build-cards-seed.js`
- Create (generated, then committed): `dal/src/main/resources/seed/dbs_cards.json`

This is a one-off dev tool, run manually whenever the raw DeckPlanet dump needs to be re-synced. It is not part of the Maven build.

- [ ] **Step 1: Confirm the raw cache exists**

Run: `ls -la scripts/data/dbs_all_cards.json`
Expected: file exists (~17 MB). If missing, re-fetch it first:
`curl -sk "https://api.deckplanet.net/cardsearch/dbs_masters_cards?limit=100000" -o scripts/data/dbs_all_cards.json`

- [ ] **Step 2: Create `scripts/build-cards-seed.js`**

```javascript
const fs = require('fs');
const path = require('path');

const RAW_PATH = path.join(__dirname, 'data', 'dbs_all_cards.json');
const OUT_PATH = path.join(__dirname, '..', 'dal', 'src', 'main', 'resources', 'seed', 'dbs_cards.json');

function toIntOrNull(v) {
  if (v === null || v === undefined || v === '' || v === '-') return null;
  const n = Number(v);
  return Number.isNaN(n) ? null : n;
}

function toStringOrNull(v) {
  if (v === null || v === undefined || v === '' || v === '-') return null;
  return String(v);
}

function toStringArray(v) {
  return Array.isArray(v) ? v : [];
}

function mapCard(c) {
  return {
    sourceId: c.id,
    cardNumber: c.card_number,
    name: c.card_name,
    cardType: c.card_type,
    color: c.card_color,
    energyCost: toStringOrNull(c.card_energy_cost),
    zEnergyCost: toIntOrNull(c.z_energy_cost),
    power: toIntOrNull(c.card_power),
    comboCost: toIntOrNull(c.card_combo_cost),
    comboPower: toIntOrNull(c.card_combo_power),
    skill: toStringOrNull(c.card_skill_unstyled),
    characters: toStringArray(c.card_character),
    traits: toStringArray(c.card_traits),
    era: toStringArray(c.card_era),
    keywords: toStringArray(c.keywords),
    rarity: c.card_rarity,
    series: c.card_series,
    imgLink: c.img_link,
    isHorizontal: !!c.is_horizontal,
    isBanned: !!c.is_banned,
    isLimited: !!c.is_limited,
    hasErrata: !!c.has_errata,
    limitedTo: toIntOrNull(c.limited_to),
    viewCount: toIntOrNull(c.view_count),
    backName: toStringOrNull(c.card_back_name),
    backSkill: toStringOrNull(c.card_back_skill_unstyled),
    backPower: toIntOrNull(c.card_back_power),
  };
}

function mapVariant(v) {
  return {
    sourceId: v.id,
    cardNumber: v.card_number,
    series: v.card_series,
    rarity: v.card_rarity,
    imgLink: v.img_link,
    finishes: toStringArray(v.finishes),
    isBanned: !!v.is_banned,
    isLimited: !!v.is_limited,
    hasErrata: !!v.has_errata,
    limitedTo: toIntOrNull(v.limited_to),
    viewCount: toIntOrNull(v.view_count),
  };
}

const raw = JSON.parse(fs.readFileSync(RAW_PATH, 'utf-8'));
const cards = raw.data;

const seed = cards.map((c) => ({
  card: mapCard(c),
  variants: (c.variants || []).map(mapVariant),
}));

fs.mkdirSync(path.dirname(OUT_PATH), { recursive: true });
fs.writeFileSync(OUT_PATH, JSON.stringify(seed));

const variantCount = seed.reduce((sum, e) => sum + e.variants.length, 0);
console.log(`Wrote ${seed.length} cards and ${variantCount} variants to ${OUT_PATH}`);
```

- [ ] **Step 3: Run it**

Run: `node scripts/build-cards-seed.js`
Expected: `Wrote 6491 cards and 3323 variants to .../dal/src/main/resources/seed/dbs_cards.json`

- [ ] **Step 4: Sanity-check the output**

Run: `node -e "const s = require('./dal/src/main/resources/seed/dbs_cards.json'); console.log(s.length, s[0].card.cardNumber, s.filter(e => e.card.energyCost === 'X').length)"`
Expected: `6491 <some card number> <a number greater than 0>` — confirms the array length and that the `"X"` energy-cost case survived as a string instead of being dropped by numeric parsing.

- [ ] **Step 5: Commit**

`scripts/data/` is gitignored (raw cache, contains a full mirror of a third-party API — not committed), but `dal/src/main/resources/seed/dbs_cards.json` is a normal tracked resource.

```bash
git add scripts/build-cards-seed.js dal/src/main/resources/seed/dbs_cards.json
git commit -m "feat: add trimmed card seed data and generator script"
```

---

### Task 7: `CardsInitializer` (startup seed)

**Files:**
- Create: `dal/src/main/java/be/technifutur/grandtourbend/seeds/CardSeedEntry.java`
- Create: `dal/src/main/java/be/technifutur/grandtourbend/seeds/CardsInitializer.java`

- [ ] **Step 1: Create `CardSeedEntry.java`**

```java
package be.technifutur.grandtourbend.seeds;

import java.util.List;

public record CardSeedEntry(CardSeedData card, List<CardVariantSeedData> variants) {

    public record CardSeedData(
            Integer sourceId,
            String cardNumber,
            String name,
            String cardType,
            String color,
            String energyCost,
            Integer zEnergyCost,
            Integer power,
            Integer comboCost,
            Integer comboPower,
            String skill,
            List<String> characters,
            List<String> traits,
            List<String> era,
            List<String> keywords,
            String rarity,
            String series,
            String imgLink,
            boolean isHorizontal,
            boolean isBanned,
            boolean isLimited,
            boolean hasErrata,
            Integer limitedTo,
            Integer viewCount,
            String backName,
            String backSkill,
            Integer backPower
    ) {}

    public record CardVariantSeedData(
            Integer sourceId,
            String cardNumber,
            String series,
            String rarity,
            String imgLink,
            List<String> finishes,
            boolean isBanned,
            boolean isLimited,
            boolean hasErrata,
            Integer limitedTo,
            Integer viewCount
    ) {}
}
```

- [ ] **Step 2: Create `CardsInitializer.java`**

```java
package be.technifutur.grandtourbend.seeds;

import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.CardVariant;
import be.technifutur.grandtourbend.repositories.CardRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CardsInitializer implements CommandLineRunner {

    private final CardRepository cardRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        if (cardRepository.count() > 0) {
            return;
        }

        List<CardSeedEntry> entries;
        try (InputStream in = new ClassPathResource("seed/dbs_cards.json").getInputStream()) {
            entries = objectMapper.readValue(
                    in,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CardSeedEntry.class)
            );
        }

        List<Card> cards = entries.stream().map(this::toCard).toList();
        cardRepository.saveAll(cards);
    }

    private Card toCard(CardSeedEntry entry) {
        CardSeedEntry.CardSeedData data = entry.card();

        Card card = new Card();
        card.setSourceId(data.sourceId());
        card.setCardNumber(data.cardNumber());
        card.setName(data.name());
        card.setCardType(data.cardType());
        card.setColor(data.color());
        card.setEnergyCost(data.energyCost());
        card.setZEnergyCost(data.zEnergyCost());
        card.setPower(data.power());
        card.setComboCost(data.comboCost());
        card.setComboPower(data.comboPower());
        card.setSkill(data.skill());
        card.setCharacters(data.characters());
        card.setTraits(data.traits());
        card.setEra(data.era());
        card.setKeywords(data.keywords());
        card.setRarity(data.rarity());
        card.setSeries(data.series());
        card.setImgLink(data.imgLink());
        card.setHorizontal(data.isHorizontal());
        card.setBanned(data.isBanned());
        card.setLimited(data.isLimited());
        card.setHasErrata(data.hasErrata());
        card.setLimitedTo(data.limitedTo());
        card.setViewCount(data.viewCount());
        card.setBackName(data.backName());
        card.setBackSkill(data.backSkill());
        card.setBackPower(data.backPower());

        for (CardSeedEntry.CardVariantSeedData v : entry.variants()) {
            CardVariant variant = new CardVariant();
            variant.setCard(card);
            variant.setSourceId(v.sourceId());
            variant.setCardNumber(v.cardNumber());
            variant.setSeries(v.series());
            variant.setRarity(v.rarity());
            variant.setImgLink(v.imgLink());
            variant.setFinishes(v.finishes());
            variant.setBanned(v.isBanned());
            variant.setLimited(v.isLimited());
            variant.setHasErrata(v.hasErrata());
            variant.setLimitedTo(v.limitedTo());
            variant.setViewCount(v.viewCount());
            card.getVariants().add(variant);
        }

        return card;
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./mvnw -pl dal -am compile -q`
Expected: no output, exit code 0.

- [ ] **Step 4: Commit**

```bash
git add dal/src/main/java/be/technifutur/grandtourbend/seeds/CardSeedEntry.java dal/src/main/java/be/technifutur/grandtourbend/seeds/CardsInitializer.java
git commit -m "feat: seed the cards catalog on first startup"
```

- [ ] **Step 5: Manual smoke test (requires a local Postgres at `localhost:5432/grand_tour_bend`, matching `api/src/main/resources/application.yaml`)**

Run: `./mvnw -pl api -am spring-boot:run` (leave it running; `ddl-auto: update` creates the `cards`/`card_variants` tables automatically). Wait for `Started GrandTourBendApplication`, then in another terminal:

Run: `curl -s "http://localhost:8080/cards?type=LEADER" | head -c 500`
Expected: a JSON array of card objects with `"cardType":"LEADER"`.

Run: `curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:8080/cards"`
Expected: `200`

Stop the app (Ctrl+C) once confirmed. If no local Postgres is available in this environment, skip this step and flag it for manual verification later — the compile/unit-test steps above already validate the code.

---

### Task 8: `InvalidLeaderCardTypeException` and `Results.leaderCard`

**Files:**
- Create: `bll/src/main/java/be/technifutur/grandtourbend/exceptions/InvalidLeaderCardTypeException.java`
- Modify: `dl/src/main/java/be/technifutur/grandtourbend/entities/Results.java`

- [ ] **Step 1: Create `InvalidLeaderCardTypeException.java`**

```java
package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidLeaderCardTypeException extends GrandTourBendException {

    public InvalidLeaderCardTypeException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
```

- [ ] **Step 2: Replace `leaderPlayed` with `leaderCard` on `Results`**

In `dl/src/main/java/be/technifutur/grandtourbend/entities/Results.java`, replace:

```java
    @Column(nullable = false)
    @Getter
    @Setter
    private String leaderPlayed;
```

with:

```java
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "leader_card_id", nullable = false)
    @Getter
    @Setter
    private Card leaderCard;
```

(`jakarta.persistence.*` is already wildcard-imported at the top of `Results.java`, so no new import is needed; `Card` is in the same `entities` package.)

- [ ] **Step 3: Verify `dl` compiles on its own**

Run: `./mvnw -pl dl -am compile -q`
Expected: no output, exit code 0 — nothing inside `dl` itself references `leaderPlayed`, so this step passes even though downstream modules are now broken.

- [ ] **Step 4: Confirm the expected downstream breakage**

Run: `./mvnw -pl cl -am compile -q`
Expected: **fails** — `ResultsRequest.java`/`ResultsResponse.java` (in `cl`) still call `results.setLeaderPlayed(...)`/`r.getLeaderPlayed()`, which no longer exist on `Results`. Confirm the compiler error specifically mentions `leaderPlayed`/`setLeaderPlayed`/`getLeaderPlayed`. This is expected and gets fixed in Task 9 — don't fix it here.

- [ ] **Step 5: Commit**

```bash
git add bll/src/main/java/be/technifutur/grandtourbend/exceptions/InvalidLeaderCardTypeException.java dl/src/main/java/be/technifutur/grandtourbend/entities/Results.java
git commit -m "feat: replace Results.leaderPlayed with a Card relation"
```

---

### Task 9: Wire the leader card through `Results`

**Files:**
- Modify: `cl/src/main/java/be/technifutur/grandtourbend/models/results/requests/ResultsRequest.java`
- Modify: `cl/src/main/java/be/technifutur/grandtourbend/models/results/responses/ResultsResponse.java`
- Modify: `bll/src/main/java/be/technifutur/grandtourbend/services/impls/ResultsServiceImpl.java`
- Test: `bll/src/test/java/be/technifutur/grandtourbend/services/impls/ResultsServiceImplTest.java`

- [ ] **Step 1: Rewrite `ResultsRequest.java`**

```java
package be.technifutur.grandtourbend.models.results.requests;

import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.entities.MatchResult;
import be.technifutur.grandtourbend.entities.Results;
import be.technifutur.grandtourbend.entities.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ResultsRequest(
        @NotBlank String deckName,
        @NotNull UUID leaderCardId,
        @NotNull Integer placement,
        @NotNull Integer totalPlayers,
        BigDecimal prizes,
        String notes,
        List<MatchResult> matches
) {
    public Results toResults(User user, Event event, Card leaderCard) {
        return new Results(user, event, deckName, leaderCard, placement, totalPlayers, prizes, notes, matches);
    }

    public void applyTo(Results results, Card leaderCard) {
        results.setDeckName(deckName);
        results.setLeaderCard(leaderCard);
        results.setPlacement(placement);
        results.setTotalPlayers(totalPlayers);
        results.setPrizes(prizes);
        results.setNotes(notes);
        results.setMatches(matches);
    }
}
```

- [ ] **Step 2: Rewrite `ResultsResponse.java`**

```java
package be.technifutur.grandtourbend.models.results.responses;

import be.technifutur.grandtourbend.entities.MatchResult;
import be.technifutur.grandtourbend.entities.Results;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ResultsResponse(
        UUID id,
        UUID userId,
        UUID eventId,
        String deckName,
        CardResponse leaderCard,
        Integer placement,
        Integer totalPlayers,
        BigDecimal prizes,
        String notes,
        List<MatchResult> matches,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ResultsResponse fromResults(Results r) {
        return new ResultsResponse(
                r.getId(),
                r.getUser().getId(),
                r.getEvent().getId(),
                r.getDeckName(),
                CardResponse.fromCard(r.getLeaderCard()),
                r.getPlacement(),
                r.getTotalPlayers(),
                r.getPrizes(),
                r.getNotes(),
                r.getMatches(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 3: Verify `dl` + `cl` compile**

Run: `./mvnw -pl cl -am compile -q`
Expected: no output, exit code 0 (this also recompiles `dl`, confirming Task 8's change is now consistent).

- [ ] **Step 4: Write the failing test for `ResultsServiceImpl`**

```java
package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.entities.User;
import be.technifutur.grandtourbend.exceptions.CardNotFoundException;
import be.technifutur.grandtourbend.exceptions.InvalidLeaderCardTypeException;
import be.technifutur.grandtourbend.models.results.requests.ResultsRequest;
import be.technifutur.grandtourbend.repositories.CardRepository;
import be.technifutur.grandtourbend.repositories.EventRepository;
import be.technifutur.grandtourbend.repositories.ResultsRepository;
import be.technifutur.grandtourbend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultsServiceImplTest {

    @Mock private ResultsRepository resultsRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private CardRepository cardRepository;

    @InjectMocks
    private ResultsServiceImpl resultsService;

    private final UUID userId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID leaderCardId = UUID.randomUUID();

    private ResultsRequest request() {
        return new ResultsRequest("My Deck", leaderCardId, 1, 10, BigDecimal.TEN, null, List.of());
    }

    @Test
    void create_whenLeaderCardIsNotALeaderType_throwsInvalidLeaderCardTypeException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(new Event()));

        Card battleCard = new Card();
        battleCard.setCardNumber("BT18-050");
        battleCard.setCardType("BATTLE");
        when(cardRepository.findById(leaderCardId)).thenReturn(Optional.of(battleCard));

        assertThatThrownBy(() -> resultsService.create(userId, eventId, request()))
                .isInstanceOf(InvalidLeaderCardTypeException.class);
    }

    @Test
    void create_whenLeaderCardDoesNotExist_throwsCardNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(new Event()));
        when(cardRepository.findById(leaderCardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultsService.create(userId, eventId, request()))
                .isInstanceOf(CardNotFoundException.class);
    }
}
```

- [ ] **Step 5: Run the test to verify it fails**

Run: `./mvnw -pl bll -am test -Dtest=ResultsServiceImplTest -q`
Expected: FAIL — compile error, `ResultsServiceImpl` has no `CardRepository` dependency yet and still calls `request.toResults(user, event)` / old 2-arg `applyTo`.

- [ ] **Step 6: Update `ResultsServiceImpl.java`**

```java
package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.ResultsService;
import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.entities.Results;
import be.technifutur.grandtourbend.entities.User;
import be.technifutur.grandtourbend.exceptions.CardNotFoundException;
import be.technifutur.grandtourbend.exceptions.EventNotFoundException;
import be.technifutur.grandtourbend.exceptions.InvalidLeaderCardTypeException;
import be.technifutur.grandtourbend.exceptions.ResultsNotFoundException;
import be.technifutur.grandtourbend.models.results.requests.ResultsRequest;
import be.technifutur.grandtourbend.models.results.responses.ResultsResponse;
import be.technifutur.grandtourbend.repositories.CardRepository;
import be.technifutur.grandtourbend.repositories.EventRepository;
import be.technifutur.grandtourbend.repositories.ResultsRepository;
import be.technifutur.grandtourbend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResultsServiceImpl implements ResultsService {

    private static final Set<String> LEADER_CARD_TYPES = Set.of("LEADER", "Z-LEADER");

    private final ResultsRepository resultsRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CardRepository cardRepository;

    @Override
    public List<ResultsResponse> getAll(UUID userId) {
        return resultsRepository.findByUser_Id(userId)
                .stream()
                .map(ResultsResponse::fromResults)
                .toList();
    }

    @Override
    public UUID create(UUID userId, UUID eventId, ResultsRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new IllegalStateException("Authenticated user " + userId + " not found")
        );

        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new EventNotFoundException("Event with id " + eventId + " not found")
        );

        Card leaderCard = cardRepository.findById(request.leaderCardId()).orElseThrow(() ->
                new CardNotFoundException("Card with id " + request.leaderCardId() + " not found")
        );

        if (!LEADER_CARD_TYPES.contains(leaderCard.getCardType())) {
            throw new InvalidLeaderCardTypeException(
                    "Card " + leaderCard.getCardNumber() + " is not a leader card (type: " + leaderCard.getCardType() + ")"
            );
        }

        Results results = resultsRepository.findByUser_IdAndEvent_Id(userId, eventId)
                .map(existing -> {
                    request.applyTo(existing, leaderCard);
                    return existing;
                })
                .orElseGet(() -> request.toResults(user, event, leaderCard));

        return resultsRepository.save(results).getId();
    }

    @Override
    public void delete(UUID userId, UUID eventId) {
        Results results = resultsRepository.findByUser_IdAndEvent_Id(userId, eventId)
                .orElseThrow(() -> new ResultsNotFoundException("No result for this event"));

        resultsRepository.delete(results);
    }
}
```

- [ ] **Step 7: Run the test to verify it passes**

Run: `./mvnw -pl bll -am test -Dtest=ResultsServiceImplTest -q`
Expected: PASS, `Tests run: 2, Failures: 0, Errors: 0`.

- [ ] **Step 8: Run the full `bll` test suite to make sure nothing else broke**

Run: `./mvnw -pl bll -am test -q`
Expected: PASS, all tests (including `CardServiceImplTest` from Task 4) green.

- [ ] **Step 9: Commit**

```bash
git add cl/src/main/java/be/technifutur/grandtourbend/models/results/requests/ResultsRequest.java cl/src/main/java/be/technifutur/grandtourbend/models/results/responses/ResultsResponse.java bll/src/main/java/be/technifutur/grandtourbend/services/impls/ResultsServiceImpl.java bll/src/test/java/be/technifutur/grandtourbend/services/impls/ResultsServiceImplTest.java
git commit -m "feat: validate and attach the leader Card on Results"
```

---

### Task 10: Full build and end-to-end smoke test

**Files:** none (verification only)

- [ ] **Step 1: Full reactor build**

Run: `./mvnw -q compile`
Expected: no output, exit code 0 — every module (`dl`, `cl`, `dal`, `bll`, `il`, `api`) compiles together.

- [ ] **Step 2: Full test suite**

Run: `./mvnw -q test`
Expected: PASS — `CardServiceImplTest` and `ResultsServiceImplTest` both green, plus the pre-existing `GrandTourBendApplicationTests`.

- [ ] **Step 3: Manual smoke test (requires local Postgres, same caveat as Task 7 Step 5)**

Run: `./mvnw -pl api -am spring-boot:run`, wait for `Started GrandTourBendApplication`. If the `cards` table was already seeded by Task 7's smoke test, this boots straight through (seed is skipped because `cardRepository.count() > 0`).

In another terminal, get a leader card id:

Run: `curl -s "http://localhost:8080/cards?type=LEADER" | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>console.log(JSON.parse(d)[0].id))"`
Expected: a UUID printed to stdout.

Log in (or register) to get a JWT via `POST /auth/login`, then create a result using that leader card id:

Run: `curl -s -X PUT "http://localhost:8080/results/<eventId>" -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"deckName":"Test Deck","leaderCardId":"<leaderCardIdFromAbove>","placement":1,"totalPlayers":8,"matches":[]}'`
Expected: `200 OK`.

Run: `curl -s "http://localhost:8080/results/me" -H "Authorization: Bearer <token>" | head -c 500`
Expected: JSON containing `"leaderCard":{"id":"<leaderCardIdFromAbove>", ... "cardType":"LEADER", ...}`.

Also confirm the rejection path — retry the same PUT with a non-leader `leaderCardId` (any card from `curl -s "http://localhost:8080/cards?type=BATTLE"`):
Expected: `400 Bad Request`.

Stop the app once confirmed. If no local Postgres is available, skip this step — Steps 1-2 already give full confidence the code is correct; note the manual check as outstanding.

---

## Spec coverage check

- Card/CardVariant data model → Task 1
- Excluded fields (`card_skill` HTML, `erratas`, `status`, `sort`) → Task 6 (never mapped into the seed)
- Seed generation from raw cache → Task 6
- Startup seeder, idempotent via `count() == 0` → Task 7
- Read-only `GET /cards`, `GET /cards/{id}` API → Tasks 2, 4, 5
- `Results.leaderPlayed` → `leaderCard` relation → Task 8
- Leader/Z-Leader type validation on assignment → Task 9
- `MatchResult.opponentLeader` untouched (explicitly out of scope) → not modified anywhere in this plan
