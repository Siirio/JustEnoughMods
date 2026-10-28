package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatTreasureProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "com.github.teamfusion.rottencreatures.common.level.blocks.TreasureChestBlock", remap = false)
public abstract class CombatTreasureOpenQuestMixin {
    @Inject(method = "m_6227_", at = @At("HEAD"))
    private void jem$begin(BlockState state, Level level, BlockPos pos, Player actor, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) { CombatTreasureProof.begin(level, pos, actor); }
    @Redirect(method = "spawnItemEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$nativeLoot(Level level, Entity output) { boolean accepted = level.addFreshEntity(output); CombatTreasureProof.spawned(output, accepted); return accepted; }
    @Inject(method = "m_6227_", at = @At("RETURN"))
    private void jem$end(BlockState state, Level level, BlockPos pos, Player actor, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) { CombatTreasureProof.end(callback.getReturnValue().consumesAction()); }
}
