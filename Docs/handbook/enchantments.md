# Handbook enchantment coverage

The handbook contains 85 source-confirmed non-vanilla enchantment entries in both Russian and English. Every enchanted-book tooltip visits every stored enchantment and uses its actual level. The existing native enchantment lines remain. Equipment with the retained, disabled Excavation state also receives an explanation and a handbook route.

This is documentation coverage, not a second gameplay registry. Original identities remain in `Catalog/registries`; JEM modifications remain in `Catalog/changes` and `Catalog/effective`. The machine-readable accounting file is [enchantments-coverage.json](enchantments-coverage.json).

## Ownership and interfaces

- `Mods/JEMAchievementGuide/src/main/resources/assets/jem_advancements/handbook/enchantments.json` owns the new bilingual explanatory prose and source references.
- `EnchantmentHelp.register()` registers client tooltip and resource-reload listeners. It contains no Patchouli references.
- `EnchantmentHelp.entryFor(ItemStack)` returns the first matching entry for the existing mapped handbook help action. Native enchantment/additional HideFlags suppress both explanations and the item-specific help route. Each enchantment remains individually searchable by Russian name, English name and exact registry ID.
- `EnchantmentPageProcessor` is instantiated by Patchouli's client template. Both the book and tooltip read the same explanatory resource. Localized entries contain names, aliases and page selectors, rather than independent copies of mechanics.
- Native `getMaxLevel()`, `isCompatibleWith()`, `isTreasureOnly()`, `isAllowedOnBooks()`, `isDiscoverable()` and `isTradeable()` supply current metadata. Compatibility is cached by registry identity and invalidated on resource reload; no recipe or item registry scan runs on hover.
- Create's verified `BacktankUtil.maxAir(int)` supplies the current synchronized backtank capacity. Its optional reflective client adapter does not load Create on a server or when the mod is absent. A failed value is visibly unavailable and logged; no default is invented.
- Long source fields are divided into bounded word-aligned book parts. Both locales receive the same number of meaningful parts. Actual composed GUI appearance remains unobserved.
- Ordinary enchanted-book tooltips show the brief effect and "Applies to" without holding a modifier. Detailed acquisition remains behind the existing Shift detail mechanism and handbook action. Additions sit under the native enchantment name when it exists; native lore and hints remain intact.
- Progression equipment reuses the owner's current-tier component and reads `ClientTierState.tier()` and `ProgressionEnchantment` eligibility. A lowest-priority listener avoids duplicating the native component; no second world-tier snapshot or eligibility rule exists in Guide.
- Seventy-two specialized enchantment entries include actual item icon/name pages. Their explicit item identities come from installed native registrations. The pages call native table eligibility and `canEnchant` separately on each destination's default stack; acquisition flags and pair conflicts remain separate. No exhaustive registry scan or per-hover item scan is performed.

No gameplay registration, damage calculation, effect, loot table, skill, cost or acquisition rule was changed by these files. In particular, the new resource does not enable books disabled by an upstream mod or grant an Epic Fight skill.

## Registration evidence

The installed registration owners were read directly, with exact artifact SHA256 values recorded per row in the coverage file:

| Owner | Confirmed enchantments | Registration evidence |
| --- | ---: | --- |
| Alex's Caves 2.0.2 | 51 | `ACEnchantmentRegistry.DEF_REG` |
| Alex's Mobs 1.22.9 | 4 | `AMEnchantmentRegistry.DEF_REG` |
| Blast From The Past 1.0.3 | 1 | `ModEnchantments.ENCHANTMENTS` |
| Critters n' Crawlers 2.2.5 | 1 | `CncModEnchantments` |
| Create 6.0.8 | 2 | `AllEnchantments` Registrate declarations |
| Epic Fight Nightfall 3.4.1 | 8 | `EFNEnchantment.ENCHANTMENTS` |
| Gouge 1.5.1 | 2 | `Gouge.ENCHANTMENTS` |
| JEM Compat 3.7.0 | 1 | `CoreEnchantments`; retained ID `jem_pack_core:excavation` |
| JEM World Boss Tiers 2.5.0 | 1 | `TierEnchantments`; current canonical source |
| Kaleidoscope End 1.0.14 | 4 | `KEEnchantments`; includes `void_shot` |
| Kaleidoscope Nether 1.1.8 | 1 | `KNEnchantments`; only `lava_walker` |
| Luminous Nether 1.3.3 | 1 | `LuminousNetherModEnchantments` |
| Nether Expansion 2.3.5 | 5 | `JNEEnchantments` |
| Trials 2.3.3 | 3 | `TrialsEnchantments` |

