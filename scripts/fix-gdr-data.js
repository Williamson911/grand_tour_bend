// One-off script to fix GDR (God Rare) rarity data in scripts/data/dbs_all_cards.json:
// - 3 variants mislabeled with a "_PR" card_number/img_link instead of "_GDR"
// - 2 duplicate "_PR" entries where a correct "_GDR" sibling already exists
// - 6 entirely missing GDR variants
// Also repoints every GDR variant's img_link to our own locally-hosted copy
// (api/src/main/resources/static/images/cards/), since DeckPlanet's CDN doesn't
// reliably have these. Run once, then `node scripts/build-cards-seed.js`.
const fs = require('fs');
const path = require('path');

const RAW_PATH = path.join(__dirname, 'data', 'dbs_all_cards.json');
const GDR_RARITY = 'God Rare[GDR]';

const RENAME_FROM_PR = ['BT31-151', 'BT30-150', 'BT26-138'];
const REMOVE_DUPLICATE_PR = ['BT23-140', 'BT24-138'];
const MISSING = [
  { parent: 'BT18-147', id: 900001 },
  { parent: 'BT21-147', id: 900002 },
  { parent: 'BT29-149', id: 900003 },
  { parent: 'BT30-149', id: 900004 },
  { parent: 'BT30-151', id: 900005 },
  { parent: 'BT7-131', id: 900006 },
];

const raw = JSON.parse(fs.readFileSync(RAW_PATH, 'utf-8'));
const items = raw.data;

function localImgLink(cardNumber) {
  return `/images/cards/${cardNumber}.webp`;
}

for (const num of RENAME_FROM_PR) {
  const parent = items.find((c) => c.card_number === num);
  if (!parent) throw new Error(`parent not found: ${num}`);
  const variant = (parent.variants || []).find(
    (v) => v.card_number === `${num}_PR` && v.card_rarity === GDR_RARITY,
  );
  if (!variant) throw new Error(`expected mislabeled _PR GDR variant not found: ${num}`);
  variant.card_number = `${num}_GDR`;
  variant.img_link = localImgLink(variant.card_number);
  console.log(`renamed ${num}_PR -> ${num}_GDR`);
}

for (const num of REMOVE_DUPLICATE_PR) {
  const parent = items.find((c) => c.card_number === num);
  if (!parent) throw new Error(`parent not found: ${num}`);
  const before = (parent.variants || []).length;
  parent.variants = (parent.variants || []).filter(
    (v) => !(v.card_number === `${num}_PR` && v.card_rarity === GDR_RARITY),
  );
  if (parent.variants.length === before) throw new Error(`expected duplicate _PR GDR variant not found: ${num}`);
  console.log(`removed duplicate ${num}_PR`);
}

for (const { parent: num, id } of MISSING) {
  const parent = items.find((c) => c.card_number === num);
  if (!parent) throw new Error(`parent not found: ${num}`);
  const cardNumber = `${num}_GDR`;
  parent.variants = parent.variants || [];
  if (parent.variants.some((v) => v.card_number === cardNumber)) {
    throw new Error(`variant already exists, not actually missing: ${cardNumber}`);
  }
  parent.variants.push({
    id,
    variant_of: parent.id,
    card_number: cardNumber,
    card_series: parent.card_series,
    card_rarity: GDR_RARITY,
    img_link: localImgLink(cardNumber),
    finishes: [],
    is_banned: false,
    is_limited: false,
    has_errata: false,
    limited_to: null,
    view_count: null,
  });
  console.log(`added missing ${cardNumber}`);
}

// Repoint every GDR variant's img_link to local storage, whether it was
// already correctly named or just fixed above.
let repointed = 0;
for (const c of items) {
  for (const v of c.variants || []) {
    if (v.card_rarity === GDR_RARITY) {
      const expected = localImgLink(v.card_number);
      if (v.img_link !== expected) {
        v.img_link = expected;
        repointed++;
      }
    }
  }
}
console.log(`repointed ${repointed} additional GDR img_link(s) to local storage`);

fs.writeFileSync(RAW_PATH, JSON.stringify(raw));
console.log('wrote', RAW_PATH);
