# Major Update 4 stabilization plan

Date: 2026-11-15

Status: plan only

Target branch: `develop`

## Scope

This plan covers the regressions reported after the Major Update 4 MineRENT publication:

- Preserve player keybinds and resource-pack order across AutoModpack updates
- Remove Weather Glass and all remaining active Luxium surfaces
- Complete locked-file updates only after Minecraft closes, with a wait of up to three minutes
- Keep the native AutoModpack state directory and repair only its managed projection and transactions
- Make a disconnect from a hosted boss fight an immediate defeat and release the arena
- Restore reliable FTB Quests menu access and reward claiming
- Restore arena borders for every registered structural boss
- Repair the F6 race/player preview renderer
- Remove Blast From The Past camera shake from Frostomper and Psycho Bear attacks
- Offer Boss Raid only when a matching Raid event is active
- Restore the Events tab in `/smp`
- Rework all boss handbook pages into short player-facing combat and reward guidance
- Fit every boss model inside its handbook viewport
- Add one top-level "Руды" section to the JEM JEI overhaul and move complete resource families out of "Остальное"
- Correct the ownership of Create workstations, countertops, furniture, transport, and Let's Do Farm & Charm content
- Keep RoadWeaver and give every Abridged bridge priority over nearby or intersecting RoadWeaver bridge spans
- Restore StructBlend as the terrain-blending owner without replacing Structurify or Structure Layout Optimizer
- Add Better Villages through a separate village route that does not replace the current Grand Capitals, Trek, or Nature's Spirit routes

This document does not implement, build, publish, deploy, or launch any change

## Branch baseline

`develop` and `main` are not equal

- `develop` is at `df0be060a2` and matches `origin/develop`
- `main` is at `b244bda4a`
- `origin/main...origin/develop` reports `0 135`
- `develop` contains 135 commits that are not in `main`
- The current `develop` history includes the Major Update 4 merge and MineRENT deployment record

Implementation must continue on `develop`. Promotion to `main` is a separate production release action

## Investigation result

### Confirmed causes

