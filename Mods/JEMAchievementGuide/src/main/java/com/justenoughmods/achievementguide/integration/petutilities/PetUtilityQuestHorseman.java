package com.justenoughmods.achievementguide.integration.petutilities;
import io.github.mortuusars.horseman.horse.HitchableHorse;
import com.justenoughmods.achievementguide.mixin.PetUtilityQuestHorseAccess;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
public final class PetUtilityQuestHorseman {
 public record Hitch(ServerPlayer actor,AbstractHorse horse,ItemStack lead){}
 private PetUtilityQuestHorseman(){}
 public static InteractionResult observe(Entity target,Player p,InteractionHand h,Supplier<InteractionResult> nativeCall){var actor=PetUtilityQuestProof.actor(p);
  if(actor==null || !(target instanceof AbstractChestedHorse horse) || !horse.hasChest() || !horse.isTamed() || horse.isBaby() || horse.isVehicle() || !(p.getItemInHand(h).getItem() instanceof ShearsItem))return nativeCall.get();
  var inventory=((PetUtilityQuestHorseAccess)horse).jem$horseInventory();var contents=new ArrayList<ItemStack>();int end=inventory.getContainerSize();if(horse instanceof HitchableHorse hh && HitchableHorse.shouldHaveLeadSlot(hh))end=Math.min(end,HitchableHorse.getLeadSlotIndex(hh));for(int i=2;i<end;i++)if(!inventory.getItem(i).isEmpty())contents.add(inventory.getItem(i).copy());
  return PetUtilityQuestDrops.observe(nativeCall,drops->{if(horse.hasChest() || !horse.isAlive() || PetUtilityQuestProof.actor(p)!=actor)return;int chests=drops.stream().filter(e->e.getItem().is(Items.CHEST)).mapToInt(e->e.getItem().getCount()).sum();if(chests<1)return;for(var stack:contents){int required=contents.stream().filter(s->ItemStack.isSameItemSameTags(s,stack)).mapToInt(ItemStack::getCount).sum();int emitted=drops.stream().filter(e->ItemStack.isSameItemSameTags(e.getItem(),stack)).mapToInt(e->e.getItem().getCount()).sum();if(emitted<required)return;}PetUtilityQuestProof.emit(actor,"v3_pet_trailcraft");});
 }
 public static void hitch(ServerPlayer p,AbstractHorse horse,ItemStack lead){if(PetUtilityQuestProof.actor(p)==null || p.getRootVehicle()!=horse || !(horse instanceof HitchableHorse h) || !HitchableHorse.isHitched(h) || !(horse.getLeashHolder() instanceof LeashFenceKnotEntity) || !HitchableHorse.shouldHaveLeadSlot(h) || !ItemStack.matches(HitchableHorse.getLead(h),lead) || !lead.is(Items.LEAD))return;PetUtilityQuestProof.emit(p,"v4_pet_horse_hitched");}
}
