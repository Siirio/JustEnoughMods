# JEM Races

## Born in Bloom boss-root duration — 2026-10-29

Born in Bloom roots keep their ordinary 3-second duration against normal targets. Every boss registered through the canonical World Tier boss profile uses exactly 40% of that duration, currently 1.2 seconds, so bosses escape 60% sooner. The same ratio is derived from the shared root duration and therefore remains correct if the ordinary value changes later. Boss native ticking and phase behavior continue while rooted, existing repeat protection remains unchanged, and the client vine release now matches the shorter server-owned boss duration.

## Ancestral Guardian damage ownership — 2026-10-29

The Ancestral Guardian is an ordinary damageable iron golem for every player and mob except the player who summoned that exact guardian. Direct and indirect attacks attributed to the summoning player are canceled on the server. Other damage keeps native Minecraft armor, immunity, hurt-cooldown, death, knockback and attacker behavior. The guardian still drops no loot or experience and still expires through its existing tier duration.

## Ancestral Guardian peaceful-kill penalty — 2026-10-29

The Ancestral Guardian keeps its ordinary 60-second cooldown. Killing a peaceful animal or villager now starts a separate 2-minute guardian lock. Another qualifying kill restarts that penalty at the full 2 minutes. When the penalty expires, only the independently running ordinary cooldown can still block the guardian. Existing saves with the former oversized guardian cooldown migrate once: the ordinary cooldown is clamped to 60 seconds and any larger remainder becomes at most the new 2-minute penalty. The race screen displays whichever guardian gate has more time remaining.

## Quiet race health changes — 2026-10-26

The user requested that race-owned heart changes not shake the camera or flash the player red. Vanilla `LocalPlayer.hurtTo` treats an ordinary server health decrease as a hit even when the cause was a race's maximum-health adjustment. JEM Races now pre-synchronizes the final health value to the affected player for race-owned max-health clamps, Born in Bloom's daylight regeneration, Life Chorus player healing, Sculk resonance recovery, and Nether lava drinking. The Void water-contact penalty still uses native drowning damage, preserving absorption, invulnerability, totems, Forge damage hooks and death behavior. A short server-marked packet window suppresses only that particular drowning damage event on the affected player and its tracking clients; the final health is then pre-synchronized to the owner. Combat hits, hostile race abilities and unrelated damage still use native damage presentation. Leaving a green biome only stops Born in Bloom's regeneration and applies its existing healing penalty; it does not itself subtract health. This is source behavior pending local player acceptance.