| Area | Evidence | Cause |
| --- | --- | --- |
| Weather Glass | `Client/ManagedPack/mods/weatherglass-0.5.0-1.20.1-forge.jar` and `Client/ManagedPack/config/weatherglass.properties` still exist | The canonical managed pack still publishes Weather Glass |
| Remaining Luxium state | The stable profile still has `config/jem_luxium-client.toml` and `config/luxium-client.toml` | The active JARs were retired, but client-local active configuration was not removed by a completed owned transaction |
| Partial client projection | The stable profile contains old and new copies of several JEM JARs | The installed token can match the published head while stale live files remain, so token equality alone is not a sufficient readiness check |
| Keybind loss | The stable `options.txt` has Hotbar 1 through 9 and FTB Quests set to `key.keyboard.unknown` | The live player state was changed. The exact writer is not yet proven |
| Resource-pack order loss | Stable `options.txt` has a different order and a different set from the maintained template | The live player state was changed. AutoModpack excludes `options.txt`, so the exact writer must be traced before a fix |
| Raid without an event | `StructureStaging.raidAvailable()` falls back to `RaidArenaApi.compatible(...)`, and `chooseInScope()` creates `manual:raid:*` | JEM Server intentionally creates an ordinary manual Raid when no Raid event exists |
| Missing Events tab | `DomainFeatures` exposes Events only when `EventRuntime.enabled(server)` is true. The current world config has `enabled = false` | UI visibility is incorrectly coupled to scheduler activation |
| Delayed disconnect defeat | `EncounterContext.expireDisconnected()` and the hosted encounter owner use `HostedEncounterApi.disconnectGraceMillis()` | Disconnects keep a participant reservation during the grace interval instead of ending the attempt immediately |
| Legendary Monsters borders | Frostbitten Golem and Overgrown Colossus are marked `hosted_encounter: false` in the World Boss Tiers boss profiles | These structural bosses never enter the shared hosted arena lifecycle that supplies walls and teleport restrictions |
| Blast From The Past camera shake | Installed bytecode sends `ScreenShakePayload(20.0F, 60)` from both Frostomper and Psycho Bear and installs it through `ClientPayloadHandler.handleScreenShake()` | The mod has a private unconditional screen-shake path and no relevant config option |
| Oversized handbook bosses | All generated boss entries use the unbounded native `patchouli:entity` page without per-entity fitting data | Sirok and other large entities can leave the page viewport |
| Complex handbook text | `Mods/JEMAchievementGuide/src/main/handbook/bosses.json` generates the long technical pages shown in the screenshots | The authored source is engineering-oriented and duplicates implementation detail instead of player actions and rewards |
| JEI "Остальное" overflow | `JemCatalogCategories.classify()` sends ores and listed non-building resources directly to `Kind.OTHER`, has no resource-family category, and lets many unmatched Create and farming blocks fall through to the same catch-all | The classifier has no canonical "Руды" owner and relies on short name heuristics instead of installed tags plus bounded exceptions |
| Misclassified requested items | The exact installed IDs include `create:andesite_table_cloth`, `create:brass_table_cloth`, `create:copper_table_cloth`, `brewery:sideboard`, `unearthed_journey:canoe_item`, and `farm_and_charm:chicken_coop` | Furniture, transport, and farming classifiers do not cover these installed identities or their full semantic families |
| Better Villages competition | Grand Capitals replaces the five `minecraft:village_*` definitions with `revampedvillages:start`, while Better Villages replaces NBT under vanilla village paths and can replace village-set placement through its Mixin | Reinstalling Better Villages on the same five IDs would either be inactive behind Grand Capitals or restore two owners for the vanilla village route |
| Abridged and RoadWeaver bridge conflict | Abridged registers the separate `abridged:bridge` structure at `surface_structures`, while RoadWeaver places its road feature later at `top_layer_modification` and selects both short and long native bridge builders from its own span masks | RoadWeaver can see Abridged starts before it places blocks, but its current generic avoidance checks only one exact segment center inside a surface structure piece, has no 15–20 block clearance, and does not suppress the complete RoadWeaver bridge range |
| Structure stack change | Structure Layout Optimizer owns jigsaw assembly performance, Structurify owns placement and overlap prevention, RoadWeaver owns roads, and StructBlend is absent | StructBlend can own terrain blending without method or responsibility overlap and without replacing RoadWeaver's roads |

### Ruled out as the primary owner

- JEM Security does not intercept FTB Quest screens or the FTB Quests packet path
- JEM Security's resource-pack guard blocks only forbidden selected packs. It does not reorder the pack list
- F6 itself is not the failed input path. The client log shows `RaceExperienceScreen` opened after F6
- An offline player is not directly returned by `StructureStaging.blockers()`. That method reads connected online players only

### Causes that still require runtime proof

#### FTB Quests menu

Static evidence shows:

- FTB Quests 2001.4.22 loads its quest data and `ftbquests:open_quest_book`
- `disable_gui` is false
- The inventory sidebar button is enabled
- The latest client log has no FTB quest parse failure
- The stable FTB Quests keybind is unbound
- `QuestBookItem.use()` and the inventory route should call the native `ClientQuestFile.openGui()` path

The missing keyboard route is proven, but it does not explain failure from both the book and inventory. Add bounded one-shot diagnostics at these points before changing behavior:

1. FTB key click and inventory sidebar click
2. Quest book item use
3. `ClientQuestFile.INSTANCE` presence
4. Entry into `ClientQuestFile.openGui()`
5. `Minecraft.setScreen(QuestScreen)` result on the next client tick

Fix the first missing transition only. Do not replace the native FTB Quest GUI or reward owner

#### False arena occupancy

The current blocker query cannot select a fully offline player. Instrument each non-empty blocker result with UUID, name, connection acceptance, dimension, spectator/operator state, participant membership, and bounding-box intersection. Then determine whether the visible message comes from:

- A disconnect race while the connection is still considered accepting messages
- A different online player that the UI does not name
- A persisted party or encounter reservation reported through another path
- A stale prompt that survives after blocker state changes

The final UI must name each real blocker. A prompt must be invalidated immediately when its blocker list becomes empty

#### F6 player renderer

The input and screen-open path works. The failure is inside `JEMRoles` presentation:

- `RaceExperienceScreen`
- `RaceStageRenderer`
- The detached `RemotePlayer`
- The custom viewport and projection state
- Native player renderer compatibility, including Customizable Player Models

Add one-frame diagnostics for entity readiness, skin/model readiness, renderer selection, pose stack, viewport, scissor, depth state, and render exceptions. Compare the custom path with the native inventory player-render helper. Keep the server race state unchanged

## Implementation plan

### Phase 1: Repair the update ownership boundary

1. Reproduce the update plan from the exact published MineRENT generation and the affected stable client witness
2. Identify the writer that changed `options.txt` by correlating file timestamps with bootstrap, launcher, Minecraft shutdown, and native transaction logs
3. Preserve the complete pre-update `options.txt` before any managed mutation
4. Treat keybinds and resource-pack order as player-owned semantic state
5. Merge only newly introduced default keys that have no existing player value
6. Never replace an existing keybind, resource-pack order, language, video option, audio option, or accessibility option during an ordinary update
7. Add a post-commit audit that compares the preserved semantic fields with the installed live file
8. Require both the active generation token and the live managed-file inventory to match before reporting the profile ready
9. Detect duplicate mod IDs and obsolete owned JAR paths after commit
10. Repair duplicates only through the native AutoModpack transaction and ownership ledger

Do not delete the whole `automodpack` directory. It contains native journals, content-addressed objects, trust, routes, ownership, generation witnesses, and recovery state

### Phase 2: Handle files that are in use

1. Before a replace or delete, detect an active Minecraft Java process and an in-use target
2. Show the player: "Close Minecraft. The update will continue automatically for up to 3 minutes"
3. Start the existing detached native helper before the game process exits
4. Wait for the exact Minecraft process to end, with a maximum wait of three minutes
5. Resume the same signed native transaction under the mutation lock
6. Replan against current file hashes after the process exits
7. Commit the generation only after every requested replace and delete succeeds
8. If the wait expires, keep the previous generation active and show the exact blocked paths
9. Never mark a partially applied transaction as current

This flow must cover JARs, configs, resource packs, and obsolete managed directories that Windows locks while Minecraft runs

### Phase 3: Remove Weather Glass and remaining Luxium active state

1. Remove the Weather Glass JAR and property file from `Client/ManagedPack`
2. Remove Weather Glass from the dedicated Server only if its exact installed metadata proves a server component is active
3. Add native deletion entries for the previously owned Weather Glass paths
4. Remove active Luxium and JEM Luxium config, GUI, mod-list, Sandbox, packaging, and publication surfaces
5. Keep historical changelog and Atlas records as history. Mark them retired rather than deleting history
6. Confirm no active pack, runtime, build graph, public release, or Sandbox path can install Luxium or JEM Luxium
7. Preserve Oculus and VSS
8. Preserve Neo Voxy as a separate manual client installation under its current license rule

### Phase 4: Make disconnect an immediate boss defeat

1. Replace the disconnect grace path for hosted Boss Fight, Campaign, and Raid participation with immediate elimination
2. Record defeat once at the server logout boundary
3. Remove the player from the active participant set in the same authoritative mutation
4. Recalculate encounter ownership and release the arena immediately when no valid participants remain
5. Save a safe-exit return intent through the existing durable return-pending mechanism
6. On the next login, place the defeated player outside the registered arena with full collision-box clearance
7. Never teleport the offline entity
8. Do not apply this rule to unrelated network reconnection state outside hosted boss encounters
9. Keep actual death as immediate elimination with no hidden lives or restoration
10. Name real online blockers in the occupied prompt and discard stale prompts

### Phase 5: Restore all structural boss borders

1. Audit every installed boss entity against its exact installed native structure or generated JEM site
2. Use Atlas lookup and an impact scan before changing any boss profile
3. Correct `hosted_encounter` and `arena_strategy` in the canonical World Boss Tiers profiles
4. Map Frostbitten Golem to its installed Frostbitten Temple structure
5. Map Overgrown Colossus to its installed native temple structure after exact JAR structure-ID confirmation
6. Apply the same audit to every other structural boss. Do not add a two-boss exception list
7. Reuse the shared registered three-dimensional structure-volume walls, collision, teleport restrictions, safe arrival, and safe exit
8. Keep underground structures bounded by their real 3D volume. Do not block the full column above them
9. Keep Blood Moon and Resource Rush boundary contracts unchanged
10. Update `Catalog/changes`, derived effective data, handbook coverage, and owning event documentation in the same implementation task

