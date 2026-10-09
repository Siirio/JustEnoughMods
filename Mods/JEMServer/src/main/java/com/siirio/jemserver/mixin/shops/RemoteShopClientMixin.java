package com.siirio.jemserver.mixin.shops;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.spudacious5705.shops.block.entity.AbstractShopEntity;
import net.spudacious5705.shops.screen.ShopScreenHandlerCustomer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = ShopScreenHandlerCustomer.class, remap = false)
public abstract class RemoteShopClientMixin {
    private static final int NATIVE_OPEN_BYTES = Long.BYTES + 1;

    @Inject(method = "create", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jemRemoteCustomer(int id, Inventory inventory, FriendlyByteBuf buffer, CallbackInfoReturnable<ShopScreenHandlerCustomer> callback) {
        if (buffer.readableBytes() <= NATIVE_OPEN_BYTES) return;
        var pos = buffer.readBlockPos();
        boolean top = buffer.readBoolean();
        var level = inventory.player.level();
        var state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), buffer.readNbt());
        var snapshot = buffer.readNbt();
        var entity = BlockEntity.loadStatic(pos, state, snapshot);
        if (!(entity instanceof AbstractShopEntity shop)) throw new IllegalArgumentException("Invalid remote shop");
        shop.setLevel(level);
        callback.setReturnValue(new ShopScreenHandlerCustomer(id, inventory, pos, top, shop));
    }
}
