package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.beach.BeachBallActor;
import com.justenoughmods.achievementguide.integration.beach.BeachGoalRun;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.satisfy.beachparty.core.entity.BeachBallEntity", remap = false)
public abstract class BeachBallActorQuestMixin implements BeachBallActor {
    @Unique private UUID jem$nativeBallActor;

    @Override
    public UUID jem$lastNativeBallActor() { return jem$nativeBallActor; }

    @Inject(method = "m_7334_(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE",
            target = "Lnet/satisfy/beachparty/core/entity/BeachBallEntity;m_20334_(DDD)V", shift = At.Shift.AFTER), remap = false)
    private void jem$pushed(Entity actor, CallbackInfo callback) { jem$motionActor(actor); }

    @Inject(method = "m_6071_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE", target = "Lnet/satisfy/beachparty/core/entity/BeachBallEntity;m_20334_(DDD)V",
                    shift = At.Shift.AFTER), remap = false)
    private void jem$kicked(Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        jem$motionActor(actor);
    }

    @Unique
    private void jem$motionActor(Entity actor) {
        Entity ball = (Entity)(Object)this;
        if (ball.level().isClientSide) return;
        // A later native actor replaces attribution even when that actor is ineligible for credit.
        jem$nativeBallActor = null;
        var motion = ball.getDeltaMovement();
        if (actor instanceof ServerPlayer player && BeachGoalRun.realActor(player) && player.level() == ball.level()
                && !ball.isRemoved() && Double.isFinite(motion.x) && Double.isFinite(motion.y)
                && Double.isFinite(motion.z) && (motion.x != 0 || motion.z != 0)) {
            jem$nativeBallActor = player.getUUID();
        }
    }
}
