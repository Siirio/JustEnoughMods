package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatCorpseProof;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "net.mcreator.deepdarkregrowth.entity.VaultChestEntity", remap = false)
public abstract class CombatVaultDeathScopeQuestMixin {
    @Inject(method = "m_6667_", at = @At("HEAD"))
    private void jem$begin(DamageSource source, CallbackInfo callback) { CombatCorpseProof.enter((Entity)(Object)this); }
    @Inject(method = "m_6667_", at = @At("RETURN"))
    private void jem$end(DamageSource source, CallbackInfo callback) { CombatCorpseProof.leave(); }
}
