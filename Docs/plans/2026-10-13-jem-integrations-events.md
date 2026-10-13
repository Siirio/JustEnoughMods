# JEM Integrations and Events Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans`. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the exact required third-party Forge stack, building-project map regions, FTB guide content, two calendar holidays, recurring light events, Iron/world-tier integration and peaceful optional activities.

**Architecture:** Do not create `JEMIntegrations`. Each change stays with its canonical owner: JEMServer owns map overlays, events, onboarding and SMP UX; JEMWorldBossTiers owns world-tier spell scaling; Roles/Professions own their Iron modifiers; Settlements owns workforce adapters; JEMPackCore/KubeJS owns FTB pack content.

**Tech Stack:** Java 17, Forge 1.20.1/47.4.10, existing JEM modules, Xaero World Map 1.46.0, official third-party Forge artifacts and data resources.

**Spec:** ZIP entry `05_INTEGRATIONS_EVENTS_AND_MISSING_SYSTEMS_CODEX_PROMPT.md` plus the cross-module requirements in entries 01–04.

## Global Constraints

- Download only official Forge 1.20.1 artifacts into `Client/ManagedPack/mods` and `Server/mods`; record source URL, filename, SHA-256, mod id, license and dependency metadata. Do not touch an active profile or `Client/JustEnoughMods`.
- No tests, test classes/directories, fixtures, mocks, scripts, frameworks, GameTests, smoke workflows or runtime launches. Compile affected production modules with test tasks excluded; do not lint, format or run static analysis.
- Preserve current uncommitted map protocol-v5/event-area work and every logic lock.
- Roles and Professions also operate in integrated singleplayer. Economy, Settlements, calendar/shared events and server-owned integrations remain dedicated-server gameplay; clients retain only compatibility/network/rendering code required to join.
- Add Server Login server-side only and use its exact connection-bound `AuthGate` as the shared offline-mode authentication provider. Account-bound changes fail closed only for locked/unverified connections; `online-mode=false` is not itself a disable switch.
- No third-party JAR is modified or redistributed outside its official artifact.

## Review Focus

- Exact downloaded JAR classes/registries drive adapters; no guessed API or ID enters source.
- Third-party claim/block-denial logic cannot become town influence or building-project permissions.
- World-tier, role and profession spell handling each apply once in a declared order.
- Calendar state/rewards/cleanup are idempotent across restart and clock rollback.
- Project regions persist source rectangles and split/union deterministically without touching block permissions.

---

### Task 1: Acquire the Exact Required Mod Stack

**Files:**
- Add server-only: `Server/mods/server-login-<exact-1.20.1-version>.jar`
- Add: `Client/ManagedPack/mods/irons_spellbooks-1.20.1-3.15.6.jar`
- Add: `Client/ManagedPack/mods/irons_lib-1.20.1-1.0.2.jar`
- Add: `Client/ManagedPack/mods/player-animation-lib-forge-1.0.2-rc1+1.20.jar`
- Add: `Client/ManagedPack/mods/recruits-1.20.1-1.15.2.jar`
- Add: `Client/ManagedPack/mods/workers-1.20.1-2.0.3.jar`
- Add: `Client/ManagedPack/mods/ftb-library-forge-2001.2.13.jar`
- Add: `Client/ManagedPack/mods/ftb-teams-forge-2001.3.2.jar`
- Add: `Client/ManagedPack/mods/ftb-quests-forge-2001.4.22.jar`
- Add: `Client/ManagedPack/mods/ftb-xmod-compat-forge-2.1.3.jar`
- Add matching files: `Server/mods/`
- Create: `Docs/integrations/2026-10-13-mod-matrix.md`

