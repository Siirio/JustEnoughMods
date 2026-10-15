# Race VFX and Nether Runtime Implementation Plan

> **For the implementing agent:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Subagent execution is optional only when the user explicitly requests delegation.

**Goal:** Repair the confirmed Piglin null-target crash and false Nether fire-hit feedback, then give every current race ability a distinct, layered, performant Minecraft-native visual timeline.

**Architecture:** Existing server owners keep all gameplay validation and emit one bounded `RaceVfxPacket` only after a real state transition. A client-only controller composes Lodestone particles with a small camera-relative geometry renderer; quality presets reduce decorative layers without hiding critical cues. No command or synthetic gameplay route can trigger the catalog.

**Tech Stack:** Java 17, Forge 1.20.1-47.4.10, SimpleChannel protocol 3, Lodestone 1.20.1-1.6.4.1, Mojmap, ForgeConfigSpec, `RenderLevelStageEvent`, Registry Atlas, native JEM Pack Sync.

**Spec:** `Docs/superpowers/specs/2026-10-16-race-vfx-and-nether-runtime-design.md`

## Global Constraints

- Preserve server authority for damage, healing, movement, targets, cooldowns, progression, temporary blocks and zones.
- Preserve all current race balance values and the five-race/three-tier contract.
- Never pass null through `LivingChangeTargetEvent.setNewTarget`; cancel protected acquisition instead.
- Nether Hearteds must receive neither fire/lava health damage nor vanilla hurt feedback.
- No artificial camera shake, full-screen flash, custom GLSL shader, per-particle packet or unbounded visual collection.
- Critical shapes render at every quality preset; presets scale only decorative layers.
- Use the exact installed Lodestone `1.20.1-1.6.4.1`; add no new runtime library.
- Do not create or run automated tests, fixtures or test scenarios. Project policy overrides generic TDD instructions.
- Do not launch `test jem`, stable `JustEnoughMods`, a disposable server or MineRENT without separate explicit authorization.
- Production compilation, ordinary local dedicated-server synchronization and native AutoModpack publication are authorized; Gradle test tasks remain excluded.
- Do not claim Embeddium/Oculus/Photon/Complementary compatibility without an authorized live client inspection.

## Review Focus

- Brain-driven Piglin targeting: protected acquisition is canceled and no target value becomes null inside vanilla `StartAttacking`; Task 1 inspects both Forge event semantics and the final handler.
- Fire source variants: `in_fire`, `on_fire`, lava and modded sources carrying `DamageTypeTags.IS_FIRE` are rejected before hurt feedback; Task 1 keeps a later defense-in-depth guard.
- Packet abuse and stale clients: invalid ordinals, non-finite coordinates, excessive durations, wrong dimensions and targets beyond range cannot allocate persistent client state; Task 2 validates every field and raises the protocol version.
- Effect storms: multiple players casting simultaneously cannot remove critical cues or grow active instances without a cap; Task 3 separates critical and decorative budgets and expires all timelines.
- World lifecycle and shaders: logout, dimension unload and Oculus shadow passes clear or skip render state; Task 3 owns cleanup and the conservative shadow-pass adapter.

---

## File Structure

### Existing files to modify

- `Mods/JEMRoles/build.gradle` — compile against exact installed Lodestone.
- `Mods/JEMRoles/src/main/resources/META-INF/mods.toml` — declare exact Lodestone dependency.
- `Mods/JEMRoles/src/main/java/com/siirio/jemroles/JemRoles.java` — register client config and command/runtime subscribers.
- `Mods/JEMRoles/src/main/java/com/siirio/jemroles/network/RolesNetwork.java` — protocol 3 and S2C VFX registration/send helpers.
- `Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceTraitRuntime.java` — crash/fire repair plus accepted passive-transition VFX events.
- `Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceAbilities.java` — replace direct generic particle calls with authoritative VFX events.
- `Catalog/changes/records/races-five-lineages.json` — protocol, repair and VFX ownership history.
- `Docs/races/README.md`, `CHANGELOG.md` — current behavior and verification boundary.

