# Catalog, Fishing, Boss, Weather, and Stability One-Shot Plan

Status: approved execution contract

Branch: `develop`

This plan supersedes `Docs/plans/2026-11-21-jei-catalog-manifest.md`

## Result

Complete this update in one implementation session. Repair the confirmed client crash causes first. Rebalance all 101 Tide fish and make fish length affect Shipping Box value. Add "Deposit All" to the Shipping Box. Remove hosted group and barrier behavior from ordinary bosses. Extract craftable spawn eggs into an uninstalled source project and remove the third-party mineable-spawner JAR. Rebuild the JEI manifest from actual registered items and apply the assignment contract at the end of this document. Correct the Locator Bar and Particle Rain configuration. Perform the settlement and villager performance rewrite last

No product or category decision remains open. Do not ask the next session to choose values, categories, selectors, or architecture

## Hard constraints

- Preserve all existing Test JustEnoughMods content that is not explicitly changed here
- Do not touch the unrelated dirty files under `Server/config`
- Do not change `main`, MineRENT, the stable `JustEnoughMods` profile, or the immutable release baseline
- Keep Blood Moon and Resource Rush boundary behavior unchanged
- Keep ordinary boss aggression native. A hit must not open a confirmation GUI or start a hosted lobby
- Keep Boss Raid only as participant and reward record ownership
- Do not implement multiplayer boss scaling in this update
- Do not add or run automated tests, test fixtures, Gradle tests, linters, formatters, or static-analysis jobs
- Build only the required production artifacts after source changes are complete
- Do not launch the Test client unless the user explicitly requests that launch in the implementation session. Start or restart only the ordinary local server when artifact installation or Pack Sync publication requires it
- Use the Registry Atlas workflow for every gameplay change
- Use native JEM Pack Sync for every managed client addition, replacement, or deletion
- Keep every category assignment in one checked-in manifest. Runtime code must not infer categories
- Make settlement work event-driven and finite. Do not solve load by abandoning work, delaying it across ticks, or imposing a per-tick work budget

## Phase 0: Preflight and ownership

- [ ] Run `python tools/jem.py status` from the repository root
- [ ] Fetch `origin` and confirm `develop` divergence without changing unrelated local work
- [ ] Inspect tracked, untracked, and staged files and preserve all `Server/config` runtime dirt
- [ ] Run `python Catalog/generator/maintain_atlas.py scan` before source edits
- [ ] Query the existing Atlas behavior and impact for Tide fish, Shipping Box, structural bosses, spawn eggs, spawners, JEI categories, Locator Bar, Particle Rain, villagers, beds, job sites, and storage
- [ ] Record one Atlas change entry per independent behavior owner, then update the same entries through implementation

## Phase 1: Repair the confirmed client crash causes

### 1.1 Stop the Modefite log and reflection storm

Confirmed cause: Modefite 1.0.3 calls `MathUtils.initializeFields()` from JEI item rendering. The method looks for unobfuscated `GlobalPos` fields, then mapped fields `field_25064` and `field_25065`. Both lookups fail in this runtime. `fieldsInitialized` stays false, so every render repeats reflection and prints a full stack trace. The last client log reached 1.54 GB and contained 131,962 failed lookups and more than 6.6 million MathUtils stack lines

Files:

- Create `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/mixin/client/modefite/ModefiteMathUtilsMixin.java`
- Modify the JEMCompat client mixin configuration
- Modify `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/client/jei/CuratedJeiOverlay.java`

Actions:

- [ ] Add an optional client Mixin for `timmychips.modefiteitemdefinitions.comp.util.MathUtils`
- [ ] Inject at the head of static `getPos(GlobalPos)` and return `globalPos == null ? null : globalPos.pos()`
- [ ] Inject at the head of static `getDimension(GlobalPos)` and return `globalPos == null ? null : globalPos.dimension()`
- [ ] Make both injections cancellable so `initializeFields()` is never reached through these public methods
- [ ] Keep Modefite installed because current resource packs use its modern item-definition support
- [ ] Replace the curated JEI toggle's dynamic compass `ItemStack` rendering with a static JEM texture or direct sprite already owned by the overlay
- [ ] If the optional Mixin cannot apply, write one bounded warning for the session. Never print a per-frame stack trace

Acceptance:

- Modefite keeps working for resource-pack item definitions
- Rendering the JEI toggle does not call Modefite MathUtils
- No `Failed to find GlobalPos` message repeats in the client log

### 1.2 Prevent the local server from exhausting the client's native memory

