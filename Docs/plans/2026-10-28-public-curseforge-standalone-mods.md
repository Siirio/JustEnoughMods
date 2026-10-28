# Public CurseForge Standalone Mods Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prepare eleven independently installable Forge 1.20.1 projects for CurseForge without requiring JustEnoughMods, TLauncher, AutoModpack, MineRENT, private files, or the maintainer's folder layout

**Architecture:** Existing standalone modules remain the canonical owners where they already exist. Features still owned by `JEMCompat` move into focused modules with preserved registry, save, configuration, and network identities, then their old handlers are removed from `JEMCompat` to prevent duplicate behavior. Every project receives its own reproducible build, metadata, documentation, assets, dependency declarations, and release gate

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, Gradle, CurseForge project metadata

**Spec:** User-approved public standalone scope from 2026-10-28, including the corrected dependency and credit policy captured below

**Future scope:** JEM Serene Seasons Compatibility is recorded as a separate multi-mod compatibility candidate after the frozen eleven. It is planning-only until its own extraction and release gate are approved

## Global Constraints

- Public projects: JEM Animal Hints, JEM Drill Durability, JEM Mob Home, JEM Excavation, JEM Freecam, JEM Food Blessings, JEM Seasons HUD, JEM Inventory Shortcuts, In Chat FTB, JEM Adaptive Culling, and Unofficial Luxium Compatibility
- Forge is required for every project, with exact additional required and optional dependencies declared per project
- JEM Animal Hints, JEM Drill Durability, In Chat FTB, JEM Seasons HUD, and JEM Adaptive Culling list their upstream integrations as dependencies, not as credit obligations
- JEM Drill Durability is presented as a Create addon
- Unofficial Luxium Compatibility alone carries the explicit unofficial disclaimer and Vinlanx attribution required by this plan
- Never bundle, mirror, modify, or redistribute Luxium code, shaders, assets, branding, or JAR content, users obtain the official unmodified Luxium separately
- Preserve existing registry IDs, enchantment IDs, NBT, saved data, configuration keys, translations, network compatibility, and safe migrations wherever a feature already has players or worlds
- Remove extracted handlers from `JEMCompat` only after the replacement project owns the same behavior and JEM distributions reference the new JAR
- Public builds must not read `Client/ManagedPack`, TLauncher profiles, environment-specific game directories, `Catalog/changes`, or private runtime files
- Public JARs contain no third-party JARs, credentials, world data, private configuration, generated runtime caches, or assets without redistribution permission
- Repository policy forbids automated tests and test fixtures, verification therefore uses production builds, JAR inspection, clean-instance startup checks, and explicitly authorized manual gameplay review
- CurseForge upload, publication, and moderation submission require a separate explicit user authorization after the relevant release gate passes

## Review Focus

- Existing JEM worlds must retain saved state and registered identities after replacing an old module with its standalone owner
- A standalone project must start cleanly when every optional dependency is absent
- Installing the standalone project beside a transitional JEM build must not register duplicate events, keybinds, packets, tooltips, or registries
- Client-only projects must not be required by a dedicated server, while shared gameplay projects must reject incompatible network versions clearly
- Public source checkouts must build without any local Minecraft profile or private repository data

---

### Task 1: Freeze the public release matrix

**Files:**
- Create: `Docs/public-mods/release-matrix.md`
- Modify: `tools/module-inventory.json`
- Modify: `Docs/DEVELOPMENT.md`

**Interfaces:**
- Produces: one canonical row per project containing public name, mod ID, owner module, side, required dependencies, optional dependencies, supported mods, preserved identities, and migration source

- [ ] Record exactly the eleven approved public projects and no additional candidates
- [ ] Mark the five corrected projects as dependency-only attribution cases
- [ ] Mark Unofficial Luxium Compatibility as the only project with the special unofficial and Vinlanx credit block
- [ ] Record whether each project is client-only, both-sided, or server-capable before source extraction starts
- [ ] Record the exact upstream and content mods each project supports without treating optional compatibility as a required dependency
- [ ] Review the matrix against installed mod metadata and the current module inventory
- [ ] Commit the release matrix independently

### Task 2: Establish the reusable public-project structure

**Files:**
- Create per project: `Mods/<Module>/gradle/wrapper/*`
- Create per project: `Mods/<Module>/gradlew`, `Mods/<Module>/gradlew.bat`
- Modify per project: `Mods/<Module>/settings.gradle`
- Modify per project: `Mods/<Module>/build.gradle`
- Create per project: `Mods/<Module>/LICENSE`
- Create per project: `Mods/<Module>/README.md`
- Create per project: `Mods/<Module>/CHANGELOG.md`

