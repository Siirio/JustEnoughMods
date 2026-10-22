# GUI/patpat

[All GUI groups](../README.md) | [Gallery](../index.html)

Original reference assets. Control sheets retain their source dimensions and alpha. Resource namespace and containing mod are recorded separately; a path is not evidence of a live screen.

No separate GUI PNG was selected for this group. This does not prove the mod has no interface: it may use shared controls, procedural rendering, dynamic resources or an unclassified asset path.

## UI class candidates

Static names and declared superclasses only. Helpers/mixins can occur here. Shared vanilla controls, procedural drawing, item/entity renders and runtime state do not necessarily have their own PNG.

- `net/lopymine/patpat/client/keybinding/PatPatClientKeybindingManager` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/client/keybinding/PatPatClientKeybindingManager.class`
- `net/lopymine/patpat/client/keybinding/PatPatKeybinding` extends `net/minecraft/client/KeyMapping`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/client/keybinding/PatPatKeybinding.class`
- `net/lopymine/patpat/client/packet/PatPatClientPacketManager` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/client/packet/PatPatClientPacketManager.class`
- `net/lopymine/patpat/mixin/KeyBindsScreenAccessor` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/mixin/KeyBindsScreenAccessor.class`
- `net/lopymine/patpat/mixin/KeyBindsScreenMixin` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/mixin/KeyBindsScreenMixin.class`
- `net/lopymine/patpat/mixin/KeyEntryMixin` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/mixin/KeyEntryMixin.class`
- `net/lopymine/patpat/mixin/KeyboardHandlerMixin` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/mixin/KeyboardHandlerMixin.class`
- `net/lopymine/patpat/mixin/MinecraftClientMixin` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/mixin/MinecraftClientMixin.class`
- `net/lopymine/patpat/mixin/controlling/KeyBindsScreenAccessor` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/mixin/controlling/KeyBindsScreenAccessor.class`
- `net/lopymine/patpat/mixin/controlling/NewKeyBindsScreenMixin` extends `net/minecraft/client/gui/screens/controls/KeyBindsScreen`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/mixin/controlling/NewKeyBindsScreenMixin.class`
- `net/lopymine/patpat/mixin/controlling/NewKeyEntryMixin` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/mixin/controlling/NewKeyEntryMixin.class`
- `net/lopymine/patpat/mixin/tests/AbstractSelectionListInvoker` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/mixin/tests/AbstractSelectionListInvoker.class`
- `net/lopymine/patpat/modmenu/bridge/ClothConfigBridge` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/modmenu/bridge/ClothConfigBridge.class`
- `net/lopymine/patpat/modmenu/bridge/YACLBridge$1` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/modmenu/bridge/YACLBridge$1.class`
- `net/lopymine/patpat/modmenu/bridge/YACLBridge` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/modmenu/bridge/YACLBridge.class`
- `net/lopymine/patpat/modmenu/common/image/PatRenderer$RenderMethod` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/modmenu/common/image/PatRenderer$RenderMethod.class`
- `net/lopymine/patpat/modmenu/integration/AbstractModMenuIntegration` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/modmenu/integration/AbstractModMenuIntegration.class`
- `net/lopymine/patpat/modmenu/integration/PatPatModMenuIntegration` extends `net/lopymine/patpat/modmenu/integration/AbstractModMenuIntegration`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/modmenu/integration/PatPatModMenuIntegration.class`
- `net/lopymine/patpat/modmenu/screen/NoConfigLibrariesScreen$CustomConfirmScreen` extends `net/minecraft/client/gui/screens/ConfirmScreen`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/modmenu/screen/NoConfigLibrariesScreen$CustomConfirmScreen.class`
- `net/lopymine/patpat/modmenu/screen/NoConfigLibrariesScreen` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/modmenu/screen/NoConfigLibrariesScreen.class`
- `net/lopymine/patpat/tests/PatPatTestClientAgent$TitleScreenHolder` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/tests/PatPatTestClientAgent$TitleScreenHolder.class`
- `net/lopymine/patpat/tests/PatPatTestClientAgent` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/tests/PatPatTestClientAgent.class`
- `net/lopymine/patpat/utils/mixin/ScreenWithPatPatKeybinding` extends `java/lang/Object`; `$PROFILE/automodpack/client/active/mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/net/lopymine/patpat/utils/mixin/ScreenWithPatPatKeybinding.class`
