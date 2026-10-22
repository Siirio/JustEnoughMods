package com.siirio.jemrewards;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;

@Mod("jem_rewards")
public final class JemRewards {
    public JemRewards() {
        RewardsNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::login);
        MinecraftForge.EVENT_BUS.addListener(this::clonePlayer);
        MinecraftForge.EVENT_BUS.addListener(this::stopped);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }
    private void started(ServerStartedEvent event) { RewardsData.get(event.getServer()).retryBatches(event.getServer()); }
    private void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && event.getServer().getTickCount() % 20 == 0)
            RewardsData.get(event.getServer()).retryBatches(event.getServer());
    }
    private void stopped(ServerStoppedEvent event) { RewardsNetwork.clear(); }
    private void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) RewardsData.get(player.server).recover(player);
    }
    private void clonePlayer(PlayerEvent.Clone event) {
        var receipt = event.getOriginal().getPersistentData().get(RewardsData.RECEIPT);
        if (receipt != null) event.getEntity().getPersistentData().put(RewardsData.RECEIPT, receipt.copy());
    }
}
