# JEM Professions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans`. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add ten simultaneous level-1-to-50 player professions with server-authored action XP, concurrent branches and the exact approved perks.

**Architecture:** Create standalone `JEMProfessions`. It owns XP, levels, node state, cooldowns and action transactions. Existing mods remain owners of their native mechanics; adapters translate native events. Economy, Settlements, Roles and Iron are optional consumers/providers, never duplicated stores.

**Tech Stack:** Java 17, Forge 1.20.1, SavedData, Forge events, SimpleChannel, existing JEM screen primitives and optional installed-mod adapters.

**Spec:** ZIP entry `02_PROFESSIONS_CODEX_PROMPT.md`.

## Global Constraints

- Do not add Pufferfish Skills or Project MMO in this pass; the requested logic is predominantly JEM-specific and Pufferfish would be presentation-only.
- No skill points, exclusive branches, respec or visible Calm/Royal/Warrior class categories.
- Professions run normally in integrated singleplayer under the local owner UUID. Dedicated-server XP/admin mutations require the shared connection-bound authenticated principal; `online-mode=false` alone does not disable progression.
- No Quick Harvest, Quick Plant, Fish Journal, Bite Sense, Tunnel Miner, Ore Scanner, Discovery Book or Biome Finder.
- No tests or test infrastructure. Compile the production module with test tasks excluded after implementation.

## Review Focus

- One accepted gameplay action creates at most one audit ID and one XP credit.
- Player-only actions exclude NPC workers, fake players, virtual town output and automation.
- Multi-block operations reserve material/tool state before mutation and never copy block-entity NBT.
- Optional adapters cannot load absent classes.
- Economy/settlement perks delegate every money/town mutation to the owning API.

---

### Task 1: Scaffold `JEMProfessions`

**Files:**
- Create: `Mods/JEMProfessions/{build.gradle,gradle.properties,settings.gradle}`
- Create: `Mods/JEMProfessions/src/main/resources/META-INF/mods.toml`
- Create: `.../pack.mcmeta`
- Create: `.../java/com/siirio/jemprofessions/JemProfessions.java`

- [ ] Follow the standalone Forge module pattern with Forge-only hard dependency, Java 17, disabled test tasks and embedded `JEMMenuSupport`.
- [ ] Depend on the narrow `JEMIdentity` principal API; register gameplay listeners on dedicated and integrated servers, with distribution-safe screens/network on clients.

### Task 2: Create the Progression Owner

**Files:**
- Create: `.../progress/ProfessionId.java`
- Create: `.../progress/ProfessionRecord.java`
- Create: `.../progress/ProfessionSavedData.java`
- Create: `.../progress/ProfessionFormula.java`
- Create: `.../progress/ProfessionService.java`
- Create: `.../api/ProfessionApi.java`
- Create: `.../api/ProfessionProgressRequest.java`
- Create: `.../api/ProfessionProgressResult.java`
- Create: `.../api/ProfessionProgressEvent.java`
- Create: `.../api/PerkUnlockedEvent.java`

**Interfaces:**
```java
public record ProfessionProgressRequest(ProfessionId profession, SourceKind sourceKind,
        ResourceLocation sourceId, int baseDelta, UUID auditId) {}

public static ProfessionProgressResult credit(ServerPlayer player,
        ProfessionProgressRequest request);
```

- [ ] Implement displayed levels 1..50 using `round(100 * pow(1 + 0.08 * level, 1.4))`, integer total XP and configurable caps.
- [ ] Persist XP, toggles, cooldowns, action fingerprints, UTC-day counters and schema version under authenticated UUID.
- [ ] Reject duplicate audit IDs and unauthenticated dedicated-server connections before mutation; accept the integrated singleplayer owner and authenticated Server Login principals. Emit one progress event and one-way unlock events after a committed change.

### Task 3: Define Data-Driven Trees and UI DTOs