Confirmed cause: the local server was allowed to reserve a 12 GB Java heap while the client was allowed 6 GB on a 16 GB machine. At the fatal client stop, physical memory had 14 MB free and the page file had 34 MB available. Four Embeddium chunk workers then failed native allocations. The client heap itself was below its 6 GB maximum, so increasing the client heap is not the fix

Files:

- Modify the canonical local Test server JVM-argument owner documented by `Docs/DEVELOPMENT.md`
- Do not modify MineRENT JVM settings

Actions:

- [ ] Set the ordinary local Test server to `-Xms2G -Xmx5G -Xss1M`
- [ ] Keep the Test client at `-Xmx6144M`
- [ ] Document that these numbers apply only to the 16 GB local Test workstation
- [ ] Do not add forced periodic garbage collection

Acceptance:

- The local server cannot reserve 12 GB while the Test client is running
- At least several gigabytes remain available for Embeddium, shaders, Voxy, the OS, and native libraries

### 1.3 Make Voxy and VSS state coherent

Confirmed state: Neo Voxy rendering was disabled, but raw Voxy ingestion and VSS LOD receipt remained enabled. This kept background services and server LOD ingestion active without an enabled renderer

Files:

- Modify `Client/ManagedPack/config/vss-client-config.json`
- Modify the canonical JEMCompat client compatibility source for optional Neo Voxy
- Modify local Test Neo Voxy configuration only through its native configuration owner during installation

Actions:

- [ ] When Neo Voxy rendering is disabled, force VSS `receiveServerLods=false` and Voxy `ingest_enabled=false`
- [ ] When Neo Voxy rendering is enabled, use 32 client LOD chunks, 2 service threads, prediction disabled, and map bridge disabled
- [ ] On world unload, server change, dimension change, Pack Sync generation replacement, and shader reload, cancel obsolete Voxy and Embeddium chunk work and release completed buffers that no renderer can consume
- [ ] Sample `me.jellysquid.mods.sodium.client.util.NativeBuffer.getTotalAllocated()` once every 10 seconds while a world is loaded
- [ ] Log only threshold transitions at 512 MB, 1 GB, and 2 GB. Do not log every sample
- [ ] Remove the sampler on disconnect and never keep world references in it

Acceptance:

- Disabled Voxy means no VSS LOD receipt and no raw Voxy ingestion
- Enabled Voxy uses one coherent low-memory configuration
- Native-buffer diagnostics are bounded and do not create another log storm

## Phase 2: Rebalance all Tide fish and Shipping Box prices

### 2.1 Create one exact Tide fish economy manifest

Files:

- Create `Mods/JEMEconomy/src/main/resources/data/jem_economy/economy/tide_fish_values.json`
- Modify `Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/policy/ItemPolicyRegistry.java`
- Modify the JEM Economy sale-quote and sale-consumption owners
- Modify `Mods/JEMEconomy/src/main/resources/data/jem_economy/economy/item_policies.json`

Actions:

- [ ] Load `tide_fish_values.json` once during the existing economy resource reload
- [ ] Require exactly 101 unique registered `tide:*` fish item IDs
- [ ] Require one of `ORDINARY`, `RARE`, `MYTHIC`, or `LEGENDARY` for every row
- [ ] Require exactly 39 Ordinary, 25 Rare, 20 Mythic, and 17 Legendary rows
- [ ] Fail reload on duplicates, missing fish, unknown fish, invalid sizes, or invalid prices
- [ ] Store base values in whole Copper units only
- [ ] Use these base values: Ordinary 10 Copper, Rare 100 Copper, Mythic 500 Copper, Legendary 10,000 Copper
- [ ] Read the native Tide average as `(typical_low_cm + typical_high_cm) / 2`
- [ ] Read the native Tide `record_high_cm`
- [ ] Copy the exact assignments from the fish appendix in this plan

### 2.2 Apply the length bonus without fractional currency

Use this formula for each individual fish stack:

```text
base = tier base value in Copper
average = native Tide average length
record = native Tide record length
length = FishLength NBT
maxBonus = max(1 Copper, floor(base * 0.01))
progress = clamp((length - average) / (record - average), 0, 1)
lengthBonus = floor(maxBonus * progress)
finalValue = base + lengthBonus
```

Exact maximum bonuses:

- Ordinary: 1 Copper
- Rare: 1 Copper
- Mythic: 5 Copper
- Legendary: 100 Copper, which is 1 Gold

Actions:

