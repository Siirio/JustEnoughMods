# JEM Handbook

Implementation owner: JEMAchievementGuide. Product brief: the user's universal handbook request of 2026-10-22, resumed after FTB delivery `67d9f215` on 2026-10-23. The physical item is Patchouli's native `patchouli:guide_book` with `patchouli:book=jem_advancements:just_enough_guide`. One dirt plus one stick crafts one handbook in any two slots, including the inventory grid. Vanilla books retain their normal behavior. The 2026-10-24 amendment removes all book advancement notes and their click routing, while actual gameplay advancement IDs and progress remain intact, reading awards no progression, discoveries, items or permissions.

## Implementation plan

- [x] Restore native Patchouli book identity and its inventory-grid shapeless recipe.
- [x] Extend the native book screen with five localized, keyboard-accessible ribbon sections, search and scoped return from native manuals. Reuse native resources; no replacement book GUI or new runtime library.
- [x] Inventory installed native manuals, inspect their actual opening contracts and preserve native research/player state.
- [x] Trace confirmed enchantment registrations and add owner-backed explanations, per-level multi-enchantment tooltips and handbook navigation. Acquisition gaps remain explicit below.
- [x] Trace boss profiles, summon/use chains and actual defenses; publish only established instructions and record unresolved fields honestly.
- [x] Connect authored item help to tooltip/JEI, and current boss/drill state to Jade. Complete whole-pack natural-language/no-JEI coverage remains open.
- [x] Record coverage and Atlas impact, produce required production artifacts and synchronize ordinary local targets.
- [x] Native generation 64 published and applied to `test jem` during the JEMRewards metadata repair; exact token and installed hashes are in `delivery.json` under `delivery_followup`. Gameplay acceptance remains pending.
- [x] Review only task-owned changes, commit and normal-push current main; preserve parallel edits, stable profiles and release baseline.

## Constraints and review focus

No automated tests, gameplay launch, runtime dump, lint or static analysis. Production compilation and the mandatory Atlas maintenance/delivery workflow follow standing project authorization. No gameplay rebalance, research unlock, rewards, third-party recipe changes or new progression. Optional client classes remain isolated from dedicated/common loading. Reading native books must not manufacture personal discoveries. Search must not promise inaccessible foreign content. Native return must retain the actual previous screen and terminate normally. Small screens, GUI scale, focus and long Russian names require real runtime evidence before acceptance can be claimed.

## Coverage

Coverage files in this directory track evidence and implementation status; they do not replace Catalog registries, changes or effective behavior. A source-backed registration is distinct from an inferred language key or an authorized runtime observation. Source implementation and successful compilation never establish player acceptance.

| Surface | Implemented source scope | Evidence and limits |
| --- | --- | --- |
| Navigation | Getting started, Bosses, Enchantments, Items, Mod manuals | Native Patchouli spreads/history/search; five left-edge icon ribbons; original right-edge bookmarks remain native. GUI acceptance is unobserved. |
| Native manuals | Eight general manual families; acquired Aquamirae NBT variants | [Exact API/version/condition ledger](manuals.md). No discovery fabrication; MCreator read-only menus use a fixed, throttled server request and require the handbook in inventory. |
| Enchantments | 85 confirmed non-vanilla registrations, both languages, actual stored levels; target icons for 72 specialized entries | [Coverage](enchantments-coverage.json), [mechanics and remaining acquisition gaps](enchantments.md). Six EFN book sources remain unestablished. |
| Bosses | All 56 current canonical boss profiles have distinct source-backed tactical guidance | [Per-field coverage](bosses-coverage.json). Complete native phase/attack catalogs, every damage predicate and live world overrides are not claimed. |
| Items | 56 authored difficult-item cards plus mechanical drill help | [Coverage](items-coverage.json), [reference evidence](items-reference-evidence.json). Native manual routes supplement these cards. An exhaustive current survival/no-JEI inventory is not established. |
| Jade | Server-authored waiting/participant state; native Ignis/Harbinger/Leviathan protection signals; drill service/replacement action | Existing taming/breeding/settlement providers remain their owners. No client damage-admission model or new gameplay authority. |

