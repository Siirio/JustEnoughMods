package com.siirio.jemsettlements.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.siirio.jemsettlements.JemSettlements;
import com.siirio.jemsettlements.network.TownViewPacket;
import com.siirio.jemsettlements.network.FoodPolicyPacket;
import com.siirio.jemsettlements.happiness.FoodValueRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemSettlements.MOD_ID, value = Dist.CLIENT)
public final class SettlementClientState {
    private static TownViewPacket status;
    private static boolean foodPolicySynced;
    private static final KeyMapping OPEN = new KeyMapping("key.jem_settlements.open", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), "key.categories.jem_settlements");
    private SettlementClientState() {}
    public static void accept(TownViewPacket packet) {
        status = packet; Minecraft client = Minecraft.getInstance();
        if (packet.open()) {
            if (packet.view().getBoolean("Founding")) client.setScreen(new TownFoundingScreen(packet.view()));
            else openSmp();
        }
    }
    public static void acceptFoodPolicy(FoodPolicyPacket packet) { FoodValueRegistry.applySynced(packet.values()); foodPolicySynced = true; }
    @Mod.EventBusSubscriber(modid = JemSettlements.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {}
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (OPEN.consumeClick()) openSmp();
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if (!foodPolicySynced) return;
        var value = FoodValueRegistry.classify(event.getItemStack(), Minecraft.getInstance().player); if (value.isEmpty()) return;
        var food = event.getItemStack().getItem().getFoodProperties(event.getItemStack(), Minecraft.getInstance().player); if (food == null) return;
        int satiety = value.get().satiety() != 0 ? value.get().satiety() : Math.max(0, food.getNutrition()) * 4;
        event.getToolTip().add(Component.translatable("jem.settlements.food.header").withStyle(ChatFormatting.GOLD));
        event.getToolTip().add(Component.translatable("jem.settlements.food.happiness", String.format("%+d", value.get().mood())).withStyle(value.get().mood() < 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
        event.getToolTip().add(Component.translatable("jem.settlements.food.satiety", String.format("%+d", satiety)).withStyle(satiety < 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) event.getToolTip().add(Component.translatable("jem.settlements.food.detail").withStyle(ChatFormatting.DARK_GRAY));
    }
    private static void openSmp() {
        try { Class.forName("com.siirio.jemserver.client.smp.SmpScreen").getMethod("openRoute", String.class, java.util.UUID.class).invoke(null, "settlements", null); }
        catch (ReflectiveOperationException | LinkageError ignored) { }
    }
}
