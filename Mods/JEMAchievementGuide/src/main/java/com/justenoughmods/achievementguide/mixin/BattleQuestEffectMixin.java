package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntity.class)
public abstract class BattleQuestEffectMixin {
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("RETURN"))
    private void jem$effect(MobEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> ci) {
        LivingEntity target=(LivingEntity)(Object)this;
        if(ci.getReturnValue()&&target.hasEffect(effect.getEffect())) {
            BattleQuestArtifacts.effect(target,effect,true);
            if(net.minecraftforge.fml.ModList.get().isLoaded("efn")) BattleQuestNightfall.effect(target,effect);
            BattleQuestRelics.effect(target,effect);
        }
    }
}