- [ ] Give no discount below the native average length
- [ ] Clamp fish longer than the native record to the maximum bonus
- [ ] Treat a missing or invalid `FishLength` as the native average for price only
- [ ] Keep the existing canonical-stack validation for invalid NBT
- [ ] Quote and consume the exact same `ItemStack` NBT
- [ ] Remove the current rule that lets a quoted longer fish match and consume a shorter fish of the same species
- [ ] Keep all arithmetic in checked `long` Copper units

Safety result: fractional Copper cannot exist. The minimum maximum bonus is 1 Copper. This formula cannot produce a null value, divide by zero because every listed record is above its average, or create a price below the base

Acceptance:

- Every Tide fish has one tier and one base value
- A creative-tab/default-length fish sells for its base value
- A fish at or below average sells for its base value
- A record-length Legendary fish sells for 1 Arcane plus 1 Gold
- The Shipping Box cannot quote one fish length and consume another

## Phase 3: Add Shipping Box "Deposit All"

Files:

- Modify `Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/shipping/ShippingBoxMenu.java`
- Modify `Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/full/client/ShippingBoxScreen.java`
- Modify `Mods/JEMEconomy/src/main/resources/assets/jem_economy/lang/en_us.json`
- Modify `Mods/JEMEconomy/src/main/resources/assets/jem_economy/lang/ru_ru.json`

Actions:

- [ ] Reserve menu button ID 4 for "Deposit All"
- [ ] Accept ID 4 only from the player whose active menu is this valid Shipping Box, on sale tab 0, while inventory is unlocked and no payout lock is active
- [ ] Iterate player menu slots 63 through the last slot exactly once
- [ ] For each stack, call the existing `ItemPolicyRegistry.acceptsShipping` policy
- [ ] Move accepted items into input slots 0 through 53 with the existing merge rules
- [ ] Preserve NBT, capabilities, partial stacks, and player-slot leftovers
- [ ] Leave rejected items, coins, unsellable items, and stacks that do not fit in the player inventory
- [ ] Never sell automatically and never collect payout from this action
- [ ] Mark changed slots and the block entity, then broadcast menu changes once after the transfer
- [ ] Add the sale-tab button at x 190, y 111, width 164, height 22
- [ ] Keep Sell or Collect at x 190, y 137, width 164, height 22
- [ ] Use English "Deposit All" and Russian "Переложить всё"
- [ ] Add a tooltip that says the button moves only sellable goods that fit
- [ ] Disable the button while payout is pending, sale inventory is locked, the session is invalid, or the exchange tab is open

Acceptance:

- One press moves every accepted stack that fits
- Rejected and remaining items stay in their original slots
- The server owns the result and the client does not predict inventory changes

## Phase 4: Remove hosted boss groups and ordinary boss barriers

Files:

- Modify or delete ordinary hosted behavior from `Mods/JEMWorldBossTiers/src/main/java/com/siirio/jemworldbosstiers/encounter/HostedEncounters.java`
- Delete ordinary ownership from `HostedGroup.java`, `HostedLobby.java`, and `HostedEncounterApi.java`
- Modify `Mods/JEMServer` structure staging, boundary, teleport, participant, and reward owners
- Modify the boss boundary client renderer and its mixins
- Preserve Blood Moon and Resource Rush boundary classes and data

Actions:

- [ ] Remove ordinary Boss Fight, Campaign, and Boss Raid group creation
- [ ] Remove invite, ready, lobby, retry, confirmation, first-hit admission, and hosted-start state for ordinary bosses
- [ ] Remove visible walls, physical collision, teleport restrictions, containment, player return, safe-exit portals, and nested arena ownership for ordinary bosses
- [ ] Remove participant-only targeting and damage gates for ordinary bosses
- [ ] Let all registered structural bosses use normal native aggression and combat immediately
- [ ] Keep structure identity and boss revival/placement repair only where they are required to find and restore the registered boss
- [ ] Bind revival only to the exact structure-owned boss UUID and registered arena record. Never adopt or revive a boss spawned manually, by a tomb, by a spawn egg, or outside its registered structure
- [ ] Keep Boss Raid event participant records and reward records only
- [ ] Keep Campaign progression records only if another campaign system consumes them. Do not use them to host combat
- [ ] Remove dead packets, saved fields, commands, GUI routes, and client render state that only supported ordinary hosted groups or barriers
- [ ] Add tolerant one-time migration that ignores old hosted lobby, group, attempt, and barrier fields without retaining active behavior
- [ ] Keep Blood Moon territory, spectator, target-exclusion, and reward behavior unchanged
- [ ] Keep Resource Rush territory and reward behavior unchanged
- [ ] Move future multiplayer boss scaling to `Docs/LONG_TERM_IDEAS.md`
- [ ] Update conflicting boss logic locks and owning event documentation so they no longer require restored ordinary walls