**Interfaces:**
- Produces: `gradlew clean build` from each module's own clean checkout without repository-private paths

- [ ] Define a common Forge 1.20.1 and Java 17 project convention without coupling module builds together
- [ ] Replace local `files(...)` dependencies with reproducible Maven or Curse Maven coordinates where public artifacts exist
- [ ] Keep optional integrations compile-time optional and runtime guarded
- [ ] Give every project an explicit license covering only JEM-owned code and assets
- [ ] Ensure generated JAR metadata declares the exact side and dependency ranges
- [ ] Verify each empty public scaffold with its own production build before feature migration
- [ ] Commit the reusable structure separately from gameplay extraction

### Task 3: Extract JEM Animal Hints

**Files:**
- Create: `Mods/JEMAnimalHints/`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/compat/jade/BreedingFoodCatalog.java`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/compat/jade/BreedingProvider.java`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/compat/jade/TamingCatalog.java`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/compat/jade/TamingDataLoader.java`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/compat/jade/TamingProvider.java`
- Modify: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/compat/jade/JemJadePlugin.java`

**Interfaces:**
- Consumes: Jade as a required dependency
- Produces: taming, breeding, rotating food, and genuine mountability Jade lines without JEM or Mob Home

- [ ] Separate animal hints from all boss-help providers that remain in `JEMCompat`
- [ ] Preserve datapack extension behavior and verified mod-specific adapters
- [ ] Before public release, audit every installed tameable and mountable species missing from the curated catalog, starting with Barnyard Buddies and Blast From The Past, then add only source-proven taming items and genuine riding paths
- [ ] Add `Mountable` and `Можно оседлать` only when the species has a real supported riding path
- [ ] Keep tameable and mountable classification independent
- [ ] Verify vanilla animals, one supported modded animal per adapter, a tameable non-mount, and an unsupported entity
- [ ] Remove only the replaced animal registrations from the old Jade plugin
- [ ] Build and inspect the standalone JAR, then commit the extraction

### Task 4: Publish-ready JEM Drill Durability

**Files:**
- Modify: `Mods/JEMDrillLifecycle/build.gradle`
- Modify: `Mods/JEMDrillLifecycle/src/main/resources/META-INF/mods.toml`
- Modify: `Mods/JEMDrillLifecycle/src/main/resources/`
- Create: `Mods/JEMDrillLifecycle/README.md`

**Interfaces:**
- Consumes: Create as a required dependency, Jade as optional
- Produces: a public Create addon preserving the current drill wear contract

- [ ] Rebrand the project as JEM Drill Durability while preserving its existing mod ID and saved wear data unless a documented migration proves safe
- [ ] Preserve moving and stationary drill wear, worn-speed reduction, iron-ingot servicing, repair limits, and replacement behavior
- [ ] Keep Jade condition display optional
- [ ] Remove JEM-specific Patchouli content from the public JAR
- [ ] Replace local Create and Jade JAR lookups with reproducible dependencies
- [ ] Verify clean startup with Create only and with Create plus Jade
- [ ] Build and inspect the standalone JAR, then commit the release preparation

### Task 5: Publish-ready JEM Mob Home

**Files:**
- Modify: `Mods/MobHome/build.gradle`
- Modify: `Mods/MobHome/src/main/resources/META-INF/mods.toml`
- Modify: `Mods/MobHome/src/main/resources/`
- Create: `Mods/MobHome/README.md`

**Interfaces:**
- Consumes: Forge only, all animal-mod integrations optional
- Produces: the existing `mobhome` registries and saved data in an independent project

- [ ] Remove build-time reads from `Client/ManagedPack/mods`
- [ ] Preserve both house sizes, selection GUI, binding, unbinding, death recovery, safe oversized-pet spawning, and native follow behavior
- [ ] Preserve `mobhome` registry IDs and existing world data
- [ ] Ensure absent optional animal mods do not load their adapters
- [ ] Verify an existing saved home and a newly placed home in a clean Forge instance
- [ ] Build and inspect the standalone JAR, then commit the release preparation

### Task 6: Extract JEM Excavation

