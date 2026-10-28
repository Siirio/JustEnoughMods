package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestMotion;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=com.github.alexmodguy.alexscaves.server.potion.DarknessIncarnateEffect.class,remap=false)
public abstract class CaveQuestDarknessEffectMixin {
    @Inject(method="m_6742_",at=@At("RETURN"))private void jem$nativeFlight(LivingEntity actor,int amplifier,CallbackInfo ci){if(actor instanceof ServerPlayer player)CaveQuestMotion.of(player).effect(player);}
}
