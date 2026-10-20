# C1: directory and chest integration editor views

**Aseprite editor compositions from current consumer coordinates. Not Minecraft screenshots, a runtime pass or user approval.** One C1 correction round addresses the stale C0 directory/chest compositions only. No historical gallery file or production asset was overwritten.

[Directory1×](01-directory-c1.png) · [Directory2× nearest](01-directory-c1-2x.png) · [Chest1×](11-chest-content-c1.png) · [Chest2× nearest](11-chest-content-c1-2x.png)

Editable sources: `01-directory-c1.aseprite` and `11-chest-content-c1.aseprite`, each with existing-material, source-control-geometry and clearly labelled sample-data layers. `compose-c1.lua` composes directly inside native Aseprite using the current module's materials/frames, existing locally extracted Minecraft glyph providers and sample item bitmaps. No new production pixel painting, AI image or outside PNG rasterizer is used.2× is review-only.

Source owners read: `SettlementScreen.rebuildContent`, `selector`, `navigation`, `chestContents`, `chestPageSize`, `listWidgets`, `drawRow`, `render`; `SettlementDashboardArt`; `SettlementPageArt`. Coordinates below are panel-relative, before the20-pixel transparent review margin. Panel origin in each exported canvas is20,20 so existing header vines are visible above the header.

| Consumer bound | Directory sample | Chest sample |
| --- | --- | --- |
| Panel |460×277 (`148+3×43`) |364×276 (`168+6×18`) |
| Sidebar |None, selector is compact |None, storage height276<310 |
| `cx / cy / cw / bottom` |8 /60 /444 /255 |10 /80 /344 /254 |
| Search |8,60;444×20 |Not present in contents mode |
| Compact section picker |Not present before selection |10,53;344×20 |
| Rows/grid |Cards at8,86 /129 /172;440×40 |Grid at101,120;9×4 native18-pixel slots |
| Back/name and coordinates |Not applicable |Back10,80; name68,85; coordinates10,105 |
| Page capacity |3 directory rows |36, from `9×floor((254−52−120)/18)` |
| Pagination |No page controls for total3 |Previous10,210 disabled; next38,210 enabled |
| Count/footer |Count8,237; Found352,231;100×20 |Slots68,216; Feed10,234;344×20 |
| Exported canvas including review margin |500×317 |404×316 |

Directory data is explicitly illustrative: Речная долина12/84, Сосновый берег8/70, Тихий луг5/36; no row is hovered or selected. Chest sample: known empty54-slot container, offset0, coordinates128,64,−207;36 slots are visible with the remaining18 reachable by native pagination. Empty is an intentional state, not substituted imagery for missing item-render evidence.

The actual source title uses1.4× GPU scaling; this editor composition applies its native glyphs at the same intended scale/anchor with nearest rasterization. Native bell/bread bitmaps represent the sample items; an Aseprite bitmap is not the Forge ItemRenderer. Hover, blinking caret, keyboard focus, tooltips, hit-testing, arbitrary text and real GPU blending remain unobserved. No screenshot background or invented game world is added.

Reproduction uses the already installed native Aseprite and the existing ignored local Minecraft font/item reference inputs:

```powershell
& 'C:/Users/user/.codex/tools/aseprite/build-1.3.18.6/bin/aseprite.exe' --batch --script-param 'project=C:/MyProjects/Minecraft/JustEnoughMods' --script-param 'output=C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/settlements-pages-2026-10-20/integration-preview' --script 'C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/settlements-pages-2026-10-20/integration-preview/compose-c1.lua'
```

Original GUI+05/+06 and negative runtime01/06 images were reopened for this round; both new outputs were opened at native size and integer2×. [Owning C0/C1 audit](../integration-visual-review.md) records the separate art/editor/runtime verdicts. No build, automated test, Minecraft launch, runtime installation or release action was performed for C1.
