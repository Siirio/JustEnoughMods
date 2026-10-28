package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ItemEntity.class)
public abstract class WorldPrehistoricQuestPickupMixin {
 @Unique private int jem$count;
 @Unique private int jem$held;
 @Unique private ItemStack jem$item;
 @Unique private CompoundTag jem$receipt;
 @Inject(method="playerTouch",at=@At("HEAD"))private void jem$before(Player p,CallbackInfo ci){var e=(ItemEntity)(Object)this;jem$count=e.getItem().getCount();jem$item=e.getItem().copy();jem$held=WorldPrehistoricQuestProof.count(p.getInventory(),jem$item);jem$receipt=WorldPrehistoricQuestProof.data(e).copy();}
 @Inject(method="playerTouch",at=@At("RETURN"))private void jem$after(Player p,CallbackInfo ci){var e=(ItemEntity)(Object)this;int taken=jem$count-e.getItem().getCount();var receipt=jem$receipt;jem$receipt=null;if(taken>0){if(taken==jem$count && WorldPrehistoricQuestProof.count(p.getInventory(),jem$item)-jem$held==taken)WorldPrehistoricQuestDrops.picked(e,WorldPrehistoricQuestProof.actor(p),receipt,jem$item);e.getPersistentData().remove(WorldPrehistoricQuestProof.ROOT);}}
}
