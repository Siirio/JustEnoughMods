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

@Mixin(ItemStack.class)
public abstract class CompanionQuestStackMixin implements CompanionQuestStackCarrier {
 @Unique private CompoundTag jem$companion;
 @Override public CompoundTag jem$companionReceipt(){return jem$companion==null?new CompoundTag():jem$companion.copy();}
 @Override public void jem$companionReceipt(CompoundTag receipt){jem$companion=receipt==null?null:receipt.copy();}
 @Inject(method="<init>(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("RETURN"))private void jem$read(CompoundTag tag,CallbackInfo ci){if(tag.contains(CompanionQuestStack.SAVE,10)){jem$companion=tag.getCompound(CompanionQuestStack.SAVE).copy();if(CompanionQuestStack.get((ItemStack)(Object)this).isEmpty())jem$companion=null;}}
 @Inject(method="save",at=@At("RETURN"))private void jem$save(CompoundTag tag,CallbackInfoReturnable<CompoundTag> ci){if(jem$companion!=null){var proof=CompanionQuestStack.get((ItemStack)(Object)this);if(!proof.isEmpty())ci.getReturnValue().put(CompanionQuestStack.SAVE,proof);}}
 @Inject(method="copy",at=@At("RETURN"))private void jem$copy(CallbackInfoReturnable<ItemStack> ci){if(jem$companion!=null){var proof=CompanionQuestStack.get((ItemStack)(Object)this);if(!proof.isEmpty())((CompanionQuestStackCarrier)(Object)ci.getReturnValue()).jem$companionReceipt(proof);}}
 @Inject(method="setCount",at=@At("HEAD"))private void jem$count(int count,CallbackInfo ci){if(count!=((ItemStack)(Object)this).getCount())jem$companion=null;}
}
