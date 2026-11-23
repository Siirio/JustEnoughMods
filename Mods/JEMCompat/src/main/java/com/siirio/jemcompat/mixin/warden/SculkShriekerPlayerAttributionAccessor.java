package com.siirio.jemcompat.mixin.warden;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SculkShriekerBlockEntity.class)
public interface SculkShriekerPlayerAttributionAccessor {
    @Invoker("tryGetPlayer")
    static ServerPlayer jemcompat$resolvePlayer(Entity source) {
        throw new AssertionError("Mixin invoker was not applied");
    }
}
