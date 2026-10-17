# Core control family R1

State: **CANDIDATE_NOT_APPROVED**. Last achieved evidence state: **SOURCE_EDITABLE** through an actual OpenRaster reopen/composite/export. Runtime gate: **WAITING_FOR_REAL_GAME_CAPTURE**. No installation, game capture, visual acceptance, or user approval is claimed.

This is one Group A material/control candidate, not an implemented screen, theme approval, or completion of all artwork. It contains the panel, four button states, four-cell tabs, bar track and tintable fill mask. No icon or guide illustration batch was produced.

## Editable source and export

`control-family-r1.ora` is an OpenRaster 0.0.3 document, native 112 x 112 RGBA, with five separately editable raster layers: surfaces, edge undercuts, bevel planes, fixed corners and state accents. There are no vectors, baked labels, fractional scaling, antialiasing or random surface noise in the sprite source. OpenRaster can be opened by supporting layered raster editors. This task reopened the actual ORA using its ZIP/XML/PNG stack and composited its visible layers; a desktop editor session was not performed.

The eight exact exports and their source rectangles, sizes, slices and SHA-256 values are in `manifest.json`. `regions.json` also names the individual tab cells. Exported files stay under `exports/`; none was copied to mod resources, a JAR, Client or Server.

To export the current edited ORA, run from this directory:

```powershell
python export_family.py --review-font-jar C:/Users/user/.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar
```

This command really completed in this task. `export-evidence.json` records the actual Python/Pillow versions and source hash. `author_family.py` records the initial hand-specified pixel clusters and can regenerate the initial ORA; rerunning it replaces manual ORA edits, so it is not the ordinary export command.

The configured Pixelorama executable is present. The skill's referenced `Mods/JEMVisualSandbox/production/visual_pipeline.py` is absent from this checkout; no Pixelorama exporter or absent pipeline was claimed to work. The candidate instead uses the explicitly permitted source-local OpenRaster/Pillow compositor, whose actual export produced these files.

## Reference decisions

Exact pair: `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Forge-1.20.1-jem-smp-screens/{TASTE,TECHNICAL}.md`. Taste is READY for labelled material preferences; technical readiness remains PROVISIONAL for game rendering.

Actually inspected original labelled references:

- GUI+05: `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Reference/I_like/Снимок экрана 2026-10-16 181050.png`. Extracted construction cue: dark, readable cut material and stepped pixel boundaries. Its banner/vines/layout were not copied or prescribed for this family.
- GUI+06: `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Reference/I_like/Снимок экрана 2026-10-16 181055.png`. Extracted cue: solid edge planes and a coherent physical surface family. Its wooden inventory art was not cropped or reproduced.
- GUI-01: `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Reference/I_dont_like/Снимок экрана 2026-10-14 194553.png`. Rejection comparison: tiny text-heavy brown cards and thin frames do not establish visual finish. This candidate deliberately stays a control swatch rather than claiming a full screen based on the same formula.

Also inspected the actual vanilla 1.20.1 `assets/minecraft/textures/gui/widgets.png` and `container/inventory.png` from the installed client JAR. This informed native 20px button height, restrained bevels, integer pixels and compact proportions. No vanilla texture pixels were incorporated in the eight sprite exports. A web reference search additionally located [widgets.png documentation](https://minecraft.wiki/w/Widgets.png); the installed version's actual pixels, not a newer web screenshot, were used for version-specific inspection.

Native Minecraft bitmap glyphs are rendered only into labelled review sheets so Russian label density can be inspected at the actual native size. No font atlas is retained in the source/exports. Fonts remain Mojang assets; the new control pixels are original project candidate work. Third-party reference art was not copied and no license to it is asserted.

## Candidate choices and handoff

Silhouette: compact clipped stone shoulders, transparent single exterior corner pixels, fixed 3px corners. Focal point: the live action label, with a small selected-tab gold edge. Material: neutral shallow stone controls against restrained parchment, following the existing brief's proposed palette. Surface areas remain quiet; two fixed end-cap chips avoid repeated generated noise. Default, lighter hover, inverted pressed bevel and subdued disabled values share exact geometry. The selected tab adds gold at its left edge. No ornament, fake slot, illustration or baked text substitutes for a real control.

Runtime contract remains a proposal until accepted: tile middle/edge pixels with fixed 3px corners; use 2px fixed ends for the 16 x 8 bar track. The 1 x 4 white fill is tinted and given integer width by the renderer. Pressed label shift is 1px in code. Focus border is programmatic; the first review button illustrates a 1px outline outside its regular hit geometry. Text, money, gameplay state and enabled/disabled conditions remain runtime-owned.

## Offline visual review

Review files are in `Docs/settlements/assets/review/control-family-r1/`:

- `dark-native1x.png` and `light-native1x.png`: 384 x 272, with an actual 352 x 240 control placement area and native glyphs.
- `dark-nearest4x.png` and `light-nearest4x.png`: exact nearest-neighbor four-times enlargements.
- `atlas-native1x.png` and `atlas-nearest4x.png`: source-region and pixel-edge inspection aids.

The light/dark surroundings are plain value swatches, not game captures or invented cave/grass screenshots. The two bar fractions are labelled-art swatches, not simulated resident data. These sheets do not test the native screen or gameplay.

Inspected the native dark sheet, enlarged light sheet, enlarged atlas, then the revised native sheet after adding native text shadows/focus. Russian action text fits the 224 x 20 target without reducing font size; button states retain the same silhouette; bar labels remain separate; enlargement preserves hard pixels. Remaining questions: the material is intentionally restrained, the selected-tab gold may need stronger differentiation in actual lighting, long/localized labels outside this sample and whole populated composition are unreviewed. This offline inspection grants no aesthetic approval.

The next gate requires separately authorized actual game rendering/capture of this exact candidate in the owning screen, with GUI scale, localization, keyboard focus and disabled states. Until that exists, retain **WAITING_FOR_REAL_GAME_CAPTURE** and do not integrate unaccepted textures.
