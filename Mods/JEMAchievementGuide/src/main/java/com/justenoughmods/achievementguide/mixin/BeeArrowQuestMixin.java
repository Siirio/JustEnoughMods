package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The native bee projectile must actually install its unique sting effect. */
@Pseudo
@Mixin(targets = "net.sweenus.simplybows.entity.BeeArrowEntity", remap = false)
public abstract class BeeArrowQuestMixin {
    @Redirect(method = "applyStackingPoison", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;m_147207_(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$nativeStingCommitted(LivingEntity target, MobEffectInstance effect, Entity owner) {
        boolean accepted = target.addEffect(effect, owner);
        Projectile bee = (Projectile) (Object) this;
        var proof = bee.getPersistentData();
        MobEffectInstance installed = target.getEffect(MobEffects.POISON);
        if (accepted && effect.getEffect() == MobEffects.POISON && effect.getDuration() > 0
                && installed != null && installed.getDuration() > 0 && target instanceof Enemy
                && !(target instanceof TamableAnimal pet && pet.isTame())
                && owner instanceof ServerPlayer actor && bee.getOwner() == actor && !actor.isAlliedTo(target)
                && proof.hasUUID("JemQuestNativeBowActorV4")
                && actor.getUUID().equals(proof.getUUID("JemQuestNativeBowActorV4"))
                && "simplybows:bee_bow/bee_bow".equals(proof.getString("JemQuestNativeBowSubjectV4"))) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_bow_bee");
        }
        return accepted;
    }
}
