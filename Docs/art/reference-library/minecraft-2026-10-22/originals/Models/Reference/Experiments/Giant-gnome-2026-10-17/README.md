# Giant gnome — separate model and texture calibration

Date:2026-10-17. User requested five texture-painting approaches and, separately, five actual model approaches on a giant gnome. **USER SELECTED M-C + T-A.** E is negative overall, with hat/boot exceptions only. No study is installed in Minecraft.

## What is actually produced

- Five real editable Generic-format Blockbench models, each with native hero/front/side captures. Geometry is roughly7 blocks tall at16 model units per block, with the same character identity, palette, neutral stance, camera and editor lighting. Cuboid and mesh approaches differ intentionally; Generic meshes are not automatically valid for a cuboid-only game exporter.
- Five imagegen paint-over/look-development studies based on the same Model-D control render, with the same intended material palette. These are **not painted UV atlases, native layered pixel sources, real rerenders of changed3D textures, or runtime evidence**. Generative output can drift in silhouette, camera, local light and detail; judge painting language here and geometry only from the native model board.
- Two labelled comparison boards. Originals and source variants remain separately available.

## Comparison boards

- [Model construction](<Models-comparison.jpg>)
- [Painting looks](<Painting-comparison.jpg>)

## Model approaches — actual sources

| ID | Construction | Source / native views |
| --- | --- | --- |
| M-A | Economical axis-aligned large cuboids; deliberate simple baseline. | [.bbmodel](<Models/I_dont_like/Model-A/giant-gnome-A.bbmodel>), [hero](<Models/I_dont_like/Model-A/hero.png>), [front](<Models/I_dont_like/Model-A/front.png>), [side](<Models/I_dont_like/Model-A/side.png>) |
| M-B | Related stepped widths for hat, belly and beard taper. Its visible staircase treatment is a proposed construction, not assumed vanilla correctness. | [.bbmodel](<Models/I_dont_like/Model-B/giant-gnome-B.bbmodel>), [hero](<Models/I_dont_like/Model-B/hero.png>), [front](<Models/I_dont_like/Model-B/front.png>), [side](<Models/I_dont_like/Model-B/side.png>) |
| M-C | Rotated cap, shoulders and beard segments; compare join coherence and distracting gaps. | [.bbmodel](<Models/I_like/Model-C/giant-gnome-C.bbmodel>), [hero](<Models/I_like/Model-C/hero.png>), [front](<Models/I_like/Model-C/front.png>), [side](<Models/I_like/Model-C/side.png>) |
| M-D | Continuous faceted low-poly lofts for body, beard and bent cap, with cuboid limbs. Requires a compatible mesh consumer. | [.bbmodel](<Models/I_dont_like/Model-D/giant-gnome-D.bbmodel>), [hero](<Models/I_dont_like/Model-D/hero.png>), [front](<Models/I_dont_like/Model-D/front.png>), [side](<Models/I_dont_like/Model-D/side.png>) |
| M-E | Hybrid hull and cap with separate shaped beard locks, moustache and cuboid limbs; compare integrated detail against added fragmentation. | [.bbmodel](<Models/I_dont_like/Model-E/giant-gnome-E.bbmodel>), [hero](<Models/I_dont_like/Model-E/hero.png>), [front](<Models/I_dont_like/Model-E/front.png>), [side](<Models/I_dont_like/Model-E/side.png>) |

All models were created through native structured Blockbench MCP cube/mesh tools. `model-studies.mjs` records the construction and cameras. It reuses the existing private offscreen view `race_r4_nether` because the running editor already had its maximum four offscreen views; no user viewport was resized. The editor source is saved before rendering. This is a static silhouette/material benchmark, not a completed animation rig.

## Painting approaches — visual proposals

| ID | Intended artistic method | Image |
| --- | --- | --- |
| T-A | Controlled material clusters: a vanilla-adjacent painted cloth/beard/leather interpretation. | [image](<Paintings/I_like/Texture-A.png>) |
| T-B | Large sculptural shadow/light planes with little microtexture; revised to remove the initial repeated mosaic. | [image](<Paintings/I_dont_like/Texture-B.png>) |
| T-C | Rich illustrative material painting, stronger local color variation and directional beard/fold detail. | [image](<Paintings/I_dont_like/Texture-C.png>) |
| T-D | Graphic clean fields and selective accents, guided by the restrained ember-character reference; revised to remove all-over texture. | [image](<Paintings/I_dont_like/Texture-D.png>) |
| T-E | Ornamental material narrative: designed gold seams/embroidery, cap stitch path, wear and moss accents. | [image](<Paintings/I_dont_like/Texture-E.png>) |

