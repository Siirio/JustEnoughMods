# Food Blessing icon candidate

Status: C1 USER APPROVED, installed locally through native AutoModpack generation 80

The user requested the golden chalice, purple grapes and raised roast drumstick shown in user-reference.png, authored in Aseprite and shown first. This is the selected composition reference, not an approved production export

The actual consumer is jemfood:textures/mob_effect/food_blessing.png. The current source is 18 by 18 pixels and the native inventory hover region is 18 by 18 GUI units, confirmed in BlessingPresentation.java and FoodTooltips.java. This candidate uses an 18 by 18 RGBA canvas, binary alpha and native pixel clusters. At GUI scales 2, 3 and 4 its intended displayed footprint is 36, 54 and 72 physical pixels

The icon uses a front-view golden cup with a small visible rim, a connected stem and foot, a roast drumstick inclined toward the upper right, a compact grape bunch inside the left half of the cup, with its bottom occluded by a separate foreground rim and restrained blessing sparkles. Gold carries the focal value, reddish browns separate roast meat, purple identifies grapes and green identifies leaves. Light comes from the upper left. The screenshot's frame and live progress indicator remain outside this icon's pixel scope

Active Pair: Minecraft/GUI/Forge-1.20.1-jem-gui-and-icon-asset-production. The micro-emoji Pair was inspected and excluded because it owns concepts for chat glyphs rather than native effect icons

Original references inspected:

- user-reference.png, selected by the current request for the specific composition
- The existing native 18 by 18 Food Blessing resource, inspected at native size and 12 times nearest-neighbour enlargement
- Taste Engine Icons/I_like/Снимок экрана 2026-10-20 215003.png, for connected silhouettes and material/value separation
- Icons/I_dont_Like has no classified image files, so no negative icon preference is invented

Pixels are painted directly in Aseprite through native draw_grid and pixel operations. No reference-image extraction, tracing, downsampling or generated production pixels are used. The layered source and PNG remain together. Enlarged renders are review evidence only

The initial preview left runtime resources unchanged. The subsequent explicit user approval authorized installation of the exact C1 PNG. See delivery.json for the completed local native application

Current editable source: food-blessing-c1.aseprite. Native export: food-blessing-c1.png. C0 remains preserved for comparison. The user rejected C0 because the grapes were outside the cup. C1 redraws the bunch inside the bowl width and places the front rim over the lower fruit pixels. C1 user approval is recorded below

The user explicitly approved C1 on 2026-10-25 with "yeah use that one and redeploy". The exact PNG is now the JEMFood source resource at Mods/JEMFood/src/main/resources/assets/jemfood/textures/mob_effect/food_blessing.png. Native delivery evidence belongs to delivery.json