Acceptance:

- A structural boss attacks and can be attacked through native behavior
- No ordinary boss creates a group, lobby, barrier, portal, or teleport block
- Overlapping boss structures cannot nest JEM barriers
- Boss Raid still knows who participated and who receives rewards
- A manually or tomb-spawned Maledictus disappears after `/kill` and is not recreated by structure repair
- Blood Moon and Resource Rush remain unchanged

## Phase 5: Extract craftable spawn eggs and remove mineable spawners from both packs

Files:

- Create `Mods/JEMCraftableSpawnEggs` as an independent Forge 1.20.1 source project
- Move the current JEMCompat `feature/spawner` source, 430 recipes, 25 fallbacks, policy, and NBT rules into that project
- Remove the moved feature from JEMCompat source and artifact
- Remove `mineable-spawner-v2.4.2+1.16-1.20.1.jar` from `Client/ManagedPack/mods`
- Remove the matching JAR from `Server/mods`

Actions:

- [ ] Give the extracted project its own mod ID, metadata, version, package root, mixin configuration if required, and README
- [ ] Preserve current multi-mod recipe and NBT behavior in source
- [ ] Do not install the new JAR in either pack
- [ ] Remove the third-party mineable-spawner JAR from both packs without deleting its external source or historical records
- [ ] Remove recipes and data that become unreachable only because the feature JAR is excluded
- [ ] Update pack manifests and Atlas artifact ownership

Acceptance:

- Spawn-egg source remains buildable as an independent future mod
- Neither client nor server pack contains craftable spawn eggs or the mineable-spawner mod
- JEMCompat no longer packages the extracted feature

## Phase 6: Rebuild the JEI catalog from actual registered items

### 6.1 Correct the registry source

Files:

- Modify `Catalog/generator` item extraction so model paths, translation keys, render variants, descriptions, and recipe IDs cannot become item registry IDs
- Modify `tools/generate_jei_catalog.py`
- Replace `Mods/JEMCompat/src/main/resources/assets/jemcompat/jei_catalog.json`
- Modify `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/catalog/JemCatalogCategories.java`
- Modify JEI category navigation and icons
- Modify `Docs/catalog/JEI_CATALOG_MANIFEST.md`

Actions:

- [ ] Add a temporary development-only `/jemcatalog export` command that iterates `ForgeRegistries.ITEMS` on the logical server and writes sorted registry IDs with namespace, description ID, item tags, block-item identity, food property, armor slot, and class name
- [ ] Use that export once against the exact Test pack to establish the registered-item universe
- [ ] Remove the command before the production artifact is built
- [ ] Reject every manifest ID not present in the export
- [ ] Reject duplicate IDs, parent assignments, missing registered IDs, and IDs in more than one leaf
- [ ] Do not preserve the current 17,160-row polluted manifest as truth
- [ ] Keep the current manifest only as a migration hint for valid registered IDs
- [ ] Apply the complete assignment contract at the end of this plan in its exact priority order
- [ ] Check in the final sorted manifest
- [ ] After the manifest exists, make update mode preserve every existing assignment and add only newly registered IDs to `OTHER`

### 6.2 Use direct lookup only

- [ ] Load the checked-in JSON once at JEMCompat resource initialization
- [ ] Build one immutable `ResourceLocation -> leaf` map
- [ ] Classify each JEI stack by its registered item ID in constant time
- [ ] Never scan recipes, tags, names, classes, models, or block state during JEI startup
- [ ] Never regenerate or rewrite the manifest during a normal client start
- [ ] Put a newly installed item missing from the manifest in `OTHER` and log its ID once
- [ ] Keep JEM Economy building and nature admission on the same manifest result

### 6.3 Add the required leaf structure

Navigation parents never own items. Use these leaves:

- Weapons: Totems, Ranged, Melee, Training
- Wearables
- Food: Edibles, Cooking Equipment
- Ores
- Create and Redstone
- Building: Furniture, Nature, Containers, Holiday, Building Blocks, Glass, Light
- Fishing
- Books
- Potions
- Transport
- Spawn Eggs
- Other

### 6.4 Fix curated and original JEI mode separation

