package com.siirio.jemroles.vfx;

import com.siirio.jemroles.network.RaceVfxPacket;
import com.siirio.jemroles.network.RolesNetwork;
import net.minecraft.server.level.ServerLevel;

public final class RaceVfxEmitter {
    private RaceVfxEmitter() {}

    public static void near(ServerLevel level, RaceVfxPacket packet) {
        if (packet != null && packet.valid() && level.dimension().location().equals(packet.dimension())) {
            RolesNetwork.sendNear(level, packet.origin(), packet);
        }
    }
}
