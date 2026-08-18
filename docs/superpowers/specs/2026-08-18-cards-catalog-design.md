# Cards Catalog — Design

## Context

Grand Tour tracks DBS TCG tournament results. Players currently enter their `leaderPlayed` as a free-text `String` on `Results`. This spec adds a `Cards` catalog (imported from the DeckPlanet API, which mirrors the official dbs-cardgame.com card list) and links `Results.leaderPlayed` to it via a real relation.

Out of scope for this spec (future, separate spec): user card collections and market price integration (e.g. Cardmarket).

## Data source

`https://api.deckplanet.net/cardsearch/dbs_masters_cards?limit=100000` — public JSON API, discovered via a downloaded `cgs.json` config (Dragon Ball Super plugin config for the CGS deck-building app). Site scraping of dbs-cardgame.com itself was blocked by a local network filter, so this API is used instead.

Raw dataset stats (fetched 2026-08-18): 6491 top-level cards, 2649 of which have variants, 3323 total variant entries, 93 distinct series.

Raw cache lives at `scripts/data/dbs_all_cards.json` (gitignored — `scripts/data/` is never committed).

## Data model

### `Card` (table `cards`, extends `UuidBaseEntity`)

Identity + rules text, built from the top-level object of each source record.

| Field | Type | Notes |
|---|---|---|
| `sourceId` | Integer, unique | DeckPlanet numeric `id`, for idempotent re-import |
| `cardNumber` | String, unique, not null | e.g. `BT18-030` |
| `name` | String, not null | |
| `cardType` | String, not null | `LEADER`, `BATTLE`, `EXTRA`, `UNISON`, `Z-LEADER`, `Z-BATTLE`, `Z-EXTRA`, `Z-UNISON`, `TOKEN` — stored as free String (not a Java enum) so new values from Bandai don't break import |
| `color` | String | e.g. `"Red/Blue"` |
| `energyCost`, `zEnergyCost`, `power`, `comboCost`, `comboPower` | Integer, nullable | source `"-"` / empty → `null` |
| `skill` | Text | unstyled skill text (`card_skill_unstyled`), no HTML |
| `characters`, `traits`, `era`, `keywords` | `List<String>`, JSONB | same pattern as `Results.matches` (`@JdbcTypeCode(SqlTypes.JSON)`) |
| `rarity`, `series`, `imgLink` | String | `imgLink` is the token substituted into the DeckPlanet image URL template client-side, not a full URL |
| `isHorizontal`, `isBanned`, `isLimited`, `hasErrata` | boolean | |
| `limitedTo` | Integer | deck copy limit |
| `viewCount` | Integer | |
| `backName`, `backSkill`, `backPower` | String/Text/Integer, nullable | Leader card flip side (`card_back_*`) |
| `variants` | `@OneToMany(mappedBy = "card")` → `CardVariant` | |

Explicitly excluded: styled HTML skill (`card_skill`), `erratas` full text (3% of cards; `hasErrata` boolean is enough), `status`/`sort` (constant/always-null across the full dataset).

### `CardVariant` (table `card_variants`, extends `UuidBaseEntity`)

Printing-specific data only — rules text lives on the parent `Card`, not duplicated here (variants rarely diverge in name/type/color; when skill text does diverge, e.g. errata reprints, that nuance is not modeled — the canonical `Card` is what leader/deck features reference).

| Field | Type | Notes |
|---|---|---|
| `card` | `@ManyToOne`, not null, `FetchType.LAZY` | |
| `sourceId` | Integer | variant's own DeckPlanet `id` |
| `cardNumber`, `series`, `rarity`, `imgLink` | String | |
| `finishes` | `List<String>`, JSONB, nullable | rare, e.g. `"Winner Version"` |
| `isBanned`, `isLimited`, `hasErrata` | boolean | |
| `limitedTo`, `viewCount` | Integer | |

## Import

1. Dev-only Node script `scripts/build-cards-seed.js` reads the raw cache `scripts/data/dbs_all_cards.json` and writes a trimmed, committed seed file `dal/src/main/resources/seed/dbs_cards.json` containing only the fields above, shaped as `[{ card: {...}, variants: [{...}] }, ...]`.
2. `CardsInitializer implements CommandLineRunner` (`dal/seeds/`), same pattern as the existing `Initializer`: if `cardRepository.count() == 0`, reads the seed file from the classpath via Jackson, maps to `Card`/`CardVariant` entities, and `saveAll`s in batches (500 at a time) to avoid one huge Hibernate flush for ~6491 + 3323 rows.
3. `CardRepository`, `CardVariantRepository` — standard Spring Data JPA interfaces (dal).

## API

- `CardService` / `CardServiceImpl` (bll):
  - `getAll(String type, String search)` — optional filter on `cardType` (e.g. `type=LEADER` for a leader picker) and case-insensitive name search
  - `getById(UUID id)` — includes variants
- `CardController` (api), same shape as `EventTypeController`: `GET /cards`, `GET /cards/{id}`
- DTOs (cl): `CardResponse` (list, no variants), `CardDetailResponse` (single card, with variants). No `CardRequest` — catalog is read-only, populated only by the seed.

## `Results` integration

- `Results.leaderPlayed` (String) → replaced by `leaderCard` (`@ManyToOne` → `Card`, `nullable = false`, `FetchType.LAZY`, `cascade = CascadeType.MERGE`), matching the existing `user`/`event` relations on `Results`.
- `ResultsRequest`: `leaderPlayed` (String) → `leaderCardId` (UUID).
- `ResultsResponse`: `leaderPlayed` (String) → `leaderCard` (nested `CardResponse`).
- `ResultsServiceImpl`:
  - resolves `Card` via `CardRepository.findById(leaderCardId)`, throws `CardNotFoundException` (new, same pattern as `EventNotFoundException` etc.) if absent
  - validates `card.getCardType()` is `LEADER` or `Z-LEADER`, throws `InvalidLeaderCardTypeException` (new) otherwise
- `MatchResult.opponentLeader` (nested inside `Results.matches` JSONB) stays a free String — out of scope.

## Testing

- `CardsInitializer`: seed runs once, is idempotent (guarded by `count() == 0`), row counts match the seed file.
- `CardService`: filter by type, name search (case-insensitive, partial match).
- `ResultsServiceImpl`: creating/updating a `Results` with a non-existent `leaderCardId` → `CardNotFoundException`; with a non-LEADER card type → `InvalidLeaderCardTypeException`; with a valid LEADER/Z-LEADER card → succeeds and `leaderCard` is populated in the response.
