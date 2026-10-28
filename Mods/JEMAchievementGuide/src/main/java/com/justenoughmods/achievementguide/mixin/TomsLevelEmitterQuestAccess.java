package com.justenoughmods.achievementguide.mixin;

import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.tom.storagemod.tile.LevelEmitterBlockEntity", remap = false)
public interface TomsLevelEmitterQuestAccess {
    @Accessor(value = "top", remap = false) LazyOptional<IItemHandler> jem$nativeEmitterNetwork();
}
