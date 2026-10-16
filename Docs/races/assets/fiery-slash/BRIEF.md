# Three-hit fiery slash — sandbox candidate

User request, 2026-10-17: three successive fiery slashes with three different pixel assets/animations; pull the player forward; replace the existing sandbox main skill and update its mod JAR. Codex choices below are implementation details, not independently approved preferences.

## Current contract

F6 → Nether; V → rising hook, return sweep, descending cleave. No target required for this directional attack. Starts at ticks 0/10/22; impacts at 4/14/27; complete at 38 ticks (1.9 seconds). A cast already in progress cannot stack a second combo; V is available again immediately after completion. Other four races retain their previous source-derived skills.

Server owns selected race validation, the fixed horizontal cast direction, all three hit origins, motion, damage and cancellation. Each hit applies four/four/five 0.42-block-per-tick horizontal velocity pulls, with full swept-player-box clearance, world-border and support checks. Actual distance depends on native collision/movement; no claim of exactly 5 blocks. Preserve vertical velocity; no wall phasing or automatic ledge dash. Death, logout, dimension change or server stop cancel pending work. Three front-facing strikes use line of sight, tier-3 predecessor values of 10 fire damage and 6 seconds burning, with a small forward/upward impulse. These replace the sandbox's stationary eruption/volcanic zone only. Production JEMRoles is unchanged.

Each hit: distinct 96x96 RGBA art, 10/12/16 cels × 50ms. Five editable layers: ember fringe, flame body, hot flow, yellow core, breakaway embers. Stable canvas center 48/48, vertical runtime strips96x960 /96x1152 /96x1536. Fullbright cutout, no glow/bloom dependency, nearest native texture sampling. The existing sandbox render hook draws two narrowly crossed carriers (24 degrees apart), 2.4 /3.6 /5.0 blocks square by hit, with each carrier bottom held above the caster ground plane, in front of the caster. Carriers follow the player during the first four/four/five ticks and leave their release at that endpoint. These are pixel art carriers, not the visual silhouette.

## References and production

Taste Engine VFX I_like originals `Снимок экрана 2026-10-16 183036.png` and `183623.png` were reopened. First: uneven tapered bodies, hot belly, trailing flame tongues and gaps. Second: authored stepped contours and controlled interior space. Warm crescent sheet183110 supplies core/body/fringe hierarchy, not license to reuse pixels or timing. Shapes, cutouts, clusters and cels were authored externally in author.py; no source pixels or licensed reference assets were copied. No Blockbench painting.

Current editable timelines: r5/rising_hook.aseprite, r5/return_sweep.aseprite, r5/descending_cleave.aseprite. Earlier revisions retained with strips, contact sheets and playback. `python Docs/races/assets/fiery-slash/author.py --revision r5` regenerates the current layered native files and exact runtime strips. The three `.aseprite` files preserve all five cel layers. Pixelorama imported the R2 source as five layers and ten frames; its headless shader diagnostics remain, and native CLI sheet export was not established. The external source/export route produced the actual PNG strips and moving preview.

## Scope and evidence

Standalone jem sandbox only. No ordinary Server, Client/ManagedPack, stable profile, MineRENT or AutoModpack change. User authorization covers this sandbox build/install. No gameplay launch or automated tests. Pixel playback is genuine output-frame animation; it does not demonstrate Minecraft damage, movement, shaders or camera fit. See REVIEW.md for rejected revisions and remaining limitations. Status: ART_CANDIDATE / runtime unobserved / user approval pending.


## User correction incorporated into R5

The user rejected R4: internal slashes looked strange, and three variants at one power level did not escalate. R5 uses continuous flame masses and positive hot cores, no interior slash/erosion masks. Hit1 is a compact hook with one ember stream; hit2 is a broad return sweep with three delayed knots and a longer wake; hit3 has a much larger descending body, an additional hot flame crown and five delayed knots with the longest release. The actual sprite art, spatial size and duration all escalate; damage values do not. Reference comparison still treats native Minecraft presentation as unobserved.
