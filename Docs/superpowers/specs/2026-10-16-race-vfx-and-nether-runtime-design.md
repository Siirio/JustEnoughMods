# JEM Race VFX and Nether Runtime Repair Design

## Status and intent

Approved conversational direction: replace the current one-emitter race presentation with a small reusable client VFX system whose effects have deliberate anticipation, activation, impact and dissipation stages. The same task repairs the confirmed Nether Hearteds fire-feedback regression and the confirmed Piglin targeting crash without changing race balance, cooldowns, mastery, damage areas or server authority.

The desired result is crisp Minecraft-native VFX with distinct silhouettes, controlled glow, restrained density and accurate gameplay communication. It must not look like one recolored particle burst reused five times, and it must not introduce artificial camera shake.

## Evidence-backed defect chain

### Piglin crash

The 2026-10-16 01:22:28 crash is a `NullPointerException` in vanilla `StartAttacking` while ticking a `minecraft:piglin` beside the player in the Nether. `RaceTraitRuntime.target` currently handles Nether Hearteds friendship by calling `LivingChangeTargetEvent.setNewTarget(null)`. Forge 47.4.10 documents this event as cancelable and states that cancellation keeps the previous target; its `setNewTarget` method itself does not reject null. Vanilla's declarative Piglin behavior then sends the supplied target through `MemoryAccessor` and `Optional.of`, where null is illegal. The 01:23:34 watchdog report is a consequence of the already-failed server taking more than 60 seconds to terminate, not an independent gameplay defect.

The repair cancels an attempted protected target acquisition instead of replacing the target with null. The same safe cancellation route applies to the existing Void-kin and green-biome friendship rules so another Brain-driven mob cannot hit the same invalid state.

### Nether fire feedback

Nether Hearteds currently cancel tagged fire damage only at `LivingHurtEvent`. In the vanilla/Forge damage lifecycle this hook is late enough that attack acknowledgement and hurt presentation may already begin even though the resulting damage amount becomes zero. Lava movement also clears fire only while the player remains in lava.

The repair rejects tagged fire attacks at `LivingAttackEvent` with highest priority, before hurt feedback and damage processing. The later hurt guard remains as defense in depth. The race tick clears residual fire state for a Nether Hearted both in lava and immediately after leaving it. This preserves full fire/lava immunity while removing false damage feedback.

## Ownership and data flow

Gameplay stays in the existing server owners:

1. Input reaches the current server-validated race action.
2. `RaceAbilities` validates race, cooldown, target, tier and effect result.
3. Only after an accepted transition does the server emit a compact visual event containing an effect ID, origin, orientation, tier and bounded entity or position references.
4. Nearby matching clients create a local visual timeline. Clients never decide damage, healing, teleport destination, blocks, targets, cooldowns or progression.
5. Persistent/passive visuals derive from synchronized race state or explicit bounded start/stop events; they do not reconstruct gameplay authority from particles.

The existing `jem_roles:main` channel advances to a matching protocol version because older clients do not understand the new visual event. AutoModpack distributes the exact matching JAR.

## Client VFX foundation

The foundation remains inside JEM Roles and consists of four small responsibilities:

- `RaceVfxPacket`: bounded server-to-client event data with a closed effect enum and validated payload sizes.
- `RaceVfxController`: owns active timelines, distance checks, preset budgets and cleanup.
- `RaceVfxRenderer`: draws only the critical world-space shapes needed for readable telegraphs, rings, seams and arcs using camera-relative interpolation and conservative vanilla render state.
- `RaceVfxParticles`: composes Lodestone 1.20.1-1.6.4.1 particles and vanilla terrain/item particles into named family recipes.

Lodestone is already present in both canonical Client and Server packs. JEM Roles will compile against that exact installed version and declare the matching dependency. Its builders provide animated scale, transparency, color, spin, motion, lifetime, culling, circles and lines without adding another runtime library. Custom JEM geometry is limited to shapes particles cannot communicate precisely; no general-purpose rendering framework or custom GLSL shader is introduced.

Every timeline follows named phases rather than scattering particle calls through gameplay code. Partial-tick interpolation is used for moving visual anchors. Completed timelines remove themselves, entity-bound effects stop if their entity disappears, and dimension changes clear incompatible instances.

## Visual language by race

### Ancestors

- Life Chorus: a restrained ivory-gold floor sigil contracts during anticipation, seven short ancestral arcs connect eligible allies, then soft upward motes and one clean healing pulse rise from each actually affected target. Tier upgrades add longer links and an absorption crown without increasing noise everywhere.
- Guardian: blocky amber fragments sketch the golem silhouette from feet upward before a brief gold eye-flash; expiry dissolves the fragments downward rather than popping the entity out visually.
- Merchant blessing and animal affinity remain quiet passives. A short coin-gold glint on opening an actually discounted trade is sufficient; permanent ambient emitters are forbidden.

### Void Walkers

- Rift Ambush: the departure point collapses inward through dark fragments, a narrow violet seam traces the real travel direction, and the arrival produces a directional crescent behind the target. The tier-two echo uses a dim unstable silhouette and orbiting fragments rather than a visible debug name.
- Charged projectile: charge particles tighten toward the hand, release forms a compact projectile trail, and impact folds inward before teleportation.
- Water rejection and void rescue: a sharp cyan-violet fracture appears at the invalid position and a softer reassembly effect marks the confirmed safe destination.
- Gravity Denial: sparse fragments drift upward around the player with a slow vertical pulse; no full-screen overlay or camera movement.