### New JEM Roles files

- `network/RaceVfxPacket.java` — closed, bounded wire payload.
- `vfx/RaceVfxId.java` — stable visual event enum shared by server and client.
- `vfx/RaceVfxEmitter.java` — nearby-player packet distribution; no gameplay logic.
- `vfx/RaceVfxClientConfig.java` — `LOW/MEDIUM/HIGH/ULTRA`, default `HIGH`.
- `client/vfx/RaceVfxController.java` — active timeline lifecycle, budgets and cleanup.
- `client/vfx/RaceVfxInstance.java` — immutable accepted packet plus client start tick.
- `client/vfx/RaceVfxRenderer.java` — rings, seams, arcs, cracks and pillars.
- `client/vfx/RaceVfxParticles.java` — Lodestone family recipes and deterministic emission.
- `client/vfx/RaceVfxShaderState.java` — optional Oculus shadow-pass check without hard linking.
- `client/vfx/RaceVfxPalettes.java` — immutable per-race colors and interpolation helpers.

### Taste Engine files

- `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/knowledge-sources/29_jem_minecraft_race_vfx_master_brief.md`
- `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/pairs/minecraft-runtime-vfx/forge-1.20.1-jem-race-abilities/TASTE.md`
- `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/pairs/minecraft-runtime-vfx/forge-1.20.1-jem-race-abilities/TECHNICAL.md`
- `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/knowledge-sources/NAVIGATOR.md`

---

### Task 1: Repair Piglin targeting and early fire immunity

**Files:**
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceTraitRuntime.java:38-43,188-209,230-242,324-333`

**Interfaces:**
- Consumes: `RaceApi.hasRace(ServerPlayer, RaceId)`, Forge `LivingAttackEvent`, `LivingHurtEvent`, `LivingChangeTargetEvent`.
- Produces: `private static boolean protectedTarget(LivingChangeTargetEvent event, ServerPlayer player, RaceId race)` and early attack cancellation used by no other module.

- [ ] **Step 1: Confirm the exact failing state before editing**

Read `Server/crash-reports/crash-2026-10-16_01.22.28-server.txt` and Forge 47.4.10 `LivingChangeTargetEvent.java`. Record in the working notes that entity `minecraft:piglin` failed in `StartAttacking`, and that Forge requires cancellation to keep a target unchanged.

- [ ] **Step 2: Add the early fire attack guard**

Import `LivingAttackEvent` and add:

```java
@SubscribeEvent(priority = EventPriority.HIGHEST)
public static void attack(LivingAttackEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)
            || !RaceApi.hasRace(player, RaceId.NETHER_HEARTEDS)
            || !event.getSource().is(DamageTypeTags.IS_FIRE)) return;
    event.setCanceled(true);
    player.clearFire();
}
```

Keep the existing `LivingHurtEvent` cancellation as defense in depth. Move `player.clearFire()` in `lavaMovement` before the `isInLava` return so residual fire ticks are removed immediately after leaving lava.

- [ ] **Step 3: Replace null target mutation with cancellation**

Change the three friendship branches to compute one `protectedTarget` boolean and call `event.setCanceled(true)` when it is true. Delete every `event.setNewTarget(null)` call. Do not clear another mob target or Brain memory from inside the event.

```java
boolean protectedTarget = race == RaceId.VOID_WALKERS && endKin(actor) && !endBoss(actor)
        || race == RaceId.BORN_IN_BLOOM && RaceEnvironment.green(player.serverLevel(), player.blockPosition()) && !(actor instanceof EnderMan)
        || race == RaceId.NETHER_HEARTEDS && actor instanceof AbstractPiglin;