> План от 2026-10-22: [universal amendments](../plans/2026-10-21-universal-standalone-implementation.md#актуальное-дополнение-smp-награды-market-и-первый-выбор-расы--2026-10-22) фиксирует отсутствующую модель при первом обязательном выборе и видимую модель через F6. Причина пока не установлена. Предстоят сравнение lifecycle двух входов, разрешённое воспроизведение и общий путь подготовки модели со стандартной внешностью до загрузки скина. Исправление и игровая приёмка в этой задаче не выполнялись.

## Innate spells, guardian and Jade —2026-10-21

Source implementation / in testing. Runtime acceptance is unrun; production artifact delivery, native AutoModpack application and MineRENT deployment remain pending. This section does not claim a completed release.

The source cause of the Sculk rejection is the inherited school-learning requirement on an innate racial spell. `RacialSpell.requiresLearning()` now exempts registered innate racial spells while race ownership, server authority and native cast/ability checks still apply; other Eldritch spells are unchanged. Guardian cooldown becomes0.05Minecraft-day, including a one-time clamp of a larger saved legacy remainder. The guardian follows the owner's actual eligible attacker, including a hostile player allowed by PvP/alliance checks, instead of always replacing it with the nearest Enemy. Invalid/dead/out-of-dimension targets and safe spectators remain ineligible. A Roles-owned Jade provider synchronizes only the current race ID and localizes it client-side; no second race record or private progress is exposed. Existing race panels and VFX remain outside this update's visual scope.

## Daily reselection and first-login lifecycle —2026-10-20 source handoff

The user requests a stable first choice after authentication, ordinary reselection once per Minecraft-day and unrestricted reselection only in the isolated test profile. JEMRoles RaceSavedData remains the sole production account record. RaceRecord schema5 adds ReselectReadyAt, TestChoiceEpoch and ChoiceRequired while retaining the existing record/Mechanics fields. RaceService uses authoritative overworld gameTime for the1Minecraft-day wait. A successful ordinary change starts the next wait; same-race selection returns unchanged. Legacy records lacking the new deadline are ready for their first change without losing their selected race or progress. The old JVM race-reselect flag is no longer an authority.

DedicatedTestProfile from JEMIdentity owns the exact dedicated save/identity/epoch gate described in `Docs/DEVELOPMENT.md`. A matching test epoch marks an existing record ChoiceRequired once; effective/snapshot/ability reads exclude that record until confirmation. This preserves the record instead of deleting its historical state. A new or reconfirmed choice remains in the same canonical owner. A matching test profile bypasses the ordinary reselection wait; it does not bypass IdentityPolicy. Snapshot readiness carries remaining native duration, test flag and current server nonce. Roles protocol4 and Races skills protocol7 require matching client/server artifacts.

ProductionRaceBridge reads the canonical record and presentation/mechanics view; it does not create a second race choice store. Selection requests carry the current nonce to RaceService. RaceExperienceClient tracks its actual client connection, clears status/pending view on connection change, queues the mandatory screen only after an authenticated server snapshot, and waits until player+level exist and no other screen is active. Login/loading screens therefore retain their lifecycle. Receiving a newer snapshot updates an already open RaceExperienceScreen in place; ordinary repeated status packets do not recreate it. F6 queues the same production view, and a chosen player receives no first-choice immunity merely by browsing it. The existing bounded initial-choice protection/one completion window remains owned by the server scene lifecycle.

RoleClientState/legacy native selection and final RaceExperienceScreen show remaining reselection through the shared MinecraftDays formatter. Positive sub-day values never print ready; the client can interpolate a synchronized remaining presentation but cannot grant a change. Reconnection, dimension changes or sleeping do not rewrite the persisted readiness deadline. Original race reset is not part of the test epoch policy. Without the Races presentation module, an explicit native-screen request remains queued behind another screen; passive selected snapshots do not cancel it. Successful first selection, an already open selection screen or connection reset consumes that pending request.

Implementation source is present; final artifact synchronization, native AutoModpack installation, current hosted profile startup and user acceptance remain pending their task delivery receipt. No gameplay/visual acceptance, tests or original-world migration is claimed by this section. Earlier dated sandbox/installation entries below are historical evidence, not the active production profile contract.

## Combat exploit corrections — 2026-10-20

Selected players may browse F6 without acquiring scene immunity. Initial selection remains protected, including prolonged reading; its first successful completion starts one100tick camera-exit window, and repeated acknowledgements cannot extend it. No save schema or V/B(И) control changes. Bloom's green-ground passive still pacifies ordinary eligible mobs, while canonical registered bosses retain targeting through WorldTierApi.profile. Exact mechanics and regression boundaries are recorded in the matching Atlas changes and `Docs/power-system/Balance.md`.

## Native installation recovery — 2026-10-18

The second test jem crash still loaded generation27: native preload had downloaded generation29 but refused its locally built JAR replacements without review. Publication and download did not install the correction. The already-authorized exact local artifacts were applied through the existing native planner and UpdateAttempt commit, preserving native file ownership and the preload trust rule. The installed active token now equals the generation29 head, and installed Races/Compat SHA-256 values match ManagedPack; Compat contains no narrator Mixin and Races owns the sole hook. No client was launched or tests run. User startup confirmation remains pending; the complete receipt is `server-test-migration.json`.

## Startup crash correction — 2026-10-18

The user assigns Ctrl+B narrator suppression exclusively to Races. The duplicate `JEMCompat` narrator Mixin class and configuration entry are removed; the original required Races hook remains unchanged. This removes the explicit Redirect collision that aborted test jem initialization. Physical B/И input and explicit narrator settings retain their behavior. Fresh matching production Races/Compat artifacts and native generation are delivered locally; player startup acceptance remains pending. No tests or client launch were performed.

## Final sandbox promotion — 2026-10-18

The user approved the final Races and Settlements for the local dedicated server and `test jem` via native AutoModpack. Matching fresh production Races1.1.0, Roles2.0.0 and full Settlements2.1.0 artifacts now form the local managed generation. MineRENT, stable profiles and the immutable release baseline are unchanged.

Races uses the final authored ability pipeline and previews on dedicated connections. Existing authenticated JEMRoles account records remain the sole save owner: final per-race mastery/cooldown data lives in their Mechanics compound, initialized from existing selection/progress/cooldowns. Legacy gameplay/key/loot owners yield while final Races is installed. Production has normal cooldowns and no developer authority. Origin count, DEV tier/reset controls and the crossed preview/DEV cooldown explanations are removed from the shared screen for every origin. Settlements promotes the existing final protocol9 source, without redesigning its town simulation.

Only necessary production compilation/reobfuscation and ordinary server installation/publication were performed. No tests, client launch or gameplay/visual acceptance is claimed. Artifact and publication receipt: `Docs/races/server-test-migration.json`.

## Current sandbox follow-up — 2026-10-17

User requests leaf decay10seconds at stage0 and+5seconds each later stage:200+100×stage ticks,10/15/20/25seconds. Forward reach and activation cooldown remain unchanged. Only the sandbox JAR is rebuilt/installed; ordinary Client/Server and release baseline are not promoted. Cohesive wood material applies to progression information; the protected black origin selection and quiet origin controls remain. Existing source-owned profession progression is now available in the same sandbox through F7 or `/professions`; see [professions](../professions/README.md). Installation receipts: [sandbox-only delivery](../settlements/standalone/sandbox-only-iteration-delivery.json). No game launch, tests or visual acceptance claimed.

## Earlier standalone delivery — 2026-10-17

The source-owned standalone installation is `Mods/JEMVisualSandboxR3`; its mod ID
remains `jem_visual_sandbox` to preserve existing sandbox data and resources.
`JemRacesSandbox` is now the real source entrypoint. The obsolete binary-backed
race gallery is not composed into the JAR. The sandbox artifact is
explicit `sandboxJar` output `build/sandbox-profile/libs/jem-races-1.1.0-sandbox.jar`, installed only in the existing
`jem sandbox` profile. The `productionJar` output is `build/production-profile/libs/jem-races-1.1.0.jar`; ordinary `jar` defaults to production. The previous sandbox JAR is backed up outside `mods`.

This iteration changes no ordinary `Server`, `Client/ManagedPack`, stable
profile or release baseline. Production compilation/reobfuscation packages the requested installation; it is not player acceptance. For the current sandbox-preview follow-up, static Atlas bookkeeping and artifact separation are checked under project delivery rules; no automated gameplay suite or Minecraft launch is performed. Final in-engine visuals, persistence across actual launches and multiplayer execution remain unverified.

## Controls and first choice

The integrated-world description below belongs only to the explicit standalone sandbox profile. Dedicated production uses the canonical Roles/Identity lifecycle described at the start of this document; ordinary singleplayer does not acquire shared-server gameplay.

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

This Store is the explicit standalone sandbox owner. Dedicated production uses the existing JEMRoles record and ProductionRaceBridge rather than this parallel sandbox save.

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
| Born in Bloom |5.4HP initial hit with3s movement grip at Base (attacks still allowed) →full action lock →2.7HP/s constriction →after-release1HP/s poison for4.5s. Unlocks remain sequential and cannot exceed mastery stage. | Leaf Bridge lifetime10/15/20/25s and forward reach2/3/4/5blocks;15s normal activation cooldown retained. |
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

2026-10-18: race screen uses its own fixed GUI scale 3 and farther vanilla skin camera, independent of combat pose. Iron’s Spells icons now live at its native textures/gui/spell_icons path; action misuse messages use owned Russian/English translation keys. Delivery: gui-local-delivery.json. Acceptance in testing; no gameplay launch or tests.

2026-10-18 client startup correction: the new vanilla GUI renderer requires embedded MenuSupport >=1.1.0; a broad >=1.0 range previously let the older Server library win and break client mod construction/resources. The exact dependency floor and native client/resource recovery are recorded in gui-library-crash-recovery.json. Client startup/player acceptance remains pending; no launch or tests were run.


## Black panels, race identity and contextual upgrades — 2026-10-18

User-selected screenshot02 supersedes the prior wood material on race progression only: every race and both information tabs retain the black void and black right panel, quiet controls and a subdued vertical separator. Under the actual player, a native Minecraft item icon precedes a1.35x bold wrapped race name, then the unchanged lore. Semantic icons are Totem/Ender Pearl/Oak Sapling/Echo Shard/Blaze Powder. Originals and precise before/target labels: `assets/reference/black-panels-2026-10-18/README.md`. No new bitmap art is authored or cropped.

The generic mastery count/threshold paragraph is removed from the shared screen. Only the viewed locked upgrade under Abilities & progression shows current successful uses versus its actual server threshold. Base and Origin do not show a mastery counter. Existing thresholds remain I10/II20/III30 and the cap30: the request's30/50 numbers are treated as display examples, not an implicit rebalance. `RacePowerStages.requiredUses/stageForUses` supplies the same unchanged values to the server, migration, snapshot bound and UI denominator. Native record ownership, successful V/B results, cooldowns and packet shape are unchanged.

Bloom's three bottom Roman unlock buttons are removed. Its real manual unlock remains beside the inspected V upgrade, only on Abilities & progression; server thresholds and sequential purchase rules still apply. A later upgrade explains that the earlier stage must be opened first. Preview browsing grants no progress. Runtime visual/player acceptance remains pending; no gameplay launch or automated test is performed.

The progression layout reserves a readable text region when an inspected Bloom unlock is visible. Small windows omit the 3D preview before consuming the counter/details viewport; the large-window reference layout is retained.

## Void pearl aiming readability —2026-10-19

User selects the [supplied reference](assets/void-pearl-trajectory/reference/2026-10-19/README.md)
for an off-centre readable arc with JEM's existing effect. The former shared
initial position was eye-centred; thin forward line segments projected end-on,
hiding horizontal depth and flattening the contact ring into a short bar.

`ProjectileEstimation.initialPosition` now starts at the dominant throwing hand:
0.42blocks lateral and0.24 below the eye. A native block ray clips this short
hand offset against nearby cover. Both the actual pearl launch and detached
prediction use this exact position; velocity, drag, gravity, collision and server
teleport safety remain. This small physical origin adjustment is Codex's explicit
technical choice to keep the visibly offset path truthful, not an offset fake arc.
`VoidDistancePreview` uses lavender direction chevrons every0.75world blocks with
bounded camera-facing size, and a0.46block contact ring on the actual hit face.
Existing authored afterfire assets are unchanged. This is a sandbox presentation
iteration; production profile is prepared but ordinary Server/ManagedPack are not
deployed. No tests, gameplay launch or visual acceptance is claimed.

## Void Walker trajectory HUD encoding —2026-10-19

The user's runtime screenshot showed mojibake in the distance/contact label. The first incorrect data was already in `VoidDistancePreview.hud`: Russian phrases and the middle-dot separator were stored as corrupted Java literals; languageCode chose those literals and the native font rendered them faithfully. The label bypassed the owning localization resources. It now uses `Component.translatable` with ru_ru/en_us distance, clear/blocked/unknown keys, so the selected resource language owns the wording. Prediction, projectile motion and teleport safety are unchanged. Sandbox and production profiles are built; only jem sandbox is installed. No tests or launch; user acceptance pending.

## Native preview restoration —2026-10-21

The user restricted the custom player renderer to SMP GUI. RaceStageRenderer and AbilityPreviewRenderer now use native entity renderers; the black screen composition, camera, GUI scale and race mechanics retain their existing owners. No visual runtime pass or user acceptance is claimed.
