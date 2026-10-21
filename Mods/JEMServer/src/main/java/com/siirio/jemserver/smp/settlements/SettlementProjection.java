package com.siirio.jemserver.smp.settlements;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class SettlementProjection {
    private SettlementProjection() { }
    public static CompoundTag view(ServerPlayer viewer, java.util.UUID selected) {
        if (com.siirio.jemserver.smp.DomainFeatures.loaded("jem_settlements"))
            return com.siirio.jemserver.compat.settlements.SettlementProjection.view(viewer, selected);
        CompoundTag result = new CompoundTag(); result.putString("tab", "settlements");
        result.put("rows", new net.minecraft.nbt.ListTag()); return result;
    }
}
