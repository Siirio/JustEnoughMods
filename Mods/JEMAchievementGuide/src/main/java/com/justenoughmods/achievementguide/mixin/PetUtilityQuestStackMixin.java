package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
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
public abstract class PetUtilityQuestStackMixin implements PetUtilityQuestStackCarrier {
 @Unique private CompoundTag jem$pet;
 @Override public CompoundTag jem$petReceipt(){return jem$pet==null?new CompoundTag():jem$pet.copy();}
 @Override public void jem$petReceipt(CompoundTag receipt){jem$pet=receipt==null?null:receipt.copy();}
 @Inject(method="<init>(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("RETURN"))private void jem$read(CompoundTag tag,CallbackInfo ci){if(tag.contains(PetUtilityQuestStack.SAVE,10)){jem$pet=tag.getCompound(PetUtilityQuestStack.SAVE).copy();if(PetUtilityQuestStack.get((ItemStack)(Object)this).isEmpty())jem$pet=null;}}
 @Inject(method="save",at=@At("RETURN"))private void jem$save(CompoundTag tag,CallbackInfoReturnable<CompoundTag> ci){if(jem$pet!=null){var proof=PetUtilityQuestStack.get((ItemStack)(Object)this);if(!proof.isEmpty())ci.getReturnValue().put(PetUtilityQuestStack.SAVE,proof);}}
 @Inject(method="copy",at=@At("RETURN"))private void jem$copy(CallbackInfoReturnable<ItemStack> ci){if(jem$pet!=null){var proof=PetUtilityQuestStack.get((ItemStack)(Object)this);if(!proof.isEmpty())((PetUtilityQuestStackCarrier)(Object)ci.getReturnValue()).jem$petReceipt(proof);}}
 @Inject(method="setCount",at=@At("HEAD"))private void jem$count(int count,CallbackInfo ci){if(count!=((ItemStack)(Object)this).getCount())jem$pet=null;}
}
