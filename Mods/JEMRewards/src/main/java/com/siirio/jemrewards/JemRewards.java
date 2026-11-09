package com.siirio.jemrewards;

import com.mojang.logging.LogUtils;
import com.siirio.jemidentity.api.IdentityPolicy;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;

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
    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        RewardsData.get(event.getServer()).retryBatches(event.getServer());
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (IdentityPolicy.resolve(player).isPresent()) deliverStarterKit(player);
        }
    }
    private void stopped(ServerStoppedEvent event) { RewardsNetwork.clear(); }
    private void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            RewardsData.get(player.server).recover(player);
        } catch (RuntimeException failure) {
            LogUtils.getLogger().error("Reward recovery remains pending for {}", player.getUUID(), failure);
            player.connection.disconnect(Component.translatable("jem_rewards.storage_unavailable"));
        }
    }
    private static void deliverStarterKit(ServerPlayer player) {
        try {
            ModpackStarterKit.deliver(player);
        } catch (RuntimeException failure) {
            LogUtils.getLogger().error("Starter kit delivery remains pending for {}", player.getUUID(), failure);
            player.connection.disconnect(Component.translatable("jem_rewards.storage_unavailable"));
        }
    }
    private void clonePlayer(PlayerEvent.Clone event) {
        var receipt = event.getOriginal().getPersistentData().get(RewardsData.RECEIPT);
        if (receipt != null) event.getEntity().getPersistentData().put(RewardsData.RECEIPT, receipt.copy());
    }
}