### Phase 6: Correct Raid and Events presentation

1. Make `StructureStaging.raidAvailable()` require an active matching Boss Raid event
2. Remove creation of `manual:raid:*` ordinary raids
3. Keep ordinary Boss Fight and Campaign choices available under their existing rules
4. Reject stale client Raid choices on the server even if an old prompt remains open
5. Decouple the `/smp` Events tab from scheduler activation
6. Show the tab on an eligible dedicated SMP server even when no event is active
7. Show a clear empty or disabled state when scheduling is off
8. Correct the MineRENT `jem-events-server.toml` setting separately if scheduled events are intended to run there
9. Keep Events, Raid, parties, and SMP disabled in integrated and LAN worlds under the existing project rule

### Phase 7: Restore FTB Quests access

1. Restore a valid default FTB Quests key only when the player has no intentional binding
2. Run the bounded native diagnostic described above
3. Fix the first missing transition in the native FTB flow
4. Keep the FTB quest book item, inventory sidebar, native QuestScreen, team progress, rewards, and claims as the owners
5. Keep In Chat FTB as a link adapter only
6. Remove diagnostics after the cause is proven
7. Verify all three entry routes: key, book item, and inventory button
8. Verify a completed unclaimed quest can open and claim its native reward
9. Retest with JEM Security installed to prove there is no security regression

### Phase 8: Repair F6 player rendering

1. Remove stale duplicate JEM Roles artifacts through the native client repair from Phase 1
2. Add the bounded render diagnostic described above
3. Prefer the native inventory player-render helper if it can provide the required stage presentation
4. Otherwise restore every viewport, scissor, projection, lighting, depth, and pose state after the custom render pass
5. Derive preview scale from the model bounding box and GUI viewport
6. Support the active player skin and Customizable Player Models without making CPM a required dependency
7. Keep F6 input, server race state, unlocks, and progression unchanged

### Phase 9: Remove Blast From The Past camera shake

1. Add a narrow optional client compatibility adapter for exact installed mod ID `blastfromthepast`
2. Suppress only the mod's `ScreenShakePayload` effect produced by Frostomper and Psycho Bear
3. Preserve sounds, particles, damage, knockback, AI, animations, and the Psycho shader effect
4. Do not disable global Minecraft hurt movement or camera effects from other mods
5. Bind the adapter to the inspected 1.0.3 class and method signatures and fail closed if the upstream signature changes

### Phase 10: Rework the boss handbook

Canonical authored source: `Mods/JEMAchievementGuide/src/main/handbook/bosses.json`

Generated Patchouli JSON is output, not the editing owner

For every boss, generate a compact structure:

1. "Где найти"
2. "Как сражаться"
3. "Что не действует"
4. "Что выпадает"
5. "Награда кампании" only when applicable

Writing rules:

- Use short player actions such as "Стрелы не действуют", "Босс кидается ядом", and "После замаха отойди в сторону"
- Remove implementation terms such as damage pipeline, source damage, hosted profile, threshold scaling, and format identifiers
- Do not show `Format error`
- Do not repeat generic JEI and victory-record warnings on every page
- Show only verified mechanics from the exact installed JAR, config, JEM change records, and effective Atlas
- Show actual possible drops with localized names and icons
- Show XP when the installed loot or entity code proves the amount
- Separate guaranteed drops from chance-based drops
- For Sirok, include the verified gauntlet, Black Eye campaign reward, and verified XP value
- For Frostbitten Golem, include Frozen Rune, verified ice-shard drops, and other exact native loot
- Do not invent missing loot or probabilities

Visual rules:

