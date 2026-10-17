# Connected dome — ART_CANDIDATE, runtime acceptance pending

## Evidence and cause chain

1. User reports a dome of clipping pottery-like panels. Actual native negative
   evidence: ../guardian-mosaic/native-peak.png; owning source author.py creates
   41 independent four-cuboid plaques and repeats four16px engraving motifs.
2. Each independent panel has fixed width/height and separate local transforms.
   Its radial center follows latitude, but its edges never share vertices with
   neighboring rows. Fixed plate size and shrinking circumference create overlaps.
3. The old native exporter bakes those independent cube faces, preserving overlaps;
   it cannot turn their placement into a connected shell. Each plaque also resets
   UVs to an item-like tile, so the texture reinforces the disconnected appearance.
4. The prior Java route interpolates those faces and computes flat per-face
   normals. It owns no shared position/UV/normal contract capable of removing seams.
5. The concrete missing invariant was in geometry authoring/export ownership:
   a dome must own shared topology and coherent surface attributes before export.
   New author.py and export.mjs establish that invariant once; Java consumes it.

The former geometry was actual cuboids rather than camera-facing billboards, but
that distinction did not fix its observed clipping or pottery-like appearance.
No deployed/runtime incident is inferred from editor evidence. No unrelated system
layers are implicated by this source-backed chain.

## New source and observed output

One194-position/208-face native mesh, with outer/inner surfaces and a connected
rim. No plate, sherd or item instances. Rings share vertices across the longitude
wrap, the crown shares one vertex per surface, and face winding follows the normal
direction. Per-corner UVs retain the wrap seam without splitting position ownership.
Analytic smooth normals are exported, not reconstructed as flat face crosses.

External four-layer256x64 ORA/PNG uses one painted wave, amber body, gold crests
and compact ivory accents across the whole dome. Sparse polar detail avoids UV
pinching. Target base density18.5px/block. Nearest sampling and no runtime blur.
Base.1 / radius2.2 / thickness.04 blocks preserve ordinary1.8-block player clearance.

Actual Blockbench captures: native-frames and native-motion.gif; native-peak.png
shows the connected shape and continuous texture. Native editor uses default
texture mode for geometry inspection: its initial additive preview saturated to
white and is preserved as additive-editor-overexposed.png. Do not call that
overexposed picture a successful material review. Blockbench uses different
shading from Minecraft; runtime additive composition and smooth-normal lighting
remain unobserved. Runtime selects one radial surface to avoid double patterning.

Current agent observation: the clipped medallion grid is gone from the native
view; one gold wave crosses connected faces without panel resets. The geometry
remains visibly low-poly in the editor. Artistic approval is not inferred.

## Delivery limits

GuardianDomeRenderer uses indexed source geometry, per-corner UV/normal values,
one additive material and native event lifecycle. No new gameplay authority,
network protocol, texture billboards, terrain decal or periodic particle emitter.
The earlier mosaic remains USER_REJECTED evidence only; its runtime exports are
removed. Old smooth-sphere implementation also remains rejected history.

Production jar/reobfJar is the permitted packaging activity. No tests, linters,
static-analysis commands, game launch or launcher-profile mutation. Build/receipt
in delivery.json; ordinary Server/ManagedPack and published generation18 unchanged.
First/third-person camera switching, lighting, bright terrain and shader behavior
are NOT OBSERVED in Minecraft. Runtime/user acceptance remains pending.

Native animation captures show uniform geometry growth/release only. The30-tick alpha envelope is Java-owned and is not represented by the editor GIF. No automated topology tests or runtime scenario was run.

Shared-checkout blocker: full Atlas audit/validation currently fail because a separate Economy refactor is moving JEMEconomy sources to JEMCurrencyCore while its old records still name removed files. The complete task change is covered by the canonical source-watch scan; global success is not claimed. Unrelated Economy changes and generated Atlas drift are excluded from this delivery.
