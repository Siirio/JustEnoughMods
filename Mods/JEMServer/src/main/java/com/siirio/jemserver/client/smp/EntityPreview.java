package com.siirio.jemserver.client.smp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

final class EntityPreview {
    private String key = "";
    private LivingEntity entity;
    private ItemStack fallback = new ItemStack(Items.DRAGON_HEAD);
    boolean render(GuiGraphics g, String id, int x, int y, int width, int height, int mouseX, int mouseY) {
        var client = Minecraft.getInstance();
        if (!key.equals(id)) {
            key = id;
            entity = null;
            fallback = new ItemStack(Items.DRAGON_HEAD);
            var type = EntityType.byString(id).orElse(null);
            if (type != null) {
                var egg = SpawnEggItem.byId(type);
                if (egg != null) fallback = new ItemStack(egg);
                try {
                    if (client.level != null && type.create(client.level) instanceof LivingEntity living) {
                        entity = living;
                        entity.setSilent(true);
                    }
                } catch (RuntimeException ignored) { entity = null; }
            }
        }
        if (entity == null || client.level != entity.level()) { icon(g, x, y, width, height); return false; }
        double entityWidth = Math.max(.35, entity.getBbWidth());
        double entityHeight = Math.max(.35, entity.getBbHeight());
        int scale = (int) Math.max(1, Math.min(72, Math.min(width * .72 / entityWidth, height * .82 / entityHeight)));
        int center = x + width / 2, feet = y + height - Math.max(5, height / 18);
        g.flush();
        g.enableScissor(x, y, x + width, y + height);
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, center, feet, scale, center - mouseX, y + height / 2f - mouseY, entity);
            g.flush();
        } catch (RuntimeException ignored) {
            entity = null;
            icon(g, x, y, width, height);
        } finally {
            g.flush();
            g.disableScissor();
            com.siirio.jemserver.client.ui.SmpRenderState.restoreMainTarget();
        }
        return entity != null;
    }
    private void icon(GuiGraphics g, int x, int y, int width, int height) {
        float scale = Math.max(1, Math.min(width, height) / 40f);
        g.pose().pushPose();
        g.pose().translate(x + width / 2f - 8 * scale, y + height / 2f - 8 * scale, 0);
        g.pose().scale(scale, scale, 1);
        g.renderItem(fallback, 0, 0);
        g.pose().popPose();
    }
}
