package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastBloodProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.block.BloodCauldronBlock", remap = false)
public abstract class MagicLastBloodCauldronQuestMixin {
    @Inject(method = "attemptCookEntity", at = @At("HEAD"))
    private static void jem$cookBegin(BlockState state, Level level, BlockPos pos, Entity entity, @Coerce Object execution, CallbackInfo callback) { MagicLastBloodProof.cookBegin(level, pos, entity); }
    @Inject(method = "attemptCookEntity", at = @At("RETURN"))
    private static void jem$cookEnd(BlockState state, Level level, BlockPos pos, Entity entity, @Coerce Object execution, CallbackInfo callback) { MagicLastBloodProof.cookEnd(); }
    @Inject(method = "lambda$getInteractionMap$2", at = @At("HEAD"))
    private static void jem$takeBegin(BlockState state, Level level, BlockPos pos, Player actor, InteractionHand hand, ItemStack input, CallbackInfoReturnable<InteractionResult> callback) { MagicLastBloodProof.takeBegin(level, pos, actor, input); }
    @Inject(method = "lambda$getInteractionMap$2", at = @At("RETURN"))
    private static void jem$takeEnd(BlockState state, Level level, BlockPos pos, Player actor, InteractionHand hand, ItemStack input, CallbackInfoReturnable<InteractionResult> callback) { MagicLastBloodProof.takeEnd(callback.getReturnValue().consumesAction()); }
}
