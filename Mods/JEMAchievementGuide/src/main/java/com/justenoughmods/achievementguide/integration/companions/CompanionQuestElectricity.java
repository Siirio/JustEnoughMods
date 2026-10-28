package com.justenoughmods.achievementguide.integration.companions;
import dev.xylonity.companions.common.blockentity.*;
import dev.xylonity.companions.common.entity.companion.DinamoEntity;
import dev.xylonity.companions.common.tesla.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
public final class CompanionQuestElectricity {
 public static final String LINK="jemCompanionTeslaLinkV4",PULSE="jemCompanionTeslaPulseV4";private CompanionQuestElectricity(){}
 public static boolean outgoing(Level level,ConnectionTarget source,ConnectionTarget target){if(!source.dimension().equals(level.dimension().location()) || !target.dimension().equals(source.dimension()))return false;
  if(source.isBlock() && level instanceof ServerLevel server && CompanionQuestProof.loaded(server,source.blockPos()) && level.getBlockEntity(source.blockPos()) instanceof AbstractTeslaBlockEntity node)return node.getOutgoing().contains(target);
  if(source.isEntity() && level instanceof ServerLevel server && server.getEntity(source.entityId()) instanceof DinamoEntity dinamo)return dinamo.getOutgoing().contains(target);return false;
 }
 public static void connected(Player user,ConnectionTarget source,ConnectionTarget target){var actor=CompanionQuestProof.actor(user);if(actor==null || source.equals(target) || !outgoing(actor.level(),source,target) || !TeslaNetwork.get(actor.level()).getIncoming(target).contains(source))return;
  CompanionQuestProof.emit(actor,"v3_pet_current_flows_link");if(target.isBlock() && CompanionQuestProof.loaded(actor.serverLevel(),target.blockPos()) && actor.level().getBlockEntity(target.blockPos()) instanceof AbstractTeslaBlockEntity node){var proof=new CompoundTag();proof.putUUID("actor",actor.getUUID());proof.put("source",source.serialize());proof.put("target",target.serialize());node.getPersistentData().put(LINK,proof);node.setChanged();}
 }
 public static void generator(DinamoEntity source,AbstractTeslaBlockEntity target){
  var owner=source.getOwnerUUID();var nativeSource=ConnectionTarget.forEntity(source.getUUID(),source.level().dimension().location());boolean valid=owner!=null && source.getCycleCounter()==5 && source.getOutgoing().contains(target.asConnectionTarget());target.startCycle();
  target.getPersistentData().remove(PULSE);if(valid && source.level()==target.getLevel() && target.cycleCounter==0){var proof=new CompoundTag();proof.putUUID("actor",owner);proof.putUUID("generator",source.getUUID());proof.putBoolean("repaired",source.getPersistentData().hasUUID("jemCompanionRepairedV4") && source.getPersistentData().getUUID("jemCompanionRepairedV4").equals(owner));proof.put("source",nativeSource.serialize());proof.putLong("started",source.level().getGameTime());target.getPersistentData().put(PULSE,proof);target.setChanged();}
 }
 public static void relay(AbstractTeslaBlockEntity source,AbstractTeslaBlockEntity target){var proof=source.getPersistentData().getCompound(PULSE).copy();boolean valid=source.cycleCounter==5 && proof.hasUUID("actor") && proof.hasUUID("generator") && source.getOutgoing().contains(target.asConnectionTarget());target.startCycle();target.getPersistentData().remove(PULSE);
  if(valid && source.getLevel()==target.getLevel() && target.cycleCounter==0){proof.put("source",source.asConnectionTarget().serialize());proof.putLong("started",source.getLevel().getGameTime());target.getPersistentData().put(PULSE,proof);target.setChanged();}
 }
 public static boolean powered(AbstractTeslaBlockEntity node){var proof=node.getPersistentData().getCompound(PULSE);if(!(node.getLevel() instanceof ServerLevel world) || !proof.hasUUID("actor") || !proof.hasUUID("generator") || node.cycleCounter<0 || node.getDistance()==Integer.MAX_VALUE || world.getGameTime()<proof.getLong("started"))return false;
  var generator=world.getEntity(proof.getUUID("generator"));return generator instanceof DinamoEntity d && d.isAlive() && proof.getUUID("actor").equals(d.getOwnerUUID());
 }
 public static void lamp(AbstractTeslaBlockEntity node){if(!(node instanceof PlasmaLampBlockEntity) || !powered(node) || !node.isActive() || !node.getBlockState().getValue(dev.xylonity.companions.common.block.PlasmaLampBlock.LIT))return;
  var pulse=node.getPersistentData().getCompound(PULSE);if(!pulse.getBoolean("repaired"))return;var link=node.getPersistentData().getCompound(LINK);if(link.hasUUID("actor") && link.getUUID("actor").equals(pulse.getUUID("actor")) && link.getCompound("source").equals(pulse.getCompound("source")) && node.getLevel() instanceof ServerLevel world){var actor=CompanionQuestProof.actor(world,link.getUUID("actor"));if(actor!=null && outgoing(world,ConnectionTarget.deserialize(link.getCompound("source")),node.asConnectionTarget()))CompanionQuestProof.emit(actor,"v3_pet_current_flows_lamp");}
 }
}
