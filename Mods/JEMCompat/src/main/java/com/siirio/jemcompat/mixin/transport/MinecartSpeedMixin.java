package com.siirio.jemcompat.mixin.transport;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecart.class)
public abstract class MinecartSpeedMixin {
    private static final double STRAIGHT_CAP = 18.0 / 20.0;
    private static final double ASCENDING_CAP = 14.0 / 20.0;
    private static final double CURVE_CAP = 12.0 / 20.0;

    @Inject(method = "getMaxSpeedWithRail", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemcompat$vanillaRouteSpeed(CallbackInfoReturnable<Double> callback) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        ResourceLocation cartId = ForgeRegistries.ENTITY_TYPES.getKey(cart.getType());
        if (cartId == null || !cartId.getNamespace().equals("minecraft") || !cart.canUseRail()) {
            return;
        }
        BlockPos railPosition = cart.getCurrentRailPosition();
        BlockState state = cart.level().getBlockState(railPosition);
        if (!(state.getBlock() instanceof BaseRailBlock rail)) {
            return;
        }
        ResourceLocation railId = ForgeRegistries.BLOCKS.getKey(rail);
        if (railId == null || !railId.getNamespace().equals("minecraft")) {
            return;
        }
        RailShape shape = rail.getRailDirection(state, cart.level(), railPosition, cart);
        if (shape.isAscending()) {
            callback.setReturnValue(ASCENDING_CAP);
        } else if (shape == RailShape.NORTH_SOUTH || shape == RailShape.EAST_WEST) {
            callback.setReturnValue(STRAIGHT_CAP);
        } else {
            callback.setReturnValue(CURVE_CAP);
        }
    }
}
