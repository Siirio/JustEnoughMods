package com.siirio.jemcompat.feature.transport;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class AircraftPolicy {
    public static final int CAPACITY = 16;
    public static final long UNIT_NANOS = 90_000_000_000L;
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue ASCENT_ACCELERATION;
    public static final ForgeConfigSpec.DoubleValue MAX_ASCENT_SPEED;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("aircraft");
        ASCENT_ACCELERATION = builder.comment("Extra upward acceleration in blocks/tick squared, only during a powered climb.")
                .defineInRange("ascentAcceleration", 0.045, 0.0, 0.15);
        MAX_ASCENT_SPEED = builder.comment("Maximum powered ascent in blocks/second; horizontal motion and descent stay native.")
                .defineInRange("maxAscentSpeed", 8.0, 0.5, 20.0);
        builder.pop();
        SPEC = builder.build();
    }

    private AircraftPolicy() { }

    public static boolean enabled(Entity entity) {
        return entity.level().isClientSide || entity.getServer() != null;
    }
}