The historical 86 language candidates were corrected by removing `netherexp:bloodshed` and `kaleidoscope_nether:blessing` and adding registered `kaleidoscope_end:void_shot`. `BloodshedEnchantment.class` alone is not a registration. `.desc`, numbered language keys and stale names were not promoted to enchantments.

The separate quest delivery recorded 124 total enchantments. Its dump was not retained or promoted as runtime Atlas evidence. The 85-source set plus the 39 vanilla enchantments agrees numerically; this agreement is not a new runtime capture.

## Source findings represented in the player help

- Alex's Caves has `enchantments_in_loot=false` in both current pack and ordinary-server config. Its native item enchanting route remains, while book availability and trading flags are shown separately. Describing every enchantment as a librarian book would be incorrect.
- `kaleidoscope_end:void_assault` has a real special table route: a Void Conch in the lapis slot enables an extra native enchantment-tag selection. The normal discovery flag alone does not describe that route. Its combat consumer gives 15% chance of multiplying damage by `1 + level`.
- Void Shot is a genuine fourth Kaleidoscope End registration, with a projectile consumer in `EventHandler$GAME`. Its effect checks the attacker's current main-hand item at impact. Void Echo has a fixed 25% activation chance, level-dependent area damage and no automatic ally exclusion.
- The Headhunter item category is the `cnc:headhunter_enchantable` tag, whose installed members reference swords and axes. Its death handler does not roll a head-drop chance and excludes babies; it only handles its explicit supported victim types.
- Luminous Nether Soul Cleave checks exact levels I/II, with 30%/40% chance. Native table discovery is disabled while native librarian trading remains allowed.
- Progression keeps its book-and-anvil route, 6% chest addition rule, equipment protections and World Tier II/III differences. Separate pages explain projectile launch snapshots, individual armour parts, non-additive defensive stacking and void rescue at world spawn. IV/V add no further power.
- Digger uses the retained `jem_pack_core:excavation` registry identity although Compat owns its implementation. Level I keeps the existing 3×3 plane, while level II uses one 7×7 plane. The disabled integer level is stored outside the active enchantment map and returns at its original level when re-enabled. The help explains the same Shift suppression, native mining permission and successful-block wear frequency for both levels
- Current EFN `skill_parameters` disable the enchant prerequisites for ordinary Yamato summoned swords, Doppelganger, Guard, Judgement Cut End and BroadBlade Revolution. Their entries explain that applying the book adds no second permission under those native data settings. Heavy Rain and Summoned Sword End have separate real consumers.

## Observed upstream defects, left unchanged

These are installed-code observations. They are not newly repaired gameplay and have not received player acceptance:

1. `alexscaves:charting_call`: `MagicConchItem` passes zero to bounded `nextInt` for knight or mage counts at levels I–III. IV avoids those zero bounds. The help warns that lower levels cannot complete the native summon; it does not conceal this behind the native generic description.
2. `netherexp:cartridge`: Shotgun Fist, Jackhammer Fist and Pump Charge Shotgun invoke the same ammunition-consumption method in both branches. It does not provide its advertised conservation there. Shotgun Fist also replaces pellet count with random 8–12 when Cartridge is present, overriding Barrage count.
3. `netherexp:artemis`: ordinary main-hand bow velocity gains the actual level, but both shotgun consumers calculate their range bonus using integer `level / 5`. Legal I/II therefore add no shotgun range. The book preserves the working bow route and native 50% Nether Fortress book-loot addition.

`alexscaves:seapairing` also repairs differently from its broad native description: `Math.min(0, damage - 1)` clears positive wear on a successful repair update. The help describes that installed result; no repair logic was changed.

## Explicit remaining coverage limits

Six registered EFN books have no concrete survival acquisition source established by the inspected installed registrations, classes, recipes, functions and loot producers:

- `efn:yamato_summoned_sword`
- `efn:yamato_summoned_sword_end`
- `efn:yamato_doppelganger`
- `efn:yamato_judgement_cut_end`
- `efn:yamato_guard`
- `efn:broad_blade_enhance`

Their native table/trading/discovery flags and actual consumers are established. A missing confirmed producer is unknown, not proof of creative-only status or absence. No new loot, trade, recipe or unlock was added to manufacture an answer. The five disabled prerequisite gates remain documented separately from the unresolved book sources.

The two other EFN book routes are concrete: Heavy Rain's installed shaped recipe and Scythe Enhance's independent 8% Nether Fortress chest addition. The latter vanilla loot-table identity was resolved against the installed 1.20.1 mappings (`BuiltInLootTables.NETHER_BRIDGE`).

All of the new explanations are localized, but this subtask did not claim an audit of every pre-existing translation in every mod. No game/client launch, automated test, runtime registry dump, linter or static analysis was performed. Production compilation, local delivery, publication and final acceptance are handled by the parent task.
