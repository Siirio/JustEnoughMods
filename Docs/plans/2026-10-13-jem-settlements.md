# JEM Settlements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans`. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add optional non-claim towns, one native Workforce view, physical warehouse policies, happiness and bounded distant operation.

**Architecture:** Create standalone `JEMSettlements`. It owns town IDs, membership links, facilities, happiness, work obligations and citadel progress. Recruits/Workers own NPC AI and native morale; Economy owns Coins; physical containers own items. Distant work creates nonspendable obligations that materialize once into real storage.

**Tech Stack:** Java 17, Forge 1.20.1, SavedData, item-handler capabilities, Forge chunk tickets, optional Recruits/Workers/Economy adapters.

**Spec:** ZIP entry `03_SETTLEMENTS_CODEX_PROMPT.md`.

## Global Constraints

- Town influence and Xaero building territories are labels/services/visual planning only and never grant or deny block, chest, PvP, door, teleport or interaction permissions.
- Preserve physical linked chest storage and hybrid production: loaded native workers operate normally, distant intervals create bounded nonspendable obligations, and materialization enters the same real containers exactly once. No digital warehouse or duplicate stock exists.
- Dedicated-server founding, ownership, treasury and account-bound town writes require the shared connection-bound authenticated principal; `online-mode=false` alone does not disable Settlements.
- Workers/Recruits are absent today and will be added as exact official Forge 1.20.1 artifacts by the integrations plan; adapter classes are written only after their downloaded JARs are inspected.
- Recruits are another native worker type inside the single Workforce registry and filtered Defense view, never a second employment, wage, stock, production or morale system.
- No tests or test infrastructure. Compile the production module with test tasks excluded after implementation.

## Review Focus

- One NPC, bed, workplace and container cannot belong to two towns.
- Loaded native production and distant obligations never cover the same work interval.
- Removing/filling/changing a linked chest cannot duplicate pending output.
- Taxes move already-funded resident income only.
- Town influence never enters a permission decision.

---

### Task 1: Scaffold `JEMSettlements`

**Files:**
- Create: `Mods/JEMSettlements/{build.gradle,gradle.properties,settings.gradle}`
- Create: `Mods/JEMSettlements/src/main/resources/META-INF/mods.toml`
- Create: `.../pack.mcmeta`
- Create: `.../java/com/siirio/jemsettlements/JemSettlements.java`

- [ ] Create a Forge-only Java-17 owner module with disabled test tasks and embedded menu support.
- [ ] Register dedicated-only domain listeners and multiplayer client screens/network.

### Task 2: Implement Town Identity and Lifecycle

**Files:**
- Create: `.../town/TownRecord.java`
- Create: `.../town/TownSavedData.java`
- Create: `.../town/TownService.java`
- Create: `.../town/TownTier.java`
- Create: `.../town/TownInfluence.java`
- Create: `.../api/SettlementApi.java`
- Create: `.../api/TownSnapshot.java`

- [ ] Persist UUID, dimension, bell/Town Hall anchor, owner, normalized name, description, radius, managers, tier, history and schema.
- [ ] Validate 3..32 character unique names, 16-block anchor proximity, prohibited areas, 64-block hall separation, one self-founded town and 48 default/128 max radius.
- [ ] Implement rename cooldown, accepted ownership transfer and two-step dissolution without deleting world blocks or NPCs.

### Task 3: Implement Residents, Facilities and Physical Storage Links

**Files:**
- Create: `.../resident/ResidentRegistry.java`
- Create: `.../resident/ResidentLink.java`
- Create: `.../facility/FacilityRegistry.java`
- Create: `.../storage/LinkedContainer.java`
- Create: `.../storage/StoragePolicy.java`
- Create: `.../storage/StorageSnapshot.java`

- [ ] Require real NPC UUID, native owner/consent, reachable bed/work and one-town uniqueness; never spawn or teleport residents.
- [ ] Register facilities only by server-observed interaction with real blocks/POIs and store no aesthetic score.
- [ ] Link only physically visited authorized containers connected to a supported work area; never scan arbitrary nearby inventories.
- [ ] Compute actual/reserved/eligible/pending fields separately; player inventory changes invalidate cached eligibility.

### Task 4: Implement One Workforce Registry and Optional Native Adapters

**Files:**
- Create: `.../workforce/WorkforceRegistry.java`
- Create: `.../workforce/WorkforceRecord.java`
- Create: `.../workforce/WorkforceType.java`
- Create: `.../api/WorkforceSnapshot.java`
- Create: `.../compat/recruits/RecruitsAdapter.java`
- Create: `.../compat/workers/WorkersAdapter.java`

- [ ] Represent supported Farmers/Miners/Fishers/Cooks/Builders/Couriers/Merchants/Guards and Recruits as views of genuine native NPC UUIDs in one Workforce registry.
- [ ] Keep ownership and town employment separate; the same guard appears once in Workforce and as a filtered Defense row.
- [ ] Delegate native controls, work areas, pathing, gear, morale and real block/item operations to the exact adapter.
- [ ] Keep unavailable actions visibly gated while external mods are absent; never create replacement NPC AI.

### Task 5: Implement Work Intervals and Distant Obligations

**Files:**
- Create: `.../work/WorkMode.java`
- Create: `.../work/WorkInterval.java`
- Create: `.../work/WorkOrder.java`
- Create: `.../work/WorkOrderJournal.java`
- Create: `.../work/RemoteWorkScheduler.java`
- Create: `.../work/MaterializationService.java`
- Create: `.../work/ChunkTicketBudget.java`

