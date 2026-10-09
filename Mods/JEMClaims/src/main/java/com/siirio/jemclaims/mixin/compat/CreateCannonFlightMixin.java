package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.compat.CreatePlacement;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity", remap = false)
public abstract class CreateCannonFlightMixin {
    @Redirect(method = "tickFlyingBlocks", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/schematics/cannon/LaunchedItem;update(Lnet/minecraft/world/level/Level;)Z"))
    private boolean jemClaims$land(LaunchedItem item, Level level) {
        BlockEntity cannon = (BlockEntity) (Object) this;
        if (level instanceof ServerLevel serverLevel) {
            if (!CreatePlacement.allowed(serverLevel, cannon.getBlockPos(), item)) return false;
            CreatePlacement.placing(serverLevel, cannon.getBlockPos(), item);
        }
        return item.update(level);
    }
}
