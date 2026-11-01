# JEM Building

Standalone Minecraft Forge 1.20.1 survival construction mod

## Player flow

1. Craft the single JEM Builder Hammer
2. Right-click a block face to anchor the first empty construction cell
3. Aim through air to preview an XY, ZY or XZ grid and right-click air to confirm
4. Hold a placeable block and right-click any hollow placeholder to fill available cells from that point
5. Sneak-right-click a placeholder with the hammer to remove only unfinished placeholders

The hammer has no size tier or artificial area cap. Server work is incremental, does not force-load chunks and persists the region description independently of JEM Compat or other private JEM modules. Placeholder blocks have no collision, block entity, item drop or tick callback

The 3D hammer geometry and native-size texture are independent copies of the established BBB hammer assets in `Docs/art/bbb-hammer-2026-10-21`