- [ ] When original JEI is active, set the curated search box invisible, inactive, and unfocused
- [ ] Remove it from narration, keyboard input, mouse input, and rendering in original JEI mode
- [ ] Restore it only when curated JEI becomes active
- [ ] Keep the original JEI search field and controls unchanged

Acceptance:

- Adding a category or subcategory never moves an existing item
- The manifest contains actual registered items only
- Every registered item has exactly one leaf
- A normal start parses the file once and does no category calculation
- Original JEI never shows or receives input through the curated search field

## Phase 7: Put Locator Bar above the HUD

Files:

- Modify the canonical managed Locator Bar configuration under `Client/ManagedPack/config`
- Modify its source adapter only if the installed config cannot express the placement

Actions:

- [ ] Select the CLASSIC placement above the hotbar and experience HUD
- [ ] Remove the REWORKED center-screen placement that overlaps Jade
- [ ] Keep Jade's center-screen space clear
- [ ] Do not modify generated `Server/config` runtime copies

Acceptance:

- Locator Bar is above the lower HUD
- Locator Bar does not overlap Jade in the screen center

## Phase 8: Correct Particle Rain biome coverage

Files:

- Modify the managed Particle Rain resource configuration under `Client/ManagedPack`
- Modify Cool Rain compatibility only where it owns weather rendering precedence

Actions:

- [ ] Build exact biome selectors from every installed biome registry ID and tag
- [ ] Assign normal rain only to biomes whose precipitation is rain
- [ ] Assign snow only to snowy, frozen, and native snow-precipitation biomes
- [ ] Assign sandstorm particles to dry sandy and desert biomes
- [ ] Assign red-sandstorm particles to badlands and red-sand biomes
- [ ] Assign no water rain to biomes whose native precipitation is none
- [ ] Put explicit badlands and vanilla desert entries after broad climate entries so they cannot be overridden
- [ ] Set a conservative canonical particle cap and distance. Use 600 particles, storm multiplier 1.0, and range 64 blocks
- [ ] Keep weather state as the trigger. Do not create permanent biome particles in clear weather

Acceptance:

- Vanilla desert never renders water rain
- Badlands renders red sand in rain or thunder weather
- Sandy desert biomes render normal sand
- Frozen and snowy biomes render snow
- Wet biomes render rain

## Phase 9: Perform the settlement and villager rewrite last

Confirmed defects:

- Family runtime scans and sorts all loaded entities every five seconds
- Home inspection depends on villager memories and falls back to a small block scan that misses valid structure furniture
- Uniqueness checks scan family records repeatedly
- Amenity, room, comfort, animal, guard, and threat services repeat region or town scans
- Recruit equipment enforcement runs on every living tick and reports an inventory change even when nothing changed
- Client complaint rendering repeatedly resolves, translates, wraps, and draws a large retained notice set

The fatal client crash was native-memory exhaustion and a Modefite log storm. Do not claim settlements caused that fatal stop. Settlements still have an independent CPU, allocation, and FPS defect

### 9.1 Replace recurring scans with lifecycle indexes

Files:

- Modify the owning JEM Settlements family, home, observation, comfort, room, threat, complaint, recruit, and town services
- Add one per-dimension settlement spatial index and one saved ownership map

Actions:

- [ ] Delete recurring `level.getAllEntities()` family scans
- [ ] Maintain loaded family actors on entity join, leave, death, chunk load, and chunk unload
- [ ] Index native bed and workstation POIs by dimension and chunk section
- [ ] Index loaded storage block entities by dimension and chunk section
- [ ] Update block indexes on chunk load, chunk unload, POI add, POI remove, block place, block break, and relevant block-state change
- [ ] Use `GlobalPos -> resident UUID` maps for bed, job-site, and storage ownership
- [ ] On resident creation or invalid assignment, resolve valid HOME and JOB_SITE memories first
- [ ] If memory is absent or invalid, query the nearest unclaimed indexed bed or workstation within the existing home-association radius
- [ ] Associate storage with the nearest unclaimed indexed vanilla chest or tagged/capability inventory in the same POI house cluster
- [ ] Recheck only the resident and changed position when an indexed object changes
- [ ] Complete each lookup directly. Do not queue it, slice it, abandon it, or return "unknown" because of a budget

### 9.2 Delete expensive derived simulations

