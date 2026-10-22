# GUI/blueprint

[All GUI groups](../README.md) | [Gallery](../index.html)

Original reference assets. Control sheets retain their source dimensions and alpha. Resource namespace and containing mod are recorded separately; a path is not evidence of a live screen.

## blueprint:textures/plush_campaign.png

[PNG](png/source-bf1d8e36a0e7/assets/blueprint/textures/plush_campaign.png) - 256 x 256 - `selected_stack_candidate`

Source: `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/assets/blueprint/textures/plush_campaign.png`

Override target: `assets/blueprint/textures/plush_campaign.png`

![blueprint:textures/plush_campaign.png](png/source-bf1d8e36a0e7/assets/blueprint/textures/plush_campaign.png)

## UI class candidates

Static names and declared superclasses only. Helpers/mixins can occur here. Shared vanilla controls, procedural drawing, item/entity renders and runtime state do not necessarily have their own PNG.

- `com/teamabnormals/blueprint/client/PlushCampaignHandler$CampaignButton` extends `net/minecraft/client/gui/components/Button`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/PlushCampaignHandler$CampaignButton.class`
- `com/teamabnormals/blueprint/client/PlushCampaignHandler$CampaignScreen` extends `net/minecraft/client/gui/screens/Screen`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/PlushCampaignHandler$CampaignScreen.class`
- `com/teamabnormals/blueprint/client/PlushCampaignHandler` extends `java/lang/Enum`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/PlushCampaignHandler.class`
- `com/teamabnormals/blueprint/client/screen/SlabfishHatScreen` extends `net/minecraft/client/gui/screens/Screen`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/screen/SlabfishHatScreen.class`
- `com/teamabnormals/blueprint/client/screen/shaking/EmanatingShakeSource` extends `com/teamabnormals/blueprint/client/screen/shaking/PositionedShakeSource`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/screen/shaking/EmanatingShakeSource.class`
- `com/teamabnormals/blueprint/client/screen/shaking/PositionedShakeSource` extends `com/teamabnormals/blueprint/client/screen/shaking/SimpleShakeSource`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/screen/shaking/PositionedShakeSource.class`
- `com/teamabnormals/blueprint/client/screen/shaking/ScreenShakeHandler` extends `java/lang/Enum`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/screen/shaking/ScreenShakeHandler.class`
- `com/teamabnormals/blueprint/client/screen/shaking/ShakeSource` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/screen/shaking/ShakeSource.class`
- `com/teamabnormals/blueprint/client/screen/shaking/SimpleShakeSource` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/screen/shaking/SimpleShakeSource.class`
- `com/teamabnormals/blueprint/client/screen/splash/BlueprintSplashManager` extends `net/minecraft/server/packs/resources/SimplePreparableReloadListener`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/screen/splash/BlueprintSplashManager.class`
- `com/teamabnormals/blueprint/client/screen/splash/LiteralSplash` extends `java/lang/Record`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/screen/splash/LiteralSplash.class`
- `com/teamabnormals/blueprint/client/screen/splash/Splash` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/screen/splash/Splash.class`
- `com/teamabnormals/blueprint/client/screen/splash/SplashManagerAccessor` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/screen/splash/SplashManagerAccessor.class`
- `com/teamabnormals/blueprint/client/screen/splash/SplashProvider` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/screen/splash/SplashProvider.class`
- `com/teamabnormals/blueprint/client/screen/splash/SplashSerializers` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/client/screen/splash/SplashSerializers.class`
- `com/teamabnormals/blueprint/core/mixin/client/MinecraftMixin` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/core/mixin/client/MinecraftMixin.class`
- `com/teamabnormals/blueprint/core/mixin/client/SkinCustomizationScreenMixin` extends `net/minecraft/client/gui/screens/OptionsSubScreen`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/core/mixin/client/SkinCustomizationScreenMixin.class`
- `com/teamabnormals/blueprint/core/mixin/client/SplashManagerMixin` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/core/mixin/client/SplashManagerMixin.class`
- `com/teamabnormals/blueprint/core/util/BiomeUtil$OverlayModdedBiomeProvider` extends `java/lang/Record`; `$PROFILE/automodpack/client/active/mods/blueprint-1.20.1-7.1.5.jar!/com/teamabnormals/blueprint/core/util/BiomeUtil$OverlayModdedBiomeProvider.class`
