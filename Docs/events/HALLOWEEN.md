# Halloween

Halloween is owned by JEMServer's persistent calendar lifecycle. Its default Asia/Almaty window is October 29 00:00 through November 1 00:00, end-exclusive.

The scheduler persists year, UTC boundaries and idempotent `PLANNED`, `ACTIVE`, `ENDED`, `CLEANED` transition IDs. Restart, mid-window startup and wall-clock rollback must not repeat a transition. Operator preview/force/stop and per-player presentation mute are control surfaces; neither creates a daily-login reward.

Halloween retains its saved season, calendar notices, time window, admin preview/force controls and per-player mute. The rejected calm/power activity choices and their mob chores are retired; old activity records remain readable but grant nothing.

Mute state suppresses actual lifecycle/login notices. The current implementation creates no transient block POI, so cleanup never touches world blocks or player builds.

## Halloween Darkness v2: status of the supplied package

`Halloween_Darkness_Package_v2.zip` is a design input for a future sandbox implementation. It contains one concept document and 14 visual references; it contains no Forge JAR, datapack, config, exported Blockbench asset or MineRENT-ready server payload. Instructions inside that document are reference requirements, not repository or deployment commands.

The following package ideas fit the current direction: the existing 72-real-hour Halloween window, server authority, persistent restart-safe state, darkness and lighting counterplay, a fed boss encounter, durable personal rewards, event equipment and a permanently unlocked race. Exact balance values marked as proposals in the package are not approved production settings. Its proposed `/halloween` command tree must not duplicate the existing `/jem holiday` owner. Its proposed automated tests and fixtures are not adopted because this project prohibits them.

The existing retired calm/power choice and mob chores remain retired. A Darkness implementation must extend the current calendar lifecycle rather than reactivate those records or create a second event scheduler.

### Phase 1 visual prototype status

The first sandbox art phase now contains three neutral Candy King massing studies and one refinement of the selected crawler direction in [`Docs/art/halloween-darkness-phase1-2026-10-21`](../art/halloween-darkness-phase1-2026-10-21/README.md). These are editable Generic Blockbench prototypes for direction selection only. They introduce no texture, rig, animation, hitbox, registry, renderer or runtime asset, and C1 remains an agent recommendation pending user approval.

## Sandbox boundary

Until a separate release request, all Darkness work is sandbox-only:

- use an isolated development save and development-only enablement;
- never copy sandbox player data, event progress, boss UUIDs, reward receipts, unlocks, economy balances, operator changes or forced-event flags into the production world;
- keep MineRENT, the stable `JustEnoughMods` profile and the release baseline unchanged;
- do not publish placeholder models, textures, sounds or provisional registry IDs to the normal managed pack;
- keep irreversible punishment disabled until the user explicitly chooses its exact meaning and recovery policy.

The production migration transfers reviewed source-produced artifacts and configuration, not the sandbox world. MineRENT keeps its current production world and starts a fresh production event state.

## Ownership when running on MineRENT

The feature is not server-only as a complete product. MineRENT owns decisions and persistence, while matching clients render and present them.

