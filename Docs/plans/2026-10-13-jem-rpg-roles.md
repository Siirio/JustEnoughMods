# JEM RPG Roles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans`. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Human, Dwarf and Elf as persistent cultural identities with exactly three modest server-owned passives and the approved selection/respec flow.

**Architecture:** Create standalone `JEMRoles`. It owns race persistence, selection policy, trait application and role events. Client screens render synced DTOs; optional Iron/Pehkui hooks live behind isolated adapters. JEMServer only exposes a navigation entry.

**Tech Stack:** Java 17, Forge 1.20.1, `SavedData`, Forge attributes/events, SimpleChannel, existing JEM palette and menu assets.

**Spec:** ZIP entry `01_RPG_ROLES_CODEX_PROMPT.md`.

## Global Constraints

- No Origins dependency and no default-origin powers.
- Dedicated-server race writes use the shared connection-bound principal policy: authenticated Server Login session plus player UUID. `online-mode=false` alone never disables Roles. Integrated singleplayer trusts the local owner UUID and supports the complete selection, respec, passive and progression flow.
- Dwarf physical height is disabled by default; no collision/hitbox change is implemented in this source-only pass.
- No race gates profession, boss, town, biome, item or 12 Eyes progression.
- No tests or test infrastructure. Compile the production module with test tasks excluded after implementation.

## Review Focus

- Fixed attribute modifier UUIDs never stack on clone/reconnect/config reload.
- Human merchant discount is player-relative and never mutates a shared `MerchantOffer` globally.
- Arrow and spell bonuses dedupe a logical hit and never create a second damage event.
- Active-play grace cannot advance while AFK and cannot reset on relog.
- Unknown/corrupt race data resolves to provisional Human baseline without overwriting stored data.

---

### Task 1: Scaffold the Domain Owner

**Files:**
- Create: `Mods/JEMRoles/build.gradle`
- Create: `Mods/JEMRoles/gradle.properties`
- Create: `Mods/JEMRoles/settings.gradle`
- Create: `Mods/JEMRoles/src/main/resources/META-INF/mods.toml`
- Create: `Mods/JEMRoles/src/main/resources/pack.mcmeta`
- Create: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/JemRoles.java`

**Interfaces:**
- Produces mod id `jem_roles` and archive `jem-roles`.
- Requires Forge only; JEMServer, Pehkui and Iron remain optional.

- [ ] Create the ForgeGradle Java-17 module following `JEMDrillLifecycle`, with test tasks disabled and no test source set content.
- [ ] Depend on the narrow `JEMIdentity` principal API; register config, network and gameplay runtime from `JemRoles` on both dedicated and integrated servers.
- [ ] Keep client screens/render/input distribution-safe while allowing the integrated server to own normal Roles state and traits.

### Task 2: Establish Verified Identity and Persistent Race State

**Files:**
- Create: `.../identity/IdentityPolicy.java`
- Create: `.../identity/IdentityStatus.java`
- Create: `.../race/RaceId.java`
- Create: `.../race/RaceRecord.java`
- Create: `.../race/RaceSavedData.java`
- Create: `.../race/RaceService.java`
- Create: `.../api/RaceApi.java`
- Create: `.../api/RaceSnapshot.java`

**Interfaces:**
```java
public interface IdentityPolicy {
    IdentityStatus status(MinecraftServer server, GameProfile profile);
}

public record RaceSnapshot(RaceId race, boolean confirmed, long activeTicks,
        boolean freeSwitchAvailable, long lastRespecEpochMillis, int schemaVersion) {}

