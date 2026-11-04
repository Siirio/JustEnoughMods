package com.siirio.jembuilding.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.siirio.jembuilding.item.StructureBlueprintItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class BlueprintItemRenderer extends BlockEntityWithoutLevelRenderer {
    public BlueprintItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        var client = Minecraft.getInstance();
        var modelId = stack.getItem() instanceof StructureBlueprintItem
                ? context == ItemDisplayContext.GUI ? BlueprintClientModels.FINISHED_ICON : BlueprintClientModels.CLOSED
                : BlueprintClientAnimation.shouldRenderOpen(stack) ? BlueprintClientModels.OPEN : BlueprintClientModels.CLOSED;
        var model = client.getModelManager().getModel(modelId);
        boolean left = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        client.getItemRenderer().render(stack, context, left, pose, buffers, light, overlay, model);
    }
}
