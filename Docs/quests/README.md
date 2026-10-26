# JEM Adventure Atlas

The [UI Rescue v3 specification](rescue-v3/specification.md) supersedes the earlier index and subchapter redesign. The native sidebar now contains exactly seven real chapters, without named chapter groups or a mandatory hub. Existing cards move into those canvases

The authored book contains 602 cards: 577 objectives remain visible across seven chapters, while 25 informational cards stay in source history and are not displayed. Seven invisible historical archives preserve retired identities. Existing objective, task and reward IDs remain stable

The client-only **In chat FTB** mod (`in_chat_ftb`) replaces FTB's completed-quest toast with a local chat line. The quest title is cyan, underlined and clickable, and opens that quest in the native FTB book. Chapter captions, separate guide cards and free-standing tab text are not shown; objective details, tasks and rewards remain available on their quest pages

The earlier figure of 596 cards described an older revision. The immutable [baseline inventory](baseline-20261025.json) captures the actual 589-card source and native output before this redesign

## Canonical source and generation

- `Mods/JEMAchievementGuide/tools/questbook/book.json` owns exact bindings, stable identities, rewards, both languages, chapter membership, saved geometry and image declarations
- `python Mods/JEMAchievementGuide/tools/compile_questbook.py` emits native SNBT into `Client/ManagedPack/config/ftbquests/quests`, indexed rules, reference resources, localization keys, [card-mapping.csv](card-mapping.csv), the compiled-file manifest and the canonical Atlas reward selector
- `python Mods/JEMAchievementGuide/tools/questbook/rescue_v3.py` previews deterministic migration without writing, and `--apply` stores the seven-canvas geometry
- `python Mods/JEMAchievementGuide/tools/questbook/audit_rescue_v3.py` writes the requested before/after preservation, image/model lookup and geometry receipts under [rescue-v3](rescue-v3)
- The earlier `audit_book.py`, [layout-audit-20261025.json](layout-audit-20261025.json) and migration CSV describe the superseded redesign, not current visual acceptance
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
| Boss Busters | `tab-25` | Combat preparation, power/survival, weapon exercises and twelve authentic campaign leaves |

The storage wing retains five existing network objectives and adds five backpack and five fixed-storage objectives. Upgrade acquisition is explicitly preparation, without falsely detecting an installed capability or operating network. Exact installed recipes, native controls and registry IDs ground the additions

Engineering retains its 41 mechanical objectives in short folded production and railway lanes. Native dependencies remain unchanged. The unused portal and cross-chapter image-link layer is retired

The campaign reads twelve canonical MAIN profiles and corresponding eyes from `JEMTwelveEyes`. All fifteen retained SUB objectives now live in a separate preparation zone inside Boss Busters. Bracket rails are decorative images without invented dependencies or intermediate objectives

Installed Nightfall 3.4.1 has no verified hostile challenge boss. Its guardian and doppelganger are invulnerable owner-bound summons. The atlas retains nine weapon exercises, adds three exact native acquisition milestones and links real combat chapters. It does not invent boss kills to satisfy a theme label

## Native navigation and localization

Installed FTB Quests 2001.4.22, Library 2001.2.13 and Teams 2001.3.2 remain the UI, team-progress, save and synchronization owners. Chapter image `click: "#<hex ID>"` opens an existing chapter or quest object. No unsupported `change_page` convention or reward-bearing entry quest is used

Native selection opens the last surviving chapter, otherwise the exploration chapter. The existing chapter-group client adapter filters historical technical archives even in native edit mode, which otherwise ignores `always_invisible`. The narrow `QuestbookViewportMixin` applies only to chapters tagged `jem_rescue_v3`. It evaluates the real panel and visible sidebar, initializes native zoom using the current theme spacing and uses native widget geometry for both drawing and clicks. Each chapter's camera-only snapshot is retained within the connected book session, without restoring sidebar state or overriding native search targets. A new session gets an entrance suited to the repaired layout, rather than restoring the obsolete giant-canvas camera

Titles, descriptions and subtitles use native serialized translatable components. Image hover text uses plain localization keys. Existing caption and reference adapters use the same native text parser. English and Russian player prose excludes implementation namespaces and source-mod branding. Real translated item and boss names remain intact

Captions use native localized text, one complete line where it fits and at most two lines. They reference the native empty icon instead of a dummy image resource. Backgrounds and identity images have no click action

## Progress, signals and rewards

Every original native quest, task, reward and choice-table identity survives once. Task configuration, binding filters, quantities, claim keys, consume behavior, dependency edges and authored rewards are unchanged. Presentation, chapter membership and geometry intentionally change. [The layout report](ftb-quests-layout-report.md) records exact counts and limits

Native inventory, dimension, advancement and kill tasks handle supported simple conditions. Indexed custom predicates retain exact variants, distinct groups, mature crops, positive melee damage, consumption, mounts, fishing and domain snapshots. Fake players cannot create action progress. Recommended paths remain recommendations

Race and campaign progress come from canonical owners. World victories remain world history without pretending to prove personal participation. No quest grants a race, power, encounter admission or eye. Dedicated events remain unavailable when their host feature is disabled and never gate integrated-world progression

Economy publishes immutable notifications after committed sales, withdrawals, villager purchases and market deliveries. Quotes, validation, conservation, recovery and inventory delivery retain their existing owners

Item rewards enter existing take-only reward containers through native reward presentation. Durable personal claim keys preserve guaranteed entitlements and one-of-choice semantics across team changes. Experience retains its existing native reward and personal receipt behavior. There are still 61 native choice tables

Original reward quantities and substitutions remain unchanged. The thirteen new objectives grant only small supplies and experience. No boss weapon, ready endgame arsenal, power, eye or progression key is added as a reward

## Artwork and acceptance boundary

Rescue v3 separates existing hero images from background bounds. The original six emblems are displayed at 13.8 quest units wide, independently of the tightly framed matching background. No new images are generated

Originals and extensions belong to [art-20261025](art-20261025). Native paths are `jem_advancements:textures/questbook/atlas/identity-{theme}.jpg` and `jem_advancements:textures/questbook/atlas/background-{theme}.png`. Missing exports remain explicit until real reviewed files are copied

A background is a finite native chapter image. Panning far enough can move beyond its edges. This implementation does not replace the canvas or install a repeating custom renderer

**NOT VISUALLY VERIFIED**. The user chose to check Minecraft manually. The available emblem sources are opaque JPGs, so clean transparent exports remain unavailable. Boss Busters currently uses native eye artwork and a large localized heading because no separate approved banner exists. These are explicit acceptance limits, not passed artwork gates. Screenshots A-G and user acceptance remain pending

## Local delivery

[The rescue delivery receipt](rescue-v3/delivery.json) owns current artifact hashes, local synchronization, native publication and installed development generation. Older delivery files are historical receipts. Compilation alone does not publish or apply a generation. Stable profiles, MineRENT and the immutable client release baseline remain outside this delivery
