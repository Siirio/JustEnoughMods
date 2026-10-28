package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
import com.ninni.spawn.server.entity.mob.Iguana;import net.minecraft.server.level.ServerLevel;import net.minecraft.world.entity.item.ItemEntity;import net.minecraft.world.item.ItemStack;import com.justenoughmods.achievementguide.integration.food.FoodDroppedProof;
@Pseudo @Mixin(targets="com.ninni.spawn.server.entity.mob.Iguana",remap=false)
public abstract class SpawnCareQuestIguanaSolarMixin { 
 @Unique private float jem$charge;@Unique private ItemEntity jem$output;@Unique private ItemStack jem$cooked;
 @Inject(method="cookItems",at=@At("HEAD"),remap=false) private void jem$before(CallbackInfo ci){jem$charge=((Iguana)(Object)this).getCharge();jem$output=null;jem$cooked=ItemStack.EMPTY;}
 @Redirect(method="cookItems",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/item/ItemEntity;m_32045_(Lnet/minecraft/world/item/ItemStack;)V",remap=false),remap=false) private void jem$output(ItemEntity drop,ItemStack cooked){var before=drop.getItem().copy();drop.setItem(cooked);if(!before.isEmpty()&&cooked.isEdible()&&!ItemStack.isSameItemSameTags(before,cooked)){jem$output=drop;jem$cooked=cooked.copy();}}
 @Inject(method="cookItems",at=@At("RETURN"),remap=false) private void jem$after(CallbackInfo ci){var self=(Iguana)(Object)this;var actor=SpawnCareQuestProof.actor(self,"solar_actor");if(actor!=null&&actor.getUUID().equals(self.getTrustedPlayerUUID())&&self.getCharge()<jem$charge&&jem$output!=null&&!jem$output.isRemoved()&&self.level() instanceof ServerLevel level&&level.getEntity(jem$output.getUUID())==jem$output&&ItemStack.matches(jem$output.getItem(),jem$cooked))FoodDroppedProof.update(jem$output,"v3_pet_solar_kitchen",jem$cooked.getCount(),actor.getUUID());}
 }
