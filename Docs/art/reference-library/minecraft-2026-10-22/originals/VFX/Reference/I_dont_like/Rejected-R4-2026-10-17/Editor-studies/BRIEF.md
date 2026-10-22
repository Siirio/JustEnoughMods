# Race VFX reset — sandbox R4

User scope: **jem sandbox only**. User explicitly praised the installed Bloom
R3 and asked that every race receive comparable crafted VFX where appropriate.
Keep abilities; remove prior custom GUI/VFX from the sandbox. Preserve GUI
layout knowledge separately for a future redesign.

## Production contract

R3 is the positive calibration: actual cuboid volume, original native16px
materials, connected persistent parts and authored anticipation/activation/
hold/withdrawal. It is not permission to recolor the same roots for every race.
Five race-specific signature studies form this first new set. Other historical
effects are removed rather than represented by generic placeholders. The
production ability roster, passives, damage, costs, cooldowns and server
authority remain untouched. These manual sandbox studies are not synchronized
combat integration and cannot establish full race-ability coverage.

## Inspected web references before authoring

Reference images were actually opened and inspected. Selected screenshots guide
material, volume and world fit; they are not copied into shipped textures.
Only Bloom R3 has explicit user approval. The following new designs remain
proposals until viewed and accepted individually.

- ANCESTOR-A: [vanilla iron golem image](https://img.3dmgame.com/uploads/images/news/20230706/1688638475_669593.png),
  [official golem article](https://www.minecraft.net/en-us/article/meet-iron-golem).
  Broad pale weight-bearing plates, deep joints and restrained warm weathering;
  no borrowed face, red eyes or green vines.
- VOID-A: [official ruined portal screenshot](https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/screenshots/BOTW_obsidian-ruinedportal.jpg),
  [official crying obsidian article](https://www.minecraft.net/en-us/article/block-week--crying-obsidian).
  Dark broken masonry, abrupt block steps and strong negative space; avoid a
  miniature decorative portal frame or a filled neon rectangle.
- SCULK-A: [actual shrieker in-game screenshot](https://staticg.sportskeeda.com/editor/2022/06/897b3-16552025176702-1920.jpg?w=800),
  [official echo shard article](https://www.minecraft.net/en-us/article/echo-shard).
  Ink-dark connected growth, sparse cyan clusters, ivory angular feelers and
  a quiet centre; avoid permanent cyan clouds and thin lines.
- NETHER-A: [actual basalt-delta screenshot](https://staticg.sportskeeda.com/editor/2021/10/c8c06-16351874571626-1920.jpg?w=800),
  [official basalt article](https://www.minecraft.net/en-us/article/around-block--basalt-deltas).
  Stepped grey columns, dark crust and narrow hot cracks. Hot material is
  concentrated inside substantial stone, not painted over the whole scene.
- BLOOM-POSITIVE: the unchanged editable R3 and the user's explicit positive
  response in this conversation. R2 remains negative evidence only.

## Choreography and bounded assets

All new studies: native 16×16 shell/detail/core materials, original opaque
four-color clusters; per-face UV at roughly one texel per model unit. Editable
`.bbmodel` with persistent cuboids and named bones. Native posed OBJ bake at
20Hz,2.2s/44ticks, interpolation between samples. One active study, fewer than
80 cubes, three material draws, no emitters or world writes. World anchor is
the selected block top, not an invented combat target. No camera shake or glow.

| Race / study | Silhouette, focal point and phases (ticks) |
|---|---|
| Ancestors / Guardian manifestation | Two heavy segmented arms assemble a protective open arch around an empty player-sized centre. Pale outer plates, dark warm joints, a small amber keystone. 0–5t gather below ground;5–14t arms rise, wrists close;14–30t hold with one small settlement;30–44t open and sink. Never depict an independently living or attacking mob. |
| Void Walkers / Rift ambush | Two unequal jagged obsidian lips around an open vertical tear; a short faceted violet seam is secondary. 0–5t narrow compression;5–11t lips separate with a snap;11–27t suspended open gap;27–36t collapse;36–44t fragments return underground. No filled portal billboard. |
| Born in Bloom / Verdant dominion | Preserve the exact approved R3 roots, leaves, buds, UVs and44tick clip. |
| Infected by Sculk / Vein emergence | A low connected path feeds a four-tusk opening. Deep teal pads appear in a forward sequence; ivory feelers uncurl over its dark mouth. 0–8t path progresses;8–17t feelers rise;17–29t a restrained cyan core breathes once;29–44t feelers fold and path retracts in reverse. No constant through-wall overlay. |
| Nether Hearteds / Infernal eruption | Three offset basalt teeth split along a narrow hot fissure, with small crust caps and contained magma interiors. 0–5t crust separates;5–12t staggered upward eruption;12–22t heavy caps settle;22–32t orange cores contract;32–44t stone withdraws. Distinct widths/heights keep the line readable from the side. |

Review: native hero/front/side views, fixed-camera moving previews, key-pose
and texture sheets. Compare real silhouette, texel size, material, empty space,
timing and connectedness before installing. Game launch is not authorized;
shader/world occlusion and new-candidate aesthetic acceptance remain pending.