- Replace the raw `patchouli:entity` page with a JEM-owned bounded boss preview template
- Measure the rendered entity bounds and calculate a fit scale for the available art slot
- Apply a scissor clip so no model can leave the page
- Support multipart entities such as Sirok
- Use a stable fallback icon only when entity construction fails, never an unexplained placeholder cube
- Preserve the warm parchment and tabbed-book language
- Keep the text area quiet and readable
- Review the implemented screen against the exact Taste Engine pair `gemini-layered-tabbed-book-gui`
- Review `/smp` changes against `Forge-1.20.1-jem-smp-screens`
- Run the required visual audit on the actual in-game result before delivery

### Phase 11: Add the top-level "Руды" catalog section

Canonical owners:

- `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/catalog/JemCatalogCategories.java`
- `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/client/jei/CatalogCategory.java`
- `Mods/JEMCompat/src/main/resources/assets/jemcompat/lang/ru_ru.json`
- `Mods/JEMCompat/src/main/resources/assets/jemcompat/lang/en_us.json`

1. Add one first-class category with the stable internal ID `ORES` and the Russian label "Руды"
2. Keep one leaf owner per item. An item must never appear in both "Руды" and "Строительство"
3. Classify resource families from installed Forge or common tags first: ores, raw materials, ingots, nuggets, gems, dusts, shards, crystals, and storage blocks
4. Use exact installed recipes, tags, registry data, and bounded source-backed exceptions for untagged mod resources
5. Audit every installed namespace, not only the screenshot examples
6. Include the full applicable Create families such as zinc, brass, copper nuggets, crushed raw materials, andesite alloy, and their resource blocks
7. Include the full applicable Alex's Caves families such as galena and uranium ores, uranium and tectonic shards, sulfur dust, scrap metals, neodymium ingots, gems, and resource blocks
8. Include the full applicable Unearthed Journey families such as stone-variant ores, raw lead, lead ingots and nuggets, manganese steel, and resource blocks
9. Include equivalent families from all other installed mods when the exact registry and tags prove the same material role
10. Keep decorative bricks, stairs, slabs, walls, machines, tools, armor, and ordinary construction blocks outside "Руды" even when their recipe uses an ore material
11. Give the category a stable representative ore icon and place it near the existing material and building sections
12. Add a catalog audit report that lists every remaining "Остальное" item by mod and reason, so the catch-all contains only items that truly have no better owner

The screenshots are representative evidence. They are not a closed item allowlist

### Phase 12: Correct catalog ownership outside "Руды"

1. Audit all Create items currently in "Остальное" and assign machines, mechanisms, workstations, tools, logistics, and redstone parts to their existing semantic categories
2. Put Create table-cloth countertops and all equivalent installed countertop families in `Building -> Furniture`
3. Put `brewery:sideboard` and equivalent sideboards or buffets in `Building -> Furniture`
4. Put `unearthed_journey:canoe_item` and equivalent rideable canoes in `Transport`. Keep spawn eggs in `Spawn Eggs`
5. Put `farm_and_charm:chicken_coop` in `Food and Cooking`
6. Audit all Let's Do Farm & Charm items and put cooking stations, food processing blocks, farm production blocks, utensils, ingredients, and related functional storage in `Food and Cooking`
7. Keep purely decorative Farm & Charm building pieces in the appropriate Building leaf instead of moving the whole mod by namespace
8. Prefer exact native tags, block classes, recipes, and source-backed families over translated-name matching
9. Add explicit regression coverage to the implementation review matrix without creating automated tests or test fixtures
10. Confirm that no requested family falls back to "Остальное" after the full installed-registry audit

### Phase 13: Give Abridged priority, restore terrain blending, and add Better Villages safely

The user's "Path Weaver" name refers to the installed `RoadWeaver` mod. RoadWeaver remains installed