Initial B/D generations are retained as `Texture-B-initial.png` and `Texture-D-initial.png`. Inspection found A/B/D initially too similar and mosaic-driven, so B/D were regenerated with explicit large-shape/quiet-field direction. The final candidates remain experimental; restrained B/D deliberately expose whether a simpler painted language is wanted instead of claiming that more detail is always better.

The [painting recipe and generation provenance](<Painting-recipe.md>) preserve
the control, reference inputs, approach instructions and revised-output mapping.
The native [model construction script](<model-studies.mjs>) preserves actual
geometry and camera settings separately.

## Sources and reference interpretation

User-supplied reference IDs and originals are in [the collection README](<../../README.md>). The paint studies use Model-D plus REF21, REF08, REF20 and REF15; revised B/D use Model-D plus REF15 and REF06. Those images guide only the explicitly selected craft qualities. No third-party image is cropped into a production texture.

A separate [gnome web reference](https://sketchfab.com/3d-models/gnome-c0d7253a6b46423590c0df944151428d) was actually inspected for the recognisable cap/beard/coat silhouette; it is a proposed subject reference, not user taste or downloaded mesh/code. Geometry here is original.

The [texture research reset](<../../../../Textures (Resource packs)/Blockbench-textures/Minecraft-pixel-uv-and-tiling/Knowledge-sources/01_reference_driven_texture_reset_2026-10-17.md>) records the supplied-source review, primary art sources, procedural-art research and relevant repository observations. The [active painting sequence](<../../../../Textures (Resource packs)/Blockbench-textures/Minecraft-pixel-uv-and-tiling/TECHNICAL.md>) is the single technical owner.

## User selection — 2026-10-17

**Geometry M-C is selected. Painting T-A is selected.** This is the user's judgement, not an agent ranking. No other full candidate is positive. Selected sources live under `Models/I_like` and `Paintings/I_like`; all remaining candidates, including initial B/D paintings, live under their respective `I_dont_like`.

- **M-C:** vary cube size and rotation; thicken meaningful regions with precisely fitted, differently sized and rotated horizontal and vertical layers. Retain visible angular Minecraft edges. Coherence means connected, intentional assembly, never smooth surfaces, extra tessellation or identical plate stacks.
- **T-A:** broad, legible, intentionally painted material clusters with restrained detail. Different functional parts need different material decisions, even a1–3 texel-thick plate. Use directional light/dark color shapes and selective accents inside each material; no mathematical all-over mosaic.
- **T-B:** rejected as voxelated polygon-like treatment with needless apparent complexity. **T-D:** rejected as dull and without artistry. **T-E:** rejected overall for excessive coat/thread/embroidery meaning compressed into tiny pixels and poor Minecraft fit. T-C was not selected and stays negative for future defaults. The user's Cyrillic keyboard letters И/В/У correspond to B/D/E in context.
- **Strict E exception boundary:** only hat painting and biome-grounded moss/dirt on boots are useful regional lessons. E remains `I_dont_like`; its ornate coat, yarn, stitches and general detail density MUST NOT override A. Do not present the full E image as a positive example. Moss is evidence of environment/use, not a requirement on every object.
- **Missing from the experiments:** grounded clusters without lonely parts; eruption heads lean outward from the shared center; lighter, sometimes glowing tops and darker bases; a few attached secondary growths with distinct size/rotation/material. Adapt to subject: emission is meaningful for magma/sculk, not mandatory on wood/stone everywhere.

The five paintings remain AI look studies, not implemented UV textures. Selection establishes visual direction; actual native textures, geometry rotation and runtime remain separate evidence.

## Native model integrity

| Model | Cubes (excluding stage) | Meshes | Source SHA256 |
| --- | ---: | ---: | --- |
| M-A | 33 | 0 | `bbd3547ef4111027d86cab039f5f1aacf0bf90761a301833204e879072fd7a1d` |
| M-B | 40 | 0 | `0904ab9611f2ab54b40c9462ea637b1f069afc0170ea013f05fae6bfd08cbcde` |
| M-C | 36 | 0 | `68606b810ab36812f301dfd2ea903fa5949e3247515609f5d334878a8e6d69ba` |
| M-D | 27 | 3 | `5f8ed23912e8df823bc2beea2bbce14eee405fe46fe294cd1d4226cfd8cade5b` |
| M-E | 31 | 5 | `69626e078ebe49c77ed4a29a26debe3831f1f0080444af24a92349da35bbcf76` |


## Later scope correction: outward is not universal

The user clarified that the magma example was one of many possible forms and fits Infernal Eruption only in this case. M-C construction and T-A painting remain selected craft principles; do not propagate that example into a common race silhouette. Infernal now specifically uses three successive strikes and brighter upper exterior around all four sides grading to a dark base. Other abilities use their own references, spatial behavior and lighting. See the VFX Pair and its researched spatial-pattern proposals for the current candidate mappings.
