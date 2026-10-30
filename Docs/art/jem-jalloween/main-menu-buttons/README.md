# JEM Jalloween main-menu buttons

Status: `CANDIDATE`, integrated for player testing on 2026-10-30

## Consumer contract

- FancyMenu title-screen button background
- Native texture size: 190 by 20 pixels
- Displayed size: 190 by 20 GUI pixels before GUI-scale transformation
- Geometry and alpha: preserved exactly from the existing JEM `sky_brush` controls
- Normal: `#6B3E9C`
- Hover: `#A166D1`
- Inactive: `#48325F`

## Sources and scope

The editable `.aseprite` files are direct native-size color revisions of the existing JEM button masks. The ordinary blue controls remain unchanged. Only `jalloween_title_screen.txt` selects these purple variants, so the normal title screen is restored when the seasonal layout is inactive

References inspected before the revision:

- `Taste-Engine/Pairs/Minecraft/GUI/Reference/For_JEM_Resource_Pack/Buttons and Layout 1.png`
- `Taste-Engine/Pairs/Minecraft/GUI/Reference/For_JEM_Resource_Pack/Buttons and Layout 2.png`
- `Taste-Engine/Pairs/Minecraft/GUI/Reference/For_JEM_Resource_Pack/Exact Wanted GUI Approach and Style 3.png`
- Existing JEM Jalloween background and title exports

Applied qualities: compact readable controls, strong normal/hover separation, and a violet family that belongs to the existing purple night background. Avoided changes: no altered hit bounds, no baked labels, no new controls, no change to the ordinary blue set

## C0 visual audit

- PASS: exact 190 by 20 consumer geometry and original alpha mask are preserved
- PASS: the normal, hover and inactive states remain visibly distinct at 1x and 4x
- PASS: the purple family separates from the dark violet background while staying subordinate to the orange title
- PASS: dynamic localized labels and native button behavior remain outside the textures
- UNOBSERVED: final runtime composition because the Minecraft client was not launched

`AGENT_PASS` for static integration with runtime appearance left for player testing
