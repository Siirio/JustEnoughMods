package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatRuneProof;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "net.sweenus.simplybows.world.BeeGraceShieldManager", remap = false)
public abstract class CombatRuneBeeGraceQuestMixin {
    @Inject(method = "consumeShield", at = @At("HEAD"))
    private static void jem$begin(ServerLevel level, LivingEntity target, CallbackInfoReturnable<Boolean> callback) { CombatRuneProof.enter(null, true); }
    @Redirect(method = "consumeShield", at = @At(value = "INVOKE", target = "Ljava/util/List;remove(I)Ljava/lang/Object;"))
    private static Object jem$consumed(List<?> shields, int index) { Object shield = shields.remove(index); CombatRuneProof.adopt(shield); return shield; }
    @Inject(method = "consumeShield", at = @At("RETURN"))
    private static void jem$end(ServerLevel level, LivingEntity target, CallbackInfoReturnable<Boolean> callback) { if (callback.getReturnValue()) CombatRuneProof.committed(target); CombatRuneProof.leave(); }
}
