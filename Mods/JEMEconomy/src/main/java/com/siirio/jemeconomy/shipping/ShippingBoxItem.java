package com.siirio.jemeconomy.shipping;

import com.siirio.jemidentity.api.DedicatedSmp;
import com.siirio.jemidentity.api.IdentityPolicy;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public final class ShippingBoxItem extends BlockItem {
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new com.siirio.jemeconomy.full.client.ShippingBoxItemRenderer();
                return renderer;
            }
        });
    }
    public ShippingBoxItem(Block block, Properties properties) { super(block, properties); }
    @Override public InteractionResult place(BlockPlaceContext context) {
        if (context.getLevel().isClientSide) return super.place(context);
        if (!(context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) || !DedicatedSmp.active(player)
                || IdentityPolicy.resolve(player).isEmpty()) {
            if (context.getPlayer() != null) context.getPlayer().displayClientMessage(Component.translatable("jem.economy.shipping.identity_required"), true);
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }
}
