# JEM Races

## Current standalone delivery — 2026-10-17

The source-owned standalone installation is `Mods/JEMVisualSandboxR3`; its mod ID
remains `jem_visual_sandbox` to preserve existing sandbox data and resources.
`JemRacesSandbox` is now the real source entrypoint. The obsolete binary-backed
race gallery is not composed into the JAR. The sandbox artifact is
explicit `sandboxJar` output `build/sandbox-profile/libs/jem-races-1.1.0-sandbox.jar`, installed only in the existing
`jem sandbox` profile. The `productionJar` output is `build/production-profile/libs/jem-races-1.1.0.jar`; ordinary `jar` defaults to production. The previous sandbox JAR is backed up outside `mods`.

This iteration changes no ordinary `Server`, `Client/ManagedPack`, stable
profile or release baseline. Production compilation/reobfuscation packages the requested installation; it is not player acceptance. For the current sandbox-preview follow-up, static Atlas bookkeeping and artifact separation are checked under project delivery rules; no automated gameplay suite or Minecraft launch is performed. Final in-engine visuals, persistence across actual launches and multiplayer execution remain unverified.

## Controls and first choice

A new integrated-world player receives an authoritative unselected snapshot and
the mandatory origin screen. The actual player skin is rendered in a pitch-black
perspective stage. Minimal arrows browse five origins, the right panel scrolls
five real advantages/two drawbacks and ability/progression details, and original
race icons identify the signature. `P` changes the information tab. Selection
waits for a server acknowledgement before the panel disappears and the render
camera orbits behind the same skin and dollies into eye position. World reveal
occurs only at the end. The world player stays in place; server scene ownership
freezes movement/interaction and protects first-choice entry. HUD and key state
are restored when the screen ends or is removed.

- `F6`: origin, abilities and progression. Saved choices do not reopen on login.
- `V`: signature; `B` (Russian physical `И`): utility; `Shift+B`: Void gravity.
- Nether lava drinking uses the normal held-bucket use animation; sneak-use pours.
- With optional compatible Iron's Spells3.15.6: one native HUD/wheel/cast owner;
  colliding V is delegated, Tab cycles native options unless a native wheel key
  already owns Tab. Remapped distinct JEM V may invoke the signature directly.
- Ordinary vanilla/unrelated servers receive no fake local race gameplay. F6
  explains when the matching JEM gameplay channel is absent.

## Progression and authority

`RaceExperience.Store` owns one UUID record per integrated world, saved as
`jem_races_standalone`. It preserves the selected race, active time, per-race
successful signature/utility uses, Bloom upgrades and per-race expiry-based cooldowns.
Death does not copy transient entity state into a second progression owner.
Existing explicit sandbox selections and previously unlocked Bloom stages
migrate once; new normal saves never auto-unlock everything.

All five races now have four distinct stages: Base (0),1,2,3. Successful real V/B results share each race's saved mastery counter:10 uses unlock1,20 unlock2,30 unlock3. Failed casts, charging, empty path toggles and preview browsing grant nothing. Existing earned counters remain saved; the new thresholds intentionally replace the previous1/2/3 mapping. A delayed cast retains its captured stage. Normal production choice/cooldown rules remain; SANDBOX retains unlimited switching and zero expiry.

| Race | V: Base →1 →2 →3 | B / contextual progression |
| --- | --- | --- |
| Ancestors |12-block healing →16-block radius →cleansing →full-health absorption. All retain6HP player healing, full peaceful-animal/mount healing and25% caster ward for5s. | Automatic guardian below40% health lasts30/40/50/60s; B remains contextual, not an invented attack. |
| Void Walkers |24-block rift →32-block reach →2s slow within4blocks →3s slow within8blocks. Actual melee strike and safe arrival retained. | Pearl charge remains10ticks with real collision; Shift+B gravity lasts5/6/7/8s. |
| Born in Bloom |5.4HP initial hit with3s movement grip at Base (attacks still allowed) →full action lock →2.7HP/s constriction →after-release1HP/s poison for4.5s. Unlocks remain sequential and cannot exceed mastery stage. | Leaf Bridge lifetime40/50/60/70s and forward reach2/3/4/5blocks;15s normal activation cooldown retained. |
| Infected by Sculk |12HP hostile-only wave, radius8 →knockback →5s Darkness on all nearby other mobs/players plus confused mob aim →10s temporary sculk territory radius16. Owner inside gets size and attack×1.15. | Echo range32/40/48/56; at25resonance instead restore6/7/8/9HP and grant absorption20/25/30/35s. Real cost remains25. |
| Nether Hearteds |Five eruptions at every stage; damage3.4/5.1/6.8/8.5HP each, fire1/2/4/6s. | Three authored slashes keep timing, pull and collision; each uses the same stage damage/fire. Lava and environmental passives retain their owners. |

