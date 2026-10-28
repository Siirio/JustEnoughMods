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

@Mixin(value=dev.xylonity.companions.common.blockentity.SoulFurnaceBlockEntity.class,remap=false)
public abstract class CompanionQuestFurnaceMixin implements CompanionQuestFurnaceAccess {
 @Shadow private dev.xylonity.companions.common.recipe.SoulFurnaceRecipe currentRecipe;
 @Unique private ItemStack jem$candidateInput,jem$paidInput,jem$cookInput,jem$outputBefore;
 @Unique private dev.xylonity.companions.common.recipe.SoulFurnaceRecipe jem$candidate,jem$paidRecipe,jem$cookRecipe;
 @Unique private int jem$chargesBefore;@Unique private CompoundTag jem$outputProof;
 @Inject(method="findRecipe",at=@At("RETURN"))private void jem$selected(Level level,ItemStack input,CallbackInfoReturnable<dev.xylonity.companions.common.recipe.SoulFurnaceRecipe> ci){jem$candidateInput=input;jem$candidate=ci.getReturnValue();}
 @Redirect(method={"handleCooking","m_6836_"},at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;m_41774_(I)V"))
 private void jem$nativeInputDebit(ItemStack input,int amount){jem$companionDebit(input,amount);}
 @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;m_41774_(I)V"))
 private static void jem$nativeTickDebit(ItemStack input,int amount,Level level,BlockPos pos,BlockState state,net.minecraft.world.level.block.entity.BlockEntity owner){((CompanionQuestFurnaceAccess)(Object)owner).jem$companionDebit(input,amount);}
 @Override public void jem$companionDebit(ItemStack input,int amount){var before=input.copy();input.shrink(amount);if(amount==1 && before.getCount()-input.getCount()==1 && input==jem$candidateInput && jem$candidate!=null){jem$paidInput=before;jem$paidRecipe=jem$candidate;}else{jem$paidInput=null;jem$paidRecipe=null;}jem$candidate=null;jem$candidateInput=null;}
 @Inject(method="handleCooking",at=@At("HEAD"))private void jem$beforeCook(BlockPos pos,Level level,BlockState state,CallbackInfo ci){var f=(dev.xylonity.companions.common.blockentity.SoulFurnaceBlockEntity)(Object)this;jem$cookRecipe=currentRecipe;jem$cookInput=currentRecipe==jem$paidRecipe?jem$paidInput:null;jem$chargesBefore=f.charges;jem$outputBefore=f.getItem(1).copy();jem$outputProof=f.getPersistentData().getCompound("jemCompanionOutputV4").copy();}
 @Inject(method="handleCooking",at=@At("RETURN"))private void jem$afterCook(BlockPos pos,Level level,BlockState state,CallbackInfo ci){
  var f=(dev.xylonity.companions.common.blockentity.SoulFurnaceBlockEntity)(Object)this;var now=f.getItem(1);String action="";
  if(!level.isClientSide && jem$cookRecipe!=null && jem$cookInput!=null && jem$chargesBefore-f.charges==jem$cookRecipe.requiredCharges() && jem$cookRecipe.outputsItem() && now.getItem()==jem$cookRecipe.resultItem() && now.getCount()>jem$outputBefore.getCount()){
   if(jem$cookInput.is(Items.ROTTEN_FLESH) && CompanionQuestProof.item(now,"companions:crystallized_blood"))action="v3_pet_soul_workshop";
   if(jem$cookInput.is(Items.DIAMOND) && CompanionQuestProof.item(now,"companions:soul_gem"))action="v3_pet_soul_workshop_soul";
   if(!action.isEmpty() && (jem$outputBefore.isEmpty() || jem$outputProof.getString("action").equals(action) && jem$outputProof.getCompound("item").equals(jem$outputBefore.save(new CompoundTag())))){
    var proof=new CompoundTag();proof.putString("action",action);proof.put("item",now.save(new CompoundTag()));f.getPersistentData().put("jemCompanionOutputV4",proof);f.setChanged();}
  }
  if(currentRecipe==null){jem$paidRecipe=null;jem$paidInput=null;}jem$cookRecipe=null;jem$cookInput=null;jem$outputBefore=null;jem$outputProof=null;
 }
 @Inject(method="m_6836_",at=@At("HEAD"))private void jem$unknownOutput(int slot,ItemStack item,CallbackInfo ci){if(slot==1){var f=(dev.xylonity.companions.common.blockentity.SoulFurnaceBlockEntity)(Object)this;f.getPersistentData().remove("jemCompanionOutputV4");}}
}
