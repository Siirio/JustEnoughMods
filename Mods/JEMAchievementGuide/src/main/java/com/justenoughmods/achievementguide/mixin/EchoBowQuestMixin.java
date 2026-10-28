package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.sweenus.simplybows.entity.ShoulderBowEntity", remap = false)
public abstract class EchoBowQuestMixin {
    @Shadow private Vec3 queuedShotDirection;
    @Shadow private Vec3 preparedShotDirection;
    @Shadow private int drawTicksRemaining;
    @Shadow public abstract UUID getOwnerUuid();
    @Unique private boolean jem$mirroredDraw;

    @Inject(method = "beginDrawing", at = @At("TAIL"))
    private void jem$nativeMirrorDraw(Vec3 direction, UUID target, CallbackInfo callback) {
        jem$mirroredDraw = direction.lengthSqr() > 0.000001 && queuedShotDirection != null
                && queuedShotDirection.equals(direction) && preparedShotDirection != null && drawTicksRemaining > 0;
    }

    @Redirect(method = "firePreparedArrow", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$acceptedMirrorShot(ServerLevel level, Entity result, LivingEntity owner) {
        boolean accepted = level.addFreshEntity(result);
        if (accepted && jem$mirroredDraw && owner instanceof ServerPlayer actor && actor.getUUID().equals(getOwnerUuid())
                && result instanceof Projectile projectile && projectile.getOwner() == actor
                && result.getDeltaMovement().lengthSqr() > 0
                && BuiltInRegistries.ITEM.getKey(actor.getMainHandItem().getItem()).toString()
                .equals("simplybows:echo_bow/echo_bow")) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_bow_echo");
        }
        return accepted;
    }

    @Inject(method = "firePreparedArrow", at = @At("RETURN"))
    private void jem$clearMirrorDraw(LivingEntity owner, CallbackInfo callback) { jem$mirroredDraw = false; }
}
