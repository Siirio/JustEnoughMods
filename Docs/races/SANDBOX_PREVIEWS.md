# Sandbox overrides and animated ability previews

## Current four-stage follow-up —2026-10-17

Current behavior is owned by README.md and RacePowerStages: Base/1/2/3 for every V/B, immediate stage description with no Details tab, real utility growth, Nether direct damage0.85 and cumulative Sculk knockback/Darkness/temporary radius16territory. Source profile separation, detached previews and the closed-profile installer remain required. The initial implementation evidence below is historical where superseded (three tiers and the former Sculk slow are no longer current). No new art, game launch, automated tests or player acceptance is claimed.

Implementation boundaries: server-owned original-ground snapshots and per-cast UUID/expiry prevent recasts from reviving old unloaded ground. Native ChargeCursor update is narrowly stopped on/adjacent to temporary tiles, based on actual47.4.10 bytecode showing SculkBlock can grow sensors/shriekers above charged sculk. Dimension/eye size follows a validated server flag and a client-only PlayerRenderer scale tail; preview scale remains detached. Damage×1.15 applies only while the Sculk owner is inside its unexpired domain, not as an always-on race modifier. Narrow growth/scale hooks and channel5 require matching artifacts if later deployed; this prompt installs sandbox only.

Bug / Client and Server / mastery started at1 and the preview had a separate Details state, so default and first tier were identical and stage browsing did not immediately expose mechanics / in testing. Mechanism: authoritative0..3 stage mapping, one bound preview-stage/description selection and shared runtime constants. No runtime approval yet.


Approved implementation brief: 2026-10-17, JEM Races — Sandbox Testing & Animated Ability Previews. This file records the current task implementation/verification; Docs/races/README.md remains the behavior owner.

## Delivery contract

- Build-time SANDBOX and PRODUCTION alternatives. Default production uses canonical cooldown values and the established selection lock. No user-editable switch can enable development authority there.
- Sandbox automatically permits the integrated owner to choose any of five races repeatedly. Per-race progress is retained, prior-race transient effects/attributes are cleared and ordinary resource/collision/valid-target rules remain in force.
- F6 contains explicit Switch Race and sandbox tier/reset controls. Preview V/B, tier I–III and Bloom base/upgrade0–3 independently of owned progress; inspecting a locked stage grants nothing.
- A detached current-skin player and vanilla Creeper render real shared VFX entrypoints inside a clipped black viewport. Preview entry never calls cast, hurt, addFreshEntity, sendToServer or save APIs. Defensive/healing/movement effects are labelled accordingly. B includes contextual variants where the actual implementation has them.
- Both artifact profiles retain preview/UI code; production excludes the development implementation and command handlers. Canonical JEMRoles and deployed MineRENT behavior stay unchanged.

## Implementation ownership

1. Authority/build worker: build.gradle, generate-skills and workbench lifecycle/development authority. Source alternative development hooks, sandbox/prod artifacts. Preserve normal progression and costs.
2. Visual worker: AbilityPreviewRenderer and shared presentation entrypoints of existing effect renderers. Detached models, explicit timeline/context, bounded render state, no world queues.
3. Root: RaceExperienceScreen binding, preview controls/detail descriptions, exact deployment, documentation/Atlas/source reviews and delivery.

## Root cause: sandbox switch/cooldown lock

Bug / Client and Server / normal permanent-choice rules were applied unless a separately toggled developer map entry existed / in testing.

1. A second normal change is rejected even though the player entered the sandbox profile.
2. RaceExperience.choose checks FreeSwitchUsed or ActiveTicks≥36000 unless developerMode is true.
3. developerMode depended on an explicit temporary DEVELOPERS map entry; profile installation did not create it.
4. Cooldown remaining/write paths used the same opt-in rather than a sandbox build authority, so both UI and server retained normal cooldowns.
5. The build had no reliable development/production capability boundary. Separate source alternatives now own the override, and only integrated-owner sandbox authority can use it. Production restores selection/cooldown behavior automatically without editing definitions.