if (protectedTarget) event.setCanceled(true);
```

- [ ] **Step 4: Perform permitted source verification**

Run:

```powershell
rg -n "setNewTarget\(null\)|LivingAttackEvent|clearFire" Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceTraitRuntime.java
```

Expected: no null target setter; one early attack handler; both the attack handler and per-tick Nether path clear fire.

---

### Task 2: Establish protocol 3 and the bounded VFX event contract

**Files:**
- Create: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/vfx/RaceVfxId.java`
- Create: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/network/RaceVfxPacket.java`
- Create: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/vfx/RaceVfxEmitter.java`
- Create: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/vfx/RaceVfxClientConfig.java`
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/network/RolesNetwork.java`
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/JemRoles.java`

**Interfaces:**
- Produces: `RaceVfxId` closed enum; `RaceVfxPacket(RaceVfxId id, ResourceLocation dimension, Vec3 origin, Vec3 target, int sourceEntityId, int targetEntityId, int[] relatedEntityIds, List<Vec3> path, int tier, int duration, long seed)`; `RaceVfxEmitter.near(ServerLevel, RaceVfxPacket)`.
- Consumes later: all ability hooks and `RaceVfxController.accept(RaceVfxPacket)`.

- [ ] **Step 1: Define stable visual IDs**

Create enum values grouped by race:

```java
LIFE_CHORUS, GUARDIAN_FORM, GUARDIAN_DISSOLVE, MERCHANT_BLESSING,
RIFT_DEPART, RIFT_ARRIVE, VOID_PROJECTILE_CHARGE, VOID_PROJECTILE_RELEASE,
VOID_PROJECTILE_IMPACT, VOID_RESCUE, GRAVITY_DENIAL,
VERDANT_DOMINION, LIVING_PATH_STEP, SECOND_BLOSSOM, PHOTOSYNTHESIS,
SCULK_VEINS, ECHO_VISION, ECHO_ABSORB, RESONANCE_THRESHOLD,
INFERNAL_TELEGRAPH, INFERNAL_IMPACT, VOLCANIC_ZONE,
LAVA_DRINK_START, LAVA_DRINK_COMPLETE, LAVA_SWIM, WATER_STEAM
```

Do not encode display text or gameplay numbers in the enum.

- [ ] **Step 2: Implement bounded packet encoding**

Encode the enum, a dimension string capped at 128 characters, six finite doubles, two primary VarInts, at most eight related entity IDs, at most sixteen sampled path points, tier, duration and seed. Decode into an immutable defensive copy; `handle` must call `RaceVfxPacket.valid()` before scheduling any client work. Validation rejects tier outside `0..3`, duration outside `1..200`, primary entity IDs below `-1`, related IDs below `0`, oversized collections, non-finite coordinates, and any target/path point more than 128 blocks from origin. Invalid payloads produce no accepted client instance.

- [ ] **Step 3: Register protocol 3**

Change `RolesNetwork.VERSION` from `"2"` to `"3"`; register `RaceVfxPacket` as discriminator `3`, `PLAY_TO_CLIENT`, and dispatch only through `DistExecutor` to `RaceVfxController.accept(packet)`. Expose a package-safe `sendNear(ServerLevel, Vec3, RaceVfxPacket)` using `PacketDistributor.NEAR` with a 96-block radius.

- [ ] **Step 4: Add client quality config**

Create `RaceVfxClientConfig` with enum `Quality { LOW, MEDIUM, HIGH, ULTRA }`, default `HIGH`, and register its `ForgeConfigSpec` as `ModConfig.Type.CLIENT` under `jem-races-vfx-client.toml`. Provide exact budget methods:

```java
public static int decorativeParticleLimit() { return switch (QUALITY.get()) { case LOW -> 96; case MEDIUM -> 192; case HIGH -> 384; case ULTRA -> 640; }; }
public static int trailSamples() { return switch (QUALITY.get()) { case LOW -> 4; case MEDIUM -> 6; case HIGH -> 9; case ULTRA -> 12; }; }
public static double decorativeDistance() { return switch (QUALITY.get()) { case LOW -> 24; case MEDIUM -> 40; case HIGH -> 64; case ULTRA -> 96; }; }
```

- [ ] **Step 5: Inspect the contract statically**

Run `rg -n "VERSION =|messageBuilder\(RaceVfxPacket|decorativeParticleLimit|duration > 200|Double\.isFinite" Mods/JEMRoles/src/main/java`. Expected: protocol 3, one S2C registration, explicit finite/range validation and all four budgets.

---

### Task 3: Build the client lifecycle, geometry renderer and Lodestone recipes

**Files:**
- Modify: `Mods/JEMRoles/build.gradle`
- Modify: `Mods/JEMRoles/src/main/resources/META-INF/mods.toml`
- Create: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxInstance.java`
- Create: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxController.java`
- Create: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxRenderer.java`
- Create: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxParticles.java`
- Create: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxShaderState.java`
- Create: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxPalettes.java`

**Interfaces:**
- Consumes: valid `RaceVfxPacket`, `RaceVfxClientConfig`, exact Lodestone registry particles.
- Produces: `RaceVfxController.accept`, `clear`, `tick`; renderer primitives `ring`, `arc`, `seam`, `crack`, `pillar`; particle family methods used by Tasks 4–6.

- [ ] **Step 1: Add the exact Lodestone dependency**

Add `compileOnly fg.deobf(files('../../Server/mods/lodestone-1.20.1-1.6.4.1.jar'))` and a mandatory `lodestone` dependency with version range `[1.6.4.1,1.6.5)` on `BOTH`. Do not copy Lodestone classes or assets.

- [ ] **Step 2: Define immutable client instances**

`RaceVfxInstance` stores the packet, `startedTick`, `expiresTick`, and a deterministic `RandomSource` seeded from the packet. Provide `double progress(double now)` clamped to `0..1` and `boolean expired(long now)`.

- [ ] **Step 3: Implement bounded lifecycle ownership**

Use an insertion-ordered map capped at 128 active critical instances and a separate integer decorative count reset each client tick. `accept` rejects invalid/wrong-dimension/out-of-range packets and evicts only the oldest expired or non-critical item before admitting a critical cue. `ClientPlayerNetworkEvent.LoggingOut` and client `LevelEvent.Unload` call `clear()`.

- [ ] **Step 4: Implement conservative shader state**

Reuse the existing JEM pattern: reflectively call Oculus/Iris `isRenderingShadowPass` only when `oculus` is loaded. Failure returns false and never prevents ordinary rendering.

- [ ] **Step 5: Implement critical geometry**

Subscribe at `RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS`, skip shadow passes, translate the pose stack by negative camera position, and render through one bounded translucent `RenderType`. Implement these exact primitives:

```java
ring(VertexConsumer out, PoseStack.Pose pose, Vec3 center, double radius, double width, int color, int alpha, int segments)
arc(VertexConsumer out, PoseStack.Pose pose, Vec3 from, Vec3 to, double width, int color, int alpha)
seam(VertexConsumer out, PoseStack.Pose pose, Vec3 from, Vec3 to, double width, int color, int alpha)
crack(VertexConsumer out, PoseStack.Pose pose, Vec3 center, Vec3 direction, double length, int branches, int color, int alpha, long seed)
pillar(VertexConsumer out, PoseStack.Pose pose, Vec3 base, double height, double radius, int color, int alpha)
```

Ground shapes use a `0.025F` offset and a dark readable underlay before decorative glow. Critical segment counts never fall below 24 for rings or 6 for cracks.

- [ ] **Step 6: Implement reusable Lodestone particle recipes**

Use `WorldParticleBuilder` with Lodestone `WISP`, `SPARK`, `EXTRUDING_SPARK`, `TWINKLE`, `SMOKE` and `ITEM` particles. Centralize scale/transparency/color easing with `GenericParticleData` and `ColorParticleData`; expose named methods `ancestralMotes`, `voidFragments`, `leafFragments`, `sculkVibrations`, `embers`, and `terrainDebris`. Every method consumes a requested count through the decorative budget before spawning.

- [ ] **Step 7: Inspect lifecycle and render safety**

Run `rg -n "AFTER_TRANSLUCENT_BLOCKS|shadowPass|LoggingOut|LevelEvent.Unload|128|decorativeParticleLimit|WorldParticleBuilder" Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx`. Expected: one render stage owner, both cleanup hooks, hard active cap and one budget owner.

---

### Task 4: Implement Ancestors and Void timelines

**Files:**
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxController.java`
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxRenderer.java`
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxParticles.java`
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceAbilities.java`
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceTraitRuntime.java`

