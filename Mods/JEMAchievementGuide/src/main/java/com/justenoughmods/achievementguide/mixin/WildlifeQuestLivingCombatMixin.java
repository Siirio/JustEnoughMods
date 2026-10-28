package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class WildlifeQuestLivingCombatMixin {
    @Unique private float jem$beforeHealth;
    @Inject(method = "hurt", at = @At("HEAD"))
    private void jem$before(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) { jem$beforeHealth = ((LivingEntity)(Object)this).getHealth(); }
    @Inject(method = "hurt", at = @At("RETURN"))
    private void jem$after(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        LivingEntity self = (LivingEntity)(Object)this; WildlifeQuestAbilities.exploded(self, source, jem$beforeHealth, callback.getReturnValue());
        if (callback.getReturnValue() && self.getHealth() < jem$beforeHealth && WildlifeQuestProof.type(self).equals("alexsmobs:cachalot_whale") && source.getEntity() instanceof Player player && WildlifeQuestProof.real(player) != null) WildlifeQuestWhales.attacker(self, player.getUUID());
    }
}
