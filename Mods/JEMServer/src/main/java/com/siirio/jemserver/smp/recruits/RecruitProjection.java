package com.siirio.jemserver.smp.recruits;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class RecruitProjection {
    private RecruitProjection() { }
    public static CompoundTag view(ServerPlayer viewer) {
        if (com.siirio.jemserver.smp.DomainFeatures.loaded("jem_settlements"))
            return com.siirio.jemserver.compat.settlements.RecruitProjection.view(viewer);
        CompoundTag result = new CompoundTag(); result.putString("tab", "recruits");
        result.put("rows", new net.minecraft.nbt.ListTag()); return result;
    }
}