- [ ] Remove `AmenityScanService` region traversal
- [ ] Remove `ResidentialRooms` flood fill and its 65,536-state job storage
- [ ] Remove `ThreatAssessment` custom collision and path search
- [ ] Replace room and crowding calculations with assigned-bed counts in the same POI house cluster
- [ ] Replace fear calculations with native panic, target, raid, recent-damage, and hurt signals
- [ ] Maintain amenity and comfort counters from exact block-change events and a one-time loaded-chunk palette/block-entity index
- [ ] Never rescan a settled region to refresh unchanged values

### 9.3 Remove per-tick resident and recruit work

- [ ] Run recruit equipment policy only on entity join, inventory mutation, equipment-slot change, and menu close
- [ ] Call `onInventoryChanged()` only when the policy changed an item
- [ ] Register and unregister animals and guards on lifecycle and territory-entry changes
- [ ] Use the spatial town index instead of looping all towns for each living entity
- [ ] Emit complaint packets only when complaint state changes and for one long reminder while unresolved
- [ ] Pretranslate and prewrap client complaint lines when the packet arrives
- [ ] Render at most the four nearest visible notices
- [ ] Remove resolved, unloaded, and expired notices immediately

Acceptance:

- A villager in a generated structure recognizes its present bed, workstation, and chest
- Recognition does not depend on a recurring block scan
- Settlements do no all-entity scan, all-town living-tick loop, room flood fill, or unchanged-region rescan
- Work completes from exact events and indexed lookup, not from per-tick budgets
- Standing near villagers does not create repeated large allocations or one-FPS freezes

## Phase 10: Documentation, Atlas, packaging, and delivery

- [ ] Update `CHANGELOG.md` with each user decision and each evidence-backed root cause
- [ ] Keep crash and gameplay repairs `in testing` until user runtime acceptance
- [ ] Update `Docs/DEVELOPMENT.md`, owning boss event docs, economy docs, JEI manifest docs, settlement docs, and server commands
- [ ] Add or update Atlas change records for fish economy, Shipping Box transfer, boss lifecycle, spawn-egg extraction, pack removal, JEI categories, weather presentation, and settlements
- [ ] Run Atlas rebuild, audit, scan, and validation after all owning source changes
- [ ] Build only the required production artifacts without tests
- [ ] Synchronize exact fresh artifacts to ordinary local `Server` and `Client/ManagedPack`
- [ ] Run `python tools/jem.py stage-pack-sync`
- [ ] Inspect native `/jempack preview`
- [ ] Publish through native `/jempack publish`
- [ ] Confirm staged and published manifest hashes match with `python tools/jem.py status`
- [ ] Apply the published generation to the closed `Test JustEnoughMods` profile through native AutoModpack
- [ ] Confirm the active token, managed JAR hashes, deletions, and absence of a pending transaction
- [ ] Inspect the packaged JEM Economy artifact and confirm every coin and Shipping Box texture has a valid PNG signature
- [ ] Preserve the current JEM Building hammer flow that places temporary construction blocks after area selection
- [ ] Stage only exact task paths, commit, push `develop`, and confirm `origin/develop` points to the delivered commit

## Appendix A: Exact Tide fish assignments

The split preserves Tide's native rarity order. Inside one native rarity, lower native selection weight receives the higher JEM tier. Registry ID is the final tie-breaker

### Legendary: 17 at 10,000 Copper

- `tide:alpha_fish`
- `tide:blazing_swordfish`
- `tide:bull_shark`
- `tide:coelacanth`
- `tide:darkness_eater`
- `tide:devils_hole_pupfish`
- `tide:dragon_fish`
- `tide:great_white_shark`
- `tide:marstilus`
- `tide:midas_fish`
- `tide:mooneye`
- `tide:saturn_cuttlefish`
- `tide:shadow_shark`
- `tide:shooting_starfish`
- `tide:sun_emblem`
- `tide:uranias_pisces`
- `tide:voidseeker`

### Mythic: 20 at 500 Copper

- `tide:aquathorn`
- `tide:arapaima`
- `tide:bedrock_tetra`
- `tide:blossom_bass`
- `tide:chasm_eel`
- `tide:echo_snapper`
- `tide:frostbite_flounder`
- `tide:mahi_mahi`
- `tide:manta_ray`
- `tide:mantyvern`
- `tide:mirage_catfish`
- `tide:neptune_koi`
- `tide:pluto_snail`
- `tide:sailfish`
- `tide:sand_tiger_shark`
- `tide:slimy_salmon`
- `tide:snatcher_squid`
- `tide:spore_stalker`
- `tide:sturgeon`
- `tide:swordfish`

### Rare: 25 at 100 Copper

