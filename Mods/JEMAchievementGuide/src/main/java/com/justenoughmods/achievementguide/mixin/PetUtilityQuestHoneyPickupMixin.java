package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ItemEntity.class)
public abstract class PetUtilityQuestHoneyPickupMixin {
 @Unique private int jem$old;
 @Unique private net.minecraft.world.item.ItemStack jem$item;
 @Unique private int jem$held;
 @Inject(method="playerTouch",at=@At("HEAD"))private void jem$before(Player p,CallbackInfo ci){var e=(ItemEntity)(Object)this;jem$old=e.getItem().getCount();jem$item=e.getItem().copy();jem$held=PetUtilityQuestProof.count(p.getInventory(),jem$item);}
 @Inject(method="playerTouch",at=@At("RETURN"))private void jem$after(Player p,CallbackInfo ci){var e=(ItemEntity)(Object)this;var actor=PetUtilityQuestProof.actor(p);int taken=jem$old-e.getItem().getCount();if(actor!=null && PetUtilityQuestProof.actor(e,"honey")==actor && taken>0 && PetUtilityQuestProof.count(p.getInventory(),jem$item)-jem$held==taken)PetUtilityQuestProof.emit(actor,"v3_pet_golem_jobs");}
}