## Content and projection ownership

`src/main/handbook/{navigation,manuals,translations,bosses,items}.json` owns authored navigation and boss/item prose. `tools/generate_handbook.py` materializes its localized Patchouli resources. The advancement generator retains gameplay definitions and their translations. Its final handbook materializer prunes retired book advancement entries, their categories and removed sections before writing the five current reference sections. It does not run as a build side effect.

`assets/jem_advancements/handbook/enchantments.json` owns enchantment explanations. Patchouli processors and tooltip providers read the same resource; native APIs supply current maximum level, compatibility and relevant synchronized configuration. Table applicability, anvil eligibility and item-specific rules remain distinct. Progression equipment text reuses the existing World Tier snapshot/provider; its enchantment level I is distinct from World Tier II/III. HideFlags and native lore are preserved.

`HandbookItemHelp` caches the localized authored pages on resource reload. The first spotlight is the described item, and `jem_summary` is its short tooltip. Existing JEI information recipes read the current cache when opened; ordinary recipe/use focus stays native, and an empty focus may route to the handbook. Changing the selector inventory in a resource pack still requires JEI's normal runtime registration lifecycle; changing text/language for existing selectors does not.

The only added network channel is `jem_advancements:native_manuals`, protocol `1`, C2S request `0` containing one bounded route byte. It accepts only the fixed CNC/Luminous/Unearthed read-only native menu procedures, on the server thread, for a live player carrying the exact handbook, with no unrelated open container. No arbitrary class, page, stack, NBT, coordinates or research value is accepted. Client-only screen classes remain behind the client bootstrap and optional-mod boundaries. The same fresh Guide JAR is required on client and ordinary Server.

## Native extension and visual evidence

Forge `ScreenEvent.Init.Post` adds actual focusable widgets; Patchouli 85 renders and clicks them in its own scaled coordinates. The three narrow optional client Mixins add bilingual alias matching, retain search on native screen reinitialization and expose the actual category independently of the current spread. Public screen/API seams handle opening and return; no replacement book renderer or global Escape interception exists.

The actual pre-manual `GuiBook` instance is retained only for a handbook-initiated manual session. A return control is attached only to that manual's verified native screen family, closes its native menu if needed and restores that instance. Close, disconnect, unrelated screens and unsuccessful asynchronous opens terminate the session. Native discovery/advancement locks and internal manual history remain native.

Native Patchouli bookmark pixels and item icons are reused; no generated/cutout/painted replacement asset is introduced. References inspected: original positive GUI badge reference `Снимок экрана 2026-10-16 181525.png`, negative crowded Shipping Box `Снимок экрана 2026-10-20 174338.png`, and the installed Patchouli 85 brown/gray book atlases. The applied qualities are identifiable icon-plus-color meaning, shaped native edges, restrained spacing and quiet page interiors. The actual composed screen, GUI scale, focus, narration and long-label clearance are **UNOBSERVED** under visual-auditor; no runtime visual pass or user approval is claimed.

## Preserved work and delivery boundary

FTB quest content, rewards, assets, native progress bridge and `QuestChapterImageMixin` from `67d9f215` remain intact. The parallel GUI Atlas catalogue is outside this task. The Rune of the Storm FTB text discrepancy is recorded in item coverage without modifying that session's quest files.

`Docs/quests/delivery.json` records the pre-existing ordinary Server tax-save failure and development-ledger guard. The later JEMRewards metadata repair completed ordinary Server startup, native preview/publication and application without bypassing those owners. The development-ledger guard still freezes currency; successful pack delivery is not a healthy-economy or gameplay claim. No saved data, money opt-in, trust gate, active-state pointer, stable profile, MineRENT host or release baseline is repaired or changed to bypass them. Actual build/sync/publication/application evidence belongs to [delivery.json](delivery.json), which distinguishes source completion from installed runtime and player acceptance.