**Files:**
- Create: `Mods/JEMExcavation/`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/feature/excavation/`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/client/ExcavationHighlight.java`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/mixin/excavation/`
- Modify: `Mods/JEMCompat/src/main/resources/jemcompat.mixins.json`

**Interfaces:**
- Produces: 3x3 and 7x7 excavation with preserved enchantment identity and mining rules

- [ ] Preserve the existing enchantment registry identity or provide a deterministic missing-mapping migration
- [ ] Preserve native tool suitability, durability, permissions, protected-block behavior, and preview highlighting
- [ ] Keep level-to-pattern mapping unchanged
- [ ] Remove the old registrations, events, and Mixins from `JEMCompat` after replacement
- [ ] Verify existing enchanted items and newly enchanted items in a clean instance
- [ ] Build and inspect the standalone JAR, then commit the extraction

### Task 7: Extract JEM Freecam

**Files:**
- Create: `Mods/JEMFreecam/`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/client/FreeCameraController.java`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/mixin/freecam/`
- Modify: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/OptionalMixins.java`

**Interfaces:**
- Produces: a client-only camera that leaves the real player server-owned and cannot pass through collision

- [ ] Preserve the existing keybind and camera movement contract
- [ ] Keep interaction, teleport, rotation, and Epic Fight adapters isolated and optional
- [ ] Confirm the camera cannot mutate player position or bypass collision
- [ ] Remove the old keybind, controller, and optional Mixins from `JEMCompat`
- [ ] Verify clean startup with and without Epic Fight
- [ ] Build and inspect the client-only JAR, then commit the extraction

### Task 8: Prepare JEM Food Blessings

**Files:**
- Modify: `Mods/JEMFood/src/main/java/com/siirio/jemfood/client/FoodTime.java`
- Modify: `Mods/JEMFood/src/main/java/com/siirio/jemfood/client/BlessingPresentation.java`
- Modify: `Mods/JEMFood/src/main/java/com/siirio/jemfood/client/FoodTooltips.java`
- Modify: `Mods/JEMFood/build.gradle`
- Modify: `Mods/JEMFood/src/main/resources/META-INF/mods.toml`
- Create: `Mods/JEMFood/README.md`

**Interfaces:**
- Produces: whole-second display formatting while retaining native tick calculations internally

- [ ] Format every positive remaining duration as ceiling-divided whole seconds
- [ ] Never show raw ticks, milliseconds, decimals, or `0 sec` while positive time remains
- [ ] Apply the formatter consistently to active blessing, tooltips, effect duration, and cooldown presentation
- [ ] Keep effect duration, synchronization, and gameplay calculations in ticks
- [ ] Bundle original assignment data needed at runtime instead of reading external `Catalog/changes`
- [ ] Verify boundary displays at 1, 19, 20, 21, 1199, and 1200 ticks by direct runtime inspection
- [ ] Build and inspect the standalone JAR, then commit the release preparation

### Task 9: Extract JEM Seasons HUD

