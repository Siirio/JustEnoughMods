# Race VFX R5 — selected M-C geometry / T-A painting

State: **SOURCE_EDITABLE / PRE-INSTALL CANDIDATES / USER APPROVAL PENDING**.
Scope remains `jem sandbox` only. No runtime files, production ability logic,
GUI classes, installed JARs, worlds or native AutoModpack generation changed.
The previously approved Bloom R3 remains installed until a separately accepted
replacement is connected. These five studies cover representative signatures,
not every passive or contextual ability.

## User direction

The user selected giant-gnome **model C** and **painting A**. The reusable owner
is the Taste Engine Models and Textures Pairs, reached through its root
`NAVIGATOR.md`. The texture Pair contains
`Templates/Reference-driven-texture-request.md` for subsequent AI requests.
Gnome models and paintings are classified separately into `I_like` and
`I_dont_like`. Full painting E is negative: only hat painting and biome-grounded
moss/dirt on boots are regional lessons. Coat embroidery, yarn/thread detail
and E's overall density cannot replace A.

Use angular rotated/resized cuboids, unequal fitted horizontal and vertical
layers, meaningful thickening, shared grounded masses, outward-leaning heads,
lighter tops/darker bases and a few attached secondary growths. A functional
thin plate has a distinct material reason even when1–3 texels wide. Coherence
does not mean smoothing or adding polygons to simulate painted texture.

## Actual outputs

- [Five native Blockbench views](R5-comparison.jpg)
- [Native clip key poses](R5-key-poses.jpg)
- Each race folder: editable `signature.bbmodel`, native48×48 UV atlas
  `materials.png`, layered OpenRaster `materials.ora`, native1x material data,
  nearest enlargement, hero/front/side/back captures and `preview.gif`.
- Original AI-authored production material sheet in `Painting/`, generated
  specifically as flat functional swatches. It is not a crop of the gnome,
  supplied references, rendered objects or a concept sheet. Prompt/provenance
  is in [Painting/README.md](Painting/README.md).

| Race | New form and material intent | Source | Motion |
| --- | --- | --- | --- |
| Nether | Connected fractured basalt eruption, unequal outward heads, local cooled crust shelves, exposed hot cuts and sparse outgrowths. | [model](nether/signature.bbmodel) | [GIF](nether/preview.gif) |
| Ancestors | Two grounded stone hands with unequal fingers, opposing thumbs, bronze structural layers and small amber inlays. | [model](ancestors/signature.bbmodel) | [GIF](ancestors/preview.gif) |
| Void | Unequal obsidian tear lips with fitted broken crystal shelves and supported inner seams; no isolated hovering bars. | [model](void/signature.bbmodel) | [GIF](void/preview.gif) |
| Sculk | Connected ink-teal bed and forward vein, outward ivory horns and small attached cyan colonies. | [model](sculk/signature.bbmodel) | [GIF](sculk/preview.gif) |
| Bloom | Shared knotted woody roots, varied branch angles, thicker unequal leaf layers and only two pink buds. | [model](bloom/signature.bbmodel) | [GIF](bloom/preview.gif) |

All clips last2.2s/44ticks, with persistent named geometry and a short settle
between emergence and withdrawal. GIFs use23 genuine native10Hz captures and
a replay pause; they are not AI-generated animation or Minecraft footage.
The neutral stage is explicitly named `preview_stage_OMIT_FROM_RUNTIME`.
No mesh tessellation, billboard particles, glow halos or camera shake are used.
Material semantics are3 swatches per race, not3 separate runtime materials.
Nether/void/sculk core faces use a separate native **emissive** editor material;
this does not imply world light emission, bloom or an implemented Forge pass.

## Painting workflow exercised here

`prepare_materials.py`: original flat production painting -> nearest reduction
to16×16 per semantic material ->7-color material palette -> explicit top/side/
underside value layers ->48×48 native atlas plus ORA. Code only maps native
pixels, palettes and directional layers; it does not invent repeated rectangle
marks. Reference pixels are never harvested into the production art. This
demonstrates one editable UV implementation of the selected direction, not
user approval of an algorithm or a universal template for all materials.

`author.mjs`: structured native MCP groups/cubes/UVs/keyframes and exports.
Each cube names its functional role and records its geometry/material in
`parts.json`. Texture faces select upper/side/underside regions at approximately
one texel per model unit (16 units/block); narrow/fractional faces round to a
native pixel and the few foundation faces wider than16 units use16 texels.
No fixed atlas corner is stamped on every face. Native rotation keys are
offsets from bone rest rotation; the author translates desired local angles
accordingly rather than doubling every tilt.

`make_review.py`: composites genuine captures into sheets/GIFs. The transparent
sky is filled with the same neutral stage color for review aids; model pixels
are not repainted. Raw captures remain alongside them.

Native commands:

```powershell
python Docs/races/assets/race_vfx_r5/prepare_materials.py
node Docs/races/assets/race_vfx_r5/author.mjs
python Docs/races/assets/race_vfx_r5/make_review.py
```

The author imports PNG bytes as a data URL. A first direct-file import blocked
the editor on its native file-access prompt; the user cleared that prompt.
No plugin security setting was changed. Offscreen views are discovered and
reused; the pinned plugin's broken optional disposal is not invoked.

## References and inspection limits

User model references REF01, REF08, REF18–21 and selected M-C/T-A guide the
specific craft corrections, not every motif or palette. Relevant vanilla
azalea/mangrove, iron golem, sculk shrieker, obsidian and basalt-delta images
were inspected during the preceding studies; source links remain in the
Taste Engine reference README and [art library](../../../art/REFERENCE_LIBRARY.md).
Before this new painting, close basalt/magma web analogues were searched again
and an actual [basalt-delta screenshot](https://cdn.mos.cms.futurecdn.net/b8wSdWNynNFyNsCW4QE7So.jpg)
and the previously inspected delta image were opened. These are proposed
world-fit anchors, not additional user approvals or copied material pixels.

Native hero/side/front/back, key poses and material pixels were inspected.
The first Nether iteration had excessive tilt and belt-like complete shelves;
its source/capture remain in `Iterations/Nether-r5a`. Current crust is local,
heads use corrected angles and the base is a connected irregular knot. The
first Ancestors silhouette read as pillars, so palms, unequal fingers and
thumbs now identify the hands. Void lips are unequal broken masses rather
than matching terminal rods. These are Codex revisions, not claimed user votes.

Runtime occlusion, shader behavior, true emissive Forge rendering, combat-distance
readability and aesthetic acceptance are unobserved. No game launch, build,
integration or installation was performed. Rejected uninstalled R4 studies and
exports were removed from active JEM paths and preserved byte-for-byte in
`Taste-Engine/Pairs/Minecraft/VFX/Reference/I_dont_like/Rejected-R4-2026-10-17`.
The earlier custom sandbox GUI/VFX cleanup remains a separate pending runtime
change; this art revision does not falsely claim it has been installed.
