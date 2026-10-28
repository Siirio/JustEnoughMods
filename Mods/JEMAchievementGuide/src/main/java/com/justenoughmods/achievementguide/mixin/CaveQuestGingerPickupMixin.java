package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.living.GingerbreadManEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=GingerbreadManEntity.class,remap=false)
public abstract class CaveQuestGingerPickupMixin {
    @Unique private ItemStack jem$cavePicked;
    @Inject(method="onGetItem",at=@At("HEAD"))private void jem$before(ItemEntity item,CallbackInfo ci){jem$cavePicked=item.getItem().copy();((GingerbreadManEntity)(Object)this).getPersistentData().remove("jemCavePickedV4");}
    @Inject(method="onGetItem",at=@At("RETURN"))private void jem$after(ItemEntity item,CallbackInfo ci){
        var helper=(GingerbreadManEntity)(Object)this;var held=helper.getOffhandItem();
        if(jem$cavePicked!=null && !jem$cavePicked.isEmpty() && jem$cavePicked.getCount()-item.getItem().getCount()==1 && held.getCount()==1 && ItemStack.isSameItemSameTags(held,jem$cavePicked)
                && helper.getPersistentData().hasUUID("jemCaveOvenActorV4"))helper.getPersistentData().put("jemCavePickedV4",held.save(new CompoundTag()));jem$cavePicked=null;
    }
}
