# GUI/moreculling

[All GUI groups](../README.md) | [Gallery](../index.html)

Original reference assets. Control sheets retain their source dimensions and alpha. Resource namespace and containing mod are recorded separately; a path is not evidence of a live screen.

No separate GUI PNG was selected for this group. This does not prove the mod has no interface: it may use shared controls, procedural rendering, dynamic resources or an unclassified asset path.

## UI class candidates

Static names and declared superclasses only. Helpers/mixins can occur here. Shared vanilla controls, procedural drawing, item/entity renders and runtime state do not necessarily have their own PNG.

- `ca/fxco/moreculling/MoreCulling` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/MoreCulling.class`
- `ca/fxco/moreculling/config/ModMenuConfig` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/config/ModMenuConfig.class`
- `ca/fxco/moreculling/config/cloth/AbstractDynamicEntry` extends `me/shedaniel/clothconfig2/gui/entries/TooltipListEntry`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/config/cloth/AbstractDynamicEntry.class`
- `ca/fxco/moreculling/config/cloth/DynamicBooleanListEntry` extends `ca/fxco/moreculling/config/cloth/AbstractDynamicEntry`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/config/cloth/DynamicBooleanListEntry.class`
- `ca/fxco/moreculling/config/cloth/DynamicEnumEntry` extends `ca/fxco/moreculling/config/cloth/AbstractDynamicEntry`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/config/cloth/DynamicEnumEntry.class`
- `ca/fxco/moreculling/config/cloth/DynamicFloatSliderEntry$Slider` extends `net/minecraft/client/gui/components/AbstractSliderButton`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/config/cloth/DynamicFloatSliderEntry$Slider.class`
- `ca/fxco/moreculling/config/cloth/DynamicFloatSliderEntry` extends `ca/fxco/moreculling/config/cloth/AbstractDynamicEntry`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/config/cloth/DynamicFloatSliderEntry.class`
- `ca/fxco/moreculling/config/cloth/DynamicIntSliderEntry$Slider` extends `net/minecraft/client/gui/components/AbstractSliderButton`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/config/cloth/DynamicIntSliderEntry$Slider.class`
- `ca/fxco/moreculling/config/cloth/DynamicIntSliderEntry` extends `ca/fxco/moreculling/config/cloth/AbstractDynamicEntry`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/config/cloth/DynamicIntSliderEntry.class`
- `ca/fxco/moreculling/config/cloth/MoreCullingClothConfigBuilder` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/config/cloth/MoreCullingClothConfigBuilder.class`
- `ca/fxco/moreculling/config/cloth/MoreCullingClothConfigScreen` extends `me/shedaniel/clothconfig2/gui/ClothConfigScreen`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/config/cloth/MoreCullingClothConfigScreen.class`
- `ca/fxco/moreculling/config/sodium/FloatSliderControl$Button` extends `me/jellysquid/mods/sodium/client/gui/options/control/ControlElement`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/config/sodium/FloatSliderControl$Button.class`
- `ca/fxco/moreculling/config/sodium/IntSliderControl$Button` extends `me/jellysquid/mods/sodium/client/gui/options/control/ControlElement`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/config/sodium/IntSliderControl$Button.class`
- `ca/fxco/moreculling/mixin/entities/ItemFrameRenderer_cullMixin` extends `net/minecraft/client/renderer/entity/EntityRenderer`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/mixin/entities/ItemFrameRenderer_cullMixin.class`
- `ca/fxco/moreculling/mixin/gui/SodiumOptionsGUIMixin` extends `net/minecraft/client/gui/screens/Screen`; `$PROFILE/automodpack/client/active/mods/moreculling-0.24.6-all.jar!/ca/fxco/moreculling/mixin/gui/SodiumOptionsGUIMixin.class`