**Files:**
- Create: `Mods/JEMSeasonsHud/`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/client/time/`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/compat/seasons/SeasonHudAdapter.java`
- Modify: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/config/JEMClientConfig.java`

**Interfaces:**
- Consumes: Serene Seasons as an optional dependency
- Produces: day and time HUD with optional season data

- [ ] Separate the HUD from `SpawnSunflowerSeasons` gameplay behavior, which remains outside this public project unless separately approved
- [ ] Preserve placement, visibility, localization, and the global Minecraft-duration presentation contract
- [ ] Provide useful day and time output when Serene Seasons is absent
- [ ] Load season data only when the optional dependency is present
- [ ] Remove only the replaced HUD state and configuration from `JEMCompat`
- [ ] Build and inspect the client-only JAR, then commit the extraction

### Task 10: Extract JEM Inventory Shortcuts

**Files:**
- Create: `Mods/JEMInventoryShortcuts/`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/client/InventoryDeleteKeys.java`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/feature/inventory/InventoryDeleteNetwork.java`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/feature/inventory/SophisticatedStorageDelete.java`
- Modify: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/JEMCompat.java`

**Interfaces:**
- Produces: server-validated Delete and Shift+Delete behavior with optional container integrations

- [ ] Preserve Delete for one hovered item and Shift+Delete for the complete hovered stack
- [ ] Preserve creative inventory mapping to real player slots
- [ ] Keep inaccessible, infinite, result, upgrade, and unreviewed virtual slots excluded
- [ ] Keep Sophisticated integrations optional and fail closed for unknown virtual inventories
- [ ] Remove the old key handler, packets, and registrations from `JEMCompat`
- [ ] Verify player inventory, creative inventory, a vanilla container, and each supported optional storage owner
- [ ] Build and inspect the standalone JAR, then commit the extraction

### Task 11: Publish-ready In Chat FTB

**Files:**
- Modify: `Mods/JEMInChatFTB/build.gradle`
- Modify: `Mods/JEMInChatFTB/src/main/resources/META-INF/mods.toml`
- Create: `Mods/JEMInChatFTB/README.md`

**Interfaces:**
- Consumes: FTB Quests as a required dependency
- Produces: quest-completion chat messages and native quest-page links without JEM

- [ ] Replace hardcoded local FTB Quests and FTB Library JAR paths with reproducible dependencies
- [ ] Preserve cyan clickable quest names and native page navigation
- [ ] Ensure the mod contains no quest definitions, rewards, or JEM-specific authority
- [ ] Verify clean startup and one completion message in a minimal FTB Quests instance
- [ ] Build and inspect the client-compatible JAR, then commit the release preparation

### Task 12: Publish-ready JEM Adaptive Culling

**Files:**
- Modify: `Mods/JEMAdaptiveCulling/build.gradle`
- Modify: `Mods/JEMAdaptiveCulling/src/main/resources/META-INF/mods.toml`
- Modify: `Mods/JEMAdaptiveCulling/src/main/java/com/siirio/jemadaptiveculling/client/CompatibilityPolicy.java`
- Create: `Mods/JEMAdaptiveCulling/README.md`

**Interfaces:**
- Consumes: Embeddium and Oculus only as optional compatibility dependencies
- Produces: a client-only culling mod that remains safe on plain Forge

- [ ] Remove local Oculus JAR lookup from the build
- [ ] Preserve entity and block-entity visibility caches, geometry invalidation, and compatibility exclusions
- [ ] Ensure absent Embeddium or Oculus classes are never loaded eagerly
- [ ] Verify plain Forge, Forge plus Embeddium, and Forge plus Embeddium and Oculus startup
- [ ] Measure real gameplay FPS before making any public performance claim
- [ ] Build and inspect the client-only JAR, then commit the release preparation

### Task 13: Prepare Unofficial Luxium Compatibility

**Files:**
- Modify: `Mods/JEMLuxium/build.gradle`
- Modify: `Mods/JEMLuxium/src/main/resources/META-INF/mods.toml`
- Modify: `Mods/JEMLuxium/README.md`
- Create: `Mods/JEMLuxium/THIRD_PARTY_NOTICES.md`

**Interfaces:**
- Consumes: the official unmodified Luxium, Embeddium 0.3.31, and Oculus 1.8.0 as external dependencies
- Produces: the existing mutual-exclusion switch without distributing any upstream content

- [ ] Rebrand the public project as Unofficial Luxium Compatibility while keeping a stable technical mod ID
- [ ] Put the user's exact required line on the project page and README: `This is unofficial compatability mod - credits to the author of Luxium for an insanely optimized shaders`
- [ ] Follow it with the clarification that the project is not affiliated with or endorsed by Luxium or Vinlanx
- [ ] Link only to the official Luxium project or download page
- [ ] Declare the official Luxium JAR as required and never embed, mirror, patch, or repackage it
- [ ] Preserve mutual exclusion, the narrow installed-Oculus guard redirect, and Embeddium managed-context reload
- [ ] Pin exact supported versions until separate compatibility evidence exists
- [ ] Verify both switch directions, shadows, restart persistence, and absence of the `REMOVE OCULUS` screen in a clean official-launcher-compatible Forge instance
- [ ] Build and inspect the compatibility-only JAR, then commit the release preparation

### Task 14: Remove duplicate JEM ownership and preserve migration

**Files:**
- Modify: `Mods/JEMCompat/src/main/java/`
- Modify: `Mods/JEMCompat/src/main/resources/`
- Modify: `Client/ManagedPack/mods/`
- Modify: `Docs/DEVELOPMENT.md`
- Modify: `tools/module-inventory.json`

**Interfaces:**
- Consumes: completed standalone artifacts from Tasks 3, 6, 7, 9, and 10
- Produces: one runtime owner per extracted feature

- [ ] Remove only source paths proven replaced by the new standalone projects
- [ ] Preserve boss Jade hints and unrelated JEM compatibility behavior
- [ ] Add missing-mapping or saved-data migrations where a technical ID cannot remain unchanged
- [ ] Ensure the JEM distribution installs the same public artifact instead of a private duplicate build
- [ ] Inspect registration, packet, keybind, event, Mixin, and tooltip ownership for duplicates
- [ ] Build each affected production module and inspect the resulting JAR ownership
- [ ] Commit the migration separately from the individual public projects

### Task 15: Run the per-project release gate

**Files:**
- Create per project: `Docs/public-mods/<project>/release-readiness.md`
- Create per project: `Docs/public-mods/<project>/artifact.json`

**Interfaces:**
- Produces: one independently reviewable release candidate and evidence record per project

- [ ] Build from a clean checkout using the project's own wrapper and Java 17
- [ ] Inspect the JAR for private paths, credentials, caches, bundled dependencies, forbidden assets, and unexpected classes
- [ ] Verify required and optional dependency behavior in a clean Forge 1.20.1 instance
- [ ] Verify English and Russian localization coverage
- [ ] Verify side behavior against client-only, integrated, and dedicated-server declarations
- [ ] Verify preserved IDs and existing-world migration where applicable
- [ ] Record artifact filename, version, SHA-256, dependency matrix, manual observations, and unresolved limits
- [ ] Keep the candidate private when any gate remains incomplete

### Task 16: Prepare CurseForge submissions

**Files:**
- Create per project: `Docs/public-mods/<project>/curseforge-description.md`
- Create per project: `Docs/public-mods/<project>/curseforge-metadata.json`
- Create per project: `Docs/public-mods/<project>/media/`

**Interfaces:**
- Produces: upload-ready descriptions, metadata, original media, changelogs, and approved JARs without performing the upload

- [ ] Write an informative English description and optional Russian companion text
- [ ] Declare dependency relationships without adding unnecessary credit language to the five corrected projects
- [ ] Include the exact unofficial disclaimer and Vinlanx attribution only for Unofficial Luxium Compatibility
- [ ] Produce original project icons and real gameplay screenshots through the required Minecraft visual workflow
- [ ] Attach license, compatibility, source, issue-reporting, and changelog information
- [ ] Review project slugs, categories, game versions, loaders, release channels, and side designations
- [ ] Request explicit user authorization before creating or uploading any CurseForge project
- [ ] Publish one project at a time, beginning with JEM Animal Hints, then JEM Drill Durability, JEM Mob Home, JEM Food Blessings, the remaining extractions, and Unofficial Luxium Compatibility after its upstream-facing wording is approved

### Future Task 17: Extract JEM Serene Seasons Compatibility

This future task is outside the frozen eleven and does not change their release count or completion definition

**Files:**
- Future create: `Mods/JEMSereneSeasonsCompat/`
- Move/adapt from: `Mods/JEMCompat/src/main/resources/data/sereneseasons/tags/`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/feature/seasons/SpawnSunflowerSeasons.java`
- Move/adapt from: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/mixin/seasons/SpawnSunflowerSeasonsMixin.java`
- Modify after replacement proof: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/OptionalMixins.java`

