# JEM Building

Standalone Minecraft Forge 1.20.1 survival construction mod

## Player flow

1. Craft the single JEM Builder Hammer
2. Left-click an existing block to open the circular wall, floor, column, sphere and cylinder menu
3. Choose a mode, aim at a block or through air, and right-click to confirm the live preview
4. Hold a placeable block and right-click any hollow placeholder to replace that clicked cell immediately, then fill the remaining available cells from that point through bounded server work
5. Sneak-right-click a placeholder with the hammer to remove only unfinished placeholders

Every shape is limited to an inclusive 256-block span on X, Y, and Z. Walls follow the dominant horizontal look direction and can extend to either side, columns remain 1x1 with variable height, spheres are hollow, and cylinders contain only their circular side wall without top or bottom caps. Server work is incremental, does not force-load chunks and persists the region description independently of JEM Compat or other private JEM modules. Replaced, occupied or manually broken cells are persisted as settled and are never reconstructed as placeholders. Placeholder blocks have no collision, block entity, item drop or tick callback

The 3D hammer geometry and native-size texture are independent copies of the established BBB hammer assets in `Docs/art/bbb-hammer-2026-10-21`

When JEM Compat, Handcrafted and Building But Better are also installed, JEM Compat promotes this hammer to the only player-facing hammer. Handcrafted keeps its native `Hammerable.onHammer` action, BBB keeps its native `bbb:hammers` behavior, and JEM Building handles construction only when neither external interaction consumes the click. The two registered legacy items remain loadable for old data but are removed from new acquisition through the managed Item Obliterator policy
