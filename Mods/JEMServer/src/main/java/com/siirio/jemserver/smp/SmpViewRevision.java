package com.siirio.jemserver.smp;

import net.minecraft.server.level.ServerPlayer;

public record SmpViewRevision(long data, long rewards) {
    public static SmpViewRevision current(ServerPlayer player) {
        return new SmpViewRevision(SmpData.get(player.server).revision(),
                com.siirio.jemworldbosstiers.encounter.HostedRewards.get(player.server).revision());
    }
}
