# Card Collections — Design

## Context

Grand Tour's card catalog (`Card`/`CardVariant`, ~6491 cards / 3323 variants, seeded from DeckPlanet — see `2026-08-18-cards-catalog-design.md`) is currently read-only, consumed only by the leader picker (`2026-08-19-leader-picker-design.md`). The cards-catalog spec explicitly deferred "user card collections and market price integration" as future, separate work. This spec covers that: letting a user build named collections of owned cards (with quantity + price), across both the backend (`grand_tour_bend`) and the frontend (`dbscg-circuit-planner`).

Out of scope: market price integration (e.g. Cardmarket) — price is a manual per-line entry, not looked up. Collection value has no relationship to the existing `Expense`/budget tracking (tournament costs vs. cards owned are unrelated concepts).

## Flow

1. `/collection` lists the user's collections as summary tiles: name, card count, total value. "+ Nouvelle collection" starts a new one.
2. The collection editor (same component for create and edit) shows: a name field, a filterable/searchable grid of every card printing in the catalog, and the current draft's item list.
3. Clicking a printing in the grid (or an existing item in the draft list) opens its detail + a quantity/price form with "Ajouter"/"Retirer" buttons.
4. Items already in the draft can also be removed directly (×) or have their quantity bumped inline, without opening the form.
5. Everything above is local draft state — nothing is persisted until "Enregistrer la collection", which does a single create/update call, then returns to `/collection` where the (new or updated) summary tile reflects it.
6. Reopening an existing collection from `/collection` loads the same editor, hydrated with its current items, for further edits.

## Data model (backend)

**`Collection`** (`collections` table, extends `UuidBaseEntity`)
- `user` — `@ManyToOne User`, not null
- `name` — String, not null

**`CollectionCard`** (`collection_cards` table, extends `UuidBaseEntity`)
- `collection` — `@ManyToOne Collection`, not null
- `card` — `@ManyToOne Card`, not null
- `variant` — `@ManyToOne CardVariant`, nullable (`null` = the base printing, not an alt-art)
- `quantity` — Integer, not null, ≥ 1
- `price` — BigDecimal, not null, ≥ 0 — **unit price for one copy** of that printing. Line total = `price × quantity`; collection total = sum across lines.

No DB-level uniqueness constraint on `(collection, card, variant)`: since saving always replaces the whole item list at once (per the local-draft flow), duplicate prevention is an editor-UI concern — clicking a printing already in the current draft reopens its existing entry (prefilled) instead of creating a second one, not something the DB needs to police.

## API (backend)

### Printings browse — new

`GET /cards` intentionally excludes variants from its (already paginated, potentially large) list response, to keep the leader-picker's search payload light. Browsing "every card and its variants" needs its own endpoint so pagination and filtering stay correct across the full ~10k-printing set (a card with variants can't be split across two separately-paginated queries and still page correctly):

`GET /cards/printings?search=&type=&color=&series=&page=&size=`
— paginated, one row per actual printing (base card + each of its variants), from a query unioning `cards` and `card_variants`. Each row:
```
{ cardId, variantId (nullable), name, backName, cardType, color, cardNumber, series, rarity, imgLink }
```
Filters: `search` (name, case-insensitive partial — same semantics as the existing `/cards` search), `type` (`cardType`), `color`, `series`. All optional, combinable.

### Collections — new `CollectionController` / `CollectionService`, same layering as the rest of the app

- `GET /collections` → `[{ id, name, cardCount, totalPrice }]` for the list page. `cardCount` = **sum of quantities** across items (physical cards owned), not row count.
- `GET /collections/{id}` → `{ id, name, items: [{ cardId, variantId, quantity, price, card: <printing summary, same shape as a /cards/printings row> }] }`. Items embed the printing's display info directly so reopening a collection to edit doesn't need per-item look-ups.
- `POST /collections` and `PUT /collections/{id}` → same body: `{ name, items: [{ cardId, variantId, quantity, price }] }`. Update does a full replace — delete the collection's existing `collection_cards`, insert the new set, one transaction.
- `DELETE /collections/{id}`

