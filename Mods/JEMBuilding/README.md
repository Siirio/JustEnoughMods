# JEM Building

Standalone Minecraft Forge 1.20.1 survival construction mod

## Blueprint Roll sandbox feature

The Blueprint Roll is a separate tool from the protected Builder Hammer. Left-click chooses the first corner, right-click chooses the second corner, and the native choice screen captures the inclusive volume as Copy or Move. The resulting named paper stores only the blueprint identity and display metadata while compact palette and progress data remain server-owned in overworld saved data

Copy keeps the source intact. Creative places the exact saved states through bounded work. Survival creates non-colliding placeholders only in free cells, keeps obstructed cells pending, consumes matching block items, and preserves coordinated double-block and bed placement. Move accepts only ordinary movable blocks, rejects block entities and protected or hazardous state, removes the source in bounded work, and provides a resumable placement preview with translation and 90-degree rotation

The requested rolled-paper production model and texture remain intentionally absent because the referenced visual was not included with the supplied text brief. Both items use the vanilla paper model until the original reference is available, avoiding a missing-texture item without guessing the final art. This sandbox branch is not synchronized to `test jem`, MineRENT, stable profiles, or the release baseline

## Player flow

1. Craft the single JEM Builder Hammer
2. Left-click an existing block to open the circular wall, floor, column, sphere and cylinder menu
3. Choose a mode, aim at a block or through air, and right-click to confirm the live preview
4. Hold a placeable block and right-click any hollow placeholder to replace that clicked cell immediately, then fill the remaining available cells from that point through bounded server work
5. Sneak-right-click a placeholder with the hammer to remove only unfinished placeholders

Every shape is limited to an inclusive 256-block span on X, Y, and Z. Walls follow the dominant horizontal look direction and can extend to either side, columns remain 1x1 with variable height, spheres are hollow, and cylinders contain only their circular side wall without top or bottom caps. Server work is incremental, does not force-load chunks and persists the region description independently of JEM Compat or other private JEM modules. Replaced, occupied or manually broken cells are persisted as settled and are never reconstructed as placeholders. Placeholder blocks have no collision, block entity, item drop or tick callback

The 3D hammer geometry and native-size texture are independent copies of the established BBB hammer assets in `Docs/art/bbb-hammer-2026-10-21`

When JEM Compat, Handcrafted and Building But Better are also installed, JEM Compat promotes this hammer to the only player-facing hammer. Handcrafted keeps its native `Hammerable.onHammer` action, BBB keeps its native `bbb:hammers` behavior, and JEM Building handles construction only when neither external interaction consumes the click. The two registered legacy items remain loadable for old data but are removed from new acquisition through the managed Item Obliterator policy
