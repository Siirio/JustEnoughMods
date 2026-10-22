# GUI/fragmentum

[All GUI groups](../README.md) | [Gallery](../index.html)

Original reference assets. Control sheets retain their source dimensions and alpha. Resource namespace and containing mod are recorded separately; a path is not evidence of a live screen.

No separate GUI PNG was selected for this group. This does not prove the mod has no interface: it may use shared controls, procedural rendering, dynamic resources or an unclassified asset path.

## UI class candidates

Static names and declared superclasses only. Helpers/mixins can occur here. Shared vanilla controls, procedural drawing, item/entity renders and runtime state do not necessarily have their own PNG.

- `dev/obscuria/fragmentum/client/ClientGroupTooltip` extends `java/lang/Record`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/client/ClientGroupTooltip.class`
- `dev/obscuria/fragmentum/client/FragmentumClient` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/client/FragmentumClient.class`
- `dev/obscuria/fragmentum/client/FragmentumClientRegistry` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/client/FragmentumClientRegistry.class`
- `dev/obscuria/fragmentum/client/TooltipComponentRegistry$Definition` extends `java/lang/Record`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/client/TooltipComponentRegistry$Definition.class`
- `dev/obscuria/fragmentum/client/TooltipComponentRegistry` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/client/TooltipComponentRegistry.class`
- `dev/obscuria/fragmentum/forge/mixin/MixinClientTooltipComponentManager` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/forge/mixin/MixinClientTooltipComponentManager.class`
- `dev/obscuria/fragmentum/forge/service/ForgeClientService` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/forge/service/ForgeClientService.class`
- `dev/obscuria/fragmentum/mixin/MixinCreateWorldScreen` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/mixin/MixinCreateWorldScreen.class`
- `dev/obscuria/fragmentum/service/ClientService` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/service/ClientService.class`
- `dev/obscuria/fragmentum/world/tooltip/ChromaPresets$Instance` extends `java/lang/Record`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/ChromaPresets$Instance.class`
- `dev/obscuria/fragmentum/world/tooltip/ChromaPresets` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/ChromaPresets.class`
- `dev/obscuria/fragmentum/world/tooltip/GroupTooltip` extends `java/lang/Record`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/GroupTooltip.class`
- `dev/obscuria/fragmentum/world/tooltip/StyleFlag` extends `java/lang/Enum`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/StyleFlag.class`
- `dev/obscuria/fragmentum/world/tooltip/TooltipBuilder` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/TooltipBuilder.class`
- `dev/obscuria/fragmentum/world/tooltip/TooltipOptions$Builder` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/TooltipOptions$Builder.class`
- `dev/obscuria/fragmentum/world/tooltip/TooltipOptions` extends `java/lang/Record`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/TooltipOptions.class`
- `dev/obscuria/fragmentum/world/tooltip/TooltipTags$Chroma` extends `java/lang/Enum`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/TooltipTags$Chroma.class`
- `dev/obscuria/fragmentum/world/tooltip/TooltipTags$Color` extends `java/lang/Enum`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/TooltipTags$Color.class`
- `dev/obscuria/fragmentum/world/tooltip/TooltipTags$Instance` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/TooltipTags$Instance.class`
- `dev/obscuria/fragmentum/world/tooltip/TooltipTags$LineBreak` extends `java/lang/Enum`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/TooltipTags$LineBreak.class`
- `dev/obscuria/fragmentum/world/tooltip/TooltipTags$SimpleFlag` extends `java/lang/Record`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/TooltipTags$SimpleFlag.class`
- `dev/obscuria/fragmentum/world/tooltip/TooltipTags` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/TooltipTags.class`
- `dev/obscuria/fragmentum/world/tooltip/TooltipTemplates$Processor` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/TooltipTemplates$Processor.class`
- `dev/obscuria/fragmentum/world/tooltip/TooltipTemplates` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/TooltipTemplates.class`
- `dev/obscuria/fragmentum/world/tooltip/Tooltips$MatchResult` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/Tooltips$MatchResult.class`
- `dev/obscuria/fragmentum/world/tooltip/Tooltips$Node$ClosingTag` extends `dev/obscuria/fragmentum/world/tooltip/Tooltips$Node`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/Tooltips$Node$ClosingTag.class`
- `dev/obscuria/fragmentum/world/tooltip/Tooltips$Node$Group` extends `dev/obscuria/fragmentum/world/tooltip/Tooltips$Node`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/Tooltips$Node$Group.class`
- `dev/obscuria/fragmentum/world/tooltip/Tooltips$Node$OpeningTag` extends `dev/obscuria/fragmentum/world/tooltip/Tooltips$Node`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/Tooltips$Node$OpeningTag.class`
- `dev/obscuria/fragmentum/world/tooltip/Tooltips$Node$Template` extends `dev/obscuria/fragmentum/world/tooltip/Tooltips$Node`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/Tooltips$Node$Template.class`
- `dev/obscuria/fragmentum/world/tooltip/Tooltips$Node$Text` extends `dev/obscuria/fragmentum/world/tooltip/Tooltips$Node`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/Tooltips$Node$Text.class`
- `dev/obscuria/fragmentum/world/tooltip/Tooltips$Node` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/Tooltips$Node.class`
- `dev/obscuria/fragmentum/world/tooltip/Tooltips` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/Tooltips.class`
- `dev/obscuria/fragmentum/world/tooltip/package-info` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/fragmentum-forge-1.20.1-1.5.2.jar!/dev/obscuria/fragmentum/world/tooltip/package-info.class`
