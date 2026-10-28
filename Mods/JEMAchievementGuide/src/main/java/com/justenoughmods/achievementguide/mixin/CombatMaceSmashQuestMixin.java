package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatLifeProof;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Pseudo
@Mixin(targets = "net.salju.trialstowers.events.TrialsEvents", remap = false)
public abstract class CombatMaceSmashQuestMixin {
    @Redirect(method = "onCritical", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/entity/player/CriticalHitEvent;setDamageModifier(F)V"))
    private static void jem$nativeBonus(CriticalHitEvent event, float modifier) {
        float before = event.getDamageModifier(); event.setDamageModifier(modifier); CombatLifeProof.smash(event, before);
    }
}
