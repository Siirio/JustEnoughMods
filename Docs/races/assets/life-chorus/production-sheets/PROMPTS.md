# Built-in image_gen production prompt set

The tool does not disclose its underlying model version. These are the normalized
production specifications used, not instructions inferred from the reference image.
All selected original outputs are saved beside this file. RGBA originals are not
concept crops. The native sheet output was1536x1024,4columns x4rows,384x256 per cel;
use those actual dimensions, not an unfulfilled requested output-size assumption.

Common: original Minecraft Life Chorus pixel VFX,16 coherent animation cels in a
4x4 grid, transparent RGBA empty space, fixed pivots and padding, no text/labels/
characters/floors. Crisp shaped warm gold/amber/ivory material, stepped contour
clusters, restrained attached glints; no huge diffuse bloom. Preserve persistent
parts, distinct reveal/peak/hold/dissolution poses and empty central breathing room.

gold_rune: top-down engraved guardian rune; interrupted ring, unequal pointed
compass petals, nested center diamond, small nodes and bright lips. Start from
center, unfold branches and ring, travel highlights, dissolve into surviving gold
pieces. An edit request refined the initial sheet's transparent production edges.
Native alpha inspection distinguishes zero-alpha RGB haze from real opaque fog.

gold_swirl: small gold curl at feet unfolds into a tapered rising ribbon with
near/far overlap and a three-quarter-turn twist around an empty player-sized center.
Climb, unfurl, diamond-tipped focal peak, stretch into thinner surviving strands,
then sparse diamond motes. No rods, flat ring, leaves, vines or tornado.

gold_motes: six unequal four-point/hollow-diamond light blossoms ascend from feet
to chest, with ivory centers, gold shoulders, amber recesses and small cross glints.
Low gathering, richer rising constellation, opening blossoms, breaking upward
sparks, sparse survivors. Hierarchy and meaningful clusters, not equal random dots.

gold_front: separate thin ornate circular propagation material, completely empty
center. Two broken engraved lips2–4 native pixels thick, braided stepped breaks,
eight small hollow diamond nodes and controlled glints. Tiny hollow ring→opening
front→full held ring/traveling bright chips→surviving rim fragments. Absolutely no
central star, spokes, sun rays, petals or filled disc. Needed after agent rejected
oversized filled-rune playback. Use for the shared player-origin knockback fronts.

Final source/export contract and hashes: ../gold-assets.json. The executable export
procedure is ../gold-author.py. Full copyable inspiration prompts for all13 powers:
../../../VFX_REFERENCE_BRIEFS.md (resolve from this folder through the race docs).
