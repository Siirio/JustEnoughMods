package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatRegrowthProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "net.mcreator.deepdarkregrowth.procedures.UnbreakableDoorKeyframeOnBlockRightClickedProcedure", remap = false)
public abstract class CombatSoulKeyQuestMixin {
    @Inject(method = "execute", at = @At("HEAD"))
    private static void jem$begin(LevelAccessor level, double x, double y, double z, Entity actor, CallbackInfo callback) { CombatRegrowthProof.begin("key", level, actor, null, x, y, z); }
    @Inject(method = "execute", at = @At("RETURN"))
    private static void jem$end(LevelAccessor level, double x, double y, double z, Entity actor, CallbackInfo callback) { CombatRegrowthProof.end(); }
}
