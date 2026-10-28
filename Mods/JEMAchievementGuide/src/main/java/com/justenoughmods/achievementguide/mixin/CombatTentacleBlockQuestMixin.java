package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatTentacleProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Exact native block-grapple fallback lambda, actual pre/post SpatialUtil result. */
@Pseudo
@Mixin(targets = "net.unusual.block_factorys_bosses.item.UndyingTentacleItem", remap = false)
public abstract class CombatTentacleBlockQuestMixin {
    @Inject(method = "lambda$use$5", at = @At("HEAD"))
    private static void jem$before(ServerLevel level, Vec3 eye, Player actor, CallbackInfo callback) { CombatTentacleProof.pushBegin(actor); }
    @Inject(method = "lambda$use$5", at = @At("RETURN"))
    private static void jem$after(ServerLevel level, Vec3 eye, Player actor, CallbackInfo callback) { CombatTentacleProof.pushEndDelta(actor); }
}
