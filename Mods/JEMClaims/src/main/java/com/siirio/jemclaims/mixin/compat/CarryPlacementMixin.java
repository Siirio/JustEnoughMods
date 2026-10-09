package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "tschipp.carryon.common.carry.PlacementHandler", remap = false)
public abstract class CarryPlacementMixin {
    @ModifyVariable(method = "tryPlaceEntity", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static BiFunction<Vec3, Entity, Boolean> jemClaims$destination(BiFunction<Vec3, Entity, Boolean> original,
            ServerPlayer player, BlockPos pos, Direction side, BiFunction<Vec3, Entity, Boolean> callback) {
        return (destination, entity) -> FlanBridge.can(player, player.serverLevel(), BlockPos.containing(destination), ClaimPermission.PLACE)
                && (original == null || original.apply(destination, entity));
    }
}
