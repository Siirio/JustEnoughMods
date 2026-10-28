package com.justenoughmods.achievementguide.integration.petutilities;
import com.fruityspikes.whaleborne.server.entities.*;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.CompoundTag;
public final class PetUtilityQuestWhale {
 public record Install(ServerPlayer actor,ItemStack input,CompoundTag receipt,int count,List<Entity> passengers){}
 public record Armor(ServerPlayer actor,ItemStack input,int count,boolean saddled,int armor){}
 private PetUtilityQuestWhale(){}
 public static Install before(HullbackEntity whale,Player p,InteractionHand hand){var item=p.getItemInHand(hand);return new Install(PetUtilityQuestProof.actor(p),item.copy(),PetUtilityQuestStack.get(item).copy(),item.getCount(),List.copyOf(whale.getPassengers()));}
 public static void installed(HullbackEntity whale,Player p,InteractionHand hand,Install f){var actor=PetUtilityQuestProof.actor(p);if(f==null || actor==null || f.actor()!=actor || f.count()-p.getItemInHand(hand).getCount()!=1 || !f.receipt().getString("kind").equals("looted_widget") || !actor.getUUID().equals(f.receipt().getUUID("actor")))return;for(var widget:whale.getPassengers())if(!f.passengers().contains(widget) && widget.getVehicle()==whale && (widget instanceof CannonEntity && f.input().is(com.fruityspikes.whaleborne.server.registries.WBItemRegistry.CANNON.get()) || widget instanceof AnchorEntity && f.input().is(com.fruityspikes.whaleborne.server.registries.WBItemRegistry.ANCHOR.get())) && actor.serverLevel().getEntity(widget.getUUID())==widget){PetUtilityQuestProof.put(widget,"widget",actor.getUUID());var t=PetUtilityQuestProof.data(widget);t.putUUID("hullback",whale.getUUID());t.putString("table",f.receipt().getCompound("payload").getString("table"));widget.getPersistentData().put(PetUtilityQuestProof.ROOT,t);}}
 public static boolean widget(Entity widget,ServerPlayer actor,HullbackEntity whale){var t=PetUtilityQuestProof.data(widget);return PetUtilityQuestProof.actor(widget,"widget")==actor && widget.getVehicle()==whale && t.hasUUID("hullback") && t.getUUID("hullback").equals(whale.getUUID()) && (t.getString("table").equals("minecraft:chests/shipwreck_supply") || t.getString("table").equals("minecraft:chests/buried_treasure"));}
 public static Armor armorBefore(HullbackEntity whale,Player p,InteractionHand hand){return new Armor(PetUtilityQuestProof.actor(p),p.getItemInHand(hand).copy(),p.getItemInHand(hand).getCount(),whale.isSaddled(),whale.getInventory().getItem(2).getCount());}
 public static void armored(HullbackEntity whale,Player p,InteractionHand hand,Armor f){var actor=PetUtilityQuestProof.actor(p);if(f==null || actor==null || f.actor()!=actor || f.count()-p.getItemInHand(hand).getCount()!=1)return;if(!f.saddled() && whale.isSaddled() && f.input().getItem() instanceof SaddleItem)PetUtilityQuestProof.put(whale,"saddle",actor.getUUID());if(whale.getInventory().getItem(2).getCount()==f.armor()+1 && ItemStack.isSameItemSameTags(whale.getInventory().getItem(2),f.input()))PetUtilityQuestProof.put(whale,"planks",actor.getUUID());}
 public static void helm(HullbackEntity whale,ServerPlayer actor,boolean changed){if(changed && PetUtilityQuestProof.actor(actor)!=null && whale.getControllingPassenger()==actor && actor.getVehicle() instanceof HelmEntity helm && helm.getVehicle()==whale && PetUtilityQuestProof.actor(whale,"clean")==actor && PetUtilityQuestProof.actor(whale,"saddle")==actor && PetUtilityQuestProof.actor(whale,"planks")==actor && whale.isTamed() && whale.isSaddled() && !whale.getInventory().getItem(2).isEmpty() && !whale.hasAnchorDown() && (actor.xxa!=0 || actor.zza!=0))PetUtilityQuestProof.emit(actor,"v3_pet_whale_helm");}
}
