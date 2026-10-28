package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.BowEffectProof;
import com.justenoughmods.achievementguide.integration.CosmicQuestOrigin;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.sweenus.simplybows.util.CombatTargeting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.sweenus.simplybows.world.CosmicOrbitManager", remap = false)
public abstract class CosmicOrbitQuestMixin {
    @Redirect(method = "createOrRefresh", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private static boolean jem$nativeOrbitCommitted(List<Object> orbits, Object orbit, ServerLevel level,
                                                   LivingEntity target, Entity owner, @Coerce Object upgrades) {
        boolean added = orbits.add(orbit);
        ServerPlayer actor = BowEffectProof.actor(owner, "simplybows:cosmic_bow/cosmic_bow");
        if (added && actor != null && orbit instanceof CosmicQuestOrigin origin) origin.jem$questCosmicActor(actor.getUUID());
        return added;
    }

    @Redirect(method = "leapToTarget", at = @At(value = "INVOKE",
            target = "Lnet/sweenus/simplybows/util/CombatTargeting;applyDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/LivingEntity;FZZ)Z"))
    private static boolean jem$nativeOrbitDamage(ServerLevel level, Entity owner, LivingEntity target, float amount,
                                                boolean firstFlag, boolean secondFlag, ServerLevel nativeLevel,
                                                @Coerce Object orbit, LivingEntity previousTarget, LivingEntity nativeTarget) {
        float before = target.getHealth() + target.getAbsorptionAmount();
        boolean accepted = CombatTargeting.applyDamage(level, owner, target, amount, firstFlag, secondFlag);
        if (accepted && Float.isFinite(amount) && amount > 0 && Float.isFinite(before)
                && target.getHealth() + target.getAbsorptionAmount() < before && target instanceof Enemy
                && !(target instanceof TamableAnimal pet && pet.isTame()) && nativeTarget == target
                && owner instanceof ServerPlayer actor && !actor.isAlliedTo(target)
                && orbit instanceof CosmicQuestOrigin origin && actor.getUUID().equals(origin.jem$questCosmicActor())) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_bow_cosmic");
        }
        return accepted;
    }
}
