package com.justenoughmods.achievementguide.integration.companions;
import dev.xylonity.companions.common.entity.companion.PuppetEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.nbt.CompoundTag;
public final class CompanionQuestPuppetRun {
 public record Run(PuppetEntity puppet,ServerPlayer actor,LivingEntity target){}
 private static final ThreadLocal<Run> CURRENT=new ThreadLocal<>();private CompanionQuestPuppetRun(){}
 public static void observe(PuppetEntity puppet,LivingEntity target,int slot,Runnable nativeAttack){
  var p=CompanionQuestProof.actor(puppet.getOwner());var tag=puppet.getPersistentData().getCompound("jemCompanionArmV4"+slot);boolean valid=p!=null && CompanionQuestProof.owned(puppet,p) && CompanionQuestProof.hostile(target,p) && tag.hasUUID("actor") && tag.getUUID("actor").equals(p.getUUID()) && tag.getCompound("item").equals(puppet.inventory.getItem(slot).save(new CompoundTag()));
  float health=target==null?Float.NaN:target.getHealth();var effects=target==null?java.util.Map.<net.minecraft.world.effect.MobEffect,net.minecraft.world.effect.MobEffectInstance>of():new java.util.HashMap<>(target.getActiveEffectsMap());var velocity=target==null?net.minecraft.world.phys.Vec3.ZERO:target.getDeltaMovement();
  var previous=CURRENT.get();if(valid)CURRENT.set(new Run(puppet,p,target));else CURRENT.remove();
  try{nativeAttack.run();}finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
  if(valid && (Float.isFinite(health) && target.getHealth()<health || !effects.equals(target.getActiveEffectsMap()) || target.getDeltaMovement().distanceToSqr(velocity)>1.0e-8))CompanionQuestProof.emit(p,"v3_pet_a_hand_in_action");
 }
 public static void accepted(Entity e,boolean accepted){var run=CURRENT.get();if(accepted && run!=null && e instanceof Projectile projectile && projectile.getOwner()==run.puppet() && CompanionQuestProof.hostile(run.target(),run.actor()))CompanionQuestProof.emit(run.actor(),"v3_pet_a_hand_in_action");}
}
