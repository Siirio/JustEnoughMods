# Augment Bay / native backpack workbench art

Status: editable layered OpenRaster sources and PNG exports created; native-size
and nearest-neighbour contact sheet visually inspected. Game/editor application
and user visual acceptance are not claimed. Original screenshots3/4 are absent.

Exact Taste pairs read in full:
- `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Forge-1.20.1-jem-smp-screens/{TASTE,TECHNICAL}.md`
- `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/Textures (Resource packs)/Minecraft-item-generation/{TASTE,TECHNICAL}.md`

## Visual direction and references

User requires crisp Minecraft pixels, native readability and Backpacked-compatible
frames/symbols, without blur, noisy gradients or artificial smooth shading.
Screenshots3/4 were not supplied, so no exact screenshot arrangement is claimed.

Reference search: https://www.curseforge.com/minecraft/mc-mods/backpacked/images
and its actual augment-system image
https://media.forgecdn.net/attachments/1474/817/screenshot_2-png.png . Web-selected
material is a proposal, not user-approved taste or licensed production pixels.

Actual installed reference pixels inspected directly from Backpacked3.0.9:
`assets/backpacked/textures/gui/backpack_assets.png` and
`assets/backpacked/textures/block/backpack_dock.png`.
Observed extraction: cream/brown1–3px hard frame, dark recessed16px slots, compact
12px augment icons and familiar brass/wood/fabric construction. The native API
renders its own buttons from that installed texture; no third-party PNG is copied.

Original subject: a four-legged oak installation bench with a three-by-three
green cutting mat held by brass hardware, a front drawer and a bright single
latch as focal point. Native12px augment icon repeats only that functional mat and
clamp silhouette. Lighting is a small deliberate upper-edge highlight, no noise.
GUI surfaces keep real slot bounds readable and contain no rasterized text.

## Source/export contract

`author_assets.py` is the editable exact-pixel painting/export source, with named
palette roles and hand-authored face/icon rows. It writes the6 standard layered
`.ora` documents here and RGBA PNG exports under
`Mods/JEMCompat/src/main/resources/assets/jemcompat/textures`.

- `augment_bay.ora` -> `gui/augment_bay.png`:176×220; frame, slots, clamp/hardware.
  Slot origins35,39 /80,39 /125,39. Player origins8,135, stride18; hotbarY193.
- `backpack_workbench.ora` -> `gui/backpack_workbench.png`:176×76; parchment,
  recessed3×3 ingredients, framed result, fabrication arrow. Ingredient origins
 30,16 /48,16 /66,16 with18px row stride; result124,34. Exact1× rendering.
- `workbench_augment.ora` -> `gui/workbench_augment.png`:12×12 RGBA icon;
  native FrameworkTexture12×12 region, transparent exterior.
- `augment_bay_{top,front,side}.ora` -> matching `block/*.png`:16×16 opaque
  original material faces, used by the actual slab/apron/leg model and block item.

Palette is named in the exporter (ink, edge, wood, grain, light, cream, paper,
rim, shade, recess, shadow, gold, shine, iron, steel, teal, moss).
OpenRaster sources contain named PNG layers, standard stack.xml and mergedimage.
No smoothing or concept-image crop is used. The exporter ran successfully and
`contact-sheet.png` was visually inspected; it is only a review aid, never shipped.
The expected legacy `Mods/JEMVisualSandbox/production/visual_pipeline.py` is absent,
so that exporter was not used or claimed to have succeeded. Pixel-editor opening
and real Minecraft GUI-scale/appearance review remain unverified.