| Concern | Canonical owner | MineRENT/client behavior |
| --- | --- | --- |
| Calendar window and transitions | Existing JEMServer `CalendarEventScheduler` and `CalendarEventData` | MineRENT derives the wall-clock window, persists UTC boundaries and applies each transition once across restarts. |
| Event session | JEMServer Halloween extension | MineRENT owns active phase, captured world-state changes, boss identity, feed progress and cleanup status. It must bind to the existing calendar transition rather than run a second clock. |
| Encounter and territory | Existing hosted encounter and boundary lifecycle | MineRENT validates participants, arena, targeting, damage, safe arrival/exit and completion. Darkness must not bypass the shared boss lifecycle. |
| Candy, equipment and boss registrations | The selected existing JEM gameplay/data owner after the registry impact scan | Registry IDs and item/entity behavior are identical on server and client. IDs are never prototyped in KubeJS and later silently replaced. |
| Race unlock and abilities | Existing Roles/Races owner | MineRENT validates unlocks, ability use, cooldown/state and death resolution; the client only requests an action and renders the accepted result. |
| Shop and payments | Existing CurrencyCore/Economy owner and native trade surfaces | MineRENT performs one atomic validated purchase. No Halloween wallet or duplicate currency ledger is created. |
| Personal reward claims | Existing reward ledger owner | MineRENT records idempotent account receipts before presentation. Reconnect or crash cannot issue the reward again. |
| Darkness, HUD, models, animations, sounds and VFX | Matching client resources/renderers driven by bounded server snapshots and semantic cues | The client may interpolate presentation but never decides visibility eligibility, hits, damage, unlocks, rewards or event completion. |
| Distribution | `Client/ManagedPack` plus native AutoModpack | Client-required JARs/resources/configs are published as one reviewed generation. MineRENT server artifacts and the client generation belong to the same release boundary. |

### Server-side lifecycle

1. On dedicated-server startup, JEM loads the production calendar and Darkness SavedData before accepting event mutations. Missing sandbox markers never grant development privileges.
2. The existing calendar scheduler reconciles real UTC time against the configured Halloween window. A saved transition receipt prevents duplicate start/end work after restart or clock rollback.
3. On activation, the server creates or resumes one event session and records only the world properties it actually changes. The 72-hour deadline is wall-clock based, so downtime does not extend it.
4. On login or reconnect, the server sends a bounded snapshot of public event state and that player's authorized progress. Later packets carry semantic cues or versioned deltas, not client-authored outcomes.
5. Every feed, purchase, ability, attack, reward and unlock request is validated on the server thread against authenticated player identity, active session, distance/territory, inventory and replay receipt before mutation.
6. Boss damage, targeting, drops, reward eligibility, race state, cooldowns and cleanup remain server-owned. Visual telegraphs may be client-rendered but must correspond to authoritative attack windows.
7. On scheduled end, operator stop or recovery after a crash, cleanup is idempotent: restore only captured event-owned world state, remove transient modifiers/entities and retain explicitly permanent equipment, trophies and unlocks.
8. After cleanup, MineRENT persists the terminal receipt. A restart must not reactivate the same season or repeat rewards.

## Why the final release cannot be server-only

The complete v2 concept adds client-visible registry entries and presentation: equipment, candy, lamps, a boss, race/form state, packets, models, textures, animations, sounds, fog/HUD and localization. Existing clients do not understand those contracts. A server-only release is permitted only for a later change that uses already deployed registries, protocol and resources unchanged.

Before release, compare every changed module's actual channel/version and registry output against the currently deployed MineRENT and managed-client JARs. Any changed shared channel or newly exercised required channel means the exact matching client JAR must be included. A resource pack alone cannot supply Forge registries, entity AI, hitboxes, packets or authoritative gameplay.

## Sandbox-to-MineRENT migration guide

### 1. Close product blockers before production code

Record the user's decisions for irreversible death policy and recovery, exact boss participation/reward rules, Soul transfer/reacquisition, after-event availability, target dimensions and final balance. Values labelled as proposals in the supplied package remain disabled or sandbox-only until approved.

### 2. Implement through existing owners

Run the Registry Atlas lookup and before-change impact scan before adding gameplay. Extend the existing calendar, hosted encounter, rewards, Roles/Races and Currency owners; do not create parallel clocks, wallets, race saves, boss arenas or death resolution. Add one versioned persistent schema with explicit migration and fail-closed handling for unknown state.

Visual production is a later, separately reviewed stage. The 14 supplied images are references only; they are not distributable runtime pixels. Editable Blockbench/Aseprite sources stay with project source assets, while only optimized runtime exports enter JAR/resources.

### 3. Produce a release inventory

After sandbox acceptance, list exact fresh hashes and sides for every changed artifact:

- MineRENT/shared JARs and any server-owned config/defaultconfig/datapack files;
- matching client JARs, resources, localization and client configuration;
- registry delta, network channel/version delta and SavedData schema version;
- files intentionally absent from production, including sandbox enablement, debug commands, placeholder assets, development rewards and test-world data.

Do not call the release server-only unless the installed client already understands every registry and protocol involved.

### 4. Prepare the production world safely

Use MineRENT's current production world; do not replace it with the sandbox save. While the host is stopped, create a recoverable backup of the production world and deployed artifacts, record hashes and verify the exact target paths. Preserve authentication data, operators, player data, other mod SavedData and native configs unless the release explicitly migrates their owner.

Production migration must initialize a new Darkness season/session. It may read durable existing JEM account identity and approved progression, but it must not import sandbox boss entities, receipts, wallets, race selections or forced states.

### 5. Stage one client-and-server release boundary

Build only the production artifacts explicitly authorized for the release. Synchronize fresh shared outputs to the ordinary local Server and canonical `Client/ManagedPack`; never use an installed runtime JAR as the source owner. Stage the managed pack, inspect native `/jempack preview`, then publish only the reviewed generation.

Create the required cumulative friend point-update and update the clean client/stable release surfaces according to `Docs/DEVELOPMENT.md`. The current stable bootstrap may be updated only by its own protected flow; gameplay files are never raw-copied into an active AutoModpack profile.

### 6. Deploy to MineRENT while stopped

The later explicit deployment request must use the authenticated MineRENT workflow. Stop the server cleanly, upload the exact reviewed server/shared artifacts and configs, preserve the production world, and remove replaced JAR versions so Forge cannot load duplicates. Do not upload local sandbox state, `Server/local`, test-profile markers, local credentials, archives, logs or personal runtime files.

Start MineRENT once and inspect the full startup chain before enabling the event: Forge mod list, dependency resolution, registry synchronization, mixin/application errors, SavedData load, calendar status, network channel acceptance and native AutoModpack host state. A successful process start is not player acceptance.

### 7. Publish and apply the matching client generation

On the running production server, inspect native `/jempack preview` and publish the reviewed head. Confirm the published manifest matches the staged canonical manifest. Apply that generation natively to the development client while Minecraft is closed, then confirm active token, expected JAR hashes and no pending transaction. Friends receive the same release through native AutoModpack or the cumulative point-update; never ask them to mix raw files with the managed installation.

### 8. Production smoke checklist

Manual runtime QA requires a separate explicit request. When authorized, verify at minimum:

- a normal client and a reconnecting client receive the same event deadline and phase;
- server restart resumes the same session without repeating start, feed, boss, reward or unlock transitions;
- old or mismatched clients are rejected clearly rather than joining with missing registrations or packets;
- server owns boss hits, candy consumption, purchases, abilities, rewards and permanent unlocks;
- nonparticipants and spectators follow the shared encounter targeting rules;
- event stop/end restores only event-owned world changes and retains approved permanent rewards;
- low visual settings and no-shader presentation remain playable without weakening server rules;
- MineRENT native pack head, canonical ManagedPack manifest and installed client token agree.

### 9. Rollback

If failure occurs before any production event mutation, stop MineRENT, restore the previous reviewed JAR/config set and previous native pack generation, then restart and confirm the old channel/registry set. If production SavedData or world state has been mutated, prefer the feature's idempotent stop/cleanup and schema-aware downgrade path. Restore the full pre-deployment world backup only when rollback analysis requires it; never delete event `.dat` files or entities ad hoc.

Record the deployed and rollback artifact hashes, native generation IDs, world backup identity, event state and outcome. A rollback is complete only when the server and clients again use one matching contract.

## Current readiness

This document is the migration and server-operation guide only. No Darkness source, registries, assets, config, builds, sandbox launch, MineRENT upload, client generation, stable-profile change or production event activation has been performed. The current package therefore remains **design-ready for sandbox decomposition, not deployable**.

