# JEM resource-pack style selection

Status: concept selection only. These generated sheets are references for choosing a direction; they are not Minecraft-ready textures and must not be cropped, resized or shipped as GUI pixels.

## How to choose

Reply with the category, board and tile number, for example:

- `Menu B6`
- `Player C5`
- `Blocks A4`

You may also combine parts, for example: `Menu C3 title + Menu A5 background + Player C2 controls + Blocks A4 materials`.

## Sheets

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

## C0 visual audit

- PASS: every sheet contains a readable 3x3 grid with nine numbered alternatives.
- PASS: each category preserves a recognizable Minecraft UI purpose and visibly distinct style families.
- PASS: the player and block sheets use semantic pictograms and spatial grouping instead of explanatory paragraphs.
- PASS: large masses, slot recesses and focal accents remain separable across the concepts.
- LIMIT: generated slot counts, atlas coordinates, control states and pixel scale are illustrative only and are not runtime evidence.
- LIMIT: biome-heavy HUD options intentionally explore an upper-decoration bound; several are likely too active for permanent play and should be treated as contrast options.
- LIMIT: after the user selects a direction, operational GUI, button and icon pixels must be rebuilt at exact consumer size in layered Aseprite from the inspected vanilla/installed originals. The main illustrated background may follow the separate background-generation route.

No resource pack, sandbox profile, managed client generation or server runtime was changed by this concept round.
