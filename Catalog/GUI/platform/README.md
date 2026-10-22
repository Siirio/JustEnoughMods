# GUI/platform

[All GUI groups](../README.md) | [Gallery](../index.html)

Original reference assets. Control sheets retain their source dimensions and alpha. Resource namespace and containing mod are recorded separately; a path is not evidence of a live screen.

No separate GUI PNG was selected for this group. This does not prove the mod has no interface: it may use shared controls, procedural rendering, dynamic resources or an unclassified asset path.

## UI class candidates

Static names and declared superclasses only. Helpers/mixins can occur here. Shared vanilla controls, procedural drawing, item/entity renders and runtime state do not necessarily have their own PNG.

- `com/blackgear/platform/client/event/HudRenderEvent$ElementType` extends `java/lang/Enum`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/HudRenderEvent$ElementType.class`
- `com/blackgear/platform/client/event/HudRenderEvent$RenderContext` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/HudRenderEvent$RenderContext.class`
- `com/blackgear/platform/client/event/HudRenderEvent$RenderHud` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/HudRenderEvent$RenderHud.class`
- `com/blackgear/platform/client/event/HudRenderEvent` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/HudRenderEvent.class`
- `com/blackgear/platform/client/event/screen/HudInteractions$MouseClick` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudInteractions$MouseClick.class`
- `com/blackgear/platform/client/event/screen/HudInteractions$MouseRelease` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudInteractions$MouseRelease.class`
- `com/blackgear/platform/client/event/screen/HudInteractions$MouseScroll` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudInteractions$MouseScroll.class`
- `com/blackgear/platform/client/event/screen/HudInteractions$SlotClicked` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudInteractions$SlotClicked.class`
- `com/blackgear/platform/client/event/screen/HudInteractions$StopHovering` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudInteractions$StopHovering.class`
- `com/blackgear/platform/client/event/screen/HudInteractions` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudInteractions.class`
- `com/blackgear/platform/client/event/screen/HudRendering$CloseContainer` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudRendering$CloseContainer.class`
- `com/blackgear/platform/client/event/screen/HudRendering$ContainerRenderBackground` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudRendering$ContainerRenderBackground.class`
- `com/blackgear/platform/client/event/screen/HudRendering$ContainerRenderForeground` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudRendering$ContainerRenderForeground.class`
- `com/blackgear/platform/client/event/screen/HudRendering$OpenContainer` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudRendering$OpenContainer.class`
- `com/blackgear/platform/client/event/screen/HudRendering$Rendering` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudRendering$Rendering.class`
- `com/blackgear/platform/client/event/screen/HudRendering$ScreenPostInitialization` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudRendering$ScreenPostInitialization.class`
- `com/blackgear/platform/client/event/screen/HudRendering$ScreenPostRendering` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudRendering$ScreenPostRendering.class`
- `com/blackgear/platform/client/event/screen/HudRendering$ScreenPreInitialization` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudRendering$ScreenPreInitialization.class`
- `com/blackgear/platform/client/event/screen/HudRendering$ScreenPreRendering` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudRendering$ScreenPreRendering.class`
- `com/blackgear/platform/client/event/screen/HudRendering` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/HudRendering.class`
- `com/blackgear/platform/client/event/screen/TooltipEvents$Item` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/TooltipEvents$Item.class`
- `com/blackgear/platform/client/event/screen/TooltipEvents$RenderTooltip` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/TooltipEvents$RenderTooltip.class`
- `com/blackgear/platform/client/event/screen/TooltipEvents` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/TooltipEvents.class`
- `com/blackgear/platform/client/event/screen/api/ScreenAccess` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/api/ScreenAccess.class`
- `com/blackgear/platform/client/event/screen/api/ScreenAccessImpl` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/client/event/screen/api/ScreenAccessImpl.class`
- `com/blackgear/platform/core/mixin/access/ScreenAccessor` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/core/mixin/access/ScreenAccessor.class`
- `com/blackgear/platform/core/mixin/client/AbstractContainerScreenMixin` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/core/mixin/client/AbstractContainerScreenMixin.class`
- `com/blackgear/platform/forge/client/ForgeClientEvents` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/forge/client/ForgeClientEvents.class`
- `com/blackgear/platform/forge/client/ForgeInputEvents` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/Platform-forge-1.20.1-1.3.4.jar!/com/blackgear/platform/forge/client/ForgeInputEvents.class`
