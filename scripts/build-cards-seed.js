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
