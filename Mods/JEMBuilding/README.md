# JEM Building

Standalone Minecraft Forge 1.20.1 survival construction mod

## Player flow

1. Craft the single JEM Builder Hammer
2. Left-click an existing block to open the circular wall, floor, column, and sphere menu
3. Choose a mode, aim at a block or through air, and right-click to confirm the live preview
4. Hold a placeable block and right-click any hollow placeholder to fill available cells from that point
5. Sneak-right-click a placeholder with the hammer to remove only unfinished placeholders

Every shape is limited to an inclusive 256-block span on X, Y, and Z. Walls follow the dominant horizontal look direction and can extend to either side, columns remain 1x1 with variable height, and spheres are hollow. Server work is incremental, does not force-load chunks and persists the region description independently of JEM Compat or other private JEM modules. Placeholder blocks have no collision, block entity, item drop or tick callback

The 3D hammer geometry and native-size texture are independent copies of the established BBB hammer assets in `Docs/art/bbb-hammer-2026-10-21`
