# Bloom R3 — isolated sandbox extension

## Current sandbox main skills — 2026-10-17

The user corrected the initial gallery interpretation: Blockbench effects must
replace old VFX on the actual main-skill key. This section supersedes the older
R3-only installation record below.

- **F6**: choose any of five races, repeatedly, including the same race. Selection
  is stored by the integrated server in sandbox player data and acknowledged to UI.
- **V**: execute the selected race signature, tier3, with no cooldown or account/
  one-time choice gate. This is a remappable Minecraft keybinding.
- Ancestors: canonical Life Chorus healing/absorption, shield R7.
- Void: canonical targeted safe Rift Ambush teleport/attack, Void R6 at both ends.
- Bloom: canonical roots/damage on nearby non-animal targets, coffer R4.
- Sculk: canonical connected-sculk traversal with safe exit, crawl R4 along its path.
- Nether: canonical Infernal damage/fire/knockback/zone with R6 three-impact model.
  Sandbox impact times4/10/16 ticks match the authored .18/.50/.82s sequence.
- Targeting and safe-arrival prerequisites remain. Void/Nether need a living target;
  Bloom needs nearby targets; Sculk needs connected sculk and a safe distant exit.
- **[ / ]**, **F10**: optional study selection/playback; F8 old galleries and F9 R3
  are historical tools. V uses only the new Blockbench emission route.

`generate-skills.py` extracts the actual signature helper methods and environment
checks from canonical `Mods/JEMRoles` at build time. That source remains the only
maintained gameplay owner; the generated adapter supplies sandbox selection,
tier3, no cooldown and replacement emission. It does not install full JEMRoles
passives, utility skills, account authentication or progression.

`bake-workbench.mjs` uses both installed MCP servers: headless source inspection/
opening, then desktop native animated OBJ export at20Hz. Original UVs and external
painted texture bytes are retained. Core textures are fullbright; other material
surfaces use world lighting. At most64 simultaneous model instances, no casting
cooldown; expired instances and world state are cleared. Server validates solo
owner and computes origins/targets; input packets contain no position/damage.

`Docs/races/assets/sandbox-workbench-sources` snapshots53 authored .bbmodel files
and available ORA, texture and layer sources (199 files). Rejected models remain
labelled archival studies. `snapshot-sources.py` preserves originals and excludes
original tutorials/reference artwork. `prepare.py` bundles this snapshot inside
the JAR at `assets/jem_visual_sandbox/sources.zip` and also produces
`build/distributions/blockbench-editable-sources.zip` beside the distributable.

Rebuild: `python snapshot-sources.py`, `node bake-workbench.mjs` if authored assets
changed, then the Java17 Gradle `jar` task (includes `reobfJar`) and `prepare.py`
against the pinned original base. Preserve the original standalone base; all its
classes/assets remain byte-identical except additive language-key merges.

Installed SHA256: `e62e7764bde142e6981a636576482d129dfcc6f898921e8e31757c1a71b685a7`.
Editable source/model hashes, native screenshots, generated-gameplay provenance
and installation receipt: `Docs/races/assets/sandbox-workbench-evidence` and
`src/main/resources/assets/jem_visual_sandbox/workbench/provenance.json`.

Production jar/reobfJar succeeded; composition verified preservation and exact
installed-file equality. No automated tests or game launch. Actual V/F6 input,
teleport/damage results, shader compatibility and runtime art acceptance remain
unobserved. Native Blockbench screenshots are editor evidence, not game footage.
Canonical Client/ManagedPack, ordinary Server artifacts, stable profiles,
MineRENT and AutoModpack publication are unchanged.


Installed only in the explicitly requested TLauncher profile **jem sandbox**.
Open a solo world, look at the top of a block within eight blocks, and press
**F9**. The R3 candidate plays at that fixed point every three seconds. Press
F9 again to stop. The binding is remappable in Controls. Existing F8 galleries
are preserved. Multiplayer never activates this preview.

This is a visual review tool. It sends no packets, modifies no blocks/entities,
and does not invoke race abilities or become a second gameplay owner. Leaving
the world clears the preview. Game pause freezes its world-time animation.

## Asset owner and export

The authoritative editable art is
`Docs/races/assets/bloom_r3_candidate/verdant_grasp_r3.bbmodel`.
Open that exact file in Blockbench and run `node bake.mjs` here. The script
requires matching live geometry, hierarchy and animation, then exports all
45 poses through the **native Blockbench 5.2.2 OBJ codec**. It preserves posed
vertices and per-face UVs; runtime interpolation smooths adjacent 20Hz samples.
The neutral editor stage is omitted. No handwritten approximation of bone
rotation conventions or replacement artwork is used.

Generated resources contain 49 cuboids / 294 quads, a 2.2-second clip and the
three original 16×16 textures. `r3/provenance.json` pins source and mesh hashes.
Minecraft renders cutout entity materials with depth, normals and local world
light; no fullbright layer, particles, fabricated terrain or baked stage.

## Production build and composition

Java17, Forge1.20.1-47.4.10, official mappings. Use the repository's existing
Gradle8.10 installation/wrapper to run **jar**, which finalizes **reobfJar**,
with `-x test -x check`. Never install the overlay JAR by itself.

The current checkout no longer contains the old full `JEMVisualSandbox`
source. This module therefore builds an additive extension, rather than
silently reconstructing the existing galleries from older source. Its pinned
local input is the original installed standalone JAR:

`65bc16ee2881c354f6fe069ba49a509b1ab784168ced66689da3f14a401dd66a`

Run `python prepare.py --base <original-standalone.jar>` after the build.
The composer preserves every old class/asset byte-for-byte and merges only
the new language keys. It rejects unexpected overlapping entries. R3's Forge
event subscribers attach to the existing `jem_visual_sandbox` mod container;
there is no second mod ID, network channel, dependency or Mixin.

Adding `--profile <absolute-path-to-jem sandbox>` backs up the current JAR
outside `mods`, then atomically installs the composed production artifact.
It refuses another profile name, a missing existing profile, or an open
sandbox Java process. Future compositions must use the preserved original
base, not the already extended output. The base binary is not stored in Git.

## Installation and evidence — 2026-10-17

- Installed JAR SHA256:
  `9b206b5d172e37de95feaa93b0a28ddcbcc731fad32d7ca9e88160e05b8d2dd1`.
- Original preserved at
  `%APPDATA%/.minecraft/versions/jem sandbox/visual-backups/bloom-r3-20261017-012624/jem-visual-sandbox-1.0.0.jar`.
- Local `installation.json` beside that backup records exact paths and hashes.
- Production `jar` / `reobfJar` completed. The composer confirmed preservation
  of base entries. No game launch, automated tests or runtime acceptance.
- Canonical Client/ManagedPack, ordinary Server, stable profiles, MineRENT
  and AutoModpack generation18 are outside this installation.
- Atlas compact rebuild, coverage audit and catalog validation completed.
  All eight watched R3 paths are covered. The full working-tree scan retains
  its two pre-existing JEMRoles warnings: the local `RaceVfxParticles.java`
  and `RaceVfxRenderer.java` differ from HEAD only in line endings. Those
  files are preserved; refreshing the generated index records their actual
  local byte hashes without a gameplay change.

Connecting R3 for review is not user approval of its final appearance. The
existing editor GIF is still editor evidence; Minecraft footage is pending.

Atlas validation passed; all17 watched current-task paths are covered. The two
pre-existing JEMRoles line-ending warnings remain outside this task.
