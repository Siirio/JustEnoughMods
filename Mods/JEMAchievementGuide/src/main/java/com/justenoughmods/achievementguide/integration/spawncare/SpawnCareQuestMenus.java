package com.justenoughmods.achievementguide.integration.spawncare;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
public final class SpawnCareQuestMenus {
 public record Equipment(ServerPlayer actor, List<ItemStack> inventory, ItemStack head) {}
 public static Equipment equipment(ServerPlayer actor){return SpawnCareQuestProof.real(actor)==null?null:new Equipment(actor,SpawnCareQuestProof.held(actor).stream().map(ItemStack::copy).toList(),actor.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).copy());}
 public static void equipped(Equipment before){if(before==null)return;var actor=before.actor;var head=actor.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);if(SpawnCareQuestProof.item(head).equals("barnyardbuddies:silkie_armor_helmet")&&head.canEquip(net.minecraft.world.entity.EquipmentSlot.HEAD,actor)&&!ItemStack.isSameItemSameTags(before.head,head)&&SpawnCareQuestProof.count(before.inventory,head)>0&&SpawnCareQuestProof.count(before.inventory,head)==SpawnCareQuestProof.count(SpawnCareQuestProof.held(actor),head))SpawnCareQuestProof.emit(actor,"v3_pet_farm_signatures_cap");}
 public record Binding(Container storage, TamableAnimal pet, String kind) {}
 public record Run(AbstractContainerMenu menu, ServerPlayer actor, Binding binding, List<ItemStack> storage, List<ItemStack> inventory, ItemStack result) {}
 private static final Map<AbstractContainerMenu,Binding> BINDINGS = Collections.synchronizedMap(new WeakHashMap<>());
 public static void bind(AbstractContainerMenu menu, Container storage, TamableAnimal pet, String kind) { BINDINGS.put(menu,new Binding(storage,pet,kind)); }
 public static List<ItemStack> contents(Container storage) { var list=new ArrayList<ItemStack>(); for(int i=0;i<storage.getContainerSize();i++) list.add(storage.getItem(i).copy()); return list; }
 public static Run begin(AbstractContainerMenu menu, ServerPlayer actor) { var b=BINDINGS.get(menu); if(b==null || SpawnCareQuestProof.real(actor)==null || !menu.stillValid(actor))return null; return new Run(menu,actor,b,contents(b.storage),SpawnCareQuestProof.held(actor).stream().map(ItemStack::copy).toList(),b.kind.equals("pigment")?menu.getSlot(5).getItem().copy():ItemStack.EMPTY); }
 public static void end(Run r) { if(r==null || r.actor.containerMenu!=r.menu || !r.menu.stillValid(r.actor))return; var b=r.binding; var after=contents(b.storage); var held=SpawnCareQuestProof.held(r.actor);
  if(b.kind.equals("hamster") && b.pet.isAlive() && b.pet.isOwnedBy(r.actor)) for(var stack:r.storage){int removed=SpawnCareQuestProof.count(r.storage,stack)-SpawnCareQuestProof.count(after,stack); if(removed>0 && SpawnCareQuestProof.count(held,stack)-SpawnCareQuestProof.count(r.inventory,stack)==removed){SpawnCareQuestProof.emit(r.actor,"v3_pet_cheek_delivery");break;}}
  if(b.kind.equals("trough") && b.storage instanceof BlockEntity be && be.getLevel()==r.actor.level()) { var setup=SpawnCareQuestSetups.get(r.actor.serverLevel()).at(r.actor.serverLevel(),be.getBlockPos()); if(!setup.hasUUID("actor") || !setup.getUUID("actor").equals(r.actor.getUUID()))return; var output=b.storage.getItem(0); int inserted=SpawnCareQuestProof.count(after,output)-SpawnCareQuestProof.count(r.storage,output); if(inserted>0 && SpawnCareQuestProof.count(r.inventory,output)-SpawnCareQuestProof.count(held,output)==inserted)NativeFoodSources.get(r.actor.serverLevel()).certify(r.actor.serverLevel(),r.actor.getUUID(),"spawn_care_trough",output,inserted); }
  if(b.kind.equals("pigment") && r.storage.size()==5 && !r.result.isEmpty() && SpawnCareQuestProof.item(r.storage.get(0)).equals("spawn:clam") && r.result.hasTag() && r.storage.get(0).hasTag() && r.result.getTag().getInt("ClamVariant")!=r.storage.get(0).getTag().getInt("ClamVariant")) {
   boolean shell=SpawnCareQuestProof.count(r.storage,r.storage.get(0))-SpawnCareQuestProof.count(after,r.storage.get(0))==1;
   boolean prism=false;for(int i: new int[]{3,4}){var a=r.storage.get(i);if((a.is(Items.PRISMARINE_SHARD)||a.is(Items.PRISMARINE_CRYSTALS))&&SpawnCareQuestProof.debited(a,after.get(i),1))prism=true;}
   int gain=SpawnCareQuestProof.count(held,r.result)-SpawnCareQuestProof.count(r.inventory,r.result);
   if(shell&&prism&&gain==1) for(var actual:held) if(ItemStack.isSameItemSameTags(actual,r.result)) {SpawnCareQuestContainers.certifyPigment(r.actor,actual,r.result.getTag().getInt("ClamVariant"));break;}
  }
 }
}
