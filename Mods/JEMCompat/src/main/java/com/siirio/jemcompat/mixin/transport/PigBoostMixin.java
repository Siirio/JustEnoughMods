package com.siirio.jemcompat.mixin.transport;

import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pig.class)
public abstract class PigBoostMixin {
    private static final int BOOST_DURATION = 6 * 20;
    private static final int BOOST_COOLDOWN = 12 * 20;
    private static final float BOOST_MULTIPLIER = 1.60F;
    private static final double RIDDEN_SPEED_FACTOR = 0.225;
    private static final String BOOST_COOLDOWN_KEY = "JemPigBoostCooldown";

    @Shadow @Final private ItemBasedSteering steering;

    @Inject(method = "boost", at = @At("HEAD"), cancellable = true)
    private void jemcompat$fixedBoost(CallbackInfoReturnable<Boolean> callback) {
        Pig pig = (Pig) (Object) this;
        if (pig.getPersistentData().getInt(BOOST_COOLDOWN_KEY) > 0) {
            callback.setReturnValue(false);
            return;
        }
        ItemBasedSteeringAccessor access = (ItemBasedSteeringAccessor) steering;
        access.jemcompat$setBoosting(true);
        access.jemcompat$setBoostTime(0);
        access.jemcompat$getEntityData().set(access.jemcompat$getBoostTimeAccessor(), BOOST_DURATION);
        pig.getPersistentData().putInt(BOOST_COOLDOWN_KEY, BOOST_COOLDOWN);
        callback.setReturnValue(true);
    }

    @Inject(method = "getRiddenSpeed", at = @At("RETURN"), cancellable = true)
    private void jemcompat$fixedBoostSpeed(Player player, CallbackInfoReturnable<Float> callback) {
        if (((ItemBasedSteeringAccessor) steering).jemcompat$isBoosting()) {
            Pig pig = (Pig) (Object) this;
            callback.setReturnValue((float) (pig.getAttributeValue(Attributes.MOVEMENT_SPEED)
                    * RIDDEN_SPEED_FACTOR * BOOST_MULTIPLIER));
        }
    }
}