- `tide:abyss_angler`
- `tide:angelfish`
- `tide:anglerfish`
- `tide:catfish`
- `tide:crimson_fangjaw`
- `tide:crystal_shrimp`
- `tide:crystalline_carp`
- `tide:dripstone_darter`
- `tide:dutchman_sock`
- `tide:elytrout`
- `tide:gilded_minnow`
- `tide:guppy`
- `tide:inferno_guppy`
- `tide:iron_tetra`
- `tide:lapis_lanternfish`
- `tide:luminescent_jellyfish`
- `tide:nephrosilu`
- `tide:pentapus`
- `tide:red_40`
- `tide:soulscale`
- `tide:vengeance`
- `tide:volcano_tuna`
- `tide:walleye`
- `tide:warped_guppy`
- `tide:witherfin`

### Ordinary: 39 at 10 Copper

- `tide:amber_rockfish`
- `tide:anchovy`
- `tide:ash_perch`
- `tide:bedrock_bug`
- `tide:black_crappie`
- `tide:blue_neonfish`
- `tide:bluegill`
- `tide:brook_trout`
- `tide:carp`
- `tide:cave_crawler`
- `tide:cave_eel`
- `tide:chorus_cod`
- `tide:deep_blue`
- `tide:deep_grouper`
- `tide:ember_koi`
- `tide:ender_glider`
- `tide:enderfin`
- `tide:endergazer`
- `tide:flounder`
- `tide:glowfish`
- `tide:incandescent_larva`
- `tide:judgment_fish`
- `tide:largemouth_bass`
- `tide:mackerel`
- `tide:magma_mackerel`
- `tide:obsidian_pike`
- `tide:ocean_perch`
- `tide:pale_clubfish`
- `tide:pike`
- `tide:rainbow_trout`
- `tide:red_snapper`
- `tide:shadow_snapper`
- `tide:sleepy_carp`
- `tide:smallmouth_bass`
- `tide:snook`
- `tide:tuna`
- `tide:violet_carp`
- `tide:white_crappie`
- `tide:yellow_perch`

## Appendix B: Complete JEI assignment contract

Apply this contract offline to the exact registered-item export. The output is the manifest. Runtime code must never execute these selectors

### Assignment priority

Apply the following rows from top to bottom. The first matching row owns the item unless a later row names an explicit exact-ID exception. Emit the final exact IDs into the manifest and discard selector logic from runtime

1. Exact exceptions in the table below
2. Spawn Eggs
3. Weapon Totems
4. Weapon Training
5. Melee Weapons
6. Ranged Weapons
7. Wearables
8. Food Edibles
9. Food Cooking Equipment
10. Fishing
11. Transport
12. Containers
13. Potions
14. Books
15. Ores
16. Create and Redstone
17. Furniture
18. Nature
19. Glass
20. Light
21. Holiday
22. Building Blocks
23. Other

### Exact exceptions and requested families

