package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.NativeCableStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "com.tom.storagemod.tile.AbstractInventoryCableConnectorBlockEntity$InvHandler", remap = false)
public abstract class TomsCableProxyQuestMixin implements NativeCableStorage {}
