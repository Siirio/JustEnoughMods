# Painting-study recipe — visual calibration only

Tool: built-in `imagegen`, one separate image per approach, followed by direct
inspection. This file records the reusable prompting recipe and actual inputs;
it is not a deterministic generator or a replacement for the native UV-painting
sequence. A future selected look must still be reproduced on an editable atlas.

## Shared control

Image1: `Models/I_dont_like/Model-D/hero.png` (historical control, not a selected geometry). Reference-only image inputs from the parent model
collection: REF21 mossy humanoid, REF08 painted chest, REF20 painted dinosaur,
REF15 ember creature. Preserve the gnome's silhouette, proportions, bent cap,
face, beard, limbs, boots, belt, pose, camera and complete portrait framing.
Repaint surfaces only. Neutral charcoal background, subdued grey-green floor,
ordinary diffuse lighting. All studies use rust-red cap, teal clothes, tan skin,
ivory beard, brown leather and a small gold buckle. No scene, props, labels,
borrowed characters, photoreal PBR, bloom or cinematic lighting. Request crisp
deliberate pixel clusters following the object's surfaces.

This geometry-preservation instruction is a request to a generative tool, not
a guarantee. The outputs have small form/framing/illumination changes; source
topology remains only in the unmodified control .bbmodel. Do not use pixel-wise
differences or these images to claim native UV or geometry equivalence.

## Approach-specific instructions

- **T-A — material clusters:** large readable material masses first, smaller
  wool/fold/beard/leather marks second; selective contact scuffs and tiny sharp
  buckle planes. Seek restrained vanilla-adjacent painting without random noise.
- **T-B — sculptural planes:** broad connected warm light / cooler shadow
  regions turn around hat, cheek, beard and belly, with little microdetail.
  Revision: eliminate checkerboard marks; keep the cap mostly one midtone plus
  one broad lit and one turning plane, coat3–4 substantial fold shapes and beard
  three broad ribbons. The revised input references were REF15 and REF06.
- **T-C — rich illustrative material:** directed beard locks and cloth folds,
  interlocking painted color shapes, selective wear, hue/value contrast and
  intentional asymmetry. Preserve quiet regions rather than treating every
  pixel as an equal texture event.
- **T-D — graphic quiet:** large clean color fields, a few decisive angular
  shadow cuts and focal facial shapes. Revision: prohibit all-over mosaic;
  use one cap shadow wedge, two broad blue coat folds and a very small cuff
  accent. References for the revision: REF15 and REF06. This deliberately
  tests a calmer style instead of assuming detail quantity improves the art.
- **T-E — ornament and wear:** forest-artisan character painting with designed
  coat embroidery, one cap stitch path, worn leather and small boot-level moss
  accents. Keep all motifs on existing surfaces without changing silhouette
  or adding holiday objects.

The first B/D results were too close to the other mosaic treatments. Their
initial files remain preserved; the final comparison uses the revised outputs.
The final B/D studies are intentionally simpler. No method is declared good,
preferred or better than the user references before the user's response.

## Generation provenance

Default originals were retained in the task's `.codex/generated_images` folder;
workspace copies are the files used in the comparison board.

| Workspace file | Original generated filename |
| --- | --- |
| Paintings/I_like/Texture-A.png | exec-a0e658ba-751b-475b-9c97-6751f17a60b8.png |
| Paintings/I_dont_like/Texture-B-initial.png | exec-3ca40d7c-4638-41ce-934e-3ddf0c6802e9.png |
| Paintings/I_dont_like/Texture-C.png | exec-25857a47-b787-439c-864f-e89a3867b83b.png |
| Paintings/I_dont_like/Texture-D-initial.png | exec-2c84899b-c8e8-490e-a2c9-146e46547e09.png |
| Paintings/I_dont_like/Texture-E.png | exec-e6d23546-0b3a-4bd0-ad80-605971ccf0f4.png |
| Paintings/I_dont_like/Texture-B.png | exec-797ad859-1c35-4dfe-b53f-a52e31dc6d5c.png |
| Paintings/I_dont_like/Texture-D.png | exec-3d0bc0c0-5a86-463a-af2a-72516b7f63c3.png |

Choose the model and painting axes independently in [README.md](<README.md>).
The active texture Pair remains the sole owner of production UV authoring.


## User selection — 2026-10-17

**Geometry M-C is selected. Painting T-A is selected.** This is the user's judgement, not an agent ranking. No other full candidate is positive. Selected sources live under `Models/I_like` and `Paintings/I_like`; all remaining candidates, including initial B/D paintings, live under their respective `I_dont_like`.

- **M-C:** vary cube size and rotation; thicken meaningful regions with precisely fitted, differently sized and rotated horizontal and vertical layers. Retain visible angular Minecraft edges. Coherence means connected, intentional assembly, never smooth surfaces, extra tessellation or identical plate stacks.
- **T-A:** broad, legible, intentionally painted material clusters with restrained detail. Different functional parts need different material decisions, even a1–3 texel-thick plate. Use directional light/dark color shapes and selective accents inside each material; no mathematical all-over mosaic.
- **T-B:** rejected as voxelated polygon-like treatment with needless apparent complexity. **T-D:** rejected as dull and without artistry. **T-E:** rejected overall for excessive coat/thread/embroidery meaning compressed into tiny pixels and poor Minecraft fit. T-C was not selected and stays negative for future defaults. The user's Cyrillic keyboard letters И/В/У correspond to B/D/E in context.
- **Strict E exception boundary:** only hat painting and biome-grounded moss/dirt on boots are useful regional lessons. E remains `I_dont_like`; its ornate coat, yarn, stitches and general detail density MUST NOT override A. Do not present the full E image as a positive example. Moss is evidence of environment/use, not a requirement on every object.
- **Missing from the experiments:** grounded clusters without lonely parts; eruption heads lean outward from the shared center; lighter, sometimes glowing tops and darker bases; a few attached secondary growths with distinct size/rotation/material. Adapt to subject: emission is meaningful for magma/sculk, not mandatory on wood/stone everywhere.

The five paintings remain AI look studies, not implemented UV textures. Selection establishes visual direction; actual native textures, geometry rotation and runtime remain separate evidence.
