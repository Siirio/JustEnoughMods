# JEM Races

## Current standalone delivery — 2026-10-17

The source-owned standalone installation is `Mods/JEMVisualSandboxR3`; its mod ID
remains `jem_visual_sandbox` to preserve existing sandbox data and resources.
`JemRacesSandbox` is now the real source entrypoint. The obsolete binary-backed
race gallery is not composed into the JAR. The production artifact is
`build/distributions/jem-races-1.1.0.jar`, installed only in the existing
`jem sandbox` profile. The previous sandbox JAR is backed up outside `mods`.

This iteration changes no ordinary `Server`, `Client/ManagedPack`, stable
profile or release baseline. No tests, check tasks, Atlas audit/validation,
Minecraft launch or automated visual inspection are authorized. Production
compilation/reobfuscation packages the requested installation; it is not player
acceptance. Final in-engine visuals, persistence across actual launches and
multiplayer execution remain unverified under that explicit restriction.

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
retain native cooldown defaults; explicit developer mode bypasses their expiry.

## Separate developer mode

`/jemrace developer on` enables temporary max-tier/no-cooldown controls for the
integrated-world owner. F6 can choose any race. `/jemrace developer tier 1|2|3`
changes the effective tier, and `/jemrace developer bloom 0|1|2|3` exposes the
approved Bloom stages independently. `/jemrace developer off` restores the
ordinary saved race/mastery/cooldowns. Developer mode is not persisted, never
writes ordinary mastery and cannot be enabled by a remote multiplayer client.
F9/F10 and bracket art studies are developer-only; they are not the main skills.

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