**Interfaces:**
- Requires: Serene Seasons 9.1.0.3+
- Supports through optional data tags: Alex's Caves, Beachparty, Blast from the Past, Brewery, Deep Dark Regrowth, Farm & Charm, HerbalBrews, Kaleidoscope End, Kaleidoscope Nether, Luminous Nether, Nature's Spirit, Nether's Exoticism, Oceanic Realms, Primal, Snowy Spirit, Unearthed Journey, and Vinery
- Supports through one isolated version-reviewed adapter: Spawn sunflower seed growth
- Produces: one independently installable crop-season compatibility owner without JEM Compat

- [ ] Reinspect every supported installed mod version and verify each tagged block and item still exists
- [ ] Keep every content mod optional, with missing registry IDs ignored safely
- [ ] Convert every cross-mod Serene Seasons tag member to an optional `required: false` entry so absent content mods cannot invalidate tag loading
- [ ] Keep Serene Seasons as the only required third-party dependency
- [ ] Preserve native Serene Seasons greenhouse, biome, season, fertility, and configuration ownership
- [ ] Move the complete crop-tag tree and Spawn adapter before removing their old JEM Compat owners
- [ ] Determine the narrowest valid side declaration from clean dedicated and integrated startup evidence
- [ ] Add an exact "Mods it supports" list to CurseForge metadata and the public description
- [ ] Run an independent release gate before requesting upload authorization

## Completion Definition

The program is complete only when all eleven projects build independently, pass their own release gate, contain no private environment coupling, preserve relevant player data, and have user-approved CurseForge submissions. A project may ship independently as soon as its own tasks and release gate are complete
