package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import com.talhanation.smallships.world.entity.projectile.AbstractCannonBall;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AbstractCannonBall.class, remap = false)
public abstract class ShipCannonHitQuestMixin {
    @Redirect(method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap = false), remap = false)
    private boolean jem$committedCannonDamage(Entity target, DamageSource source, float amount) {
        float before = target instanceof LivingEntity living ? living.getHealth() + living.getAbsorptionAmount() : 0;
        boolean accepted = target.hurt(source, amount);
        var projectile = (AbstractCannonBall) (Object) this;
        var proof = projectile.getPersistentData();
        if (accepted && Float.isFinite(amount) && amount > 0 && Float.isFinite(before)
                && target instanceof LivingEntity living && target instanceof Enemy && !(target instanceof Player)
                && Float.isFinite(living.getHealth() + living.getAbsorptionAmount())
                && living.getHealth() + living.getAbsorptionAmount() < before
                && !(living instanceof TamableAnimal pet && pet.isTame())
                && projectile.getOwner() instanceof ServerPlayer actor && !actor.isAlliedTo(target)
                && source.getDirectEntity() == projectile && source.getEntity() == actor
                && proof.hasUUID("JemTravelCannonActorV4") && proof.hasUUID("JemTravelCannonShipV4")
                && proof.hasUUID("JemTravelCannonProjectileV4")
                && proof.getUUID("JemTravelCannonActorV4").equals(actor.getUUID())
                && proof.getUUID("JemTravelCannonProjectileV4").equals(projectile.getUUID())) {
            TravelProof.emit(actor, "v3_world_ship_cannon");
        }
        return accepted;
    }
}
