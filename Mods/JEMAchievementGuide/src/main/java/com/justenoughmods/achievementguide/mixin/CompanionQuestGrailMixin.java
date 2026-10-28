package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.companions.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value=dev.xylonity.knightlib.common.block.GreatChaliceBlock.class,remap=false)
public abstract class CompanionQuestGrailMixin {
 @Shadow private void spawnReward(Level level,BlockPos pos,ItemStack reward,Player player){throw new AssertionError();}
 @Inject(method="m_6227_",at=@At("HEAD"))private void jem$before(BlockState state,Level level,BlockPos pos,Player user,InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){CompanionQuestGrail.begin(user,hand,level.getBlockEntity(pos) instanceof dev.xylonity.knightlib.common.blockentity.GreatChaliceBlockEntity owner?owner:null);}
 @Redirect(method="m_6227_",at=@At(value="INVOKE",target="Ldev/xylonity/knightlib/common/block/GreatChaliceBlock;spawnReward(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)V"))private void jem$nativeReward(dev.xylonity.knightlib.common.block.GreatChaliceBlock block,Level level,BlockPos pos,ItemStack reward,Player p){CompanionQuestGrail.reward(()->spawnReward(level,pos,reward,p));}
 @Inject(method="m_6227_",at=@At("RETURN"))private void jem$after(BlockState state,Level level,BlockPos pos,Player user,InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){CompanionQuestGrail.end(user,hand);}
}