1. Preserve RoadWeaver's current structure routes, ground-anchor correction, ordinary roads, and bridges when no Abridged bridge is nearby
2. Treat `abridged:bridge` as the authoritative bridge owner in every direct or nearby conflict
3. Use a 20-block horizontal clearance around the union of the real Abridged structure-piece bounding boxes. This uses the safer upper limit of the requested 15–20 block range
4. Scan Abridged starts in every chunk touched by the candidate RoadWeaver bridge footprint plus that clearance. Do not rely on the current exact-center `getAllStructuresAt` check
5. Match by the exact registered structure ID `abridged:bridge`, not by bridge materials, template names, water blocks, or translated names
6. Evaluate priority after RoadWeaver calculates a complete bridge range and before it chooses the short `BridgeSegmentPlanner` or long `BridgeSegmentPlannerNew` path
7. Expand the candidate RoadWeaver footprint by its road width and configured bridge ramps before testing it against the Abridged clearance volume
8. If the candidate footprint intersects the protected Abridged volume, mark the complete RoadWeaver bridge range and its ramps as skipped. Do not place a RoadWeaver deck, piers, rails, buoys, fill, paving, signs, or decoration inside that suppressed range
9. Keep RoadWeaver segments outside the suppressed range unchanged. Do not reroute, replace, edit, or decorate the Abridged bridge
10. Apply the rule to newly generated chunks. Do not rewrite existing generated bridges or roads
11. Keep the generic RoadWeaver structure-avoidance rule for all other structures. The Abridged rule is a stronger exact-ID priority layer, not a replacement for generic avoidance
12. Extend the disclosed exact-version RoadWeaver patch with one isolated Abridged priority service and one call before bridge-path branching. Do not add a broad Mixin into Abridged
13. Add StructBlend 1.0.3 as the server-side terrain-blending owner for eligible newly generated structures
14. Keep Structure Layout Optimizer as the jigsaw assembly performance owner
15. Keep Structurify as the structure placement and physical-overlap owner, with its current global spacing modifier disabled
16. Do not add StructureOverlapless. It would duplicate Structurify
17. Configure StructBlend exclusions from exact installed structure IDs for underground, ocean-floor, sky-only, moving or special-terrain structures, plus registered boss arenas where blending can damage registered walls, safe arrival, or safe exit geometry
18. Keep ordinary surface villages, Abridged bridges, and compatible surface structures eligible for StructBlend only after the exclusion and native-terrain audit proves that their geometry is preserved
19. Add Better Villages 3.3.1 and use the already installed Library Ferret 4.0.0 dependency
20. Disable Better Villages custom spacing so it cannot replace the merged JEM village structure-set placement
21. Keep the five current `minecraft:village_*` definitions owned by Grand Capitals and keep the unique Trek and Nature's Spirit structure IDs once each
22. Create separate JEM-owned Better Villages structure IDs that start from the vanilla biome-specific village pools whose NBT is supplied by Better Villages
23. Add those separate IDs to the one merged village structure set with reviewed weights. Do not overwrite Grand Capitals definitions or duplicate Trek and Nature's Spirit IDs
24. Confirm with exact datapack and JAR inspection that each village variant has one definition owner, one template route, and one placement owner
25. Treat Better Villages as client and server content. Before publication, prove that the selected distribution route permits the All Rights Reserved JAR to be delivered to players. If redistribution is not permitted, require an official user-installed delivery route and do not embed the JAR in AutoModpack
26. Treat StructBlend as server-side only and obtain its artifact from the official source. Do not send it to clients unless its exact metadata and license require that
27. Record the superseding structure decision in Atlas, `Docs/DEVELOPMENT.md`, `Docs/MODS.md`, and the Major Update 4 structure receipt during implementation

The noncompetition target is the current Grand Capitals village owner plus the unique Trek and Nature's Spirit village routes. This is the safest complete interpretation of the unfinished phrase "make so that it wont compete with"

### Phase 14: Package and release as one client-and-server boundary

This work is not a server-only update. It changes client state handling, client rendering, handbook assets, configs, optional client compatibility, and server encounter behavior

1. Complete source, Atlas, generated handbook, managed pack, local Server, docs, and changelog consequences
2. Build only the production artifacts authorized by the implementation prompt
3. Remove old artifact versions from the canonical managed source
4. Run `python tools/jem.py stage-pack-sync`
5. Inspect native `/jempack preview`
6. Publish through native `/jempack publish`
7. Close Minecraft if files are locked and allow the detached helper up to three minutes
8. Apply the published generation to `Test JustEnoughMods` through native AutoModpack
9. Confirm active token, exact JAR hashes, absence of duplicate mod IDs, preserved `options.txt` semantic state, and no pending transaction
10. Do not mutate the stable profile until an explicit production release request
11. Publish MineRENT, stable client, cumulative friend update, release metadata, and baseline only as one explicit reviewed production boundary