Production builds and packaging inspection passed. The subsequent metadata repair refreshed Atlas (133 change records; no artifact drift), published generation 64 and applied its seven changed paths through native AutoModpack. The installed active token matches the published head; all five changed JAR hashes match ManagedPack and Server, and no transaction remains pending. The exact follow-up evidence is in `delivery.json`; gameplay and visual acceptance remain unobserved.

## Resource reload repair, 2026-10-23

The user reported hollow-square text, a crash entering an existing world and continued failure after deleting that save. The client reports at 14:52 and 14:57 fail during initial resource reload in `HandbookItemHelp` and `HandbookSearch`: Minecraft 1.20.1 rejects directory queries ending in `/`. The 14:40 startup log records the same category-query failure and resource-pack recovery before the 14:46 world-entry crash. Dynamic lights never logs initialization in that run; its exact installed bytecode assigns its singleton during client setup, while the earlier successful startup does initialize it. The world-entry null singleton is therefore consistent with incomplete startup, rather than evidence of a broken world save. The font symptom is consistent with the aborted resource reload; visual recovery still requires the user's relaunch.

The owning listeners now query `categories` and `entries` without a trailing separator and skip the separating slash when deriving relative IDs. Native resource selection, language fallback, authored JSON, handbook item identity, category ancestry, tooltip selectors and gameplay remain unchanged. No save or native resource-pack selection is edited. The production build and native distribution result are recorded under `resource_reload_repair` in [delivery.json](delivery.json). Status remains **in testing** until user acceptance; Minecraft launch and world entry are unobserved.

The repaired production JAR was built and synchronized to ManagedPack and ordinary Server. Native preview contained only the Guide JAR and manifest; generation **65** was published and applied to stopped `test jem`. Its active token matches the published head, the installed Guide SHA-256 matches both canonical targets, and no native transaction remains pending. The ordinary Server saved and stopped cleanly. This establishes installation only; font rendering and world entry remain pending user acceptance.

## Empty book and missing JEI item repair, 2026-10-23

The user reported that `test jem` displayed inactive handbook ribbons and no entries, while the separate native-manual route worked and the physical handbook was absent from JEI. The actual client log records `Template jem_advancements:enchantment does not exist` while building the first enchantment page. Patchouli 85 then replaces the entire book contents with empty maps. Category ribbons resolve those maps, while the independent manual screen does not require them.

Both unchanged template JSON files were placed in `just_enough_guide/templates`. The installed Patchouli 85 loader discovers templates using the same `en_us/<folder>` contract as entries and categories. They now live in `just_enough_guide/en_us/templates`, with native fallback shared by both book languages. The page IDs, processors, localized content, existing category gates and native navigation remain unchanged.

The missing JEI item has a separate data cause. Without `creative_tab`, Patchouli adds this generated book variant only to native Search. The installed JEI 15.49.0.194 item-list factory admits only `CreativeModeTab.Type.CATEGORY`. The book now declares `minecraft:tools_and_utilities`, allowing Patchouli itself to contribute the existing exact book variant to a normal category. The dirt-and-stick recipe and book NBT remain unchanged. No global JEI visibility setting or custom item injection is added.

This is a resource-contract repair with no new artwork or layout change. Production build and local installation are recorded in `delivery.json`. Status remains **in testing** until the user confirms book contents, ribbon navigation and JEI discovery in game.

Native generation **66** was previewed, published and applied to stopped `test jem` after the user closed Minecraft. Its active token matches `ec96b6d809109af97342aa167884f4d506047216`. The installed Guide SHA-256 matches ManagedPack and ordinary Server, and no native update transaction remains pending. The server saved and stopped cleanly. Actual book contents, tab clicks and JEI discovery still require player acceptance.

