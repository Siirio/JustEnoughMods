package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.SkarrierWorkshopProof;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Pseudo
@Mixin(targets = {"net.mcreator.skarriermobs.procedures.HeartOfTranquilityPlayerFinishesUsingItemProcedure", "net.mcreator.skarriermobs.procedures.LeechingSackPlayerFinishesUsingItemProcedure", "net.mcreator.skarriermobs.procedures.WretchedToothCleavingProjectileHitsLivingEntityProcedure"}, remap = false)
public abstract class SkarrierNativeEffectQuestMixin {
    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean jem$effect(LivingEntity target, MobEffectInstance effect) {
        boolean accepted = target.addEffect(effect);
        SkarrierWorkshopProof.effect(target, effect, accepted);
        return accepted;
    }
}
