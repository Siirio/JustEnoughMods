package com.justenoughmods.achievementguide.integration.petutilities;
import java.util.*;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
public final class PetUtilityQuestWagon {
 public record Interaction(ServerPlayer actor,float health,ItemStack input,int count,List<Entity> passengers){}
 private PetUtilityQuestWagon(){}
 public static Interaction before(AbstractWagon wagon,Player p,InteractionHand hand){return new Interaction(PetUtilityQuestProof.actor(p),wagon.getHealth(),p.getItemInHand(hand).copy(),p.getItemInHand(hand).getCount(),List.copyOf(wagon.getPassengers()));}
 public static void after(AbstractWagon wagon,Player p,InteractionHand hand,Interaction frame){var actor=PetUtilityQuestProof.actor(p);if(actor==null || frame==null || frame.actor()!=actor)return;
  if(frame.input().is(Items.IRON_INGOT) && frame.count()-p.getItemInHand(hand).getCount()==1 && Float.isFinite(frame.health()) && wagon.getHealth()>frame.health())PetUtilityQuestProof.emit(actor,"v3_pet_fresh_wheels");
  if(PetUtilityQuestProof.actor(wagon,"hitch")!=actor)return;
  for(var e:wagon.getPassengers())if(e instanceof Mob && !frame.passengers().contains(e)){var tag=PetUtilityQuestProof.data(wagon);tag.putUUID("cargo",e.getUUID());tag.putUUID("carrier",actor.getUUID());tag.remove("moved");wagon.getPersistentData().put(PetUtilityQuestProof.ROOT,tag);}
  var tag=PetUtilityQuestProof.data(wagon);if(!tag.hasUUID("cargo") || !tag.getBoolean("moved") || !frame.input().is(Items.LEAD) || frame.count()-p.getItemInHand(hand).getCount()!=1)return;
  for(var e:frame.passengers())if(e.getUUID().equals(tag.getUUID("cargo")) && !e.isPassenger() && e.isAlive() && e instanceof Mob){PetUtilityQuestProof.put(e,"unload",actor.getUUID());var receipt=PetUtilityQuestProof.data(e);receipt.putUUID("wagon",wagon.getUUID());e.getPersistentData().put(PetUtilityQuestProof.ROOT,receipt);if(PetUtilityQuestProof.safe(e))unloaded(e);}
 }
 public static void unloaded(Entity e){var actor=PetUtilityQuestProof.actor(e,"unload");if(actor==null || !PetUtilityQuestProof.safe(e))return;var tag=PetUtilityQuestProof.data(e);if(!tag.hasUUID("wagon") || actor.level()!=e.level() || !(actor.serverLevel().getEntity(tag.getUUID("wagon")) instanceof AbstractWagon wagon))return;var cargo=PetUtilityQuestProof.data(wagon);if(cargo.hasUUID("cargo") && cargo.getUUID("cargo").equals(e.getUUID()) && cargo.getBoolean("moved") && PetUtilityQuestProof.actor(wagon,"carrier")==actor){PetUtilityQuestProof.emit(actor,"v3_pet_wagon_service");cargo.remove("cargo");cargo.remove("moved");wagon.getPersistentData().put(PetUtilityQuestProof.ROOT,cargo);tag.remove("unload");e.getPersistentData().put(PetUtilityQuestProof.ROOT,tag);}}
}
