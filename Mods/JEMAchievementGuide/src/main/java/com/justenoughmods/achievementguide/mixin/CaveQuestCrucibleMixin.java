package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import com.github.alexmodguy.alexscaves.server.block.blockentity.ConversionCrucibleBlockEntity;
import com.github.alexmodguy.alexscaves.server.item.BiomeTreatItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ConversionCrucibleBlockEntity.class,remap=false)
public abstract class CaveQuestCrucibleMixin implements CaveQuestCrucibleReceipt {
    @Unique private ItemEntity jem$caveSelected;
    @Unique private int jem$caveFillBefore,jem$caveCountBefore;
    @Unique private ItemStack jem$caveWantBefore,jem$caveMaterialBefore;
    @Inject(method="tick",at=@At("HEAD"))private static void jem$begin(Level world,BlockPos pos,BlockState state,ConversionCrucibleBlockEntity owner,CallbackInfo ci){((CaveQuestCrucibleReceipt)(Object)owner).jem$caveSelect(null);}
    @Inject(method="tick",at=@At("RETURN"))private static void jem$end(Level world,BlockPos pos,BlockState state,ConversionCrucibleBlockEntity owner,CallbackInfo ci){((CaveQuestCrucibleReceipt)(Object)owner).jem$caveSelect(null);}
    @Override public void jem$caveSelect(ItemEntity selected){jem$caveSelected=selected;}
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/item/ItemEntity;m_32055_()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jem$actualSelected(ItemEntity selected,Level world,BlockPos pos,BlockState state,ConversionCrucibleBlockEntity owner){((CaveQuestCrucibleReceipt)(Object)owner).jem$caveSelect(selected);return selected.getItem();}
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;m_41774_(I)V"))
    private static void jem$actualBoundTreat(ItemStack stack,int amount,Level world,BlockPos pos,BlockState state,ConversionCrucibleBlockEntity owner){((CaveQuestCrucibleReceipt)(Object)owner).jem$caveTreat(stack,amount);}
    @Override public void jem$caveTreat(ItemStack stack,int amount){
        var owner=(ConversionCrucibleBlockEntity)(Object)this;var proof=CaveQuestStackProof.get(stack);int before=stack.getCount();var biome=BiomeTreatItem.getCaveBiome(stack);
        stack.shrink(amount);owner.getPersistentData().remove("jemCaveCrucibleV4");
        var actor=jem$caveSelected==null?null:CaveQuestProof.actor(jem$caveSelected.getOwner());
        if(actor!=null && jem$caveSelected.getItem()==stack && amount==1 && before-stack.getCount()==1 && !proof.isEmpty() && proof.getString("kind").equals("treat")
                && proof.getUUID("actor").equals(actor.getUUID()) && !owner.isWitchMode() && owner.getFilledLevel()==1 && biome!=null && biome.equals(owner.getConvertingToBiome())){
            CompoundTag origin=new CompoundTag();origin.putUUID("actor",actor.getUUID());origin.putUUID("treat",proof.getUUID("source"));origin.putString("biome",biome.location().toString());origin.putInt("filled",1);owner.getPersistentData().put("jemCaveCrucibleV4",origin);owner.setChanged();}
    }
    @Inject(method="setConvertingToBiome",at=@At("HEAD"))private void jem$changedBiome(net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> key,CallbackInfo ci){((ConversionCrucibleBlockEntity)(Object)this).getPersistentData().remove("jemCaveCrucibleV4");}
    @Inject(method="consumeItem",at=@At("HEAD"))private void jem$beforeMaterial(ItemStack stack,CallbackInfo ci){var owner=(ConversionCrucibleBlockEntity)(Object)this;jem$caveFillBefore=owner.getFilledLevel();jem$caveCountBefore=stack.getCount();jem$caveWantBefore=owner.getWantItem().copy();jem$caveMaterialBefore=stack.copy();}
    @Inject(method="consumeItem",at=@At("RETURN"))private void jem$actualMaterial(ItemStack stack,CallbackInfo ci){
        var owner=(ConversionCrucibleBlockEntity)(Object)this;var proof=owner.getPersistentData().getCompound("jemCaveCrucibleV4");
        var actor=jem$caveSelected==null?null:CaveQuestProof.actor(jem$caveSelected.getOwner());
        boolean valid=actor!=null && proof.hasUUID("actor") && proof.getUUID("actor").equals(actor.getUUID()) && jem$caveSelected.getItem()==stack && !owner.isWitchMode()
                && proof.getInt("filled")==jem$caveFillBefore && owner.getFilledLevel()==jem$caveFillBefore+1 && jem$caveCountBefore-stack.getCount()==1
                && !jem$caveWantBefore.isEmpty() && jem$caveMaterialBefore.getItem()==jem$caveWantBefore.getItem() && owner.getConvertingToBiome()!=null && owner.getConvertingToBiome().location().toString().equals(proof.getString("biome"));
        if(valid){
            proof.putInt("filled",owner.getFilledLevel());
            if(owner.getFilledLevel()==ConversionCrucibleBlockEntity.MAX_FILL_AMOUNT){
                var biome=owner.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolder(owner.getConvertingToBiome());
                valid=biome.isPresent() && jem$caveWantBefore.getItem()==ConversionCrucibleBlockEntity.getFinalSacrificeForBiome(biome.get()).getItem();
                proof.putBoolean("final",valid);
            }
        }
        if(!valid)owner.getPersistentData().remove("jemCaveCrucibleV4");owner.setChanged();jem$caveWantBefore=null;jem$caveMaterialBefore=null;
    }
}