The user explicitly requested four stages, B development,15% lower Nether power damage, and Sculk knockback/darkness/territory. Codex chose the exact new Base values, shared10/20/30 V/B thresholds, utility scaling,5s Darkness and10s territory. Nether levels1–3 keep their previous6/8/10 direct-damage basis multiplied by0.85; Base adds4×0.85. Native ongoing fire damage remains native. These are implementation choices, not prior user-approved balance values.

Sculk uses native Darkness and lets mob health, effects and movement continue. Confused mobs forget targets, look/wander/swing toward random directions and cannot land their own immediate hits; player Darkness affects vision without disabling player controls. Wave damage still targets hostile mobs only. The domain changes only exposed loaded solid ground without inventories, fluids or unbreakables. Original block states are saved; restoration survives unloaded chunks/restarts, with distinct per-cast IDs,4096 pending tiles per dimension and128 inspected changes per tick. Piston/break/explosion and native catalyst propagation cannot farm temporary ground. No chunks are force-loaded. Exit, expiry, death, logout or race change removes the boost; enlargement waits for body clearance. Unrelated later block edits are not overwritten. Real gameplay, save recovery and rendering remain unverified by a game launch.

Default signature cooldowns come from the canonical `JEMRoles` rules where
applicable:25s Ancestors/Bloom,60s Void/Nether,20s Sculk. The standalone common
config is `jem-races-common.toml`; the generated default-rule class comes from
`JEMRoles/race/RaceRules.java`. Leaf Bridge is explicitly300ticks. Utilities
retain native cooldown defaults; the SANDBOX build bypasses their expiry.

## Build-scoped sandbox controls and ability previews

The SANDBOX build automatically enables zero cooldowns and unlimited race switching for the integrated-world owner. Normal resource costs, valid-target checks, collision, active-cast lifecycle and effect timing remain intact. Cooldown definitions remain unchanged; all expiry reads/writes, including native Iron HUD projection, use the authoritative override. Dedicated guests and remote clients cannot enable it.

The PRODUCTION build compiles a sealed no-op `RaceDevelopment` alternative. It contains no developer command or authoritative development action handler; `/jemrace developer` was removed. Normal selection retains its one free change before30minutes and normal cooldowns. Sandbox-only `DevelopmentTier*`/`DevelopmentBloomUpgrade` saved fields are ignored by production. No client-editable option, hidden packet or gamerule can enable unrestricted production switching. Do not install the `-sandbox` artifact on MineRENT; use the explicit production artifact. This task does not deploy either to the canonical pack or dedicated host.

F6 provides a **Switch Race** confirmation when browsing a different race in sandbox. Successful selection clears old race-owned timelines, passives, effects and modifiers before applying the new race; snapshots refresh HUD/key/presentation state. Normal per-race earned progress survives swaps. **DEV tier** cycles the selected race's actual testing Base/1/2/3; **DEV reset** returns only its development overrides to Base0/Bloom base without deleting earned mastery.

The compact black 3D viewport is shared by origin, progression and ability details. V/B buttons select the real signature or utility; arrows or wheel inspect Base/1/2/3 for both V and B, including locked stages, without granting progression. The selected stage immediately shows its actual characteristics, changes and status below the animation. Current-player skin and a detached vanilla Creeper are rendered through native entity renderers. Ancestors B is explicitly automatic guardian rather than a fabricated B attack; Void B shows pearl/gravity variants, Sculk B shows echo/healing variants. Existing VFX draw routines accept a detached presentation context; preview scenes do not enqueue gameplay effects, cast spells, damage entities, send action packets or insert entities into a world. Their clock/resources stop with the screen.

The standalone preview contract applies to the standalone abilities. A native dedicated JEMRoles connection keeps its native descriptions and reports preview unavailable where that different ability contract cannot be represented faithfully; it is never shown a misleading standalone Sonic/eruption simulation. Both build profiles retain the standalone animated preview feature.

Actual repeated casts, switching/passive restoration, every GUI scale and final model/VFX appearance still require in-game verification. Production compilation and artifact inspection are reported separately in `sandbox-delivery.json`; no gameplay launch or new automated suite was performed. Task evidence/contract: [SANDBOX_PREVIEWS.md](SANDBOX_PREVIEWS.md).

## Visual and projectile ownership

The exact approved smooth guardian interpolation is locked in root
`AGENTS.logic-locks.md`; immutable evidence is in
`assets/guardian-dome/texture-animation/approved-loop`. The complete one-shot
strip now drives `GuardianDomeRenderer` at24FPS using a clamped frame index,
emissive straight alpha and the unchanged connected mesh. It reaches the crown
and holds instead of reversing halfway. The30-tick guardian lifecycle remains.

