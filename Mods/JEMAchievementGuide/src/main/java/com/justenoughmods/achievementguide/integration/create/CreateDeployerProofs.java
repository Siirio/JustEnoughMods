package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Native fake-player ownership and actual committed placement, never raw interaction success. */
public final class CreateDeployerProofs {
    private record Call(DeployerFakePlayer player, List<BlockEvent.EntityPlaceEvent> placements) {}
    private static final ThreadLocal<Call> ACTIVE = new ThreadLocal<>();
    private CreateDeployerProofs() {}
    public static void register() { net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(CreateDeployerProofs.class); }
    public static void begin(DeployerFakePlayer player) {
        // An interrupted or reentrant native activation cannot lend its placement list to another call.
        ACTIVE.set(new Call(player, new ArrayList<>()));
    }
    public static void finished(DeployerFakePlayer player) {
        Call call = ACTIVE.get(); ACTIVE.remove();
        if (call == null || call.player != player) return;
        for (var event : call.placements) {
            if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level
                    && level.isLoaded(event.getPos()) && level.getBlockState(event.getPos()).equals(event.getPlacedBlock())
                    && !event.getBlockSnapshot().getReplacedBlock().equals(event.getPlacedBlock())) {
                CreateRunProofs.emit(level, player.getUUID(), "v3_eng_deployer"); break;
            }
        }
    }
    @SubscribeEvent
    public static void endTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) ACTIVE.remove();
    }
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        Call call = ACTIVE.get();
        if (call != null && event.getEntity() == call.player && call.placements.size() < 64) call.placements.add(event);
    }
}
