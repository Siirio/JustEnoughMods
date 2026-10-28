package com.justenoughmods.achievementguide.integration.companions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;
public final class CompanionQuestStack {
 public static final String SAVE="jemCompanionStackV4";private CompanionQuestStack(){}
 public static CompoundTag get(ItemStack stack){var tag=((CompanionQuestStackCarrier)(Object)stack).jem$companionReceipt();return !stack.isEmpty() && tag.hasUUID("actor") && tag.hasUUID("source") && tag.getString("item").equals(CompanionQuestProof.id(stack)) && tag.getInt("count")==stack.getCount()?tag:new CompoundTag();}
 public static void put(ItemStack stack,UUID actor,UUID source,String kind,CompoundTag payload){if(stack.isEmpty() || actor==null || source==null)return;var tag=new CompoundTag();tag.putUUID("actor",actor);tag.putUUID("source",source);tag.putString("kind",kind);tag.putString("item",CompanionQuestProof.id(stack));tag.putInt("count",stack.getCount());tag.put("payload",payload.copy());((CompanionQuestStackCarrier)(Object)stack).jem$companionReceipt(tag);}
}