- [ ] Download the named content files from their official CurseForge/Modrinth project endpoints and compute SHA-256 after download. Server Login is server-only and must not enter `Client/ManagedPack`.
- [ ] Inspect Server Login's exact metadata/source and bind JEM to its public connection-bound authentication state; never read its password store or substitute username trust.
- [ ] Do not install Xaero Regions 1.0.8: its exact metadata requires Forge 47.4.20 while JEM is pinned to 47.4.10. Preserve the existing JEM/Xaero protocol-v5 overlay and implement visual-only building territories there without land-protection semantics.
- [ ] Inspect each `META-INF/mods.toml`, embedded JarJar metadata, mixin list and dependency version ranges; update the matrix with exact discovered mod ids and required/optional sides.
- [ ] Reuse installed Curios `5.14.1`, GeckoLib `4.8.4`, KotlinForForge `4.12.0`, Architectury `9.2.14`, KubeJS/Rhino and Xaero World Map `1.46.0` only when the downloaded metadata accepts those exact versions.
- [ ] If an exact dependency range rejects an installed library, add the nearest official Forge 1.20.1 dependency version accepted by all installed consumers and record the replacement; do not guess or add optional content addons.
- [ ] Do not add Origins, Pufferfish Skills, Project MMO, Lightman's Currency, Recruits Village Expansion, Recruits QoL, cosmetic Recruits addons or an FTB UI overhaul.

### Task 2: Inspect Third-Party Registrations and Establish Adapter Boundaries

**Files:**
- Create: `Docs/integrations/irons-spell-registry-3.15.6.md`
- Create: `Docs/integrations/recruits-workers-1.20.1.md`
- Create: `Docs/integrations/ftb-xaero-1.20.1.md`
- Modify: affected module `build.gradle` files with exact `compileOnly fg.deobf(files(...))` dependencies.

- [ ] Enumerate Iron spell, school, spellbook, scroll, armor, attribute and damage-source registrations from the exact JAR and document direct/DoT/projectile/explosion/summon/heal/movement/debuff/utility categories.
- [ ] Enumerate Recruits/Workers entity types, work areas, storage interfaces, owner/faction data, commands, wage/morale hooks, claim config and chunk-ticket behavior.
- [ ] Enumerate FTB quest/theme/criterion hooks and the existing JEM/Xaero protocol-v5 storage/render/edit hooks from exact classes/resources.
- [ ] Define optional adapter packages whose classes are instantiated only after `ModList` confirms the exact mod id.

### Task 3: Implement Building Project Regions on Xaero

**Files:**
- Create: `Mods/JEMServer/src/main/java/com/siirio/jemserver/map/ProjectRegion.java`
- Create: `.../map/ProjectRegionData.java`
- Create: `.../map/ProjectRegionService.java`
- Create: `.../map/RegionUnion.java`
- Create: `.../map/RegionPalette.java`
- Create: `.../map/RegionPermissions.java`
- Create: `.../client/xaero/ProjectRegionClient.java`
- Modify: `.../MapNetwork.java`
- Modify: `.../client/xaero/MapClient.java`
- Modify: `.../mixin/xaero/MapScreenMixin.java`
- Modify: `Mods/JEMServer/src/main/resources/jem_server.mixins.json`

- [ ] Persist owner, dimension, name, icon, auto color, source rectangles, components, collaborator invitations and revision separately from JEMClaims.
- [ ] Implement positive-area overlap union, no edge/corner merge by default, transitive same-owner merge and deterministic split after rectangle removal.
- [ ] Enforce 20 projects/owner, 64 rectangles/project, one-million-block² union area and two mutations/second with bounded packet frames.
- [ ] Add dedicated-only `Building Areas → Plan Area` left-drag rectangle flow; preserve ordinary Xaero controls outside plan mode.
- [ ] Render one outer outline/no internal seams and a stable interior label; never consult regions from permission checks.
- [ ] Extend the existing JEM/Xaero protocol-v5 overlay for this contract; do not load or copy Xaero Regions classes/assets and do not consult project regions from permission checks.

### Task 4: Add Living Landmarks and Project Studio

**Files:**
- Create: `.../smp/projects/LivingLandmarks.java`
- Create: `.../smp/projects/ProjectStudio.java`
- Create: `.../smp/projects/ProjectRecords.java`
- Modify: `.../smp/SmpTable.java`
- Modify: `.../smp/SmpActionKind.java`
- Modify: `.../smp/SmpNetwork.java`
- Modify: `.../client/smp/SmpScreen.java`
- Modify: `.../client/smp/SmpSidebar.java`

