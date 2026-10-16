# R6 — different spatial patterns per ability

State: **SOURCE_EDITABLE / PRE-INSTALL CANDIDATES / USER APPROVAL PENDING**.
Sandbox-only art work. No JAR, game, GUI, gameplay, ordinary Server,
Client/ManagedPack or AutoModpack generation changed.

The user corrected R5's overgeneralization: outward-leaning heads were one
example among many, fitting Infernal Eruption in this case only. M-C's angular
rotated/resized construction and T-A's material painting remain craft choices;
they never imply a shared silhouette, value gradient or choreography.

The user then refined Infernal: **one column strikes, then second, then third**.
The **whole outer perimeter**, including all four vertical side faces, must
be bright high up and gradually darker toward the base. A glowing cap alone
does not satisfy this. Source UVs map each fragment to its actual height range
within the full column, so the gradient does not reset at every cuboid join.
`prepare_thermal_gradient.py` preserves the original painted clusters and
revalues native pixel rows into the requested wraparound gradient, retaining
an editable ORA layer. Emissive editor material carries the light upper exterior
and dark lower values; this does not claim actual Minecraft light emission.

## Current pattern set

| Ability study | Distinct form and motion | Source | Native motion preview |
| --- | --- | --- | --- |
| Infernal eruption | Three unequal outward-supported columns strike around0.18/0.50/0.82s, settle individually, then withdraw. Warm/light exterior wraps all four sides high up and grades to charcoal below. | [model](nether/signature.bbmodel) | [GIF](nether/preview.gif) |
| Ancestors ward | Three broad overlapping shields fold toward the front, with reinforced edges and a small central amber seal. Forward coverage, not radial tips. | [model](ancestors/signature.bbmodel) | [GIF](ancestors/preview.gif) |
| Void rift | Wide obsidian jaws separate vertically around a compressed horizontal slit, then close. The empty gap is the focus. | [model](void/signature.bbmodel) | [GIF](void/preview.gif) |
| Sculk veins | Low connected trunk and dendritic forks activate along the ground in sequence, then retract. No upright ivory horn bouquet. | [model](sculk/signature.bbmodel) | [GIF](sculk/preview.gif) |
| Bloom | Three linked woody arches converge inward under a compact layered canopy, retaining hollow space below. Leaves appear after the ribs settle. | [model](bloom/signature.bbmodel) | [GIF](bloom/preview.gif) |

[Five-pattern comparison](R6-patterns.jpg), [Infernal perimeter from both sides](Infernal-perimeter.jpg),
[Infernal1-2-3 poses](Infernal-sequence.jpg). These are real native Blockbench
captures, not generated concept renders or game footage. Each clip lasts2.2s,
with45 captured20Hz frames and a replay pause. All models use named rotated
cuboids with native UV textures, layered ORA source and stable bone clips.
The stage is explicitly named `preview_stage_OMIT_FROM_RUNTIME`.

Front/hero/back captures now follow Blockbench's actual -Z front convention,
so the forward shield/inlay face is visible in the main review image. Plain
stage color fills transparent sky in comparison sheets only; model pixels are
not repainted. Raw captures remain in each race folder.

## Researched analogues

The full evidence collection, exact downloaded images, source URLs, visible
lessons and scope are in
`Taste-Engine/Pairs/Minecraft/VFX/Reference/Proposals/Spatial-patterns-2026-10-17/README.md`.
Actual shield, open-shulker, mangrove-root and spreading-sculk images were
searched and inspected before these new structures were authored.

- [Raised shield reference](https://gameboyworld.com/how-to-block-with-a-shield-in-minecraft/): broad directional protective plane.
- [Official open shulker reference](https://www.minecraft.net/en-us/article/meet-shulker): substantial separated halves around negative space; no creature or texture copied.
- [Mangrove root image](https://i.imgur.com/kNyU1pk.jpg), [official context](https://www.minecraft.net/pt-br/article/the-mangrove-restoration-project): inward support arches and hollows.
- [Sculk spreading reference](https://www.sportskeeda.com/minecraft/how-sculk-catalyst-work-minecraft-the-wild-update), [official vein context](https://www.minecraft.net/sv-se/article/a-very-scary-snapshot): connected low ground coverage and branching.

These new composition mappings are Codex proposals requested for review,
not user-approved preferences. No reference image pixels/code were copied.
Materials outside Nether reuse the original R5 authored material sources;
this task changes their composition rather than claiming a new paint vote.
Full E remains rejected, with only the existing hat/mossy-boot regional lessons.

## Reproduce and inspect

```powershell
python Docs/races/assets/race_vfx_r6/prepare_thermal_gradient.py
node Docs/races/assets/race_vfx_r6/author.mjs
python Docs/races/assets/race_vfx_r6/make_review.py
```

The author uses structured live MCP calls, imports actual PNG bytes as data
URLs, converts desired bone angles into native rotation offsets and exports
the real editor project. Read `parts.json` for named part/material roles and
source hashes. Review includes genuine front/back/side/hero and key poses,
Infernal successive-strike frames and the exterior from opposite sides.
No tests, build, game launch or new runtime integration was performed.
Combat distance, shaders, terrain occlusion and aesthetic acceptance remain
unobserved. Installed Bloom R3, the prior sandbox GUI cleanup backlog and
published native generation18 are unchanged. R5 remains historical evidence;
its outward pattern is superseded for the other four races.
