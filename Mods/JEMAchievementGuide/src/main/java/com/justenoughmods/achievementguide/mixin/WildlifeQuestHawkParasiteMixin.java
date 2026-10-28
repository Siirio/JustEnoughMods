package com.justenoughmods.achievementguide.mixin;
import com.github.alexthe666.alexsmobs.entity.EntityTarantulaHawk;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Spider;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityTarantulaHawk$AIMelee", remap = false)
public abstract class WildlifeQuestHawkParasiteMixin {
    @Shadow @Final private EntityTarantulaHawk hawk;
    @Redirect(method = "m_8037_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean jem$parasite(LivingEntity target, MobEffectInstance effect) {
        boolean accepted = target.addEffect(effect); var actor = WildlifeQuestProof.owner(hawk); var actual = target.getEffect(effect.getEffect());
        if (actor != null && target instanceof Spider && accepted && effect.getEffect() == AMEffectRegistry.DEBILITATING_STING.get() && effect.getAmplifier() > 0 && actual != null && actual.getAmplifier() == effect.getAmplifier()) {
            WildlifeQuestProof.mark(target, "hawk_parasite_actor", actor.getUUID()); WildlifeQuestProof.data(target).putUUID("hawk_mother", hawk.getUUID());
        }
        return accepted;
    }
}
