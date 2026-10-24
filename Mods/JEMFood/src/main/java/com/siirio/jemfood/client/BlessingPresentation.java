package com.siirio.jemfood.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

/** Native Forge extension records the actual icon rectangle, including another mod's screen offset. */
public final class BlessingPresentation implements IClientMobEffectExtensions {
    @Override
    public boolean renderInventoryIcon(MobEffectInstance effect, EffectRenderingInventoryScreen<?> screen,
                                       GuiGraphics graphics, int x, int y, int blitOffset) {
        FoodTooltips.icon(screen, x, y + 7);
        return false;
    }
    @Override
    public boolean renderInventoryText(MobEffectInstance effect, EffectRenderingInventoryScreen<?> screen,
                                       GuiGraphics graphics, int x, int y, int blitOffset) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("effect.jemfood.food_blessing"), x + 28, y + 6, 0xFFFFFF);
        graphics.drawString(font, FoodTooltips.days(effect.getDuration()), x + 28, y + 16, 0x7F7F7F);
        return true;
    }
}
