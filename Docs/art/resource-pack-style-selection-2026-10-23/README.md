# JEM resource-pack style selection

Status: concept selection only. These generated sheets are references for choosing a direction; they are not Minecraft-ready textures and must not be cropped, resized or shipped as GUI pixels.

## How to choose

Reply with the category, board and tile number, for example:

- `Menu B6`
- `Player C5`
- `Blocks A4`

You may also combine parts, for example: `Menu C3 title + Menu A5 background + Player C2 controls + Blocks A4 materials`.

## R2 corrected sheets - use these for selection

R2 holds the represented content, camera and palette constant and varies actual
texture construction, panel topology, icon language, depth and state treatment.

### 1. Fancy Menu and main title

- `r2-01-menu-a-texture-grammars.png` - nine pixel-cluster and surface-rendering grammars on one fixed scene.
- `r2-01-menu-b-title-construction.png` - nine physically different title constructions.
- `r2-01-menu-c-menu-models.png` - nine diegetic menu models with icon-first controls.

### 2. Buttons and player interface

- `r2-02-player-a-slot-construction.png` - nine slot, edge and surface constructions.
- `r2-02-player-b-icon-languages.png` - nine icon drawing languages on a fixed HUD.
- `r2-02-player-c-hud-systems.png` - nine distinct HUD architectures carrying the same information.

### 3. Block interfaces

- `r2-03-blocks-a-material-recipes.png` - nine material and edge recipes on fixed crafting/furnace/chest geometry.
- `r2-03-blocks-b-object-models.png` - nine interface-as-object approaches.
- `r2-03-blocks-c-depth-and-states.png` - nine depth, module and interaction-state systems.

## R1 superseded sheets

The original sheets below are retained as rejection history. They varied biomes,
dimensions, seasonal subjects and palettes too heavily. The user correctly noted
that this did not provide enough real texture or construction experimentation.
Do not use R1 as the primary style-selection set.

### 1. Fancy Menu and main title

- `01-menu-a-grounded-worlds.png` - grounded overworld materials and locations.
- `01-menu-b-dimensions-moods.png` - dimensions, dramatic biomes and lighting moods.
- `01-menu-c-title-systems.png` - title construction and menu-layout systems.

### 2. Buttons and player interface

- `02-player-a-materials.png` - daily-use material families.
- `02-player-b-biomes.png` - biome and event accents.
- `02-player-c-interaction-shapes.png` - button shapes, icon placement and interaction language.

### 3. Block interfaces

- `03-blocks-a-materials.png` - classic workshop material families.
- `03-blocks-b-environments.png` - environmental and biome stories.
- `03-blocks-c-layout-systems.png` - structural layout and input/output language.

## R2 C0 visual audit

- PASS: every sheet contains a readable 3x3 grid with nine numbered alternatives.
- PASS: each R2 category holds its comparison subject stable while varying the intended construction axis.
- PASS: the player and block sheets use semantic pictograms and spatial grouping instead of explanatory paragraphs.
- PASS: large masses, slot recesses and focal accents remain separable across the concepts.
- PASS: R2 removes biome, dimension and seasonal swaps as nominal style options.
- LIMIT: generated slot counts, atlas coordinates, control states and pixel scale are illustrative only and are not runtime evidence.
- LIMIT: some generator results still simplify or regularize material marks; selection identifies a direction, not finished pixel craft.
- LIMIT: after the user selects a direction, operational GUI, button and icon pixels must be rebuilt at exact consumer size in layered Aseprite from the inspected vanilla/installed originals. The main illustrated background may follow the separate background-generation route.

No resource pack, sandbox profile, managed client generation or server runtime was changed by this concept round.
