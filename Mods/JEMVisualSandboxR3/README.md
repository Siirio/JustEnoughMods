# Bloom R3 — isolated sandbox extension

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
