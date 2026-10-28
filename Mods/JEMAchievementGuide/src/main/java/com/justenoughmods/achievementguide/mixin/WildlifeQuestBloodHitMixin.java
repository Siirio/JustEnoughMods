package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestBlood;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo
@Mixin(targets = {"com.github.alexthe666.alexsmobs.entity.EntityMosquitoSpit", "com.github.alexthe666.alexsmobs.entity.EntityHemolymph"}, remap = false)
public abstract class WildlifeQuestBloodHitMixin {
    @Redirect(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$hit(Entity target, DamageSource source, float damage) {
        float before = target instanceof LivingEntity living ? living.getHealth() : -1; boolean accepted = target.hurt(source, damage);
        if (target instanceof LivingEntity living) WildlifeQuestBlood.hit((Entity)(Object)this, living, before, accepted); return accepted;
    }
}
