# Sandbox elemental attacks — 2026-10-17

**Latest behavior:** [FOLLOWUP.md](<FOLLOWUP.md>) supersedes the first handoff
below: chunky angular lower roots and hostile action lock, independent ±10Y
eruptions plus connected ground/fire VFX, collision-top Sculk, passive low-health
guardian/shield, corrected Void pursuit and restored И/B utility controls.

User direction: preserve the liked escalating fire slashes on Russian **И**
(physical English **B**). Nether **V** becomes successive earth eruptions moving
outward from the player, with several authored Blockbench assets. Replace the
active Void and Sculk Blockbench presentations with animated VFX. The later
explicit Bloom correction supersedes the earlier VFX-only request: use actual
Blockbench vines wrapping rooted entities, adapting to their size. Repaint roots
brown, thick at the base and tapering upward; leaves must be green and leaf-shaped.
The user selected the existing cuboid-model + pixel-UV Taste Engine Pairs.

## Implemented choreography

- F6 selects a race; V invokes its main skill. Ancestors is preserved.
- Nether V: five outward bursts, two blocks apart, at ticks0/4/8/12/16; each
  strikes five ticks after emergence. Three different basalt/magma models cycle
  along the wave. Fixed cast direction, supported floors and clear paths;
  no terrain destruction.10 fire damage, six seconds burning and upward push.
- Nether И/B: the approved R5 combo, original three assets, timings and forward
  pulls, byte-identical to the preceding delivery. No redraw or rebalance.
- Void V: four-tick horizontal pause; the diagonal tear opens, then the safe
  targeted teleport is revalidated and executed at tick14. Departure and arrival
  tears close. The5-block carrier matches the final slash's extent; the painted
  aperture tapers at both tips and opens at the center. **Actual vanilla
  RenderType.endPortal**, including vanilla end_sky/end_portal textures, fills
  the authored aperture; it is not a replacement star texture. Target and safe
  destination rules remain canonical.
- Bloom V: canonical nearby-root damage/slow emits one target-bound model per
  affected entity. Brown bark and pointed green leaves;12 connected root pieces
  progressively taper upward. Runtime follows the target and scales independently
  to its current bounding-box width, height and depth.90-tick grow/hold/release
  matches the rooted interval. No artificial entity-size ceiling.
  Each branch has its own wrapping tracks: sequential tip growth, opposite curl
  and settle, delayed second-branch growth, leaf unfolding and top-first release.
  The whole coil is never uniformly scaled as a substitute for wrapping.
- Sculk V: canonical connected-sculk traversal and safe exit; authored dark
  branching tissue with broken cyan vein accents propagates along sampled path
  positions. Active main/gallery routes no longer load the old crawl model.

## Production ownership

Java and adapter: `Mods/JEMVisualSandboxR3`. Original gameplay helpers are
extracted from JEMRoles; explicit new sandbox choreography belongs only to this
standalone extension. The integrated server owns targets, damage and teleport;
clients render results. No canonical production race system is changed.

Native models: eruption0/1/2.bbmodel and entity_vines.bbmodel; MCP-authored cubes,
rigs, UVs and native keyframes. `author-elemental-models.mjs` authors these;
`bake-workbench.mjs` exports actual native posed OBJ at20Hz. Each clip has25 poses
over1.2s. Eruptions:42 quads/two materials each. Vines:84 quads/two materials.
Leaf thickness faces are omitted to avoid compressed green alpha-edge artifacts.

Textures: external editable ORA + PNG pairs for rock, molten, root and leaf.
The user's later Nether-hearted repaint uses orange upper faces grading down to
near-black bases, mapped over the shared model height on every side. Cooled
plates and hot fracture lips are separate external layers. `paint-magma.py`
preserves that source. The hot-stone material is fullbright; it emits no world light.
Rift/sculk: external layered Aseprite, exported96px RGBA cels, authored by
author.py. Native model painting is never used. `prepare.py` bundles these
editable sources with the existing source archive into the composed sandbox JAR.

## Evidence and scope

`roots-native.gif` assembles actual Blockbench viewport captures from
`native-frames/`; `roots-native.png` is the full-size mature pose.
`rift.gif` is aperture/rim playback with a labelled neutral interior;
`sculk.gif` shows exported pixel cels. These are **not Minecraft footage**.
Shader compatibility, target fit during combat and complete action feel remain
unobserved. No game launch or automated test was run.

Install target: only the existing `jem sandbox` profile, with backup and atomic
replacement. Ordinary Server, Client/ManagedPack, stable profiles, release baseline,
MineRENT and native AutoModpack generation18 are unchanged. Historical F8/F9
studies remain archival tools, not active main-skill variants.