## Item links, native boss models and compact groups - 2026-10-24

User requested removal of Controls, JEI links for enchanted books and applicable items, five fixed target slots plus a cycling sixth slot, collapsed quest groups, and facing boss model/description/drop pages. The Controls category, entry and ribbon are removed from their actual source owners. Key bindings and native manual controls retain their behavior

The optional JEI bridge opens the exact rendered stack, including StoredEnchantments NBT for a real enchanted book. Left click requests recipes and right click uses. Native canEnchant/canApplyAtEnchantingTable predicates enumerate available equipment once per enchantment, cached until resource reload. Five equipment slots are stable. The sixth cycles through every remaining item and freezes while hovered, with clicks following the actually displayed stack. Existing detailed application/level text remains intact

Each of 56 authored boss entries begins with Patchouli 85's real entity page, fitted by the native maximum-dimension renderer, and a facing native description template with actual drop items. Existing detailed tactics continue on later pages. Introductory text is split at word boundaries while retaining whole hyperlinks to leave the footer clear. Possible drop identities come from the exact installed primary tables and five reviewed native death-drop procedures. Conditions and probabilities stay native. 55 profiles have item links, Fractured remains explicitly unconfirmed rather than receiving invented rewards. Provenance is in [boss-drop-links.json](boss-drop-links.json). Dynamic world loot overrides are not simulated

Bug / Client / native FTB ChapterGroup constructor sets guiCollapsed=false, and the authored chapter-group file has no collapsed-default property / in testing. A client-only constructor Mixin initializes nondefault groups collapsed, retaining native toggles for that object and preserving server quest definitions/progress

Production compilation and reobfuscation completed. The first JVM attempt failed on native memory allocation, so the retry used a smaller heap and worker count. The first new source build omitted its JEI compile-only dependency, corrected in the owner. Packaging inspection also caught four stale deleted Controls outputs and removed those exact generated files. Completion review found no additional concrete source defect requiring a rebuild

Read-only packaging inspection confirmed all 1,237 source JSON resources match the production JAR, no Controls resources remain, all 56 boss entries per language begin with a native entity page, and all 85 enchantment entries per language begin with their item-link page. The required templates, component classes and client Mixin registration are packaged. The build, ManagedPack and ordinary Server artifacts have the same SHA-256, `7978cbc7211caeaf800de0b548082c1bc24a11dd6d500d814e9979360af1e3bd`

No automated tests, static analysis, gameplay launch, FPS measurement or player acceptance is claimed. Packaging and local JAR synchronization do not establish native generation publication or installation in `test jem`. Exact evidence and remaining runtime limits are recorded separately in [links-delivery.json](links-delivery.json)


## Reference-only book, 2026-10-24

The user requested every advancement removed from the JEM book and the Mechanics tab removed. The book now contains only Getting started, Bosses, Enchantments, Items and Mod manuals, with 205 reference entries and five categories in each language. Old custom/native/campaign advancement notes and Mechanics pages are removed from resources and their generators. The independent FTB quest book is unchanged

Actual advancement definitions, IDs, criteria, translations, saved progress and rewards are unchanged. Boss reference pages remain available independently of achievements. Better Advancements no longer opens old book descriptions. The legacy directClick binary entry point remains as an inert compatibility method because an installed caller reaches it through JEMCompat. The earlier click-to-description lock is superseded by this explicit user removal

Canonical authoring no longer contains the Mechanics section, its pages or legacy advancement category mappings. Handbook regeneration prunes retired pages before writing current content, and the advancement generator no longer writes book notes. The obsolete campaign-book validator and unused advancement guide index are retired. No texture, native manual API, FTB quest logic, tooltip/JEI item help or recipe is redesigned

Production packaging, Atlas and native publication/application status are recorded in book-content-removal-20261024.json. Client closure is required before native application. No automated tests, gameplay launch or visual acceptance is claimed
