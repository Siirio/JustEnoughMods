# Gemini HUD brief

User direction, 2026-10-23: locally installed Google Gemini should draw the HUD. The previous two authored clock candidates are rejected. The last explicit visual correction was to make the sun and moon bigger and more expressive. Rejected candidates are anti-examples, not geometry/style templates.

Create one coherent Minecraft-native HUD pair: a day/night circular dial and a seasonal circular wheel. Each actual game asset is exactly 24 x 24 RGBA pixels, origin top left. Transparent exterior, hard pixel edges, nearest sampling, no text, no gradients, bloom or smooth/vector contours. Do not return a screenshot or a preview labelled as the native PNG. No resizing/downsampling/cropping of a large illustration into the game assets. If exact native export is unavailable, return a labelled concept and disclose the actual image dimensions.

The day/night icon must have a large readable square sun and a clear crescent moon, separated by quiet day-blue and night-dark regions. An unobtrusive rim, strong material/value hierarchy and connected pixel clusters; avoid a tiny sun lost inside a thick brown frame. The game draws a moving marker over the rim at runtime; do not bake a current phase pointer or time text. Preserve useful interior space. The previous plus-like yellow symbol and dull brown frame were not accepted.

The seasons icon is a separate 24 x 24 wheel: Spring centred at TOP, Summer RIGHT, Autumn BOTTOM, Winter LEFT, with diagonal quadrant boundaries. Four clearly distinct pixel-art season regions, quiet enough for a small moving runtime pointer. Use readable native plant/snow/sun cues rather than ornate glyphs. Do not bake a pointer or season label. The game tints the same wheel gray when the dimension has no seasons.

Display at actual game size: both icons 24 UI pixels, top-left anchor x8 y8, 4px spacing. They must remain readable with ordinary Minecraft scenery behind them and at common GUI scales. No oversized illustrated background.

Outputs requested: day_cycle.png and season_cycle.png, each exactly24x24 with alpha. Keep an enlarged nearest-neighbour preview clearly separate from the native outputs. Describe any generation/export limit honestly.

## References to attach

- Native Minecraft1.20.1 clock reference: native resource in the installed vanilla client JAR, `assets/minecraft/textures/item/clock_00.png` (technical/native silhouette reference, not a prescribed copy).
- `../reference-library/minecraft-2026-10-22/originals/Icons/I_like/Снимок экрана 2026-10-20 215003.png`: connected forms and selective material/value contrast only; do not copy cart/tree content.
- `../reference-library/minecraft-2026-10-22/originals/GUI/Reference/I_like/Снимок экрана 2026-10-16 181525.png`: compact, legible semantic pixel symbols only; do not copy labels.
- `day_cycle-c1.png`, `day_cycle-c2.png`: rejected anti-examples. Do not trace or reuse.

Exact active taste profile: `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Forge-1.20.1-jem-gui-and-icon-asset-production/TASTE.md`; companion TECHNICAL.md and shared ART_DIRECTION.md. Personal GUI taste is PROVISIONAL; user feedback above controls this specific revision. No image has been approved for installation.

## Local app handoff

Installed process found at `C:/Users/user/AppData/Local/Google/Gemini/app-1.16.0/GeminiAppLauncher.exe`. No prompt was submitted: native-app input is disabled in this Codex session. Do not record this prepared prompt as an executed Gemini generation.
