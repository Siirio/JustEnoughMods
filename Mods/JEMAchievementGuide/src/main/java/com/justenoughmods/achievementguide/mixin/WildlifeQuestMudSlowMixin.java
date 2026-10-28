package com.justenoughmods.achievementguide.mixin;
import com.github.alexthe666.alexsmobs.entity.*;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityMudBall", remap = false)
public abstract class WildlifeQuestMudSlowMixin {
    @Redirect(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean jem$slow(LivingEntity target, MobEffectInstance effect) {
        MobEffectInstance before = target.getEffect(effect.getEffect()); int duration = before == null ? 0 : before.getDuration(); boolean accepted = target.addEffect(effect);
        if (((EntityMudBall)(Object)this).getShooter() instanceof EntityMudskipper mud) {
            var actor = WildlifeQuestProof.owner(mud); var actual = target.getEffect(effect.getEffect());
            if (actor != null && accepted && actual != null && actual.getDuration() > duration && WildlifeQuestProof.hostile(target, actor)) WildlifeQuestProof.emit(actor, "v3_fauna_companion_powers");
        }
        return accepted;
    }
}