**Interfaces:**
- Emits/consumes: Ancestors and Void `RaceVfxId` values from Task 2.
- Preserves: existing healing, guardian, merchant, teleport, projectile and gravity gameplay results.

- [ ] **Step 1: Emit accepted Ancestor events**

Emit `LIFE_CHORUS` after eligible targets have been collected and affected; put at most seven actually affected target entity IDs into the bounded related-entity field so the client never scans the world to guess recipients. Emit `GUARDIAN_FORM` only after `addFreshEntity` succeeds, then emit `GUARDIAN_DISSOLVE` immediately before the timed guardian is discarded by its existing server-owned expiry path. Emit `MERCHANT_BLESSING` only when at least one native emerald offer receives a real reduction.

- [ ] **Step 2: Compose Ancestor phases**

At progress `0..0.30`, contract an ivory-gold ring; at `0.20..0.65`, create seven bounded ancestral arcs; at `0.55..1.0`, raise motes from affected nearby living entities. Guardian formation builds blocky fragments feet-to-head and flashes the eyes once; the dissolve timeline falls from head to feet and completes before the server removes the guardian. Merchant blessing spawns at most eight coin-gold twinkles at menu open.

- [ ] **Step 3: Emit accepted Void events**

Emit departure and arrival as separate confirmed events around `riftAmbush`; charge/release/impact around the existing ender-pearl lifecycle; `VOID_RESCUE` at invalid and safe positions only after a safe destination exists; `GRAVITY_DENIAL` only after cooldown state is written.

