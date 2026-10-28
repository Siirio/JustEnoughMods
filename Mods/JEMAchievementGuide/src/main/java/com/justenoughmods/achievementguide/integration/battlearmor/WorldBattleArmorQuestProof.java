package com.justenoughmods.achievementguide.integration.battlearmor;
import com.justenoughmods.achievementguide.integration.battle.*;import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;import java.util.*;import net.minecraft.server.level.ServerPlayer;import net.minecraft.world.entity.*;import net.minecraft.world.item.*;
public final class WorldBattleArmorQuestProof {
 private WorldBattleArmorQuestProof(){}
 public static ServerPlayer real(Entity entity){return BattleQuestProof.real(entity)?(ServerPlayer)entity:null;}
 public static boolean smith(ServerPlayer actor,EquipmentSlot slot,String family,String id){var stack=actor.getItemBySlot(slot);return BattleQuestProof.item(stack).equals("cataclysm:"+id)&&BattleQuestSmithing.certified(actor,stack,family);}
 public static boolean same(ItemStack before,ItemStack after){return before.getItem()==after.getItem()&&NativeFoodSources.claims(before).stream().anyMatch(a->a.count()==1&&NativeFoodSources.claims(after).stream().anyMatch(b->b.count()==1&&a.batch().equals(b.batch())));}
 public static boolean useful(net.minecraft.world.effect.MobEffectInstance before,net.minecraft.world.effect.MobEffectInstance after){return after!=null&&after.getDuration()>0&&(before==null||after.getAmplifier()>before.getAmplifier()||after.getAmplifier()==before.getAmplifier()&&after.getDuration()>before.getDuration());}
 public static void emit(ServerPlayer actor,String goal){BattleQuestProof.emit(actor,goal);}
 public static List<ItemStack> held(ServerPlayer actor){var list=new ArrayList<ItemStack>();for(int i=0;i<actor.getInventory().getContainerSize();i++)list.add(actor.getInventory().getItem(i));list.add(actor.containerMenu.getCarried());return list;}
 public static int count(List<ItemStack> stacks,ItemStack sample){return stacks.stream().filter(s->ItemStack.isSameItemSameTags(s,sample)).mapToInt(ItemStack::getCount).sum();}
}
