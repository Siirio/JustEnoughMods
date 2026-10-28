package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.NativeTerminalNetworkOwner;
import java.lang.ref.WeakReference;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "com.tom.storagemod.util.MultiItemHandler", remap = false)
public abstract class TomsNetworkOwnerQuestMixin implements NativeTerminalNetworkOwner {
    @Unique private WeakReference<BlockEntity> jem$owner = new WeakReference<>(null);
    @Override public BlockEntity jem$nativeNetworkOwner() { return jem$owner.get(); }
    @Override public void jem$nativeNetworkOwner(BlockEntity owner) { jem$owner = new WeakReference<>(owner); }
}