- [ ] **Step 4: Compose Void phases**

Departures implode toward the center; arrivals expand as a directional crescent. Projectile charge tightens fragments toward the hand, release owns a sampled trail, and impact collapses inward. Rescue uses a sharp invalid-position fracture and soft safe-position reassembly. Gravity uses sparse upward fragments with no screen overlay.

- [ ] **Step 5: Remove replaced generic bursts**

Delete direct `HEART` and generic `PORTAL` bursts owned by these abilities only after their VFX events exist. Do not remove gameplay sounds or particles owned by unrelated mods.

---

### Task 5: Implement Bloom and Sculk timelines

**Files:**
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxController.java`
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxRenderer.java`
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxParticles.java`
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceAbilities.java`
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceTraitRuntime.java`

**Interfaces:**
- Emits/consumes: Bloom and Sculk `RaceVfxId` values.
- Preserves: 25-second Verdant Dominion, 20-second Sculk Veins, connected-sculk search, actual placed path blocks and existing status effects.

- [ ] **Step 1: Emit Bloom transitions**

Emit `VERDANT_DOMINION` once with tier and radius after `rooted.size() > 0`; emit a throttled `LIVING_PATH_STEP` no more than once every four ticks when at least one leaf block was newly placed; emit `SECOND_BLOSSOM` after death cancellation and recovery state are applied; emit `PHOTOSYNTHESIS` only on an actual half-heart heal tick.

- [ ] **Step 2: Compose Bloom phases**

Ground root arcs travel to affected targets and clamp at feet; tier two branches once from rooted positions; tier three keeps a low-density grove perimeter. Living Path traces only new leaf edges and never delays block placement. Second Blossom closes then opens one bud silhouette with petals; photosynthesis uses at most six upward motes.

- [ ] **Step 3: Emit Sculk transitions**

Retain the BFS result, deterministically sample it down to at most sixteen ordered positions, and emit `SCULK_VEINS` from start to confirmed destination before teleport; emit `ECHO_VISION` after applying glow and include at most eight newly revealed entity IDs; emit `ECHO_ABSORB` after consuming 25 resonance; emit `RESONANCE_THRESHOLD` only when crossing 25, 50 or 75 resonance, not on every kill.

- [ ] **Step 4: Compose Sculk phases**

Render bounded cyan vibration arcs along the start/destination direction, pulse the destination twice, and use dark-teal dissolve/reform columns. Echo Vision expands one ring and briefly pulses already-glowing targets. Echo Absorb pulls motes inward then releases a compact shell. Threshold feedback is one short chest pulse.

- [ ] **Step 5: Remove replaced generic bursts**

Delete the generic `HAPPY_VILLAGER` and `SCULK_SOUL` ability bursts only where the new event owns presentation. Keep vanilla glowing and gameplay block/state feedback.

---

### Task 6: Implement Nether timelines and passive transition throttles

**Files:**
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxController.java`
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxRenderer.java`
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/vfx/RaceVfxParticles.java`
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceAbilities.java`
- Modify: `Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceTraitRuntime.java`

**Interfaces:**
- Emits/consumes: Nether `RaceVfxId` values.
- Preserves: exact eruption impact ticks, tier damage/radius, volcanic zone, lava movement, bucket consumption/healing and water stiffness.

- [ ] **Step 1: Separate eruption telegraph and impact events**

Emit `INFERNAL_TELEGRAPH` when each `Eruption` is queued with duration equal to `impactAt - now`; emit `INFERNAL_IMPACT` immediately before damage/ignite/push; emit `VOLCANIC_ZONE` only when tier three creates the zone. Preserve the current ordered `8 + i * 3` impact timing.

- [ ] **Step 2: Compose the eruption**

Telegraph uses a dark-red ground crack with a bright core tightening toward impact. Impact raises a short opaque-underlay pillar plus extruding sparks, terrain debris and one ground shock ring. Chained fissures share palette but retain distinct deterministic seeds. The zone uses sparse perimeter embers and periodic wisps without smoke fill.

- [ ] **Step 3: Compose lava utility and movement**

Emit `LAVA_DRINK_START` only when native item use starts and `LAVA_DRINK_COMPLETE` only after the bucket becomes empty and healing succeeds. Emit `LAVA_SWIM` at most every ten ticks while moving faster than `0.02` blocks/tick. Emit `WATER_STEAM` only when water stiffness crosses from zero to positive or returns to zero.

- [ ] **Step 4: Remove old pillar spam**

Delete the seven-layer server `sendParticles` loop and generic 35-particle impact burst after the event-driven pillar exists. Keep the authoritative `Eruption` and `Zone` collections server-side.

- [ ] **Step 5: Inspect feedback invariants**

Run `rg -n "sendParticles|INFERNAL_TELEGRAPH|INFERNAL_IMPACT|LivingAttackEvent|setNewTarget\(null\)" Mods/JEMRoles/src/main/java`. Expected: no old race-owned generic burst path, both Nether phases present, early fire guard present and no null target setter.

---

### Task 7: Record Taste Engine and canonical project knowledge

**Files:**
- Create/modify the Taste Engine files listed under File Structure.
- Modify: `Catalog/changes/records/races-five-lineages.json`
- Modify: `Docs/races/README.md`
- Modify: `CHANGELOG.md`

**Interfaces:**
- Produces: exact reusable taste/technical pair; Atlas coverage for every changed watched source; current player documentation.

- [ ] **Step 1: Preserve the supplied brief as raw evidence**

Copy the user-supplied “Minecraft VFX Master — Visual Effects Engineering” text into the numbered flat knowledge source without rewriting it as instructions to unrelated capabilities.

- [ ] **Step 2: Create the exact Taste Engine pair**

`TASTE.md` status is `READY` and records crisp Minecraft silhouettes, distinct motion families, controlled glow, readable timing, limited smoke and rejection of generic mobile circles/recolored clones. `TECHNICAL.md` status is `PROVISIONAL` and records Forge/Lodestone APIs, budgets, render stage, server-authority packet flow and the pending shader/client validation. Add one exact Navigator row under Minecraft runtime VFX.

- [ ] **Step 3: Update Registry Atlas history and source ownership**

Change the recorded matching protocol from 2 to 3, add all new watched JEM Roles Java/config files to `cause.source_files`, and append a 2026-10-16 `SELF` history entry describing the null-target repair, early fire cancellation and client-only VFX presentation. Do not represent visual particles as gameplay modifiers; document them as presentation and network consequences.

- [ ] **Step 4: Update owning docs and changelog**

Document quality presets, the absence of any VFX command, exact no-camera-shake rule, AutoModpack matching-client requirement and shader verification boundary. Record both bugs using the required `Bug / side / root cause / in testing` format.

- [ ] **Step 5: Rebuild and validate Atlas**

Run:

```powershell
python Catalog/generator/maintain_atlas.py rebuild
python Catalog/generator/maintain_atlas.py audit
python Catalog/generator/maintain_atlas.py scan
python Catalog/generator/validate_atlas.py
python Catalog/generator/query_atlas.py behavior minecraft:piglin
python Catalog/generator/query_atlas.py behavior minecraft:lava
```

Expected: no uncovered changed source, schema valid, both representative queries show the updated race overlay.

---

### Task 8: Build, synchronize, publish and verify the ordinary local server

**Files:**
- Build output: `Mods/JEMRoles/build/libs/jem-roles-2.0.0.jar`
- Replace: `Client/ManagedPack/mods/jem-roles-2.0.0.jar`
- Replace: `Server/mods/jem-roles-2.0.0.jar`
- Update generated pack state through `tools/jem.py` and native `/jempack` commands.

**Interfaces:**
- Consumes: all earlier tasks.
- Produces: matching production artifacts, restarted ordinary server and downloadable native AutoModpack generation.

- [ ] **Step 1: Perform final source checks**

Run JSON parsing for changed resources, `git diff --check`, the null-target/generic-burst searches from Tasks 1 and 6, and confirm all intended files are covered by the Atlas record. Do not run tests, linters or formatters.

- [ ] **Step 2: Build the production artifact**

Run from `Mods/JEMRoles`:

```powershell
.\gradlew.bat build -x test
```

Expected: `BUILD SUCCESSFUL`, reobfuscated JAR present, no client-only class referenced from common/server initialization.

- [ ] **Step 3: Synchronize exact hashes**

Stop the ordinary local server if running, copy the fresh JAR to the two exact targets, and compare SHA-256 of build, Client and Server copies. Expected: all three hashes identical. Do not touch the stable `JustEnoughMods` profile.

- [ ] **Step 4: Stage and publish AutoModpack**

Run `python tools/jem.py stage-pack-sync`, start the ordinary local dedicated server only as required, execute native `/jempack preview`, inspect its add/replace/delete set, then `/jempack publish`. Run `python tools/jem.py status`; expected `packSyncPublication.published: true` and identical staged/published manifest SHA-1.

- [ ] **Step 5: Verify server startup without gameplay QA**

Inspect `Server/logs/latest.log` for Forge `Done`, JEM Roles protocol/resource load errors, null-target exceptions and watchdog failures. Confirm port `31604` listens. Do not enter the world or launch a client.

- [ ] **Step 6: Deliver exact Git state**

Fetch `origin/main`, inspect divergence and all tracked/untracked changes, stage only task-owned paths, commit with a short professional message, push normally, fetch again and verify `HEAD == origin/main`. If an in-scope tracked file changes after the push, commit and push the newer complete snapshot.

- [ ] **Step 7: Report the verification boundary**

Report commit/hash, server state, artifact hashes, AutoModpack generation/hash, Atlas result and the untouched stable profile. State explicitly that actual appearance, FPS and Oculus/Photon/Complementary behavior remain `in testing` until the user separately authorizes the `test jem` client launch.
