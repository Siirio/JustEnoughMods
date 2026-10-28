package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.NativeTerminalNetworkOwner;
import com.tom.storagemod.util.MultiItemHandler;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.tom.storagemod.tile.InventoryConnectorBlockEntity", remap = false)
public abstract class TomsMasterNetworkQuestMixin {
    @Shadow private MultiItemHandler handlers;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void jem$nativeMaster(CallbackInfo callback) {
        ((NativeTerminalNetworkOwner) handlers).jem$nativeNetworkOwner((BlockEntity) (Object) this);
    }
}
