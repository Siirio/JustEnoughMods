package com.siirio.jemsettlements.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.siirio.jemsettlements.JemSettlements;
import com.siirio.jemsettlements.happiness.FoodValueRegistry;
import com.siirio.jemsettlements.network.*;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Connection-scoped projections; no local balances or simulated action outcomes. */
@Mod.EventBusSubscriber(modid = JemSettlements.MOD_ID, value = Dist.CLIENT)
public final class SettlementClientState {
    private static CompoundTag view = new CompoundTag();
    private static UUID pending;
    private static long sentAt, lastAttempt;
    private static UUID pendingTown;
    private static CompoundTag pendingPayload;
    private static String pendingAction;
    private static boolean foodPolicySynced, awaitingOpen;
    private static final KeyMapping OPEN = new KeyMapping("key.jem_settlements.open", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), "key.categories.jem_settlements");
    private SettlementClientState() { }
    public static CompoundTag view() { return view.copy(); }
    public static boolean pending() { return pending != null; }
    public static boolean outcomeUnknown() { return pending != null && System.currentTimeMillis() - sentAt > 10000; }
    public static void open() {
        var client=Minecraft.getInstance();
        var target=ScreenTarget.target(client);
        client.setScreen(new SettlementScreen(new CompoundTag()));
        if(target!=null && client.screen instanceof SettlementScreen screen)screen.focusEntity(target);
        if (SettlementNetwork.supported()) { awaitingOpen = true; SettlementNetwork.requestOpen(); }
    }
    public static void openGuide() { open(); if (Minecraft.getInstance().screen instanceof SettlementScreen screen) screen.guide(); }
    public static boolean action(String action, UUID town, CompoundTag payload) {
        if (!SettlementNetwork.supported() || pending != null) return false;
        pending = SettlementNetwork.newRequestId(); pendingAction = action; sentAt = System.currentTimeMillis(); lastAttempt=sentAt; pendingTown=town; pendingPayload=payload.copy();
        SettlementNetwork.action(action, town, payload, pending); return true;
    }
    public static void accept(TownViewPacket packet) {
        SettlementNetwork.acceptSession(packet.view());
        if (packet.view().contains("Notice")) { SettlementNotices.accept(packet.view()); return; }
        if(packet.view().getBoolean("Heartbeat") || packet.view().getString("Action").equals("heartbeat"))return;
        view = packet.view().copy();
        boolean acknowledged = pending != null && view.hasUUID("Request") && pending.equals(view.getUUID("Request")) && !view.getString("Result").equals("OUTCOME_UNKNOWN") && !view.getString("Result").equals("RETRY");
        String completed = acknowledged ? pendingAction : "";
        if (acknowledged) { pending = null; pendingAction = null; pendingPayload=null; pendingTown=null; }
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof SettlementScreen screen) screen.accept(view, completed);
        else if (packet.open() && (!view.contains("Request") || awaitingOpen)) {
            client.setScreen(new SettlementScreen());
            if (client.screen instanceof SettlementScreen screen) screen.accept(view, completed);
        }
        awaitingOpen = false;
    }
    public static void reconcileNow(){lastAttempt=0;reconcile();}
    public static void reconcile() {
        if (pending!=null && pendingPayload!=null && System.currentTimeMillis()-lastAttempt>=10000 && SettlementNetwork.supported()) {
            lastAttempt=System.currentTimeMillis(); SettlementNetwork.action(pendingAction,pendingTown,pendingPayload,pending);
        }
    }
    private static final class ScreenTarget {
        static UUID target(Minecraft client){return net.minecraft.client.gui.screens.Screen.hasShiftDown()&&client.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit?hit.getEntity().getUUID():null;}
    }
    public static void acceptFoodPolicy(FoodPolicyPacket packet) { FoodValueRegistry.applySynced(packet.values()); foodPolicySynced = true; }
    public static void closed() { awaitingOpen = false; if (SettlementNetwork.supported()) SettlementNetwork.action("close", null, new CompoundTag(), UUID.randomUUID()); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        SettlementNetwork.clearClientSession();
        SettlementNotices.clear();
        view = new CompoundTag(); pending = null; pendingAction = null; pendingTown=null; pendingPayload=null; foodPolicySynced = false; awaitingOpen = false;
        FoodValueRegistry.applySynced(java.util.Map.of());
    }
    @Mod.EventBusSubscriber(modid = JemSettlements.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() { }
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (OPEN.consumeClick()) if (Minecraft.getInstance().player != null) open();
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if (!foodPolicySynced) return;
        var value = FoodValueRegistry.classify(event.getItemStack(), Minecraft.getInstance().player);
        if (value.isEmpty()) return;
        var food = event.getItemStack().getItem().getFoodProperties(event.getItemStack(), Minecraft.getInstance().player);
        if (food == null) return;
        int satiety = value.get().satiety() != 0 ? value.get().satiety() : Math.max(0, food.getNutrition()) * 4;
        event.getToolTip().add(Component.translatable("jem.settlements.food.header").withStyle(ChatFormatting.GOLD));
        event.getToolTip().add(Component.translatable("jem.settlements.food.happiness", String.format("%+d", value.get().mood())).withStyle(value.get().mood() < 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
        event.getToolTip().add(Component.translatable("jem.settlements.food.satiety", String.format("%+d", satiety)).withStyle(satiety < 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) event.getToolTip().add(Component.translatable("jem.settlements.food.detail").withStyle(ChatFormatting.DARK_GRAY));
    }
}
