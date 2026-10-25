# JEM Adventure Atlas

The 2026-10-25 redesign keeps one central native FTB chapter with ten theme portals. Each opens a real content chapter, with further real chapters for substantial topics. Every content chapter has a native route back to the index. Navigation images have no tasks, rewards or progress to complete

The authored book contains 602 cards in 39 visible chapters, including the index. There are 577 objectives and 25 informational cards. All 589 cards present before this redesign remain. Seven invisible legacy chapters retain retired quest and task identities without reusing them for a different mechanic

The earlier figure of 596 cards described an older revision. The immutable [baseline inventory](baseline-20261025.json) captures the actual 589-card source and native output before this redesign

## Canonical source and generation

- `Mods/JEMAchievementGuide/tools/questbook/book.json` owns exact bindings, stable identities, rewards, both languages, chapter membership, saved geometry and image declarations
- `python Mods/JEMAchievementGuide/tools/compile_questbook.py` emits native SNBT into `Client/ManagedPack/config/ftbquests/quests`, indexed rules, reference resources, localization keys, [card-mapping.csv](card-mapping.csv), the compiled-file manifest and the canonical Atlas reward selector
- `python Mods/JEMAchievementGuide/tools/questbook/audit_book.py` inventories authored/native data against the immutable baseline and writes [layout-audit-20261025.json](layout-audit-20261025.json) and [chapter-migration-20261025.csv](chapter-migration-20261025.csv)
- This audit is an offline content inventory. It does not launch Minecraft or create automated tests, fixtures or gameplay scenarios
- `tools/questbook/legacy` retains the original seven chapters byte for byte, with retired identities remaining invisible and optional in native output
- The old HTML importer is historical input tooling, never a compilation or runtime dependency

The compiler reads saved positions. It does not regenerate geometry, turn recommended `after` paths into dependencies or create quests merely to make navigation buttons

## Themes and geometry

| Theme | Main source | Canvas language |
| --- | --- | --- |
| Very Many Biomes | `tab-16` | Irregular regional continuations and isolated discoveries |
| Acompany | `tab-17` | Unequal care, husbandry, wildlife and aquatic tributaries |
| Cozy Home | `tab-07` | Source-grounded gallery islands with at most three cards |
| Farm 'n Food | `tab-03` | Seasonal plots and kitchen paths |
| Cogs & Trains | `tab-12` | Kinetic spine, factory sidings and railway subroutes |
| Secrets of Magic | `tab-26` | Asymmetric schools and enchanting branches |
| Boss Busters | `tab-25` | Preparation trails and retained encounters |
| Power System | `tab-02` | Existing choice and five independent race paths |
| Nightfall | `nightfall-gear` | Acquisition lane and genuine combat exercises |
| The Twelve Eyes | `tab-28` | Exactly twelve MAIN leaves in a mirrored bracket |

The storage wing retains five existing network objectives and adds five backpack and five fixed-storage objectives. Upgrade acquisition is explicitly preparation, without falsely detecting an installed capability or operating network. Exact installed recipes, native controls and registry IDs ground the additions

Engineering retains 41 mechanical objectives. Three compact links open existing button, seasonal sensor and copper-helper objectives in their own chapters. Those links never duplicate progress or rewards. The mechanical source share is 41 of these 44 destinations, approximately 93 percent

The campaign reads twelve canonical MAIN profiles and corresponding eyes from `JEMTwelveEyes`. All fifteen retained SUB objectives live in the preparation chapter. Six prep links beside the relevant leaves open genuine retained objectives. Bracket rails are decorative images without invented dependencies or intermediate rounds

Installed Nightfall 3.4.1 has no verified hostile challenge boss. Its guardian and doppelganger are invulnerable owner-bound summons. The atlas retains nine weapon exercises, adds three exact native acquisition milestones and links real combat chapters. It does not invent boss kills to satisfy a theme label

## Native navigation and localization

Installed FTB Quests 2001.4.22, Library 2001.2.13 and Teams 2001.3.2 remain the UI, team-progress, save and synchronization owners. Chapter image `click: "#<hex ID>"` opens an existing chapter or quest object. No unsupported `change_page` convention or reward-bearing entry quest is used

The hub is the earliest chapter in the first group. Native fresh selection reaches it. Saved chapter, zoom and scroll remain respected on reopening. Persistent navigation state is never erased to force the index

Titles, descriptions and subtitles use native serialized translatable components. Image hover text uses plain localization keys. Existing caption and reference adapters use the same native text parser. English and Russian player prose excludes implementation namespaces and source-mod branding. Real translated item and boss names remain intact

Captions and navigation hit areas have separate bounds. Header routes sit beyond authored card extents. Backgrounds, original identity images and gallery backplates have no click action

## Progress, signals and rewards

Every original native quest, task, reward and choice-table identity survives once. Task configuration, binding filters, quantities, claim keys, consume behavior, dependency edges and authored rewards are unchanged. Presentation, chapter membership and geometry intentionally change. [The layout report](ftb-quests-layout-report.md) records exact counts and limits

Native inventory, dimension, advancement and kill tasks handle supported simple conditions. Indexed custom predicates retain exact variants, distinct groups, mature crops, positive melee damage, consumption, mounts, fishing and domain snapshots. Fake players cannot create action progress. Recommended paths remain recommendations

Race and campaign progress come from canonical owners. World victories remain world history without pretending to prove personal participation. No quest grants a race, power, encounter admission or eye. Dedicated events remain unavailable when their host feature is disabled and never gate integrated-world progression

Economy publishes immutable notifications after committed sales, withdrawals, villager purchases and market deliveries. Quotes, validation, conservation, recovery and inventory delivery retain their existing owners

Item rewards enter existing take-only reward containers through native reward presentation. Durable personal claim keys preserve guaranteed entitlements and one-of-choice semantics across team changes. Experience retains its existing native reward and personal receipt behavior. There are still 61 native choice tables

Original reward quantities and substitutions remain unchanged. The thirteen new objectives grant only small supplies and experience. No boss weapon, ready endgame arsenal, power, eye or progression key is added as a reward

## Artwork and acceptance boundary

The latest user amendment uses six supplied images as page identity emblems over extended matching backgrounds. Full original JPGs remain uncut. Six background declarations preserve the 3072 by 2048 export ratio and include actual canvas content plus margins. The hub reuses the exploration background

Originals and extensions belong to [art-20261025](art-20261025). Native paths are `jem_advancements:textures/questbook/atlas/identity-{theme}.jpg` and `jem_advancements:textures/questbook/atlas/background-{theme}.png`. Missing exports remain explicit until real reviewed files are copied

A background is a finite native chapter image. Panning far enough can move beyond its edges. This implementation does not replace the canvas or install a repeating custom renderer

No gameplay client was launched for this redesign. Actual clicks, both GUI scales, text wrapping, saved completion, detection and reward claims remain unexercised. A content inventory or successful production build does not establish runtime acceptance

## Local delivery

[delivery.json](delivery.json) owns actual artifact hashes, local synchronization, native publication and installed development generation when recorded. Compilation alone does not publish or apply a generation. Stable profiles, MineRENT and the immutable client release baseline remain outside this delivery