## Manual acceptance checklist

Automated tests and gameplay test fixtures are prohibited by the project rules. Use static inspection and explicit player acceptance

- Existing custom keybinds remain unchanged after an update
- Existing resource-pack order remains unchanged after an update
- A newly added action receives a default key only when no player choice exists
- A locked JAR update asks the player to close Minecraft and completes within three minutes after exit
- The native `automodpack` state directory remains intact
- Weather Glass is absent from Forge and its config is removed
- No active Luxium or JEM Luxium file, page, config, or build target remains
- No old and new JEM JAR pair remains in the client `mods` directory
- FTB Quests opens from the key, quest book, and inventory button
- A native quest reward can be claimed
- Disconnecting during a hosted boss fight records defeat immediately
- Other players can start the encounter immediately after that disconnect
- The disconnected player returns outside the arena on the next login
- Every registered structural boss has the correct visible 3D border
- Underground borders do not extend to the sky
- Raid is absent when no matching Raid event is active
- `/smp` keeps the Events tab and shows an empty state when applicable
- F6 shows the player model with the active skin and preserves GUI render state
- Frostomper and Psycho Bear no longer shake the camera
- Other native and modded camera effects still work
- Boss pages use simple Russian, verified mechanics, exact drops, and verified XP
- Sirok and every other boss model stay inside the handbook art slot at all GUI scales
- The top-level "Руды" section contains all verified installed ore and material families, not only the screenshot examples
- Decorative masonry remains in Building while resource storage blocks remain in "Руды"
- Create countertops and Brewery sideboards appear under Building -> Furniture
- Unearthed Journey canoe appears under Transport
- Farm & Charm chicken coop and all verified cooking or production families appear under Food and Cooking
- The remaining "Остальное" report contains no item with an established semantic owner
- RoadWeaver still generates ordinary roads and non-conflicting bridges
- A direct overlap with `abridged:bridge` suppresses the complete RoadWeaver bridge range and its ramps
- A RoadWeaver bridge footprint within 20 horizontal blocks of an Abridged bridge is suppressed before either RoadWeaver bridge builder runs
- A RoadWeaver bridge more than 20 blocks from Abridged remains unchanged
- Abridged bridge blocks, pieces, terrain behavior, loot, and generation rate remain native
- StructBlend changes terrain only around eligible new structures and does not damage excluded boss, underground, ocean-floor, or sky structures
- Structure Layout Optimizer and Structurify retain their separate current responsibilities
- Grand Capitals, Better Villages, Trek, and Nature's Spirit village routes each generate through one definition and placement path without duplicate village IDs

## Protected behavior

- Native FTB Quest state and rewards remain owned by FTB Quests
- JEM Security attestation, anti-xray, creative provenance, and forbidden-pack checks remain active
- Player-owned Xaero waypoints and settings remain private
- Boss combat remains server-authoritative
- Ordinary boss borders reuse registered structure geometry
- Blood Moon and Resource Rush keep their separate boundary contracts
- Integrated and LAN worlds do not gain JEM SMP, Events, or Raid
- Neo Voxy remains a manual client install
- Stable `JustEnoughMods` remains untouched until an explicit release request
- Decorative and gameplay category ownership remains semantic. A whole mod namespace is not forced into one category
- Grand Capitals remains the owner of the five current vanilla village definitions
- Structurify remains the only physical-overlap owner
- Existing generated roads, bridges, and structures are not rewritten

## Delivery gate

Do not mark this plan resolved until all confirmed causes have owner-level fixes and all unresolved causes have runtime evidence. A successful build, matching token, or server start is not player acceptance

After every implementation and acceptance gate passes, report "Все готово". Then perform the planned normal `develop` to `main` production promotion, push `main` without force, and verify that MineRENT, the stable client release, release metadata, and the immutable baseline describe the same reviewed production boundary. Do not promote `main` before those gates pass
