package com.siirio.jemfood.client;

import com.siirio.jemfood.FoodCatalog;
import com.siirio.jemfood.FoodNetwork;
import com.siirio.jemfood.JEMFood;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = JEMFood.ID, value = Dist.CLIENT)
public final class FoodTooltips {
    private static final int[] COLORS = {0x9298A2, 0x52AE75, 0x63B3E8, 0xB17BEA};
    private static FoodNetwork.Snapshot snapshot = new FoodNetwork.Snapshot(0, "", 0, 0, List.of());
    private static final Set<MobEffectInstance> HIDDEN = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private static long received;
    private static Screen iconScreen;
    private static int iconX, iconY;
    private static int scroll, scrollMaximum;
    private FoodTooltips() {}
    private static long now() { return Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime(); }
    public static void receive(FoodNetwork.Snapshot packet) {
        snapshot = packet; received = now(); HIDDEN.clear();
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        for (var detail : packet.traits()) {
            if (detail.remaining() <= 0 || detail.effect().isEmpty() || detail.waiting()) continue;
            var effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(detail.effect()));
            var instance = player.getEffect(effect);
            if (instance != null && instance.getAmplifier() == 0 && !instance.isVisible() && !instance.showIcon()
                    && Math.abs(instance.getDuration() - detail.remaining()) <= 2) HIDDEN.add(instance);
        }
    }
    public static boolean hidden(MobEffectInstance instance) {
        return HIDDEN.contains(instance) && snapshot.remaining() > now() - received;
    }
    public static double value(String name) {
        if (snapshot.remaining() <= now() - received) return 0;
        return snapshot.traits().stream().filter(t -> t.trait().equals(name)).mapToDouble(FoodNetwork.Detail::power).findFirst().orElse(0);
    }
    public static MutableComponent days(int nativeDuration) {
        String value = BigDecimal.valueOf(Math.max(0, nativeDuration)).divide(BigDecimal.valueOf(24000), 5, RoundingMode.CEILING).stripTrailingZeros().toPlainString();
        return Component.translatable("tooltip.jemfood.days", value);
    }
    private static MutableComponent quality(int tier) {
        return Component.translatable("tooltip.jemfood.quality." + tier).withStyle(style -> style.withColor(COLORS[tier]));
    }
    public static void icon(Screen screen, int x, int y) { iconScreen = screen; iconX = x; iconY = y; }
    @SubscribeEvent public static void pre(ScreenEvent.Render.Pre event) { iconScreen = null; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        snapshot = new FoodNetwork.Snapshot(0, "", 0, 0, List.of()); HIDDEN.clear(); iconScreen = null;
    }
    @SubscribeEvent
    public static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        if (event.getScreen() == iconScreen && scrollMaximum > 0 && event.getMouseX() >= iconX && event.getMouseX() < iconX + 18
                && event.getMouseY() >= iconY && event.getMouseY() < iconY + 18) {
            scroll = Math.max(0, Math.min(scrollMaximum, scroll + (event.getScrollDelta() < 0 ? 2 : -2)));
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void item(ItemTooltipEvent event) {
        var dish = FoodCatalog.find(ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()));
        if (dish == null) return;
        var lines = event.getToolTip();
        lines.add(quality(dish.tier()));
        if (dish.policy().equals("native_only")) lines.add(Component.translatable("tooltip.jemfood.native_only").withStyle(ChatFormatting.GOLD));
        for (String name : dish.traits()) {
            var trait = FoodCatalog.trait(name);
            lines.add(Component.translatable("trait.jemfood." + name + ".name").withStyle(ChatFormatting.AQUA));
            if (trait.bridge()) {
                var preferred = ForgeRegistries.MOB_EFFECTS.getValue(trait.preferred());
                if (preferred != null) lines.add(Component.translatable("tooltip.jemfood.preferred", preferred.getDisplayName()).withStyle(ChatFormatting.GRAY));
                var fallback = ForgeRegistries.MOB_EFFECTS.getValue(trait.fallback());
                if (fallback != null) lines.add(Component.translatable("tooltip.jemfood.fallback", fallback.getDisplayName()).withStyle(ChatFormatting.GRAY));
            }
        }
        if (!dish.traits().isEmpty()) {
            lines.add(Component.translatable("tooltip.jemfood.duration", days(dish.duration())).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.jemfood.blessing_replaced").withStyle(ChatFormatting.GRAY));
        }
        if (dish.variety()) {
            lines.add(Component.translatable("tooltip.jemfood.variety", snapshot.variety()).withStyle(ChatFormatting.GRAY));
            if (snapshot.variety() >= 5) lines.add(Component.translatable("tooltip.jemfood.variety_active").withStyle(ChatFormatting.GREEN));
        }
    }
    @SubscribeEvent
    public static void hover(ScreenEvent.Render.Post event) {
        var player = Minecraft.getInstance().player;
        if (event.getScreen() != iconScreen || player == null || !player.hasEffect(JEMFood.BLESSING.get())
                || event.getMouseX() < iconX || event.getMouseX() >= iconX + 18
                || event.getMouseY() < iconY || event.getMouseY() >= iconY + 18) { scroll = 0; scrollMaximum = 0; return; }
        int elapsed = (int) Math.max(0, now() - received);
        if (snapshot.remaining() <= elapsed) return;
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("effect.jemfood.food_blessing").withStyle(ChatFormatting.GOLD));
        var id = ResourceLocation.tryParse(snapshot.dish());
        var source = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        if (source != null) lines.add(Component.translatable("tooltip.jemfood.food_source", source.getDescription()));
        else if (id != null) {
            var block = ForgeRegistries.BLOCKS.getValue(id);
            if (block != null) lines.add(Component.translatable("tooltip.jemfood.food_source", block.getName()));
        }
        lines.add(Component.translatable("tooltip.jemfood.quality", quality(snapshot.tier())));
        lines.add(Component.translatable("tooltip.jemfood.remaining", days(snapshot.remaining() - elapsed)));
        for (var detail : snapshot.traits()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("trait.jemfood." + detail.trait() + ".name").withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("trait.jemfood." + detail.trait() + ".description").withStyle(ChatFormatting.GRAY));
            var trait = FoodCatalog.trait(detail.trait());
            Object power = trait.units().equals("ticks") ? days((int) detail.power()) : detail.power();
            lines.add(Component.translatable("tooltip.jemfood.current_strength", power).withStyle(ChatFormatting.GRAY));
            if (!detail.effect().isEmpty()) {
                var effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(detail.effect()));
                if (effect != null) lines.add(Component.translatable("tooltip.jemfood.fallback", effect.getDisplayName()));
            }
            if (detail.waiting()) lines.add(Component.translatable("tooltip.jemfood.effect_waiting").withStyle(ChatFormatting.YELLOW));
            else if (detail.remaining() > elapsed) lines.add(Component.translatable("tooltip.jemfood.remaining", days(detail.remaining() - elapsed)));
            else if (!detail.effect().isEmpty() || detail.trait().equals("secondwind")) lines.add(Component.translatable("tooltip.jemfood.effect_suspended").withStyle(ChatFormatting.GRAY));
            if (detail.cooldown() > elapsed) lines.add(Component.translatable("tooltip.jemfood.cooldown", days(detail.cooldown() - elapsed)));
        }
        var font = Minecraft.getInstance().font;
        var wrapped = lines.stream().flatMap(line -> font.split(line, Math.min(280, Math.max(100, event.getScreen().width - 24))).stream()).toList();
        int capacity = Math.max(3, (event.getScreen().height - 32) / (font.lineHeight + 1) - 1);
        scrollMaximum = Math.max(0, wrapped.size() - capacity);
        scroll = Math.min(scroll, scrollMaximum);
        var visible = new ArrayList<>(wrapped.subList(scroll, Math.min(wrapped.size(), scroll + capacity)));
        if (scrollMaximum > 0) visible.add(Component.translatable("tooltip.jemfood.scroll", scroll + 1, wrapped.size()).withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
        event.getGuiGraphics().renderTooltip(font, visible, event.getMouseX(), event.getMouseY());
    }
}
