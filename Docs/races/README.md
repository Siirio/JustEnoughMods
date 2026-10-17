# JEM Races

## Current standalone delivery — 2026-10-17

The source-owned standalone installation is `Mods/JEMVisualSandboxR3`; its mod ID
remains `jem_visual_sandbox` to preserve existing sandbox data and resources.
`JemRacesSandbox` is now the real source entrypoint. The obsolete binary-backed
race gallery is not composed into the JAR. The production artifact is
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
successful signature uses, Bloom upgrades and per-race expiry-based cooldowns.
Death does not copy transient entity state into a second progression owner.
Existing explicit sandbox selections and previously unlocked Bloom stages
migrate once; new normal saves never auto-unlock everything.

All five races start at tier I;10 successful signatures unlock tier II,30 unlock
III, matching the established production mastery thresholds. Invalid targets,
blocked casts and rejected Rift arrival do not grant mastery or a fresh cooldown.
The tier at cast time is retained for delayed effects. Normal selection retains
the established one free change before30minutes of active time; developer
selection is a separate temporary override. This sandbox keeps per-race mastery
so ordinary browsing/developer switches cannot erase earned progress.

| Race | Signature progression | Utility / preserved identity |
| --- | --- | --- |
| Ancestors | Life Chorus:12-block healing at I;16-block radius and harmful-effect cleanse at II; full-health absorption boon at III. All tiers retain5-second25% caster damage reduction and four large outward-facing shields. | Automatic invulnerable guardian below40%health, normal guardian cooldown and oath; golden healing responses include the ridden living mount. No floor runes. |
| Void Walkers | Rift Ambush:24-block targeted melee ambush;32-block reach at II; nearby slowdown at III. The previously rejected decoy is not restored. | Hold B at least10ticks for a real pearl; Shift+B gives100ticks of gravity denial. Water/void safety and End-kin neutrality retain their owners. |
| Born in Bloom | The approved6-damage root bind stays. Sequential I action lock, II3damage/second constriction, III poison are manually activated from progression once the corresponding mastery tier is reached. | B toggles the unchanged temporary Leaf Bridge generator, with a15-second activation cooldown. Second Blossom/environmental passives preserved. |
| Infected by Sculk | Sonic Wave is the approved replacement of sandbox traversal:8-block radius,12HP (previous8HP plus the requested4HP), one hit per enemy. II adds pressure knockback;III adds a2-second slow. | Echo Vision and25-resonance heal/absorption remain. Underground strength/health, daylight/fire weaknesses, XP and resonance retain their canonical formulas. |
| Nether Hearteds | Five authored outward eruptions retain their shape/timing:6/8/10damage with2/4/6seconds fire at tiers I/II/III. | The liked three-hit B fiery-slash combo remains unchanged; lava immunity/swimming/drinking, piglin neutrality, cold and water weakness are preserved. |

The user requested all-race progression and preservation of established powers.
The Sonic II/III control effects and Nether tier scaling are Codex implementation
choices adapting existing normal tier progression to the newer approved sandbox
skills. They are not previously user-approved mechanics. Current UI displays these
actual standalone values rather than the old traversal/decoy descriptions.

Default signature cooldowns come from the canonical `JEMRoles` rules where
applicable:25s Ancestors/Bloom,60s Void/Nether,20s Sculk. The standalone common
config is `jem-races-common.toml`; the generated default-rule class comes from
`JEMRoles/race/RaceRules.java`. Leaf Bridge is explicitly300ticks. Utilities
retain native cooldown defaults; the SANDBOX build bypasses their expiry.

## Build-scoped sandbox controls and ability previews

The SANDBOX build automatically enables zero cooldowns and unlimited race switching for the integrated-world owner. Normal resource costs, valid-target checks, collision, active-cast lifecycle and effect timing remain intact. Cooldown definitions remain unchanged; all expiry reads/writes, including native Iron HUD projection, use the authoritative override. Dedicated guests and remote clients cannot enable it.

The PRODUCTION build compiles a sealed no-op `RaceDevelopment` alternative. It contains no developer command or authoritative development action handler; `/jemrace developer` was removed. Normal selection retains its one free change before30minutes and normal cooldowns. Sandbox-only `DevelopmentTier*`/`DevelopmentBloomUpgrade` saved fields are ignored by production. No client-editable option, hidden packet or gamerule can enable unrestricted production switching. Do not install the `-sandbox` artifact on MineRENT; use the explicit production artifact. This task does not deploy either to the canonical pack or dedicated host.

F6 provides a **Switch Race** confirmation when browsing a different race in sandbox. Successful selection clears old race-owned timelines, passives, effects and modifiers before applying the new race; snapshots refresh HUD/key/presentation state. Normal per-race earned progress survives swaps. **DEV tier** cycles the selected race's actual testing tier I–III; **DEV reset** returns only its development overrides to tierI/Bloom base without deleting earned mastery.

The compact black 3D viewport is shared by origin, progression and ability details. V/B buttons select the real signature or utility; arrows inspect tiersI–III and Bloom base0/upgradesI–III, including locked stages, without granting progression. **Details** shows actual characteristics, changes and status. Current-player skin and a detached vanilla Creeper are rendered through native entity renderers. Ancestors B is explicitly automatic guardian rather than a fabricated B attack; Void B shows pearl/gravity variants, Sculk B shows echo/healing variants. Existing VFX draw routines accept a detached presentation context; preview scenes do not enqueue gameplay effects, cast spells, damage entities, send action packets or insert entities into a world. Their clock/resources stop with the screen.

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
