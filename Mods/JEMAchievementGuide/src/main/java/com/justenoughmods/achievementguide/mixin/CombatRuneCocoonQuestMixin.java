package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatRuneProof;
import java.util.UUID;
import java.util.List;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "net.sweenus.simplybows.world.CosmicGraceTrailManager", remap = false)
public abstract class CombatRuneCocoonQuestMixin {
    @Inject(method = "consumeCocoon", at = @At("HEAD"))
    private static void jem$begin(ServerLevel level, LivingEntity target, float damage, CallbackInfoReturnable<Boolean> callback) { CombatRuneProof.enter(null, true); }
    @Redirect(method = "consumeCocoon", at = @At(value = "INVOKE", target = "Ljava/util/List;remove(I)Ljava/lang/Object;"))
    private static Object jem$consumed(List<?> cocoons, int index) { Object cocoon = cocoons.remove(index); CombatRuneProof.adopt(cocoon); return cocoon; }
    @Inject(method = "consumeCocoon", at = @At("RETURN"))
    private static void jem$end(ServerLevel level, LivingEntity target, float damage, CallbackInfoReturnable<Boolean> callback) { if (callback.getReturnValue()) CombatRuneProof.committed(target); CombatRuneProof.leave(); }
}
