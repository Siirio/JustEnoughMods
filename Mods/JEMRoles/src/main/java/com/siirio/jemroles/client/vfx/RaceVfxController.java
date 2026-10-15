package com.siirio.jemroles.client.vfx;

import com.siirio.jemroles.JemRoles;
import com.siirio.jemroles.network.RaceVfxPacket;
import com.siirio.jemroles.vfx.RaceVfxClientConfig;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemRoles.MOD_ID, value = Dist.CLIENT)
public final class RaceVfxController {
    private static final int MAX_ACTIVE = 128;
    private static final Map<Long, RaceVfxInstance> ACTIVE = new LinkedHashMap<>();
    private static long nextId;
    private static int decorativeRemaining = RaceVfxClientConfig.decorativeParticleLimit();

    private RaceVfxController() {}

    public static void accept(RaceVfxPacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (!packet.valid() || client.level == null || client.player == null
                || !client.level.dimension().location().equals(packet.dimension())) return;
        long now = client.level.getGameTime();
        ACTIVE.values().removeIf(instance -> instance.expired(now));
        if (packet.id() == com.siirio.jemroles.vfx.RaceVfxId.VOID_PROJECTILE_RELEASE)
            ACTIVE.values().removeIf(instance -> instance.packet().id() == com.siirio.jemroles.vfx.RaceVfxId.VOID_PROJECTILE_CHARGE
                    && instance.packet().sourceEntityId() == packet.sourceEntityId());
        RaceVfxInstance instance = new RaceVfxInstance(packet, now);
        double maximumDistance = instance.critical() ? Math.sqrt(RaceVfxPacket.MAX_OFFSET_SQR)
                : RaceVfxClientConfig.decorativeDistance();
        if (client.player.distanceToSqr(packet.origin()) > maximumDistance * maximumDistance) return;
        if (ACTIVE.size() >= MAX_ACTIVE && !makeRoom(instance.critical())) return;
        ACTIVE.put(++nextId, instance);
        RaceVfxParticles.onAccepted(client.level, instance);
    }

    private static boolean makeRoom(boolean incomingCritical) {
        if (!incomingCritical) return false;
        Iterator<Map.Entry<Long, RaceVfxInstance>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().getValue().critical()) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    static List<RaceVfxInstance> instances() { return List.copyOf(ACTIVE.values()); }

    static int claimParticles(int requested) {
        int accepted = Math.max(0, Math.min(requested, decorativeRemaining));
        decorativeRemaining -= accepted;
        return accepted;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        decorativeRemaining = RaceVfxClientConfig.decorativeParticleLimit();
        if (client.level == null) {
            clear();
            return;
        }
        long now = client.level.getGameTime();
        ACTIVE.values().removeIf(instance -> instance.expired(now)
                || !instance.packet().dimension().equals(client.level.dimension().location()));
        for (RaceVfxInstance instance : ACTIVE.values()) RaceVfxParticles.tick(client.level, instance, now);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) clear();
    }

    public static void clear() {
        ACTIVE.clear();
        nextId = 0L;
    }
}
