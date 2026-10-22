# Enchanting GUI: native-grid candidate

User request: remake the complete enchanting interface with every slot exactly
matching the vanilla PNG in Atlas; the two upper slots must match inventory size.

This is a native painted candidate, not a resized generated concept. Aseprite
retains the original vanilla layer and a separate material-painting layer.
The enlarged generated image was not traced, cropped, imported or downsampled.
The native treatment is more restrained than that illustration; it is not a claim
that the illustration's full decorative finish was recreated at native size.

## Source and layout

Atlas original: `minecraft:textures/gui/container/enchanting_table.png`, profile
vanilla archive, 256x256. Its exact source/export hashes and all slot coordinates
are in [geometry.json](geometry.json). The main panel remains 176x166. Reference
original inspected: the designated `For_JEM_Resource_Pack/Exact Wanted GUI
Approach and Style 1.png`: wood counter, dark recesses and activity-specific props.
The material study retains restrained walnut, brass, lapis-blue cloth and an
obsidian cradle for the existing live book renderer.

- Every frame is 18x18, with a 16x16 item interior.
- Item/lapis frame origins: (14,46), (34,46).
- Inventory frame origins: (7+18*c,83+18*r), c=0..8, r=0..2.
- Hotbar frame origins: (7+18*c,141), c=0..8.
- No slot moved, stretched or repainted from a different-size template.
- Slot pixels receive one position-preserving palette change; native lapis mark
  and frame topology remain intact. Authored margin painting excludes all slots.
- Pixels outside x=0..175,y=0..165 and the entire original alpha mask are unchanged.
  Lower atlas state sprites remain original, including their sampling coordinates.

## Files and production boundary

- [Native PNG](enchanting_table.png): direct Aseprite export, 256x256.
- [Editable source](enchanting_table.aseprite): original and painting layers.
- [4x preview](preview-4x.png): main panel only, integer nearest-neighbour.
- [Light native-size view](native-light.png), [dark native-size view](native-dark.png).
- [Geometry receipt](geometry.json): image-coordinate inspection, not gameplay QA.

MCP's large draw_pixels call failed with Windows ENAMETOOLONG before painting.
The same installed Aseprite operation was then run with its JSON read from a
request file instead of a command-line argument; no installed server was modified.
Painting and PNG export both occurred in Aseprite.

## Visual-auditor C0

Observed: slot size/position preservation, separate hotbar, native alpha and source
sprite preservation; readable native-scale recesses; connected cloth/book cradle
and restrained materials. Verified by direct per-coordinate comparison: all 38
slot regions match the original's geometry under the declared palette mapping,
zero mismatches. The original source itself determines these coordinates.

Artwork remains CANDIDATE, without user visual approval. The surface is simpler
than the generated reference, and the source layer's vanilla option treatment
is retained. This is not a finished-art or runtime acceptance verdict. Live book,
text, item placement, resource precedence and other mods' sampled UVs have not
been inspected in game. No game launch or installation performed. The managed
pack, ordinary local Server, stable profile and release baseline are untouched.
