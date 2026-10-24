# Candy King pumpkin skeleton - GeckoLib editor study

This directory preserves the user-supplied visual target and the separate Blockbench reconstruction requested on 2026-10-21. It supersedes the earlier low crawling Phase 1 direction for current visual review without deleting that historical study.

## Editable deliverables

- `candy_king_geckolib.bbmodel` - Blockbench `geckolib_model` source, identifier `geometry.candy_king_pumpkin_skeleton`.
- `textures/candy_king_atlas.aseprite` - layered 128 x 128 native pixel source.
- `textures/candy_king_atlas.png` - direct Aseprite export embedded in the model.
- `ANIMATION_BRIEFS.md` - reference-grounded art direction for each clip.
- `renders/c2-*.png` - final editor renders used for the bounded visual review.
- `reference-user.png` - exact image supplied by the user for this reconstruction.

## Rig and clips

The model uses 22 named bones and 102 cuboids. Head/neck, body/chest, pelvis, abdominal maw halves, back branches, shoulder-elbow-wrist chains and hip-knee-foot chains are separate articulated parts. The source contains:

- `animation.candy_king.idle` - 2.8 s looping hold;
- `animation.candy_king.walk` - 1.2 s looping alternating gait;
- `animation.candy_king.maw_bite` - 1.15 s one-shot abdominal bite.

These clips demonstrate an animation-ready hierarchy inside the editor. Forge/GeckoLib controller code, exported runtime JSON, hitboxes, navigation, emissive rendering, performance and in-game scale are intentionally outside this sandbox-only task and remain unverified.

## Reference and authorship boundary

The supplied image owns the square pumpkin head, elongated skeletal proportions, rib cage, back branches, long hands and feet, ragged abdominal mouth and visible candy. External scarecrow searches were used only as technical proposals for joint and silhouette reading. No pixels were copied, traced, cut out or downsampled from any reference; production texture pixels were painted directly in the layered Aseprite source.

