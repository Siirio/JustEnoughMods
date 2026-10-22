# GUI/fabric_game_rule_api_v1

[All GUI groups](../README.md) | [Gallery](../index.html)

Original reference assets. Control sheets retain their source dimensions and alpha. Resource namespace and containing mod are recorded separately; a path is not evidence of a live screen.

No separate GUI PNG was selected for this group. This does not prove the mod has no interface: it may use shared controls, procedural rendering, dynamic resources or an unclassified asset path.

## UI class candidates

Static names and declared superclasses only. Helpers/mixins can occur here. Shared vanilla controls, procedural drawing, item/entity renders and runtime state do not necessarily have their own PNG.

- `net/fabricmc/fabric/impl/gamerule/client/GameRuleReflectionUtilsClient` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-game-rule-api-v1-1.0.40+683d4da877.jar!/net/fabricmc/fabric/impl/gamerule/client/GameRuleReflectionUtilsClient.class`
- `net/fabricmc/fabric/impl/gamerule/widget/DoubleRuleWidget` extends `net/minecraft/client/gui/screens/worldselection/EditGameRulesScreen$GameRuleEntry`; `$PROFILE/automodpack/client/active/mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-game-rule-api-v1-1.0.40+683d4da877.jar!/net/fabricmc/fabric/impl/gamerule/widget/DoubleRuleWidget.class`
- `net/fabricmc/fabric/impl/gamerule/widget/EnumRuleWidget` extends `net/minecraft/client/gui/screens/worldselection/EditGameRulesScreen$GameRuleEntry`; `$PROFILE/automodpack/client/active/mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-game-rule-api-v1-1.0.40+683d4da877.jar!/net/fabricmc/fabric/impl/gamerule/widget/EnumRuleWidget.class`
- `net/fabricmc/fabric/impl/gamerule/widget/package-info` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-game-rule-api-v1-1.0.40+683d4da877.jar!/net/fabricmc/fabric/impl/gamerule/widget/package-info.class`
- `net/fabricmc/fabric/mixin/gamerule/client/EditGameRulesScreenAccessor` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-game-rule-api-v1-1.0.40+683d4da877.jar!/net/fabricmc/fabric/mixin/gamerule/client/EditGameRulesScreenAccessor.class`
- `net/fabricmc/fabric/mixin/gamerule/client/EditGameRulesScreenRuleListWidgetMixin` extends `net/minecraft/client/gui/components/AbstractSelectionList`; `$PROFILE/automodpack/client/active/mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-game-rule-api-v1-1.0.40+683d4da877.jar!/net/fabricmc/fabric/mixin/gamerule/client/EditGameRulesScreenRuleListWidgetMixin.class`
- `net/fabricmc/fabric/mixin/gamerule/client/RuleListWidgetVisitorMixin` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-game-rule-api-v1-1.0.40+683d4da877.jar!/net/fabricmc/fabric/mixin/gamerule/client/RuleListWidgetVisitorMixin.class`
