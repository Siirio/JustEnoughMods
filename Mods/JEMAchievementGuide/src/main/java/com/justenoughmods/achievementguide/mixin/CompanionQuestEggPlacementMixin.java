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

@Mixin(BlockItem.class)
public abstract class CompanionQuestEggPlacementMixin {
 @Shadow public abstract net.minecraft.world.level.block.Block getBlock();
 @Unique private static final ThreadLocal<Integer> jem$eggCount=new ThreadLocal<>();
 @Inject(method="place",at=@At("HEAD"))private void jem$before(net.minecraft.world.item.context.BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> ci){jem$eggCount.set(context.getItemInHand().getCount());}
 @Inject(method="place",at=@At("RETURN"))private void jem$placed(net.minecraft.world.item.context.BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> ci){Integer before=jem$eggCount.get();jem$eggCount.remove();var p=CompanionQuestProof.actor(context.getPlayer());if(p!=null && before!=null && before-context.getItemInHand().getCount()==1 && ci.getReturnValue().consumesAction() && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof dev.xylonity.companions.common.blockentity.CroissantEggBlockEntity egg && egg.getBlockState().getBlock()==getBlock()){egg.getPersistentData().putUUID("jemCompanionEggActorV4",p.getUUID());egg.setChanged();}}
}
