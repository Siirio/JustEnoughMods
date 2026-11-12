package dev.taste.engine.client;

import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import dev.taste.engine.TasteMod;
import dev.taste.engine.config.ConfigDraft;
import dev.taste.engine.config.ConfigSnapshot;
import dev.taste.engine.config.ConfigStore;
import dev.taste.engine.effects.ClientInteractionEffects;
import dev.taste.engine.effects.ClientPlacementEffects;
import dev.taste.engine.effects.PhotoCapture;
import dev.taste.engine.effects.SnowSurfaceOverlay;
import dev.taste.engine.environment.ClientWeatherParticles;
import dev.taste.engine.render.TasteRenderer;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TasteMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {
    private static final ClientWeatherParticles WEATHER_PARTICLES = new ClientWeatherParticles();
    private static final ClientInteractionEffects INTERACTION_EFFECTS = new ClientInteractionEffects();
    private static final ClientPlacementEffects PLACEMENTS = new ClientPlacementEffects();
    private static final SnowSurfaceOverlay SNOW_SURFACE = new SnowSurfaceOverlay();

    private ClientEvents() { }

    public static List<ClientWeatherParticles.WaterRipple> waterRipples() {
        return Stream.concat(INTERACTION_EFFECTS.waterRipples().stream(), WEATHER_PARTICLES.waterRipples().stream())
                .sorted(java.util.Comparator.comparingLong(ClientWeatherParticles.WaterRipple::bornTick).reversed()).toList();
    }

    public static boolean ownsPrecipitation() {
        return ClientState.store().renderSnapshot().requestedEnabled()
                && WEATHER_PARTICLES.ownsPrecipitation();
    }

    public static List<ClientInteractionEffects.PortalCell> portalCells() {
        return INTERACTION_EFFECTS.portalCells();
    }

    public static void onExplosion(double x, double y, double z, float power) {
        if (TasteRenderer.instance().graphicsActive() && ClientState.store().renderSnapshot().requestedEnabled()) {
            INTERACTION_EFFECTS.onExplosion(new Vec3(x, y, z), power);
        }
    }

    public static void confirmPlacement(BlockPos position, BlockState state) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && TasteRenderer.instance().graphicsActive()
                && ClientPlacementEffects.active(ClientState.store().renderSnapshot())) {
            PLACEMENTS.confirm(level, position, state);
        }
    }

    @SubscribeEvent
    public static void renderInteraction(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !TasteRenderer.instance().graphicsActive()) return;
        ConfigSnapshot settings = ClientState.store().renderSnapshot();
        if (settings.requestedEnabled()) {
            Minecraft minecraft = Minecraft.getInstance();
            SNOW_SURFACE.render(minecraft, event.getCamera(), event.getPoseStack().last().pose(), settings);
            INTERACTION_EFFECTS.render(event.getCamera(), event.getPoseStack().last().pose(), event.getPartialTick(), settings);
            PLACEMENTS.render(minecraft, event.getCamera(), event.getPoseStack().last().pose(), settings);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void watchPlacement(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ClientLevel level) || event.getEntity() != Minecraft.getInstance().player
                || event.getUseItem() == Event.Result.DENY || !(event.getItemStack().getItem() instanceof BlockItem item)
                || !TasteRenderer.instance().graphicsActive()
                || !ClientPlacementEffects.active(ClientState.store().renderSnapshot())) return;
        BlockPlaceContext context = new BlockPlaceContext(event.getEntity(), event.getHand(), event.getItemStack(), event.getHitVec());
        PLACEMENTS.watch(level, context, item);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void watchBlockBreak(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getLevel() instanceof ClientLevel level)
                || event.getEntity() != Minecraft.getInstance().player
                || !TasteRenderer.instance().graphicsActive()) return;
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.ABORT) {
            WEATHER_PARTICLES.cancelBreak(event.getPos());
        } else {
            WEATHER_PARTICLES.watchBreak(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        while (ClientRegistration.OPEN_MENU.consumeClick()) {
            if (minecraft.screen == null && minecraft.level != null)
                minecraft.setScreen(new EngineSettingsScreen(null, ClientState.store()));
        }
        while (ClientRegistration.TOGGLE_MASTER.consumeClick()) {
            if (minecraft.screen != null || minecraft.level == null) continue;
            ConfigStore store = ClientState.store();
            ConfigDraft draft = store.draft();
            draft.set("general.enabled", new JsonPrimitive(!store.current().requestedEnabled()));
            try {
                store.apply(draft);
                if (minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable("taste.ui.saved"), true);
            } catch (IOException | RuntimeException failure) {
                LogUtils.getLogger().error("Could not save Taste master preference", failure);
                if (minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable("taste.ui.saveFailed"), true);
            }
        }
        ConfigSnapshot settings = ClientState.store().renderSnapshot();
        boolean graphicsActive = TasteRenderer.instance().graphicsActive();
        boolean freezeBirths = PhotoCapture.instance().freezesDecoration();
        WEATHER_PARTICLES.tick(minecraft, settings, graphicsActive, freezeBirths);
        INTERACTION_EFFECTS.tick(minecraft, settings, graphicsActive, WEATHER_PARTICLES.ownedCount(), freezeBirths);
        PLACEMENTS.tick(minecraft.level, settings, graphicsActive);
        SNOW_SURFACE.tick(minecraft, settings, graphicsActive);
    }
}
