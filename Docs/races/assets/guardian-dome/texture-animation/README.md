# Guardian wave texture animation

The user explicitly approved and locked the smooth two-cel animation on
2026-10-17: “THAT WHAT I WANTED - LOCK THAT TEXTURE ANIMATION LOGIC”.
The same prompt requests one-shot full-surface coverage, warm color gradients,
emission and slight transparency. These are the user's decisions; the timings,
palette bands and path conditioning below are Codex implementation choices.

## Protected reference

`approved-loop/guardian-smooth.bbmodel` and `preview.gif` are the exact approved
24-frame/20-FPS loop. `original-user-frames.bbmodel` preserves the live unsaved
user artwork before interpolation. `approved-loop/author.py` is the historical
script snapshot, including its original temporary input path; do not regenerate
over the approved evidence. The current portable owner is `author.py` here.

Preserve coherent silhouette interpolation from authored cels, signed-distance
fields, stable 256x64 UV frames and crisp unfiltered native pixels. Playback,
coverage and material are intentionally extended by the latest user request.
See the precise persistent contract in root `AGENTS.logic-locks.md`.

## One-shot candidate

- `one-shot/guardian-one-shot.bbmodel`: unchanged connected dome topology and UVs;
  external animated atlas, emissive preview material; `guardian_appear_once`
  is a **Hold on Last Frame** clip, 35/24 seconds long.
- `one-shot/gold-wave-one-shot.png`: 36 native256x64 frames, vertical256x2304 strip,
  24FPS; wave reaches complete surface coverage by frame30 and holds afterward.
- `one-shot/gold-wave-cels.ora`: native-size per-frame editable groups, with
  amber-field and ivory-crest layers. Timing is in `timing.json`.
- `one-shot/contact-sheet.png`, `one-shot.gif`: actual exported texture sequence.
- `one-shot/native-frames/*.png`, `native-one-shot.gif`: 36 actual Blockbench
  timeline captures on the mapped model, not a replacement CPU model rendering.
  Capture waits120ms after each timeline seek for the texture/UV update to reach
  the render; immediate captures after reopening produced empty stale frames.

Open the candidate, choose **Animate**, select `guardian_appear_once`, return
the timeline to0 and press Space. The timeline drives the texture frames and
holds the completed shell. The Paint panel's independent texture Play button
is a repeating flipbook preview; it is not the one-shot playback control.

The reveal extends the approved shape interpolation with one full longitudinal
wrap and upward travel. Main-contour tracing excludes detached brush crumbs;
seam correction joins the wrap. Monotonic coverage prevents new holes behind
the front. A native stepped amber/gold/ivory palette shades the moving crest and
settled body; alpha is110–135/255 on the body and up to about223/255 at the crest.
Emissive preview means unlit/fullbright, not guaranteed bloom or light casting.

Review: actual native middle/end poses show a continuous moving crown opening
followed by complete top coverage. Prior spike artifacts from per-column first
pixels were corrected by tracing the largest painted run and conditioning only
the contour path; exported texture pixels remain unfiltered. The approved loop
remains accepted; this one-shot extension awaits user acceptance.

This is editable-art production only. No Java/resource runtime replacement, JAR,
Client/ManagedPack or Server mutation is part of this iteration. The current
custom Java renderer still needs frame selection, emissive alpha material and
one-shot hold behavior wired in before Minecraft can show this animation.
Use `min(floor(ageSeconds*24),35)`, never modulo, for that future integration;
this is a consumer contract, not a claim of installed support. Existing30-tick
guardian gameplay lifetime and authority remain untouched. No game launch,
automated tests, build or static-analysis run was performed.
