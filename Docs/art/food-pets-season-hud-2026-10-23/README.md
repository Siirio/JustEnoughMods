# Food, shelters and seasonal HUD: visual handoff

The user requested the final v2 feature specification on2026-10-23, then took ownership of creating both shelter models with Gemini and requested two exact prompts. The source implementation continues independently. No shelter model or new HUD texture is approved or installed.

## Current visual status

- `day_cycle.aseprite` and `day_cycle.png` are an unapproved24x24 native Aseprite candidate. `day_cycle-c0.png` / `c1.png` / `c2.png` are12x review enlargements, never game textures. C0 had a clipped/ragged right outline; C1 removed out-of-canvas pixels. The user rejected C1 and requested larger, more expressive sun/moon symbols. C2 enlarged both; the user then chose to draw the asset or delegate it to local Gemini rather than accepting it. Neither candidate can be treated as production-approved.
- `day_cycle-c1.aseprite` preserves the earlier native source. The latest native source separates the dial and glyph layer. The glyph-layer name originated as a preview guide; its visible pixels are actual editable symbols, not an approved pointer. Runtime draws its moving pointer independently.
- `GEMINI_HUD_PROMPT.md` is the exact24x24 consumer brief and attachment list. It is prepared, not submitted. The installed local Google Gemini was located at `C:/Users/user/AppData/Local/Google/Gemini/app-1.16.0`. Computer Use successfully listed Windows windows, but launching both GeminiAppLauncher.exe and Gemini.exe exposed no targetable Gemini window. No authentication, security setting or permission was changed.
- `GEMINI_SMALL_HOME_PROMPT.md` and `GEMINI_LARGE_HOME_PROMPT.md` are the user-requested handoff prompts for real editable models. They explicitly separate genuine `.bbmodel` delivery from a generated concept picture.
- `oak_planks.png`, `spruce_planks.png`, `stripped_oak_log.png`, `stripped_oak_log_top.png` are unchanged Minecraft1.20.1 reference/editor materials, not newly authored JEM production textures. `native-material-provenance.json` records their exact native paths and hashes. They are not copied into game resources; eventual baked models may reference the vanilla resources directly.

## Actual consumer contracts

HUD: `assets/jemcompat/textures/gui/time/day_cycle.png` and `season_cycle.png`, each24x24 RGBA; default top-left anchor8,8;4px gap. Day dial has upper daylight and lower night. Seasonal wedges have Spring centred top, Summer right, Autumn bottom, Winter left; boundaries diagonal. No baked pointer, names or clock text. Runtime owns native synchronized state and pointer placement. The dimension-disabled season presentation tints the same wheel gray. The source references these exact resources, which remain absent until approved art is supplied; do not install that incomplete resource set or hide the gap with placeholders.

Shelters: full art-master bounds32x32x32 and48x48x48 model units, +Z open front, +Y up, origin bottom rear-left. Static game models must be split into eight or twenty-seven16-unit cells with matching four facing rotations and collision geometry. `ShelterGeometry.java` currently contains the source geometry contract and precomputed per-cell shapes. It must be reconciled with the user's returned genuine models before baked-model integration. House size never gates pet eligibility.

Shelter GUI reuses the native Minecraft `textures/gui/demo_background.png`248x166 panel region and native widgets; dynamic localized rows and actions remain code. There is no fabricated custom panel PNG. Runtime GUI scaling/interaction remains unobserved because no gameplay launch is authorized.

## Reference and review receipt

Active exact Pair: `Minecraft/GUI/Forge-1.20.1-jem-gui-and-icon-asset-production`, both axes PROVISIONAL. Model geometry additionally follows `Models/Blockbench-models/Minecraft-cuboid-entity-models`; reused native textures preserve original pixels rather than generating/painting replacement materials. Shared Minecraft art contract and `visual-auditor` were read.

Inspected original positive references in the existing project/Taste Engine collection: GUI `Снимок экрана 2026-10-16 181525.png` (compact semantic silhouettes), Icons `Снимок экрана 2026-10-20 215003.png` (connected mass/value contrast); negative GUI `Снимок экрана 2026-10-20 174338.png` (flat muddy field and cramped presentation). Model `REF07_chained_red_chest.png` was inspected only for fitted supports and clear mass separation; its fantasy palette/chain motifs are not shelter direction. Existing original bytes and classifications remain in `../reference-library/minecraft-2026-10-22`.

Independent web analogues were searched before authoring: [native clock example](https://ijaminecraft.com/en/tutorial/set-time-using-commands-in-minecraft) and [Minecraft dog-house build](https://www.planetminecraft.com/project/dog-house--/). These are proposed technical/composition references, not classified personal taste and not production pixel sources.

Visual-auditor result: native tool/export dimensions and C1 no-clipping observation are established; user approval FAIL/OPEN, full composed HUD/season wheel/light-dark contour matrix UNOBSERVED, runtime UNOBSERVED. No AGENT_PASS or user acceptance is claimed. The two automatic correction rounds are retained in C1/C2; subsequent artwork is user/Gemini-owned under the user's later direction.

Local-only small-shelter geometry/editor study remains outside the repository under the Taste Engine model workspace. It is not a delivered/approved shelter model and is not a substitute for the user's requested Gemini files. No large study or generated placeholder was integrated.
