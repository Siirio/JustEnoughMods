package com.siirio.jemworldbosstiers.encounter;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import com.siirio.jemworldbosstiers.balance.EncounterScaler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = JemWorldBossTiers.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PendingHealthSync {
    private static final Map<UUID, ResourceKey<Level>> PENDING = new LinkedHashMap<>();

    private PendingHealthSync() {
    }

    public static void enqueue(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel level) {
            PENDING.put(entity.getUUID(), level.dimension());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, ResourceKey<Level>>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ResourceKey<Level>> pending = iterator.next();
            ServerLevel level = server.getLevel(pending.getValue());
            Entity entity = level == null ? null : level.getEntity(pending.getKey());
            if (!(entity instanceof LivingEntity living) || EncounterScaler.finishInitialHealthScaling(living)) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PENDING.clear();
    }
}
