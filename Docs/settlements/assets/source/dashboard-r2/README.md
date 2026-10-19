# Settlements dashboard R2 materials

State: `SOURCE_EDITABLE`; `WAITING_FOR_REAL_GAME_CAPTURE`. Neither the source atlas nor the offline material composition is a Minecraft screenshot or user approval.

The current user direction selects parchment content, dark stone navigation, broad blue selected controls with narrow gold edges, wood header, cloth banners, lanterns, foliage and large meaningful live icons from the three supplied dashboard references. This set interprets the material construction in original native pixels. It does not copy/crop any reference imagery. Native recruit materials remain owned by `SettlementMaterials` and are unchanged.

## Editable source and export

`dashboard-r2.ora` is a 256×128 OpenRaster canvas with five independently editable RGBA layers: material planes, recess/edges, fixed corner craft, cloth/iron, leaves/light. Mood expressions are separate native16px regions within those layers. Transparent pixels have binary alpha; no antialiasing or soft glow. All native sources and the nearest4x review atlas are retained here.

Reproduce production export from the repository root:

```powershell
python Docs/settlements/assets/source/dashboard-r2/author_dashboard.py
```

The script authors the ORA, closes it, reopens its actual layer stack, composites bottom to top and exports the `regions.json` bounds into `Mods/JEMSettlements/src/main/resources/assets/jem_settlements/textures/gui/dashboard`. This export completed successfully with Pillow. It is a production art export, not a test, static analysis, gameplay or build. The older visual sandbox pipeline no longer exists in this checkout; the saved ORA compositing route is self-contained. Desktop-editor opening has not been claimed.

## Renderer contract

`SettlementDashboardArt` owns only the separate JEM dashboard family. Every24×24 surface has protected5px corner regions,14px repeatable middle spans and native pixel tiling; the whole frame is never scaled. Paper, wood, blue and stone are32px quiet tiles. Quiet centers carry low-contrast material clusters rather than random pixel noise. Each native GUI pixel is one authored pixel at ordinary Minecraft GUI scaling.

Banner16×32, lantern16×28 and vine40×32 have fixed source sizes and positions. The header should be at least40px high;56px at both ends are ornament bounds. Title/actions stay inside these bounds. Leaves extend16px above the header; the containing screen must reserve that top space. The lower sidebar vine occupies the final34px of the shell. No click bounds or labels are baked into art.

Control states: default charcoal, lighter hover, blue/gold selected, subdued disabled. Live font, focus behavior, names, prices, state, item/entity content and action availability belong to the real screen. `mood` picks sad below35, neutral below80, happy at80 or above; the mouth silhouette changes along with color. Native16px expressions may use integer scaling. Track uses a programmatic inset and five subdivisions so narrow8px tracks do not distort24px frame corners; the live value is clamped for presentation only.

## Provenance and review

Original artwork authored for this project. No external pixel assets were imported. The user-supplied three references are design direction, not production texture sources. Supplemental web search for close Minecraft lantern/spruce analogues returned the standard pixel lantern silhouette: https://www.pinterest.com/pin/minecraft-lanterns-a-complete-guide--899171881863006737/ . This is a proposed silhouette analogue, not a user taste classification or licensed asset source. The original supplied reference images were inspected directly. Their thick readable geometry, warm glass/dark cage, broad cloth folds, large leaf clusters and quiet parchment fields guide this original sprite set.

Inspected actual exported atlas at native bounds and nearest4x: corner shoulders, binary silhouette edges, separate banner/lantern/vine shapes, blue/gold control state and readable three-way face expressions. This establishes export/source review only. Populated live screen behavior and final composition still require authorized real game capture and user acceptance.

`materials-native1x.png` and `materials-nearest2x.png` are explicitly labelled offline material compositions, authored by `compose_materials.py <Minecraft client.jar>`. The script reuses the existing art-export native-font reader and draws example Russian labels solely for density review. Those values are art specimens, not a snapshot of server data, and the composition does not reproduce the screen implementation. The final screen supplies actual native item/entity icons and live contents. Font glyphs are read from the local client solely for these labelled review images and are not redistributed as a font atlas.
