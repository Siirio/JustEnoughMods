package com.siirio.jemeconomy.full.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.siirio.jemeconomy.registry.EconomyBlocks;
import com.siirio.jemeconomy.shipping.ShippingBoxBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Reuses the approved block body and exact native goods anchors in every item context. */
public final class ShippingBoxItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final ShippingBoxRenderer goods;
    public ShippingBoxItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        goods = new ShippingBoxRenderer(Minecraft.getInstance().getItemRenderer());
    }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var state = EconomyBlocks.SHIPPING_BOX.get().defaultBlockState();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buffers, light, overlay);
        goods.render(new ShippingBoxBlockEntity(BlockPos.ZERO, state), 0F, pose, buffers, light, overlay);
    }
}
