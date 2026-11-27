# GUI/offgrid

[All GUI groups](../README.md) | [Gallery](../index.html)

Original reference assets. Control sheets retain their source dimensions and alpha. Resource namespace and containing mod are recorded separately; a path is not evidence of a live screen.

No separate GUI PNG was selected for this group. This does not prove the mod has no interface: it may use shared controls, procedural rendering, dynamic resources or an unclassified asset path.

## UI class candidates

Static names and declared superclasses only. Helpers/mixins can occur here. Shared vanilla controls, procedural drawing, item/entity renders and runtime state do not necessarily have their own PNG.

- `dev/tazer/offgrid/client/AssembledOverlayRender` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/client/AssembledOverlayRender.class`
- `dev/tazer/offgrid/client/OffgridClient` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/client/OffgridClient.class`
- `dev/tazer/offgrid/client/OverlayBlockEntities` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/client/OverlayBlockEntities.class`
- `dev/tazer/offgrid/client/OverlayBreaking` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/client/OverlayBreaking.class`
- `dev/tazer/offgrid/client/OverlayCracks$Crack` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/client/OverlayCracks$Crack.class`
- `dev/tazer/offgrid/client/OverlayCracks$Destruction` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/client/OverlayCracks$Destruction.class`
- `dev/tazer/offgrid/client/OverlayCracks` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/client/OverlayCracks.class`
- `dev/tazer/offgrid/client/OverlayEffects$1` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/client/OverlayEffects$1.class`
- `dev/tazer/offgrid/client/OverlayEffects` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/client/OverlayEffects.class`
- `dev/tazer/offgrid/client/OverlayGeometry$Entry` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/client/OverlayGeometry$Entry.class`
- `dev/tazer/offgrid/client/OverlayGeometry` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/client/OverlayGeometry.class`
- `dev/tazer/offgrid/client/OverlayOutline` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/client/OverlayOutline.class`
- `dev/tazer/offgrid/client/OverlayShaderTags` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/client/OverlayShaderTags.class`
- `dev/tazer/offgrid/compat/create/CarriedOverlayHit` extends `net/minecraft/world/phys/BlockHitResult`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/compat/create/CarriedOverlayHit.class`
- `dev/tazer/offgrid/compat/create/CarriedOverlays` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/compat/create/CarriedOverlays.class`
- `dev/tazer/offgrid/compat/create/OverlayPrinter` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/compat/create/OverlayPrinter.class`
- `dev/tazer/offgrid/compat/create/PrintedOverlay` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/compat/create/PrintedOverlay.class`
- `dev/tazer/offgrid/compat/light/OverlayLight` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/compat/light/OverlayLight.class`
- `dev/tazer/offgrid/forge/client/ForgeOverlayGeometry` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/forge/client/ForgeOverlayGeometry.class`
- `dev/tazer/offgrid/forge/compat/oculus/OculusMeshAppender` extends `dev/tazer/offgrid/client/OverlayShaderTags`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/forge/compat/oculus/OculusMeshAppender.class`
- `dev/tazer/offgrid/forge/compat/oculus/OculusOverlayTags` extends `dev/tazer/offgrid/client/OverlayShaderTags`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/forge/compat/oculus/OculusOverlayTags.class`
- `dev/tazer/offgrid/mixin/client/DebugInfoMixin` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/mixin/client/DebugInfoMixin.class`
- `dev/tazer/offgrid/network/BreakOverlay$Phase` extends `java/lang/Enum`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/network/BreakOverlay$Phase.class`
- `dev/tazer/offgrid/network/BreakOverlay` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/network/BreakOverlay.class`
- `dev/tazer/offgrid/network/OpenOverlaySign` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/network/OpenOverlaySign.class`
- `dev/tazer/offgrid/network/OverlayData` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/network/OverlayData.class`
- `dev/tazer/offgrid/network/OverlayDestruction` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/network/OverlayDestruction.class`
- `dev/tazer/offgrid/network/OverlayEvent` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/network/OverlayEvent.class`
- `dev/tazer/offgrid/network/PickOverlay` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/network/PickOverlay.class`
- `dev/tazer/offgrid/network/UseCarriedOverlay` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/network/UseCarriedOverlay.class`
- `dev/tazer/offgrid/network/UseOverlay` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/network/UseOverlay.class`
- `dev/tazer/offgrid/world/AssembledCell$AssembledOverlay` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/world/AssembledCell$AssembledOverlay.class`
- `dev/tazer/offgrid/world/Overlay` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/world/Overlay.class`
- `dev/tazer/offgrid/world/OverlayBlockEntity` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/world/OverlayBlockEntity.class`
- `dev/tazer/offgrid/world/OverlayContext$Frame` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/world/OverlayContext$Frame.class`
- `dev/tazer/offgrid/world/OverlayContext` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/world/OverlayContext.class`
- `dev/tazer/offgrid/world/OverlayFluids` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/world/OverlayFluids.class`
- `dev/tazer/offgrid/world/OverlayFocus$Focus` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/world/OverlayFocus$Focus.class`
- `dev/tazer/offgrid/world/OverlayFocus` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/world/OverlayFocus.class`
- `dev/tazer/offgrid/world/OverlayHitResult` extends `net/minecraft/world/phys/BlockHitResult`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/world/OverlayHitResult.class`
- `dev/tazer/offgrid/world/OverlayShapes` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/world/OverlayShapes.class`
- `dev/tazer/offgrid/world/OverlayTicker$Job` extends `java/lang/Record`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/world/OverlayTicker$Job.class`
- `dev/tazer/offgrid/world/OverlayTicker` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/world/OverlayTicker.class`
- `dev/tazer/offgrid/world/Overlays` extends `java/lang/Object`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/world/Overlays.class`
- `dev/tazer/offgrid/world/Placement$OverlayPlaceContext` extends `net/minecraft/world/item/context/BlockPlaceContext`; `$PROFILE/mods/offgrid-forge-1.0.7+1.20.1.jar!/dev/tazer/offgrid/world/Placement$OverlayPlaceContext.class`