- [ ] Add opt-in landmark types, real anchor, public/private flag, optional Exposure photo reference and seven-day authenticated cosmetic guestbook count.
- [ ] Add self-managed project names/tags/map reference/notes/material wishlist/milestones/images with self-declared completion and no XP/Coin/value reward.
- [ ] Use explicit collaborator invitations and grant no world-block permissions.

### Task 5: Add FTB Quests and Theme Content

**Files:**
- Add: `Client/ManagedPack/kubejs/assets/ftbquests/ftb_quests_theme.txt`
- Add: `Client/ManagedPack/config/ftbquests/quests/chapters/*.snbt`
- Add: `Client/ManagedPack/config/ftbquests/quests/data.snbt` updates
- Create: `Mods/JEMPackCore/src/main/java/com/siirio/jempackcore/compat/ftb/FtbQuestBridge.java`
- Create: `.../compat/ftb/JemQuestCriterion.java`
- Modify: `Mods/JEMPackCore/build.gradle`

- [ ] Create Getting Started, Professions, Races, Exploration, Bosses/12 Eyes, Create, Living World and Events chapters using the exact installed FTB schema.
- [ ] Keep 12 Eyes/profession/race/town/economy owners authoritative; FTB observes committed events and never grants duplicate state or XP.
- [ ] Route any Coin reward through funded `EconomyApi` idempotency keys; otherwise use cosmetic/nonmonetary rewards.
- [ ] Apply the existing JEM blocky brown/copper/cream visual language with crisp icons and no chest-grid imitation or proprietary UI-overhaul assets.

### Task 6: Implement Calendar Event Lifecycle

**Files:**
- Create: `Mods/JEMServer/src/main/java/com/siirio/jemserver/smp/events/calendar/CalendarEvent.java`
- Create: `.../CalendarEventState.java`
- Create: `.../CalendarEventScheduler.java`
- Create: `.../CalendarEventData.java`
- Create: `.../HolidayCommands.java`
- Modify: `.../smp/events/EventRuntime.java`
- Modify: `.../smp/SmpConfig.java`

- [ ] Persist event/year/state/UTC boundaries/transition IDs with configurable default timezone `Asia/Almaty`.
- [ ] Define Halloween `[Oct 29 00:00, Nov 1 00:00)` and New Year `[Dec 24 00:00, Jan 7 00:00)` in configured local time.
- [ ] Make planned/active/ended/cleaned transitions once-only across restart, mid-window startup and clock rollback.
- [ ] Add OP preview/force-start/stop commands and per-player mute state; no daily login reward.

### Task 7: Implement Halloween, New Year and Light Events

**Files:**
- Create: `.../calendar/HalloweenEvent.java`
- Create: `.../calendar/NewYearEvent.java`
- Create: `.../calendar/GrandMarketFair.java`
- Create: `.../calendar/SeasonalRiftWaves.java`
- Create: `Docs/events/HALLOWEEN.md`
- Create: `Docs/events/NEW_YEAR.md`
- Create: `Docs/events/GRAND_MARKET_FAIR.md`
- Create: `Docs/events/SEASONAL_RIFT_WAVES.md`

- [ ] Implement the exact calm/market/optional-combat activities, real-item gift escrow, ordinary seasonal fishing and funded Operations-reserve rewards.
- [ ] Place only transient event-owned POIs in approved generated/loaded land away from homes/towns/spawn/boss structures and clean only owned transient content.
- [ ] Reuse existing World Tier/event party/boundary/travel contracts and prevent simultaneous overlapping wave owners.
- [ ] Keep Grand Market one in-game day and Seasonal Rifts opt-in with block damage off by default; never raid an offline settlement.

### Task 8: Integrate Iron with World Tier, Roles and Professions