Sonic Wave now has a thick elevated closed pressure cross-section, compressed
anticipation, sharp radial release, offset trailing volumes and bounded cuboid
fragments. Actual accepted damage alone emits target-impact flashes. It no longer
uses the former flat floor band as its main presentation. Pearl afterfire, large
R7 shields, healing pulse, chunky root models and the approved fiery combo retain
their authored assets. Renderer success is not visual acceptance.

`ProjectileEstimation` shares actual pearl launch coordinates/velocity with the
bounded native-collision predictor. For deterministic agreement, this iteration
intentionally removes the former0.25random launch spread; speed1.5, native0.03
gravity,0.99air drag,0.8water drag, inherited motion and native swept collision
remain. This is a Codex technical choice supporting the user's accuracy request.
The marker lies on the actual contact face, with invalid/body-clearance feedback;
server arrival safety may independently choose a safe nearby position and is not
misrepresented as the exact impact point. Releasing, changing race/world or
opening UI clears the preview. No decorative Bezier prediction is used.

## Dedicated compatibility and optional integrations

Dedicated production race gameplay remains owned by `JEMRoles`, its confirmed
`JEM Identity` account and `RaceSavedData`; it is not moved to client memory.
When the standalone channel is also installed on a compatible JEMRoles server,
`ProductionRaceBridge` delegates authentication, normal selection, casts,
utilities, mastery and cooldown snapshots to those exact native owners. The UI
uses native descriptions there rather than pretending sandbox-specific Sonic or
eruption overrides changed the live server. No dedicated source/artifact or
existing server balance is replaced in this sandbox installation.

Optional Iron's classes load only through the guarded compat entrypoint; no Iron,
Identity or Lodestone runtime dependency is required for standalone gameplay.
The Iron adapter presents the common race state via native innate spells/HUD and
mirrors the authoritative cooldown. Invalid casts never enter the successful
native lifecycle. Its startup/runtime compatibility has not been launched here.

Production SMP entry remains `/smp` → Races. Offline-mode identity is profile UUID
owned by the authenticated native account; this standalone work neither joins
accounts across nicknames nor copies server identity state.

## Sources and history

- `Mods/JEMRoles`: canonical production traits, identity-bound ledger and defaults.
- `Mods/JEMVisualSandboxR3`: standalone owner, optional native adapters, GUI and VFX.
- `assets/guardian-dome/texture-animation`: approved interpolation and one-shot art.
- `assets/ancients-pearl`, `assets/life-chorus`, `assets/elemental-vfx`,
  `assets/fiery-slash`: authored effect sources and historical review evidence.
- Prior sphere, mosaic plaques, tails and Bloom R2 remain rejected history.
  Their old plans/receipts do not override this current source contract.

Sandbox installation: close `jem sandbox`, then run `python Mods/JEMVisualSandboxR3/install-sandbox.py` from the repository root. This guarded installer refuses a running profile and replaces the complete verified JAR atomically. Never copy over a loaded mod JAR.

F6 stage arrows or scrolling over the miniature/stage selector immediately update the description below it. There is no separate Description tab. Scrolling the text panel still scrolls text. Inspection never grants a stage; DEV changes the actual sandbox stage. Optional Iron native spell levels1–4 project these four stages without owning progression. The sandbox network contract is now5; no MineRENT/managed-pack deployment is performed.

## B narrator and Bloom follow-up —2026-10-17

The Races client Mixin disables only the native Ctrl+B narrator toggle, verified against1.20.1 KeyboardHandler bytecode; the physical B/И event still reaches normal key mappings/screens and explicit narrator settings remain accessible.

Bloom main-attack damage is10% lower: initial5.4HP and stage2constriction2.7HP/s. Grip and authored vines now last60ticks at every stage. Base pins movement after normal AI ticks and permits attacks; stage1+ uses the existing full action pause. Stage3 begins a90tick poison phase only after release, with four1HP ticks at+20/+40/+60/+80; a shared custom clock bypasses native POISON application immunity and avoids native double ticks. Damage must still be accepted by the native hurt pipeline and reduce health; full invulnerability, cancelled hits, fully absorbed hits and boss phase rejection are respected. Accepted health loss is normalized to1HP and capped nonlethal at1HP. Stage3 preview loop is8seconds to show grip, release and poison. No runtime claim is made for every installed custom boss.

User follow-up: Sculk B recovery requires at least25resonance, consumes the entire current pool and resets it to0 on its single recovery activation. It cannot heal again until resonance is earned again; echo vision remains a separate low-resonance contextual action.