Validation on create/update: each `cardId` must exist (`CardNotFoundException`, reused from the existing cards catalog); if `variantId` is present it must belong to that `cardId` (new `VariantNotOwnedByCardException` or similar); `quantity ≥ 1`; `price ≥ 0`.

## Frontend

**Models** (`core/models/collection.ts`):
```ts
export interface CardPrinting {
  cardId: string;
  variantId: string | null;   // null = base printing
  name: string;
  backName: string | null;
  cardType: string;
  color: string | null;
  cardNumber: string;
  series: string | null;
  rarity: string | null;
  imgLink: string | null;
}
export interface CollectionItem { quantity: number; price: number; card: CardPrinting; }
export interface CollectionDraft { id: string | null; name: string; items: CollectionItem[]; }
export interface CollectionSummary { id: string; name: string; cardCount: number; totalPrice: number; }
```
Printing identity for dedup/tracking within a draft = `` `${cardId}:${variantId ?? 'base'}` ``.

> `core/models/card.ts` already has an unused `CardOption` interface + `cardDisplayName()` — added ahead of this spec, apparently as a head start on `CardPrinting`, but missing `variantId`/`backName`/`color`/`series`/`rarity`. This spec's `CardPrinting` supersedes it; `CardOption`/`cardDisplayName` should be reconciled into it or deleted when this is implemented, not left dangling.

**Services:**
- `core/services/cards.ts` — add `searchPrintings(params: { search?, type?, color?, series?, page, size })`, thin wrapper on `GET /cards/printings`, same shape as the existing `searchLeaders`.
- `core/services/collections.ts` (new) — `list()`, `getById(id)`, `create(draft)`, `update(id, draft)`, `remove(id)`. Write methods return an ok/error union, same pattern as `EventService.createEvent`/`updateEvent`.

**Components:**
- `features/collection-list/` — summary tiles (name, cardCount, totalPrice) + "+ Nouvelle collection" → `/collection/new`; clicking a tile → `/collection/:id`.
- `features/collection-editor/` — handles both create and edit (draft signal starts empty vs. hydrated via `getById`):
  - name input, bound to `draft.name`
  - `shared/components/card-grid/` — debounced search + type/color/series filters, paginated, calls `searchPrintings`; emits `printingSelected`
  - "My collection" list — draft items as rows: inline quantity stepper + × remove, no form needed for either
  - `shared/components/card-detail-panel/` — opens on a grid click or on clicking an existing draft row (prefilled with its current quantity/price); shows card detail + quantity/price form + Ajouter/Retirer; emits `added { quantity, price }` / `removed`, editor mutates `draft.items` (upsert-by-key on add, filter-out-by-key on remove)
  - "Enregistrer" → `create()` if `draft.id` is null else `update()`, then navigate to `/collection`

**Data flow:** a single `draft` signal owned by `collection-editor`, mutated locally by grid/panel/list interactions. Nothing touches the backend except the read-only `searchPrintings` calls until "Enregistrer" — matches the local-draft decision exactly. Create and edit are the same component; only the draft's starting value differs.

**Error handling:** save failure → inline error on the editor, draft stays intact (nothing lost), same pattern as `EventWriteResult`'s error surfacing already used for event create/update. A card/variant vanishing between browse and save (catalog is read-only/seeded, so effectively never happens) would just surface as the same generic save error — no special handling needed.

## Testing

- Backend: `CollectionServiceImplTest` (Mockito, mirrors `ResultsServiceImplTest`) — create, update-as-full-replace, `cardCount`/`totalPrice` aggregation, validation errors (missing card, variant not belonging to card, invalid quantity/price). The `/cards/printings` UNION query is spot-checked against real seeded data via psql, same as the leader-search `backName` fix — this codebase has no repository-level DB tests anywhere, so that convention isn't being introduced here either.
- Frontend: `collections.ts` — hydrate/dehydrate-style mapping tests (wire format ↔ `CollectionDraft`), same convention as `event.ts`'s `hydrate`/`dehydrate`. `card-detail-panel` — add/remove event emission, line-total (`price × quantity`) computation. `card-grid` — debounced search calling `CardsService.searchPrintings`, same pattern already proven in `LeaderPicker`'s tests.
