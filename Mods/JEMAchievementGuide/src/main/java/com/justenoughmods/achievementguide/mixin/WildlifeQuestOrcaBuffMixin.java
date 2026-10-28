package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityOrca$SwimWithPlayerGoal", remap = false)
public abstract class WildlifeQuestOrcaBuffMixin {
    @Redirect(method = "m_8037_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean jem$nativeBuff(Player player, MobEffectInstance effect) {
        var before = player.getEffect(AMEffectRegistry.ORCAS_MIGHT.get()); int duration = before == null ? 0 : before.getDuration();
        boolean accepted = player.addEffect(effect); var after = player.getEffect(AMEffectRegistry.ORCAS_MIGHT.get()); var actor = WildlifeQuestProof.real(player);
        if (accepted && actor != null && effect.getEffect() == AMEffectRegistry.ORCAS_MIGHT.get() && after != null && after.getDuration() > duration) WildlifeQuestProof.emit(actor, "v3_fauna_exotic_travel");
        return accepted;
    }
}
