package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractArrow.class)
public abstract class ArrowHitQuestMixin {
    @Redirect(method = "onHitEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$committedArrowHit(Entity target, DamageSource source, float amount) {
        float caveHealth = target instanceof net.minecraft.world.entity.LivingEntity living ? living.getHealth() : Float.NaN;
        float before = target instanceof Mob mob ? mob.getHealth() + mob.getAbsorptionAmount() : 0;
        boolean accepted = target.hurt(source, amount);
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        Entity caveActor = source.getEntity();
        if (caveActor != null && arrow.getPersistentData().hasUUID("jemCaveVolleyV4")
                && arrow.getPersistentData().getUUID("jemCaveVolleyV4").equals(caveActor.getUUID())) {
            com.justenoughmods.achievementguide.integration.caves.CaveQuestProof.damageObserved(
                    target, amount, caveActor, "v3_cave_shadow_arsenal_volley", caveHealth, accepted);
        }
        if (accepted && Float.isFinite(amount) && amount > 0 && target instanceof Mob mob && mob instanceof Enemy
                && Float.isFinite(before) && mob.getHealth() + mob.getAbsorptionAmount() < before
                && !(mob instanceof TamableAnimal pet && pet.isTame())
                && arrow.getOwner() instanceof ServerPlayer actor && !actor.isAlliedTo(mob)
                && source.getDirectEntity() == arrow && source.getEntity() == actor) {
            if (arrow.getPersistentData().hasUUID("JemQuestNativeBowActorV4")
                    && actor.getUUID().equals(arrow.getPersistentData().getUUID("JemQuestNativeBowActorV4"))) {
                QuestActionTrigger.INSTANCE.emit(actor, "hostile_arrow_hit");
            }
            if (arrow.getPersistentData().hasUUID("JemQuestNativeSiegeActorV4")
                    && actor.getUUID().equals(arrow.getPersistentData().getUUID("JemQuestNativeSiegeActorV4"))
                    && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(arrow.getType())
                    .toString().equals("siegeweapons:ballista_projectile")) {
                QuestActionTrigger.INSTANCE.emit(actor, "siege_hit");
            }
        }
        return accepted;
    }
}
