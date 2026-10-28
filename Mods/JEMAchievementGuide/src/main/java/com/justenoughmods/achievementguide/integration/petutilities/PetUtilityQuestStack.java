package com.justenoughmods.achievementguide.integration.petutilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;
public final class PetUtilityQuestStack {
 public static final String SAVE="jemPetUtilityStackV4";private PetUtilityQuestStack(){}
 public static CompoundTag get(ItemStack stack){var tag=((PetUtilityQuestStackCarrier)(Object)stack).jem$petReceipt();return !stack.isEmpty() && tag.hasUUID("actor") && tag.hasUUID("source") && tag.getString("item").equals(PetUtilityQuestProof.id(stack)) && tag.getInt("count")==stack.getCount()?tag:new CompoundTag();}
 public static void put(ItemStack stack,UUID actor,UUID source,String kind,CompoundTag payload){if(stack.isEmpty() || actor==null || source==null)return;var tag=new CompoundTag();tag.putUUID("actor",actor);tag.putUUID("source",source);tag.putString("kind",kind);tag.putString("item",PetUtilityQuestProof.id(stack));tag.putInt("count",stack.getCount());tag.put("payload",payload.copy());((PetUtilityQuestStackCarrier)(Object)stack).jem$petReceipt(tag);}
}