| Registered item or exact family | Target leaf | Exact decision |
|---|---|---|
| `companions:book_ice_shard` | Wearables | The companion book is wearable, not Other or Books |
| `companions:hourglass` and every registered item explicitly named sand clock or hourglass | Other | Utility time objects stay in Other |
| `legendary_monsters:totem_of_moss` | Other | Moss object is not a combat totem category member |
| `legendary_monsters:eye_of_moss` | Other | Moss object is not Building or Nature |
| `spawn:ant_crystal`, `spawn:moonshine`, `spawn:moonstone` | Other | Remove these exact items from Ores |
| Every valid registered item in namespace `moretotems` whose ID is a totem item | Weapon Totems | Include all More Totems variants |
| `skarrier_mobs:totem_of_extinction`, `skarrier_mobs:totem_of_freezing`, `skarrier_mobs:totem_of_melding`, `skarrier_mobs:totem_of_repulsion`, `skarrier_mobs:totem_of_seething` | Weapon Totems | Include all real Skarrier totems and exclude description or model IDs |
| Every registered item whose exact item name is spear | Melee Weapons | Spears are melee even when the mod also has a thrown render model |
| Every registered item whose exact item name is javelin or trident | Ranged Weapons | Do not move these with spears |
| Every Villager Recruits training block item | Weapon Training | Training blocks receive their own weapon leaf |
| Every registered white-doll wearable item | Wearables | Dolls worn by the player are not food |
| Every player head, mob head, skull, mask, crown, hat, helmet, chestplate, leggings, boots, cape, backpack, quiver, Curios item, and native wearable | Wearables | Use actual registered items and wearable APIs or tags |
| Every raw meat, cooked meat, fruit, vegetable, meal, drink, edible crop, and item with native food properties | Food Edibles | Raw meat and fruits leave Ores and Other |
| Every cooking furnace, oven, stove, grill, cutting board, millstone, cooking pot, cauldron used for recipes, brewery machine, kitchen worktop, food container used in preparation, and registered cooking station | Food Cooking Equipment | Equipment must actively prepare, process, or store food ingredients |
| Every decorative Beach Party block with no cooking or food-processing function | Furniture | Beach Party decorations are not Food |
| Every Let's Do and Kaleidoscope container that prepares or stores food ingredients | Food Cooking Equipment | Cooking function overrides generic Containers |
| Every fishing rod, hook, line, bobber, bait, fish finder, fishing journal, fish item, live-fish bucket, and other fishing-specific tool | Fishing | Rods leave Ores and fish and fish buckets leave Other |
| Empty buckets and general-purpose fluid buckets | Fishing | Follow the user's bucket placement decision |
| Shulker boxes of every color and variant | Containers | Shulkers leave Other |
| Bundles of every color and variant | Containers | Bundles leave Other |
| Chests, barrels, crates, sacks, boxes, drawers, cabinets with inventory, and portable non-wearable storage | Containers | Use actual inventory capability or known container block entity |
| Snowy Spirit sleds and all installed sled variants | Transport | Sleds leave Other |
| Every Small Ships ship, rowboat, galley, cog, dhow, drakkar, brigg, and war galley | Transport | Ship transport leaves Other |
| Every vanilla or modded boat, minecart, wagon, carriage, aircraft, mount-control item, and placed vehicle item | Transport | Transport function overrides building material |
| Every nature block such as leaves, logs, saplings, flowers, grass, coral, natural soil, and unprocessed natural growth | Nature | Nature blocks leave Other |
| Every real ore block and raw mineral, ingot, nugget, gem, dust, crystal, and processed metal resource | Ores | The three exact Spawn exceptions remain Other |
| Create machines, components, belts, shafts, cogs, kinetic blocks, redstone controls, circuitry, and automation parts | Create and Redstone | Classify Create by function and keep foods and transport in their higher-priority leaves |
| Chairs, tables, desks, counters, shelves, decorative cabinets without inventory, cushions, sheets, sofas, benches, and ordinary interior decoration | Furniture | Preserve the accepted Handcrafted and furniture-family decisions |
| Doors, trapdoors, fences, walls, slabs, stairs, bricks, planks, structural stone, roofs, and ordinary construction blocks | Building Blocks | Use only when no higher functional leaf applies |
| Glass blocks, panes, stained glass, framed glass, and transparent construction glass | Glass | Glass overrides generic Building Blocks |
| Lamps, lanterns, torches, candles, light blocks, chandeliers, and dedicated illumination items | Light | Light overrides Furniture and Building Blocks |
| Christmas, Halloween, Easter, and other explicit seasonal decoration | Holiday | Holiday overrides Furniture and Building Blocks |
| Potions, splash potions, lingering potions, elixirs, and dedicated bottled status-effect consumables | Potions | Ordinary food drinks remain Food Edibles |
| Written books, guide books, manuals, journals that are not fishing-specific, spell books, and readable documents | Books | `companions:book_ice_shard` remains Wearables |
| All real spawn egg items | Spawn Eggs | Description keys, recipes, models, and creative-tab labels are not items |

### Complete residual classification

After the exact rows, enumerate every remaining registered item with these offline signals in the same priority order:

- Use Forge and common item tags only when the tag's meaning is exact for the target leaf
- Use native item interfaces and capabilities for armor, Curios wearables, food properties, fluid containers, inventory containers, vehicles, fishing rods, spawn eggs, and block items
- Use the registered block's actual block entity and recipe role for containers and cooking equipment
- Use installed recipe types to identify cooking stations and food-processing machines
- Use actual item IDs and translation display names only to select an already specified exact family such as spear, sled, chair, or lamp
- Never treat a model path, texture, localization key, tooltip key, recipe ID, creative-tab ID, block-state variant, render-state suffix, or description suffix as an item
- Assign any still-unmatched registered item to Other
- Write a review report grouped by final leaf and namespace, then check the exact final IDs into `jei_catalog.json`
- Store no selector, classifier, recipe scan, or family inference in production runtime code

This appendix is the complete category decision. The implementation session performs enumeration and validation only
