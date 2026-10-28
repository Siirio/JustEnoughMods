package com.justenoughmods.achievementguide.mixin;
import com.github.alexthe666.alexsmobs.entity.EntityMimicOctopus;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityMimicOctopus", remap = false)
public abstract class WildlifeQuestMimicGuardianMixin {
    @Redirect(method = "m_8119_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$ability(LivingEntity target, DamageSource source, float damage) {
        float before = target.getHealth(); boolean accepted = target.hurt(source, damage);
        if (source.getEntity() instanceof EntityMimicOctopus mimic) WildlifeQuestAbilities.copied(mimic, target, before, accepted);
        return accepted;
    }
}
