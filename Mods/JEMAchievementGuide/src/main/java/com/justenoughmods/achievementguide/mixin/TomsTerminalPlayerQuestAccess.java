package com.justenoughmods.achievementguide.mixin;

import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.tom.storagemod.gui.StorageTerminalMenu", remap = false)
public interface TomsTerminalPlayerQuestAccess {
    @Accessor(value = "pinv", remap = false) Inventory jem$nativeTerminalPlayerInventory();
}