public final class RaceApi {
    public static Optional<RaceSnapshot> get(MinecraftServer server, UUID accountId);
}
```

- [ ] Persist schema version, race, original selection time, active play ticks, free-switch use, cosmetics, confirmation nonce, last respec and source under UUID keys in `jem_roles` SavedData.
- [ ] Implement load migration that preserves unknown records in quarantine and exposes provisional Human baseline without saving over them.
- [ ] Implement the shared identity policy: integrated singleplayer returns `VERIFIED_LOCAL_OWNER`; an online-mode dedicated connection returns its authenticated profile UUID; an offline-mode dedicated connection returns `VERIFIED_SERVER_LOGIN` only when the exact Server Login `AuthGate` reports the same live connection unlocked. Missing/locked providers fail closed without disabling unrelated players or singleplayer.

### Task 3: Implement Selection, Grace and Administrative Policy

**Files:**
- Create: `.../race/RaceSelectionService.java`
- Create: `.../race/RaceChangePolicy.java`
- Create: `.../race/ActivePlayTracker.java`
- Create: `.../command/RoleCommands.java`
- Create: `.../api/RaceChosenEvent.java`
- Create: `.../api/RaceChangedEvent.java`
- Create: `.../api/RaceRemovedEvent.java`

**Interfaces:**
```java
SelectionResult choose(ServerPlayer player, RaceId race, UUID nonce);
SelectionResult change(ServerPlayer player, RaceId race, UUID nonce, ChangeSource source);
```

- [ ] Count active play only while the existing AFK signal says active; otherwise use bounded movement/action evidence and never per-player world scans.
- [ ] Allow one free change before 36,000 active ticks, reject stale/replayed nonces, mounted/dead/combat/hosted-arena/transaction states, and remove old traits before applying new ones.
- [ ] Register `/jem role` plus permission-gated `/jemrole inspect` and `/jemrole set ... confirm`; audit every accepted administrative mutation.

### Task 4: Implement Attribute Traits

**Files:**
- Create: `.../trait/RaceTraitRuntime.java`
- Create: `.../trait/ElfMovementTrait.java`
- Create: `.../trait/DwarfKnockbackTrait.java`
- Create: `.../trait/FallDamageTrait.java`
- Create: `.../trait/MiningTrait.java`
- Create: `.../trait/ToolDurabilityTrait.java`
- Create: `.../config/RolesConfig.java`

- [ ] Apply Elf `+10%` on-foot movement with one fixed modifier; remove it while riding, swimming, fall-flying or otherwise not grounded locomotion.
- [ ] Apply Dwarf `+0.10` knockback resistance with one fixed modifier.
- [ ] Reduce Elf ordinary on-foot fall damage by 10%; combine with future Miner Stone Step through a shared 20% cap without touching void/scripted damage.
- [ ] Apply Dwarf 1.10 mining progress only for genuine players using an appropriate tool on tag-qualified stone/ore/rock; exclude fake players and Create machinery.
- [ ] Roll one server-side 10% durability prevention for a completed eligible harvest when a real durability loss would occur.

### Task 5: Implement Trade, Projectile and Profession Bridges

**Files:**
- Create: `.../trait/HumanTradeTrait.java`
- Create: `.../trait/ElfProjectileTrait.java`
- Create: `.../compat/professions/ProfessionRaceBridge.java`
- Create: `.../compat/irons/IronsRaceBridge.java`
- Create: `.../mixin/MerchantOfferMixin.java`
- Create: `.../mixin/ProjectileShotContextMixin.java`
- Create: `.../shot/ShotContext.java`
- Create: `.../shot/ShotLedger.java`
- Create: `.../resources/jem_roles.mixins.json`

- [ ] Compute Human emerald-buy price as `max(1, ceil(nativeFinalCost * 0.90))` in a per-player merchant path; never change selling rewards, ledger shops or shared offers.
- [ ] Capture projectile owner, firing item identity and server shot nonce at launch; grant Elf `+1.0` once to a validated bow/crossbow projectile hit before ordinary mitigation, excluding fireworks, potions, spells, tridents and PvP by default.
- [ ] Apply Human `+5%` only to an accepted `ProfessionProgressEvent`, with a persisted +250 UTC-day bonus cap.
- [ ] Keep the Human Iron adapter class unloaded unless `irons_spellbooks` is present; accept only the audited direct-hit event contract and dedupe by cast/hit identity.

### Task 6: Add Network DTOs and Screens

**Files:**
- Create: `.../network/RolesNetwork.java`
- Create: `.../network/RaceStatusPacket.java`
- Create: `.../network/RaceChoosePacket.java`
- Create: `.../client/RaceSelectionScreen.java`
- Create: `.../client/RaceStatusScreen.java`
- Create: `.../client/RaceCard.java`
- Create: `.../client/RoleKeyMappings.java`
- Create: `.../client/RoleClientState.java`

- [ ] Sync only race/status/config DTOs and server-issued nonce; validate every C2S choice on the server thread.
- [ ] Implement three equal cards, preview, exact confirmation copy, dismiss behavior and provisional baseline messaging using existing brown/copper/cream JEM geometry and semantic item icons.
- [ ] Register an unbound `RPG Roles` key and keep `/jem role` as the reliable entry point.
- [ ] Show unavailable identity, Iron and physical-height reasons directly in the relevant trait lines.

### Task 7: Add Data, Localization and Navigation Adapter

**Files:**
- Create: `Mods/JEMRoles/src/main/resources/assets/jem_roles/lang/en_us.json`
- Create: `Mods/JEMRoles/src/main/resources/assets/jem_roles/lang/ru_ru.json`
- Create: `Mods/JEMRoles/src/main/resources/data/jem_roles/advancements/community_helper.json`
- Create: `.../mountain_workshop.json`
- Create: `.../forest_path.json`
- Modify: `Mods/JEMServer/src/main/java/com/siirio/jemserver/client/smp/SmpSidebar.java`
- Modify: `Docs/DEVELOPMENT.md`
- Modify: `Docs/smp-gui/README.md`
- Modify: `CHANGELOG.md`

- [ ] Add optional cosmetic-only advancement criteria backed by genuine JEM signals and no valuable rewards.
- [ ] Add a semantically iconed Roles entry to existing navigation only when `jem_roles` is loaded.
- [ ] Record `JEMRoles` as canonical race owner, the safe-mode identity behavior and the user-supplied product decisions in owning docs/changelog.