**Files:**
- Create: `.../tree/ProfessionTree.java`
- Create: `.../tree/ProfessionBranch.java`
- Create: `.../tree/ProfessionNode.java`
- Create: `.../tree/ProfessionTreeLoader.java`
- Create: `.../resources/data/jem_professions/professions/*.json`
- Create: `.../network/ProfessionsNetwork.java`
- Create: `.../client/ProfessionsScreen.java`
- Create: `.../client/ProfessionTreeScreen.java`
- Create: `.../client/ProfessionClientState.java`
- Create: `.../client/ProfessionKeyMappings.java`

- [ ] Encode every approved node with exact level, branch, numeric effect, activation, cooldown, dependency and unavailable reason.
- [ ] Render ten cards and concurrent branch tabs; locked clicks spend nothing, passive nodes say `Always on`, toggleable nodes update only after server reply.
- [ ] Keep Mage visible as `Coming Soon` until the exact Iron adapter is active; register an unbound key and `/jem professions`.

### Task 4: Implement the Shared Action Transaction Kernel

**Files:**
- Create: `.../action/ActionTransaction.java`
- Create: `.../action/ActionReservation.java`
- Create: `.../action/ActionRateLimiter.java`
- Create: `.../action/BlockPlacementPlan.java`
- Create: `.../action/BlockOperationQueue.java`
- Create: `.../action/ProtectedAreaPolicy.java`

**Interfaces:**
```java
public sealed interface ActionPlan permits PlacementPlan, HarvestPlan, VeinPlan, MovePlan {
    UUID operationId();
    ActionValidation validate(ServerPlayer player);
    ActionResult commit(ServerPlayer player);
}
```

- [ ] Centralize server recomputation, loaded-chunk checks, reach/LOS, permissions, inventory reservation, operation replay rejection and cancellation cleanup.
- [ ] Cap Phantom placement at 49 cells and one operation per four ticks; reject a whole plan before consuming items when any cell is invalid.
- [ ] Never serialize arbitrary block-entity inventories/fluids/NBT into a placement or copy plan.

### Task 5: Implement Architect and Farmer

**Files:**
- Create: `.../perk/architect/PlacementReach.java`
- Create: `.../perk/architect/PhantomBuilder.java`
- Create: `.../perk/farmer/CropAdapters.java`
- Create: `.../perk/farmer/CropSense.java`
- Create: `.../perk/farmer/SweepHarvest.java`
- Create: `.../perk/farmer/GreenhouseController.java`
- Create: `.../perk/farmer/LivestockPerks.java`
- Create: `.../registry/ProfessionBlocks.java`
- Create: `.../registry/ProfessionItems.java`

- [ ] Implement nonstacking placement-only reach totals +3/+5/+7 without changing entity interaction or attack reach.
- [ ] Implement line 1..8 and plate 3/4/5/7 previews whose server plan derives positions from anchor/orientation/size.
- [ ] Implement Crop Sense within 12 loaded blocks and bounded LOS without global crop scanning.
- [ ] Implement 4-connected Sweep capped at 1,024 and 64 positions/tick, with real loot, seed reservation, replant and cancellation semantics.
- [ ] Implement Greenhouse Controller validation and a fuel-bounded +10% growth opportunity hook without changing global random tick speed.
- [ ] Implement Herd Call, Feed Herd and Quick Shear with real items, native cooldowns and max eight eligible animals.

### Task 6: Implement Fisher, Miner and Explorer

**Files:**
- Create: `.../perk/fisher/FishingPerks.java`
- Create: `.../compat/tide/TideFishingAdapter.java`
- Create: `.../perk/miner/OreSense.java`
- Create: `.../perk/miner/VeinMining.java`
- Create: `.../perk/miner/MinerSurvival.java`
- Create: `.../perk/explorer/ExplorerMobility.java`
- Create: `.../perk/explorer/PortableCamp.java`
- Create: `.../compat/smallships/SmallShipsAdapter.java`

