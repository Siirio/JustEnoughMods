package com.justenoughmods.achievementguide.mixin;
import com.github.alexthe666.alexsmobs.entity.*;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityMobProjectile", remap = false)
public abstract class WildlifeQuestPollenDamageMixin {
    @Redirect(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$pollen(Entity target, DamageSource source, float damage) {
        float before = target instanceof LivingEntity living ? living.getHealth() : -1; boolean accepted = target.hurt(source, damage);
        if ((Object)this instanceof EntityPollenBall pollen && pollen.getShooter() instanceof EntityFlutter flutter && target instanceof LivingEntity living) {
            var actor = WildlifeQuestProof.owner(flutter); if (actor != null && accepted && living.getHealth() < before && WildlifeQuestProof.hostile(living, actor)) WildlifeQuestProof.emit(actor, "v3_fauna_companion_powers");
        }
        return accepted;
    }
}
