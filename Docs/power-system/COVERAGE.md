# Coverage

The generated source of truth is `Catalog/views/power/coverage.json`. At the current snapshot it accounts for 2,775 combat candidates:

| View | Identified |
|---|---:|
| Weapons/offensive item candidates | 379 |
| Armor/defense candidates | 261 |
| Accessory candidates | 218 |
| Enchantments | 218 |
| Iron's Spells spell IDs | 106 |
| Boss inventory | 61 |
| Other entity candidates | 1,468 |
| Ally/troop/summon/mount candidates | 106 |

The boss inventory contains 56 canonical JEM Pack Core profiles across Legendary Monsters, Cataclysm, Bosses'Rise (`block_factorys_bosses`), Luminous Beasts, Luminous Nether, Aquamirae and Alex's Caves, plus five native boss-tag/vanilla candidates not present in those JEM profiles: Wither, Ender Dragon, Sacred Pontiff, Dead King and Fire Boss.

The exact partial/unresolved totals are generated in `Catalog/views/power/coverage.json`. Zero is intentionally marked fully resolved: static evidence cannot prove every runtime stack attribute, native attack channel, target mitigation and acquisition context at once.

## What is source-resolved

- Current registered boss profile roster and shared JEM World Tier/hosted formulas.
- Ordinary-hostile World Tier multipliers and exclusions as recorded in Atlas changes.
- JEM progression weapon/armor rules.
- Iron's Spells registry roster and the JEM spell event order.
- Exact installed-bytecode formulas for the four priority beam/ray candidates.
- Existing Recruits/Workers ownership boundary: their native combat stays native; JEM settlement logic owns roster/payroll/morale, not reconstructed combat stats.

## What remains partial or unresolved

- Exact runtime weapon attributes, NBT variants and Epic Fight/Nightfall moveset timings for every item.
- Native formulas for the other Iron's Spells entries.
- Every boss projectile, explosion, beam, summon and custom damage source.
- Runtime hostility, tameability, AI uptime and attributes for entity-name candidates.
- Per-spell randomized loot probability and the actual observed beam stack's spell ID/level.
- Runtime combat validation. This task ran no Minecraft launch or gameplay scenario.

Heuristic categories are labelled `candidate`; they are accounting denominators, not claims that every listed entity is hostile or every named ring is combat-relevant. Newly introduced content remains visible as unresolved rather than disappearing from coverage.