## Verification boundaries

Production compilation/reobfuscation and exact artifact installation are permitted by the project rules. The brief requests behavior verification; no new automated test suite, fixtures or gameplay launch is created by this implementation. Actual model orientation, effect framing, every GUI scale, passive cleanup and repeated gameplay casts require manual in-game review. Build success never implies those passed. Exact performed checks and artifact receipts are recorded at delivery.

## Final implementation evidence

- `sandboxJar productionJar -x test -x check` succeeded together after assigning distinct nested build names. Both artifacts contain the actual preview classes, assets and production refmap. No `RaceCommands.class` exists in either artifact.
- Actual `javap` inspection of compiled production `RaceDevelopment` confirms false capability gates and empty development request/handle bodies, no development tier/Bloom field keys or sandbox input dispatch. Its normal validated Bloom unlock store remains active; that is normal earned progression, not a debug action. Sandbox contains its separate authoritative implementation. Original cooldown definitions were not rewritten.
- Exact output SHA256 values and installation receipt are in `sandbox-delivery.json`. Only `jem sandbox` changed; existing Settlements and unrelated mods/worlds preserved. The prior Races JAR was copied to the recorded backup then removed from mods to prevent duplicate mod IDs. The user client was already running, so restart is required; Codex did not launch or control gameplay.
- Source review confirmed detached preview entities never enter/tick in the world; the renderer restores projection, viewport, scissor intersection, camera, depth/blend/cull/shader state and releases preview references on screen removal. Long effect tails are clipped to the five-second demonstration loop; their within-loop timing is reused. These findings do not establish visual acceptance.
- Profile-local saved `DevelopmentTier*`/`DevelopmentBloomUpgrade` overrides never replace normal Uses/BloomUpgrade. DEV reset resets the selected race's testing override only. Successful actual casts may still earn ordinary mastery, as before.
- At fewer than216 logical GUI pixels high, the readable text/controls remain available and a localized GUI-scale instruction replaces the miniature viewport; no invalid negative scissor is drawn. Actual layout/legibility at supported scales remains manual-review work.
- Additional reviewed cleanup: race self effects now track the complete serialized active/hidden chain, including weaker hidden grants and their later promotion; switching restores the previous chain with elapsed duration when still race-owned, while newer external extension/replacement stays active; guardian cleanup resolves its tracked UUID across dimensions.

No automated gameplay tests, fixtures, launch or real-frame visual review were performed. A source-separated production artifact is prepared, not deployed to MineRENT. The native dedicated JEMRoles ability contract can differ from standalone; that route honestly reports previews unavailable rather than rendering the wrong power. Standalone previews are retained in both artifacts.

## User-reported crash during installation

Bug / Client / an installed JAR was overwritten while the Java profile still held its original ZIP index / in testing.

1. The user opened F6 and received `NoClassDefFoundError: WorkbenchMesh`; logout also failed loading `RaceOwnedEffects`.
2. `logs/latest.log` shows the profile started at16:58:33; the delivery receipt records overwrite at17:01:32, before the17:01:47 crash.
3. Forge had already opened the older JAR. Replacing its contents in place invalidated deferred ZIP entry reads for classes not yet loaded.
4. The installer accepted a running profile and used truncating copy into its mod path; warning that restart was required did not prevent the unsafe write.
5. Installation owned this lifecycle failure. `install-sandbox.py` now refuses when that profile is running or Java command lines cannot be inspected, checks ZIP integrity and required classes, backs up, copies to a separate temporary file, checks again and atomically replaces the closed profile artifact. Never overwrite a live profile JAR.

After the crash, the profile process was absent. The complete artifact was reinstalled through that guarded path with matching SHA256 and all named classes present. No gameplay launch or crash-free acceptance has been performed. No world or save repair was attempted.