**Files:**
- Create: `Mods/JEMWorldBossTiers/src/main/java/com/siirio/jemworldbosstiers/compat/irons/IronsSpellDamageAdapter.java`
- Create: `.../IronsSpellCatalog.java`
- Create: `.../SpellTierFactors.java`
- Modify: `Mods/JEMWorldBossTiers/build.gradle`
- Modify: `Mods/JEMRoles/.../compat/irons/IronsRaceBridge.java`
- Modify: `Mods/JEMProfessions/.../compat/irons/IronsProfessionAdapter.java`
- Add: `Client/ManagedPack/config/irons_spellbooks_spell_config/**`

- [ ] Derive tiers from `WorldTierApi`; apply 0.65/0.80/0.95/1.10/1.20 to each logical direct or DoT spell damage once, with PvP factor 1.00.
- [ ] Declare order: Iron base spell level/power → World Tier factor → Human +1 direct logical hit → ordinary target mitigation; never spawn another damage event.
- [ ] Configure every damaging registered spell by its real ID and keep healing/CC/movement/summon constraints separate.
- [ ] Enable the exact Mage nodes from the profession plan using real cast/school/mana APIs and one cast audit ID.
- [ ] Search exact installed content for Book of Progression; extend its predicate only if it exists, preserving spellbook NBT/spells/owner/mana. Otherwise add only the named adapter boundary and no fake item.

### Task 9: Integrate Recruits and Workers into Workforce

**Files:**
- Modify: `Mods/JEMSettlements/.../compat/recruits/RecruitsAdapter.java`
- Modify: `Mods/JEMSettlements/.../compat/workers/WorkersAdapter.java`
- Create: `.../compat/workers/WorkerWorkEvidence.java`
- Create: `.../compat/workers/WorkerStorageAdapter.java`
- Create: `.../compat/recruits/RecruitReadinessAdapter.java`
- Add/Modify exact server configs under `Client/ManagedPack/config/` and `Server/config/`.

- [ ] Bind authentic worker/recruit UUID, owner, native job/work area/storage/morale/commands to one Workforce record.
- [ ] Preserve native farming/mining/fishing/cooking/building/courier item behavior in ticked chunks and security service behavior for recruits.
- [ ] Disable native autoclaim and block-place/break denial through the exact supported config/API while preserving hiring, faction metadata, combat and unrelated protections.
- [ ] Map native ticket behavior into the settlement ticket budget and background transition; never simulate phantom combat or unsurveyed ore.
- [ ] Select one native-versus-Coin upkeep path per NPC type and record it in UI rather than charging twice.

### Task 10: Implement Newcomer Consent and Contextual Guide

**Files:**
- Create: `Mods/JEMServer/src/main/java/com/siirio/jemserver/smp/onboarding/NewcomerFlow.java`
- Create: `.../FriendTeleportRequest.java`
- Create: `.../ContextualGuide.java`
- Create: `.../AdventureJournal.java`
- Modify: `.../smp/SmpRuntime.java`
- Modify: `.../smp/SmpNetwork.java`

- [ ] Add `Playing with friends?` with opted-in online discovery, 60-second two-party consent and safe destination validation.
- [ ] Never reveal offline locations or bypass combat/arena/portal safety.
- [ ] Present Calm/Royalty/Power as suggestions filtered by installed features; keep one-time kits config-backed and non-Coin.
- [ ] Remind only explicitly pinned unfinished projects/leads, with no streak punishment.

### Task 11: Reconcile Owning Docs, Configuration and Changelog

**Files:**
- Modify: `Docs/DEVELOPMENT.md`
- Modify: `Docs/smp-gui/README.md`
- Modify: `Docs/SERVER_COMMANDS.md`
- Modify: relevant existing `Docs/events/*.md`
- Modify: `CHANGELOG.md`

- [ ] Record exact module owners, downloaded mod matrix, identity safe mode, Coin-only commerce, non-claim project regions, event calendar and optional-integration boundaries.
- [ ] Distinguish user-required product/technical decisions from Codex-selected module/adaptor structure.
- [ ] Preserve existing event documents as the only truth for their events and add one owning file for each new event.

