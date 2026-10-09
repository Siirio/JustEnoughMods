package com.siirio.jemcompat.feature.create;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.nbt.CompoundTag;

public final class MovingDrillState {
    private MovingDrillState() {
    }

    public static CompoundTag data(MovementContext context) {
        if (context.blockEntityData == null) {
            context.blockEntityData = new CompoundTag();
        }
        return context.blockEntityData;
    }
}
