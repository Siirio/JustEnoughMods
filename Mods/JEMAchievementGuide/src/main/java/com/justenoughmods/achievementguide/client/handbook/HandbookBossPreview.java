package com.justenoughmods.achievementguide.client.handbook;

import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.IVariable;

/** A clipped and bounds-fitted boss preview for the handbook page. */
public final class HandbookBossPreview implements ICustomComponent {
    public String entity = "";
    public String icon = "";
    public String name = "";
    private transient LivingEntity preview;
    private transient ItemStack fallback = ItemStack.EMPTY;
    private transient Component title = Component.empty();
    private transient int x;
    private transient int y;

    @Override public void onVariablesAvailable(UnaryOperator<IVariable> lookup) {
        ResourceLocation entityId = ResourceLocation.tryParse(lookup.apply(IVariable.wrap(entity)).asString());
        ResourceLocation iconId = ResourceLocation.tryParse(lookup.apply(IVariable.wrap(icon)).asString());
        title = Component.literal(lookup.apply(IVariable.wrap(name)).asString());
        var minecraft = Minecraft.getInstance();
        if (entityId != null && minecraft.level != null && ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
            var created = ForgeRegistries.ENTITY_TYPES.getValue(entityId).create(minecraft.level);
            if (created instanceof LivingEntity living) preview = living;
        }
        if (iconId != null && ForgeRegistries.ITEMS.containsKey(iconId))
            fallback = new ItemStack(ForgeRegistries.ITEMS.getValue(iconId));
    }

    @Override public void build(int left, int top, int page) { x = left; y = top; }

    @Override public void render(GuiGraphics graphics, IComponentRenderContext context, float partial, int mouseX, int mouseY) {
        graphics.drawCenteredString(Minecraft.getInstance().font, title, x + 58, y, context.getTextColor());
        if (preview == null) {
            if (!fallback.isEmpty()) context.renderItemStack(graphics, x + 50, y + 58, mouseX, mouseY, fallback);
            return;
        }
        double width = Math.max(preview.getBbWidth(), 0.5D);
        double height = Math.max(preview.getBbHeight(), 0.5D);
        int scale = (int)Math.max(7D, Math.min(42D, Math.min(68D / height, 88D / width)));
        graphics.enableScissor(x, y + 14, x + 116, y + 136);
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 58, y + 132, scale,
                    x + 58 - mouseX, y + 68 - mouseY, preview);
        } finally {
            graphics.disableScissor();
        }
    }
}