- [ ] Close the active native interval before opening a distant interval; store worker/workplace/storage/input/capacity/season evidence and one interval ID.
- [ ] Create bounded pending obligations only for supportable outputs whose real inputs and capacity were verified; never simulate unsurveyed mining, builder world changes, combat kills or loot.
- [ ] Allocate selective work-area/storage/courier chunk tickets under per-server/per-dimension budgets with idle release and explicit shed reason.
- [ ] Materialize an obligation once under worker/worksite/storage locks through real item/block pathways; quarantine conflicts and keep pending nonspendable.

### Task 6: Implement Happiness, Seasons and Consequences

**Files:**
- Create: `.../happiness/HappinessService.java`
- Create: `.../happiness/HappinessSnapshot.java`
- Create: `.../season/SeasonIndex.java`
- Create: `.../compat/sereneseasons/SereneSeasonAdapter.java`

- [ ] Calculate once per Minecraft day or after a 60-second structural debounce: `round(.25H + .25F + .20S + .15J + .15C) - taxPenalty`, clamped 0..100.
- [ ] Use tax penalties 0/3/8/16 for 0/5/10/15%, capped category evidence and user-facing cause lines.
- [ ] Apply at most +5% eligible supported work at Thriving and -10% at Struggling without recursively multiplying rewards.
- [ ] Freeze relocation while managers are offline; after three critical seasons and a recovery season, allow at most one eligible native relocation per real day.

### Task 7: Implement Treasury, Wages and Taxes Through Economy

**Files:**
- Create: `.../compat/economy/SettlementEconomyAdapter.java`
- Create: `.../treasury/TreasuryService.java`
- Create: `.../treasury/PayrollSchedule.java`
- Create: `.../treasury/TaxSchedule.java`

- [ ] Delegate deposits, withdrawals, wage transfers and resident-income tax transfers to `EconomyApi` with one idempotency key per schedule.
- [ ] Choose one cost path per native NPC after adapter inspection; never debit native upkeep and Coin wage for the same obligation.
- [ ] Apply 0/5/10/15% only to a committed funded resident inflow and never to chest stock, population or human players.

### Task 8: Implement Citadels and Project Milestones

**Files:**
- Create: `.../project/CitadelType.java`
- Create: `.../project/CitadelProject.java`
- Create: `.../project/CitadelService.java`
- Create: `.../compat/create/IndustrialMilestones.java`
- Create: `.../compat/economy/CommercialMilestones.java`
- Create: `.../compat/recruits/MilitaryMilestones.java`

- [ ] Track Industrial native machine/recipe/station/arrival signals, Military genuine troops/commands/opt-in defense, and Commercial authenticated settled sales/orders/turnover.
- [ ] Implement Camp/Outpost, Village, Town, City and optional Citadel without any End/12-Eyes dependency or art scoring.
- [ ] Keep admin stage edits permission-gated and audited.

### Task 9: Implement Town Hall Screens

**Files:**
- Create: `.../network/SettlementNetwork.java`
- Create: `.../client/TownBrowserScreen.java`
- Create: `.../client/TownDashboardScreen.java`
- Create: `.../client/WorkforceScreen.java`
- Create: `.../client/SuppliesScreen.java`
- Create: `.../client/TreasuryScreen.java`
- Create: `.../client/ProjectsScreen.java`
- Create: `.../client/SettlementClientState.java`

- [ ] Implement Overview/People/Supplies/Buildings/Treasury/Workforce/Defense/Projects/Settings with one Back path and server-confirmed mutations.
- [ ] Show `Background simulation`, `Fully active`, `Waiting for materialization`, actual/pending/sellable stock and exact unavailable-adapter reasons.
- [ ] Make Defense a filtered view of Workforce and preserve one payroll, ownership and NPC record.

### Task 10: Add Commands, Localization and Ownership Docs

**Files:**
- Create: `.../command/SettlementCommands.java`
- Create: `.../resources/assets/jem_settlements/lang/en_us.json`
- Create: `.../resources/assets/jem_settlements/lang/ru_ru.json`
- Modify: `Mods/JEMServer/src/main/java/com/siirio/jemserver/client/smp/SmpSidebar.java`
- Modify: `Docs/DEVELOPMENT.md`
- Modify: `Docs/smp-gui/README.md`
- Modify: `Docs/SERVER_COMMANDS.md`
- Modify: `CHANGELOG.md`

- [ ] Register `/jem town` and audited administration/recovery commands without introducing claim permissions.
- [ ] Add semantic Settlement/Workforce navigation only when loaded.
- [ ] Record town, workforce, storage, remote-work, happiness and no-claim ownership contracts in canonical docs/changelog.

## Phase 2 implementation status — 2026-10-13

Implemented versioned town/resident/workforce persistence, registered-villager food and amenity mood, authenticated atomic Recruit payroll, conservative six-day inactive-interval journaling, stable tier/specialization evidence, ten Town Hall tabs, RU/EN FTB guidance and personal authenticated mount marks. Town happiness keeps the established weighted categories/tax penalties with decoration bounded to +10.

Two deliberately disabled paths remain part of the contract rather than placeholders: Workers 2.0.3 has no verified remote rate/input/output-commit API, so background item production is zero and journaled; physical projects have no authored permission-safe blueprint set, so they may be planned but place zero blocks. Both require a later explicit adapter/asset decision before their unchecked tasks can be completed.

