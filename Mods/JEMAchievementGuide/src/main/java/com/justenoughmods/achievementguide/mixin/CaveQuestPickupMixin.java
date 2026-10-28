package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntity.class)
public abstract class CaveQuestPickupMixin {
    @Unique private CompoundTag jem$caveDrop;
    @Unique private ItemStack jem$caveItem;
    @Unique private ItemStack[] jem$caveSlots;
    @Inject(method="playerTouch",at=@At("HEAD"))private void jem$before(Player user,CallbackInfo ci){
        var entity=(ItemEntity)(Object)this;jem$caveDrop=CaveQuestDrops.proof(entity);jem$caveItem=entity.getItem().copy();jem$caveSlots=null;
        if(CaveQuestProof.actor(user)!=null && !jem$caveDrop.isEmpty() && jem$caveDrop.getUUID("actor").equals(user.getUUID())){
            int n=user.getInventory().getContainerSize();if(n>64)return;jem$caveSlots=new ItemStack[n];for(int slot=0;slot<n;slot++)jem$caveSlots[slot]=user.getInventory().getItem(slot).copy();
        }
    }
    @Inject(method="playerTouch",at=@At("RETURN"))private void jem$after(Player user,CallbackInfo ci){
        var entity=(ItemEntity)(Object)this;var actor=CaveQuestProof.actor(user);
        try{
            if(actor==null || jem$caveSlots==null || jem$caveDrop==null || jem$caveDrop.isEmpty())return;
            int moved=jem$caveItem.getCount()-entity.getItem().getCount(),credit=0;
            for(int slot=0;slot<jem$caveSlots.length;slot++){
                var old=jem$caveSlots[slot];var now=actor.getInventory().getItem(slot);
                if(ItemStack.isSameItemSameTags(now,jem$caveItem))credit+=now.getCount()-(ItemStack.isSameItemSameTags(old,jem$caveItem)?old.getCount():0);
            }
            if(moved<=0 || credit!=moved)return;
            if(jem$caveDrop.getString("kind").equals("forestry") && jem$caveItem.is(net.minecraft.tags.ItemTags.LOGS))CaveQuestProof.emit(actor,"v3_cave_living_forestry");
            if(jem$caveDrop.getString("kind").equals("marine") && CaveQuestProof.item(jem$caveItem,"marine_snow")){
                int assigned=0;for(int slot=0;slot<jem$caveSlots.length;slot++){var now=actor.getInventory().getItem(slot);if(jem$caveSlots[slot].isEmpty() && ItemStack.isSameItemSameTags(now,jem$caveItem))assigned+=now.getCount();}
                if(assigned==moved)for(int slot=0;slot<jem$caveSlots.length;slot++){var now=actor.getInventory().getItem(slot);if(jem$caveSlots[slot].isEmpty() && ItemStack.isSameItemSameTags(now,jem$caveItem))CaveQuestStackProof.put(now,actor.getUUID(),jem$caveDrop.getUUID("source"),"marine");}
            }
            if(!entity.getItem().isEmpty()){jem$caveDrop.put("item",entity.getItem().save(new CompoundTag()));entity.getPersistentData().put(CaveQuestDrops.KEY,jem$caveDrop);}
        }finally{jem$caveDrop=null;jem$caveItem=null;jem$caveSlots=null;}
    }
}