### Born in Bloom

- Verdant Dominion: root lines race along the ground toward actual affected targets, clamp around their feet, then shed small leaf fragments. Tier-two propagation visibly branches from rooted targets. Tier-three grove grows a readable circular boundary of stems and petals before settling into a low-noise ambient ring.
- Living Path: each confirmed temporary leaf placement receives a quick edge-growing leaf trace. It must not obscure the blocks or add delay to placement.
- Second Blossom: a closed bud shape forms during the death interception, opens in one strong petal burst and leaves a short green-gold recovery spiral.
- Photosynthesis stays subtle: only an occasional upward dust mote while healing actually occurs.

### Infected by Sculk

- Sculk Veins: cyan vibration paths crawl from the starting sculk through the selected connected route; the destination pulses twice, then the player dissolves and reforms through a dark-teal vertical echo.
- Echo Vision: one expanding vibration ring leaves the player, then newly revealed living targets receive a brief sculk-outline pulse. The persistent vanilla glow remains the readability owner.
- Echo absorption: nearby dark motes are pulled inward, compress at the chest and release a short cyan absorption shell.
- Resonance gain is represented only on meaningful thresholds to avoid a particle burst on every kill.

### Nether Hearteds

- Infernal Eruption: an ember crack draws the exact fissure position first, its color tightens from dark red to orange as impact approaches, then a fast blocky column erupts with directional sparks, small terrain debris and a short ground shock ring. Chained tiers fire in visibly ordered sequence. The volcanic zone retains a sparse perimeter of embers and periodic heat wisps rather than filling its area with smoke.
- Lava drink: embers spiral toward the held bucket during the native drink pose; completion produces one contained orange core pulse.
- Lava swimming: short trailing embers follow velocity and immediately dissipate. It has no damage flash and never fakes a hazardous state.
- Water stiffness uses faint steam at exposure transitions, not continuous opaque clouds.

## Quality and performance

The client config exposes `LOW`, `MEDIUM`, `HIGH` and `ULTRA`, defaulting to `HIGH`.

- Critical geometry, target markers and final timing cues always render.
- Presets scale decorative particle count, trail sampling, secondary fragments and ambient frequency.
- Distance bands stop decorative spawning before critical shapes; entity and position frustum checks are used where available.
- Builders and immutable effect recipes are reused; render loops avoid world scans and per-frame collection churn.
- Network traffic is one accepted-effect event plus narrowly required phase events, never per-particle packets.
- Simultaneous-effect budgets are global and per-family so one player cannot starve other critical telegraphs.

## Shader and depth policy

Critical shapes use conservative depth-tested world geometry with a small surface offset to avoid z-fighting. Additive glow is decorative; a readable alpha/color silhouette remains underneath it. Effects must remain intelligible in daylight, caves and Nether lighting without relying on bloom.

Compilation cannot establish compatibility with Embeddium, Oculus, Photon or Complementary. No claim of shader compatibility is made until the user explicitly authorizes launching the `test jem` profile and the actual configurations are inspected. Until then, shader-on presentation remains `in testing`, with shader-off static implementation evidence only.

## Developer preview

An operator-only dedicated-server command `/jem vfx preview <effect> [tier]` emits the same validated visual packet used by real gameplay but performs no damage, healing, movement, block placement, cooldown or mastery change. Effect IDs come from the closed visual catalog. A stop/clear subcommand removes persistent local preview timelines. The preview command is excluded from integrated singleplayer with other server-owned JEM commands.

## Taste Engine record

Create the exact pair `minecraft-runtime-vfx/forge-1.20.1-jem-race-abilities` rather than a broad Minecraft VFX profile. The supplied “Minecraft VFX Master” brief becomes a raw knowledge source. `TASTE.md` records the approved crisp Minecraft silhouettes, layered timing, controlled glow, readable telegraphs, distinct families and anti-goals such as generic mobile circles, smoke spam and recolored clones. `TECHNICAL.md` records Forge 47.4.10, Java 17, Lodestone 1.6.4.1, client/server ownership, quality budgets and the unverified Oculus/shader boundary. Taste can be `READY`; technical status stays `PROVISIONAL` until live rendering is inspected.

## Project consequence chain

Implementation updates JEM Roles source/resources, the owning race documentation, Registry Atlas change history/effective behavior, and `CHANGELOG.md`. It builds only the required production artifact with tests excluded, synchronizes the exact JAR to `Client/ManagedPack` and local `Server`, stages and publishes a fresh native AutoModpack generation, then restarts the ordinary local dedicated server. The stale crashing server is not restarted before the targeting repair is installed.

No MineRENT deployment, stable profile mutation, release-baseline promotion, automated test, disposable server or gameplay client launch is authorized by this design.

## Acceptance boundaries

Static and server acceptance for this task consists of successful production compilation, resource loading, server startup, no recurrence of the null-target Piglin crash during ordinary server operation, exact Client/Server artifact hashes, valid Atlas coverage and matching staged/published AutoModpack manifests.

Player-visible acceptance remains `in testing` until manually checked in `test jem`. Required manual scenarios are each race signature at tiers I–III, every listed utility/passive transition, multiple simultaneous effects, daylight/cave/Nether visibility, moving entities, dimension cleanup, all four quality presets, shader off, Photon and Complementary. Fire immunity must show neither health loss nor hurt jerk, and protected Piglins must remain non-hostile without server failure.