- [ ] Implement only Target Fishing and Double Catch for Fisher, excluding treasure/rare outputs and worker catches.
- [ ] Implement demand-only Ore Sense radius 6/45 seconds, natural-ore provenance, 8/32-node 6-neighbor vein operations and owned-drop collection.
- [ ] Implement Lava Shield, Stone Step and Cave Breath with exact conditions and cooldowns.
- [ ] Implement Wall Climb, Fast Traveler, Ledge Catch, consumable rope, Portable Camp, sprint-only Ration Saver and vanilla boat River Runner; gate unsupported Small Ships speed cleanly.

### Task 7: Implement Engineer and Warrior Native Adapters

**Files:**
- Create: `.../compat/create/CreateProfessionAdapter.java`
- Create: `.../perk/engineer/GearSwap.java`
- Create: `.../perk/engineer/ShaftBuilder.java`
- Create: `.../perk/engineer/FactoryCopy.java`
- Create: `.../perk/engineer/MachineMover.java`
- Create: `.../perk/engineer/RailwayPerks.java`
- Create: `.../compat/epicfight/EpicFightProfessionAdapter.java`
- Create: `.../perk/warrior/WarriorPerks.java`
- Create: `.../compat/playerrevive/PlayerReviveProfessionAdapter.java`

- [ ] Reuse installed Create `6.0.8` wrench, track, schedule and schematic contracts; keep trains at native speed.
- [ ] Keep Machine Mover unavailable unless a recoverable two-phase native-state move can be expressed without inventories, fluids or active kinetic state.
- [ ] Translate genuine Epic Fight dodge/guard/stamina events into Endurance, Parry Master, Battle Recovery, Counter Step, Second Wind and Battle Focus exactly once.
- [ ] Gate Field Medic if PlayerRevive exposes no stable server completion-speed hook.

### Task 8: Implement Merchant and Mayor Delegation

**Files:**
- Create: `.../compat/economy/EconomyProfessionAdapter.java`
- Create: `.../perk/merchant/MerchantPerks.java`
- Create: `.../compat/settlements/SettlementProfessionAdapter.java`
- Create: `.../perk/mayor/MayorPerks.java`

- [ ] Implement Buy Orders, Auction, Commission Escrow, Trading Post, Stall Sign, Market Network and Grand Market only through `EconomyApi` escrow/market contracts.
- [ ] Implement Mayor invitation, supply notice, resident move, taxes, Rally, patrol route and festival only through `SettlementApi` and native NPC adapters.
- [ ] Credit Merchant/Mayor XP only from authenticated committed third-party transactions or bounded real improvement milestones.

### Task 9: Implement Mage as an Optional Adapter

**Files:**
- Create: `.../compat/irons/IronsProfessionAdapter.java`
- Create: `.../perk/mage/MagePerks.java`

- [ ] Keep the adapter unloaded while Iron is absent.
- [ ] When the exact Iron artifact is added by the integrations plan, use registry-derived schools/spells for Mana Saver, Magic School, Combo Magic, Meditation, Arcane Sense, Arcane Guard and Magic Circle.
- [ ] Credit one cast once and never credit equip, scroll/offhand duplication or race/world-tier damage callbacks.

### Task 10: Add Commands, Localization and Ownership Docs

**Files:**
- Create: `.../command/ProfessionCommands.java`
- Create: `.../resources/assets/jem_professions/lang/en_us.json`
- Create: `.../resources/assets/jem_professions/lang/ru_ru.json`
- Modify: `Mods/JEMServer/src/main/java/com/siirio/jemserver/client/smp/SmpSidebar.java`
- Modify: `Docs/DEVELOPMENT.md`
- Modify: `Docs/smp-gui/README.md`
- Modify: `CHANGELOG.md`

- [ ] Register permission-audited inspect/grantxp/reload commands; grantxp must not trigger rewards.
- [ ] Add semantic navigation only when the module is loaded and retain one Back path.
- [ ] Record player-only XP ownership, removed perks and external owner boundaries in the canonical docs and changelog.

