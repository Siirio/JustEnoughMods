package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatLifeProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class CombatMaceAttackQuestMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void jem$begin(Entity target, CallbackInfo callback) { CombatLifeProof.attackBegin((Player)(Object)this, target); }
    @Inject(method = "attack", at = @At("RETURN"))
    private void jem$end(Entity target, CallbackInfo callback) { CombatLifeProof.attackEnd((Player)(Object)this); }
}
