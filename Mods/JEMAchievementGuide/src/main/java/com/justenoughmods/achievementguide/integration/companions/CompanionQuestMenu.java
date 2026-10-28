package com.justenoughmods.achievementguide.integration.companions;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import dev.xylonity.companions.common.blockentity.SoulFurnaceBlockEntity;
import dev.xylonity.companions.common.entity.companion.PuppetEntity;
import dev.xylonity.companions.common.item.PuppetArm;
public final class CompanionQuestMenu {
 public record Furnace(SoulFurnaceBlockEntity owner,ItemStack item,CompoundTag output,int held){}
 public record Frame(AbstractContainerMenu menu,ServerPlayer actor,ItemStack[] inventory,ItemStack carried,List<Furnace> furnaces,PuppetEntity puppet,ItemStack[] arms){}
 private static final class Calls{final ArrayDeque<Frame> frames=new ArrayDeque<>();int overflow;}
 private static final ThreadLocal<Calls> CALLS=ThreadLocal.withInitial(Calls::new);
 private CompanionQuestMenu(){}
 public static void observe(AbstractContainerMenu menu,net.minecraft.world.entity.player.Player user,Runnable nativeClick){begin(menu,user);boolean success=false;try{nativeClick.run();success=true;}finally{finish(menu,user,success);}}
 public static int held(ServerPlayer p,AbstractContainerMenu menu,ItemStack item){return CompanionQuestProof.count(p.getInventory(),item)+(ItemStack.isSameItemSameTags(menu.getCarried(),item)?menu.getCarried().getCount():0);}
 private static int heldBefore(Frame frame,ItemStack item){int n=ItemStack.isSameItemSameTags(frame.carried(),item)?frame.carried().getCount():0;for(var s:frame.inventory())if(ItemStack.isSameItemSameTags(s,item))n+=s.getCount();return n;}
 public static void begin(AbstractContainerMenu menu,net.minecraft.world.entity.player.Player user){
  var p=user instanceof ServerPlayer server?server:null;var calls=CALLS.get();if(p==null)return;if(calls.overflow>0 || calls.frames.size()>=32){calls.overflow++;return;}
  ItemStack[] inventory=new ItemStack[p.getInventory().getContainerSize()];for(int i=0;i<inventory.length;i++)inventory[i]=p.getInventory().getItem(i).copy();List<Furnace> rows=new ArrayList<>();
  if(CompanionQuestProof.actor(p)!=null && p.containerMenu==menu && menu.stillValid(p) && menu.slots.size()<=256)for(var slot:menu.slots)if(slot.getContainerSlot()==1 && slot.container instanceof SoulFurnaceBlockEntity furnace){var item=furnace.getItem(1).copy();rows.add(new Furnace(furnace,item,furnace.getPersistentData().getCompound("jemCompanionOutputV4").copy(),held(p,menu,item)));}
  var puppet=CompanionQuestProof.actor(p)!=null && p.containerMenu==menu && menu.stillValid(p) && menu instanceof CompanionQuestPuppetMenuAccess access?access.jem$companionPuppet():null;
  ItemStack[] arms=puppet==null?null:new ItemStack[]{puppet.inventory.getItem(0).copy(),puppet.inventory.getItem(1).copy()};
  calls.frames.push(new Frame(menu,p,inventory,menu.getCarried().copy(),List.copyOf(rows),puppet,arms));
 }
 private static void finish(AbstractContainerMenu menu,net.minecraft.world.entity.player.Player user,boolean success){var p=user instanceof ServerPlayer server?server:null;var calls=CALLS.get();if(p==null)return;if(calls.overflow>0){calls.overflow--;return;}if(calls.frames.isEmpty())return;var frame=calls.frames.pop();try{
  if(!success || CompanionQuestProof.actor(p)==null || frame.menu()!=menu || frame.actor()!=p || p.containerMenu!=menu || !menu.stillValid(p))return;
  for(var row:frame.furnaces()){
   var now=row.owner().getItem(1);var tag=row.output();int taken=row.item().getCount()-now.getCount();
   if(taken>0 && (now.isEmpty() || ItemStack.isSameItemSameTags(now,row.item())) && tag.getCompound("item").equals(row.item().save(new CompoundTag())) && !tag.getString("action").isEmpty() && held(p,menu,row.item())-row.held()==taken){
    CompanionQuestProof.emit(p,tag.getString("action"));if(now.isEmpty())row.owner().getPersistentData().remove("jemCompanionOutputV4");else{tag.put("item",now.save(new CompoundTag()));row.owner().getPersistentData().put("jemCompanionOutputV4",tag);}row.owner().setChanged();}
  }
  var puppet=frame.puppet();if(puppet!=null && CompanionQuestProof.owned(puppet,p))for(int slot=0;slot<2;slot++){
   var now=puppet.inventory.getItem(slot);var old=frame.arms()[slot];
   if(now.getItem() instanceof PuppetArm && now.getCount()==1 && old.isEmpty() && heldBefore(frame,now)-held(p,menu,now)==1){var tag=new CompoundTag();tag.putUUID("actor",p.getUUID());tag.put("item",now.save(new CompoundTag()));puppet.getPersistentData().put("jemCompanionArmV4"+slot,tag);}
  }
 }finally{if(calls.frames.isEmpty())CALLS.remove();}}
}
