# Sophisticated Storage Silver Layered GUI — First Candidate (`C2`)

## Status & Provenance

- **Art Status**: `CANDIDATE` (`C2` after `visual-auditor` cold-read passes `C0` -> `C1` -> `C2`)
- **Runtime Verification Status**: `UNOBSERVED` (offline image, alpha-mask, and bytecode UV verification complete, no runtime profile deployment or Minecraft launch performed)
- **Installed Consumer Jars**:
  - `sophisticatedstorage-1.20.1-1.5.9.2197.jar`
  - `sophisticatedcore-1.20.1-1.5.15.2427.jar`
- **Shared Namespace Scope**:
  - `assets/sophisticatedcore/textures/gui/*` is loaded by [`StorageGuiHelper`](file:///C:/Users/user/.gemini/antigravity/brain/069aca9c-07d8-4d9f-9ab6-92e58bac4b58/scratch/unzipped/consumer-bytecode.txt#L1-L82), [`StorageScreenBase`](file:///C:/Users/user/.gemini/antigravity/brain/069aca9c-07d8-4d9f-9ab6-92e58bac4b58/scratch/unzipped/consumer-bytecode.txt#L83-L329), and [`GuiHelper`](file:///C:/Users/user/.gemini/antigravity/brain/069aca9c-07d8-4d9f-9ab6-92e58bac4b58/scratch/unzipped/consumer-bytecode.txt#L330-L433)
  - Overriding `assets/sophisticatedcore/textures/gui/storage_background_9.png` and `assets/sophisticatedcore/textures/gui/slots_background.png` also styles Sophisticated Backpacks and any other Sophisticated Core storage screen that references `StorageBackgroundProperties.REGULAR_9_SLOT` or `GuiHelper.SLOTS_BACKGROUND`

## Workspace Layout

- [pack/pack.mcmeta](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/sophisticated-storage-silver-gui-2026-10-24/pack/pack.mcmeta) — clean Minecraft 1.20.1 resource-pack metadata (`"pack_format": 15`)
- [pack/assets/sophisticatedcore/textures/gui/storage_background_9.png](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/sophisticated-storage-silver-gui-2026-10-24/pack/assets/sophisticatedcore/textures/gui/storage_background_9.png) — native `256x256` RGBA 9-column storage background texture (`SHA-256: e670b159230f83c593fcdb752f020ee76141f1c2d02c75b6cf36dad52678a8c8`)
- [pack/assets/sophisticatedcore/textures/gui/slots_background.png](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/sophisticated-storage-silver-gui-2026-10-24/pack/assets/sophisticatedcore/textures/gui/slots_background.png) — native `256x256` RGBA `12x12` modular slot grid texture (`SHA-256: 7d80d8af57a92e3770aa07b67b275a336cc59cf301e12cb53ea1e2a4cc2782bb`)
- [sources/storage_background_9.aseprite](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/sophisticated-storage-silver-gui-2026-10-24/sources/storage_background_9.aseprite) — layered native Aseprite source (6 layers)
- [sources/slots_background.aseprite](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/sophisticated-storage-silver-gui-2026-10-24/sources/slots_background.aseprite) — layered native Aseprite source (4 layers)
- [geometry.json](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/sophisticated-storage-silver-gui-2026-10-24/geometry.json) — exact UV coordinates, embedded slot rectangles, SHA-256 receipts, and contrast metrics
- [previews/review-board-3x.png](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/sophisticated-storage-silver-gui-2026-10-24/previews/review-board-3x.png) — side-by-side comparison of composed 3-row and 6-row screens with 6x plane-transition detail callout
- [previews/composed-3row-4x.png](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/sophisticated-storage-silver-gui-2026-10-24/previews/composed-3row-4x.png) — 4x nearest-neighbor composed 3-row (`9x3`) storage screen simulation
- [previews/composed-6row-4x.png](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/sophisticated-storage-silver-gui-2026-10-24/previews/composed-6row-4x.png) — 4x nearest-neighbor composed 6-row (`9x6`) storage screen simulation
- [previews/plane-transitions-detail-8x.png](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/sophisticated-storage-silver-gui-2026-10-24/previews/plane-transitions-detail-8x.png) — 8x architectural detail of the Plane 1 -> Plane 2 -> Plane 3 step transitions
- [previews/slots_background-3x3-8x.png](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/sophisticated-storage-silver-gui-2026-10-24/previews/slots_background-3x3-8x.png) — 8x zoom of a `3x3` section of `slots_background.png`

## Bytecode UV Contract

1. **`storage_background_9.png` (`256x256` RGBA, active panel `x = 0..175, y = 0..255`)**
   - [`StorageGuiHelper.renderStorageBackground`](file:///C:/Users/user/.gemini/antigravity/brain/069aca9c-07d8-4d9f-9ab6-92e58bac4b58/scratch/unzipped/consumer-bytecode.txt#L28-L77) computes `halfSlotHeight = min(slotsHeight / 2, 150)`
   - Top slice blits from UV `(0, 0)` with size `(176, 17 + halfSlotHeight)` at `(x, y)`
   - Bottom slice blits from UV `(0, 159 - halfSlotHeight)` with size `(176, 97 + halfSlotHeight)` at `(x, y + 17 + halfSlotHeight)`
   - Rows `y = 17..158` are 100% vertically uniform so any storage row count joins without a horizontal seam
   - Bottom 97px (`y = 159..255`) always lands at screen `y = 17 + slotsHeight` immediately below the dynamic storage slot grid
   - Storage title text renders at `(8, 6)` in `0x404040`, `SearchBox` occupies `x = 7..119, y = 5..14`, and `SortButtons` occupy `x = 121..168, y = 4..15`
   - `"Inventory"` title renders at `(8, imageHeight - 94)` = atlas `(8, 162)` (`y = 162..170`) in `0x404040`, and transfer buttons sit at `x = 137..160, y = 160..171`
   - Player inventory `3x9` embedded slots sit at `x = 7 + 18*c` (`c = 0..8`), `y = 173 + 18*r` (`r = 0..2`)
   - Hotbar `1x9` embedded slots sit at `x = 7 + 18*c` (`c = 0..8`), `y = 231`

2. **`slots_background.png` (`256x256` RGBA, active grid `x = 0..215, y = 0..215`)**
   - [`GuiHelper.renderSlotsBackground`](file:///C:/Users/user/.gemini/antigravity/brain/069aca9c-07d8-4d9f-9ab6-92e58bac4b58/scratch/unzipped/consumer-bytecode.txt#L384-L429) tiles up to `12x12` cells (`18x18` per slot, `16x16` interior at `+1, +1`) over the storage panel at `(guiLeft + 7, guiTop + 17)`
   - Pixels outside `0..215, 0..215` remain untouched (`#00000000`)

## Three-Plane Silver Architectural Hierarchy & Layer Structure

### `storage_background_9.aseprite`
- `00_original_structural_guide` (hidden reference layer)
- `01_outer_chassis_and_plane1_shell` — Plane 1 raised pale brushed-silver upper shell (`y = 0..160`), outer `1px` dark slate silhouette (`#2D323B`), top-left specular bevels (`#F7FAFF` / `#E9EEF6`), clean header plate (`#DCE2EC` -> `#E2E8F2`, `161.33` RGB contrast delta vs `#404040`), and seamless vertical stretch rails (`y = 17..158`)
- `02_plane2_recessed_inventory_well` — Plane 2 recessed cool-steel player inventory basin (`y = 161..227`) with `2px` stepped side cheeks (`x = 1..2, 173..174`), overhanging Plane 1 lip shadow (`y = 160..161`), and readable `"Inventory"` label band (`y = 162..170`, `91.33` RGB contrast delta vs `#404040`)
- `03_plane3_raised_hotbar_apron` — `1px` structural shadow trench at `y = 228` (`#2B303A`) followed by Plane 3 raised silver hotbar apron (`y = 229..255`) with a crisp top specular rim (`#F6F9FF` at `y = 229`) and bottom bevel (`#9EA7B8` at `y = 254`)
- `04_embedded_slot_frames` — `3x9` recessed gunmetal-slate slot frames in Plane 2 (`y = 173..226`) and `1x9` raised-apron slot frames in Plane 3 (`y = 231..248`) with smooth `#C8D0DC` bottom-left miters
- `05_embedded_slot_interiors` — 3D architectural slot pockets featuring a `2px` top overhang shadow (`ty = 1..2`), vertical inner side walls (`tx = 1, 16`), calm charcoal-slate pocket floors, and a stepped lower inner shelf (`ty = 14..16`)

### `slots_background.aseprite`
- `00_original_structural_guide` (hidden reference layer)
- `01_slot_bevel_grid` — `12x12` tiled `18x18` slot frames (`#363C48` top/left recessed rim, `#EDEFF5` / `#E4E9F2` bottom/right catch rim)
- `02_slot_pocket_fill` — calm charcoal-slate `16x16` pocket floor (`#454D5C` / `#495262`) with vertical side wall shading (`tx = 1, 16`)
- `03_slot_inner_lighting` — `2px` upper overhang shadow (`#2E3440`, `#3A414F`) and stepped lower inner shelf (`#50596A`, `#586274`)

## Visual-Auditor Progression (`C0` -> `C1` -> `C2`)

- **`C0` Audit**: Identified flat diagonal slot gradients, duplicate bright white lines at `y = 159`, `y = 249`, and `x = 169` adjacent to slot `tx = 17` / `ty = 17` catch edges, and an understated `1px` side step on Plane 2
- **`C1` Refinement & Audit**: Replaced diagonal slot ramps with 3D architectural pockets (top overhang cast shadow, side walls, calm center floor, and lower inner shelf), eliminated duplicate bright lines around slot wells, and widened Plane 2's recessed side cheeks to `2px` (`x = 1..2` and `x = 173..174`), then identified three remaining micro-defects in `C1`:
  1. Plane 2 slot catch-lights (`#F0F4FA`) competed with Plane 1 storage slots
  2. Header inner box (`y = 4..14`) crowded the `"Chest"` title and `SortButtons` (`y = 4..15`)
  3. Dark bottom-left slot miter pixels (`tx = 0, ty = 17`) created 9 tiny dark teeth along the bottom edge of the hotbar (`y = 248`)
- **`C2` Final Candidate**: Uncluttered the Plane 1 header into a calm brushed-silver plate (`y = 3..14`), subordinated Plane 2's embedded `3x9` inventory grid with deeper gunmetal-slate pockets (`#313846` / `#353D4C`) and muted steel catch-lights (`#566072` / `#667186`) while keeping the `"Inventory"` label band (`y = 162..170`) crisp for `#404040` text, and smoothed the hotbar bottom-left miters (`#C8D0DC`) so `y = 248` reads as a continuous silver shelf
