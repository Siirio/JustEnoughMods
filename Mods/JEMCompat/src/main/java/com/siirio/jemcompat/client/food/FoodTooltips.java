package com.siirio.jemcompat.client.food;

import com.siirio.jemcompat.feature.food.FoodCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "jemcompat", value = Dist.CLIENT)
public final class FoodTooltips {
    private static int count;
    private FoodTooltips() {}
    public static void progress(int value) { count = value; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { count = 0; }
    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getFoodProperties(event.getEntity()) == null) return;
        var dish = FoodCatalog.find(ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()));
        if (dish == null) return;
        var lines = event.getToolTip();
        lines.add(Component.translatable("food.jemcompat.tier." + dish.tier()).withStyle(ChatFormatting.GOLD));
        String duration = switch (dish.tier()) { case 1 -> "0.4"; case 2 -> "0.75"; default -> "1.5"; };
        for (var component : dish.effects()) {
            var effect = component.effect();
            if (effect == null) continue;
            lines.add(Component.translatable("food.jemcompat.effect", Component.translatable(effect.getDescriptionId()),
                    component.amplifier() == 0 ? "I" : "II", duration).withStyle(ChatFormatting.BLUE));
        }
        lines.add(Component.translatable("food.jemcompat.replace").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("food.jemcompat.variety", Math.min(count, 5)).withStyle(count >= 5 ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        if (count >= 5) lines.add(Component.translatable("food.jemcompat.variety_active").withStyle(ChatFormatting.GREEN));
    }
}
