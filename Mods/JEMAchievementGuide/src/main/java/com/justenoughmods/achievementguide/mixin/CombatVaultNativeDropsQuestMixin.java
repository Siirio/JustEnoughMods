package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatCorpseProof;
import com.justenoughmods.achievementguide.integration.combattools.CombatDropProof;
import net.mcreator.deepdarkregrowth.DeepDarkRegrowthMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Pseudo
@Mixin(targets = {"net.mcreator.deepdarkregrowth.procedures.VaultChestEntityDiesProcedure", "net.mcreator.deepdarkregrowth.procedures.VaultChestDeathTimeIsReachedProcedure"}, remap = false)
public abstract class CombatVaultNativeDropsQuestMixin {
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/mcreator/deepdarkregrowth/DeepDarkRegrowthMod;queueServerWork(ILjava/lang/Runnable;)V"), require = 0)
    private static void jem$queue(int delay, Runnable original) { DeepDarkRegrowthMod.queueServerWork(delay, CombatCorpseProof.queued(original)); }
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean jem$spawn(ServerLevel level, Entity output) { boolean accepted = level.addFreshEntity(output); CombatDropProof.spawned(output, accepted); return accepted; }
}
