package com.siirio.jemserver.mixin.shops;

import com.siirio.jemserver.smp.shops.ShopIndex;
import com.siirio.jemserver.smp.shops.ShopSnapshot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class ShopRemovalMixin {
    @Inject(method = "setRemoved", at = @At("TAIL"))
    private void jemShopRemoved(CallbackInfo callback) {
        var block = (BlockEntity) (Object) this;
        if (!(block instanceof ShopSnapshot) || !(block.getLevel() instanceof ServerLevel level)) return;
        var server = level.getServer();
        var dimension = level.dimension().location();
        var position = block.getBlockPos().immutable();
        if (server.isSameThread()) ShopIndex.remove(server, dimension, position);
        else server.execute(() -> ShopIndex.remove(server, dimension, position));
    }
}
