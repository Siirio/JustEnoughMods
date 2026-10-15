package com.siirio.jemroles.client.vfx;

import com.siirio.jemroles.network.RaceVfxPacket;
import com.siirio.jemroles.vfx.RaceVfxId;
import net.minecraft.util.RandomSource;

public final class RaceVfxInstance {
    private final RaceVfxPacket packet;
    private final long startedTick;
    private final long expiresTick;
    private final RandomSource random;

    RaceVfxInstance(RaceVfxPacket packet, long startedTick) {
        this.packet = packet;
        this.startedTick = startedTick;
        this.expiresTick = startedTick + packet.duration();
        this.random = RandomSource.create(packet.seed());
    }

    public RaceVfxPacket packet() { return packet; }
    public long startedTick() { return startedTick; }
    public long expiresTick() { return expiresTick; }
    public RandomSource random() { return random; }

    public double progress(double now) {
        return Math.max(0.0D, Math.min(1.0D, (now - startedTick) / Math.max(1.0D, packet.duration())));
    }

    public boolean expired(long now) { return now >= expiresTick; }

    public boolean critical() {
        RaceVfxId id = packet.id();
        return switch (id) {
            case LIFE_CHORUS, GUARDIAN_FORM, GUARDIAN_DISSOLVE,
                    RIFT_DEPART, RIFT_ARRIVE, VOID_PROJECTILE_IMPACT, VOID_RESCUE,
                    VERDANT_DOMINION, SECOND_BLOSSOM, SCULK_VEINS,
                    INFERNAL_TELEGRAPH, INFERNAL_IMPACT, VOLCANIC_ZONE -> true;
            default -> false;
        };
    }
}
