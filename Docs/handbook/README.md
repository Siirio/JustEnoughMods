# JEM Handbook

Implementation owner: JEMAchievementGuide. Product brief: the user's universal handbook request of 2026-10-22, resumed after FTB delivery `67d9f215` on 2026-10-23. The physical item is Patchouli's native `patchouli:guide_book` with `patchouli:book=jem_advancements:just_enough_guide`. One dirt plus one stick crafts one handbook in any two slots, including the inventory grid. Vanilla books retain their normal behavior. Existing advancement entry IDs and click routing remain intact; reading awards no progression, discoveries, items or permissions.

## Implementation plan

- [x] Restore native Patchouli book identity and its inventory-grid shapeless recipe.
- [x] Extend the native book screen with seven localized, keyboard-accessible ribbon sections, search and scoped return from native manuals. Reuse native resources; no replacement book GUI or new runtime library.
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
| Navigation | Getting started, Bosses, Enchantments, Items, Mechanics, Mod manuals, Controls | Native Patchouli spreads/history/search; seven left-edge icon ribbons; original right-edge bookmarks remain native. GUI acceptance is unobserved. |
| Native manuals | Eight general manual families; acquired Aquamirae NBT variants | [Exact API/version/condition ledger](manuals.md). No discovery fabrication; MCreator read-only menus use a fixed, throttled server request and require the handbook in inventory. |
| Enchantments | 85 confirmed non-vanilla registrations, both languages, actual stored levels; target icons for 72 specialized entries | [Coverage](enchantments-coverage.json), [mechanics and remaining acquisition gaps](enchantments.md). Six EFN book sources remain unestablished. |
| Bosses | All 56 current canonical boss profiles have distinct source-backed tactical guidance | [Per-field coverage](bosses-coverage.json). Complete native phase/attack catalogs, every damage predicate and live world overrides are not claimed. |
| Items | 56 authored difficult-item cards plus mechanical drill help | [Coverage](items-coverage.json), [reference evidence](items-reference-evidence.json). Native manual routes supplement these cards. An exhaustive current survival/no-JEI inventory is not established. |
| Jade | Server-authored waiting/participant state; native Ignis/Harbinger/Leviathan protection signals; drill service/replacement action | Existing taming/breeding/settlement providers remain their owners. No client damage-admission model or new gameplay authority. |

## Content and projection ownership

`src/main/handbook/{navigation,manuals,translations,bosses,items}.json` owns authored navigation and boss/item prose. `tools/generate_handbook.py` materializes its localized Patchouli resources. The advancement generator invokes that materializer and prunes only its own advancement entry trees, preserving handbook data and other components in the same namespace. It does not run as a build side effect.

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
