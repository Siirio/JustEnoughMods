package com.justenoughmods.achievementguide.integration.companions;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
public final class CompanionQuestSpell {
 public record Cast(ServerPlayer actor,String item,UUID run,boolean mage,boolean blood,java.util.List<Projectile> created){}
 public record Damage(LivingEntity target,net.minecraft.world.damagesource.DamageSource source,float vitality){}
 private static final class Hits {final java.util.ArrayDeque<Damage> calls=new java.util.ArrayDeque<>();int overflow;}
 private static final ThreadLocal<Hits> HITS=ThreadLocal.withInitial(Hits::new);
 public static void clear(){HITS.remove();CURRENT.remove();}
 public static void damageBegin(LivingEntity target,net.minecraft.world.damagesource.DamageSource source){if(!(target.level() instanceof net.minecraft.server.level.ServerLevel))return;var hits=HITS.get();if(hits.overflow>0||hits.calls.size()>=32){hits.overflow++;return;}hits.calls.push(new Damage(target,source,target.getHealth()+target.getAbsorptionAmount()));}
 public static void damageEnd(LivingEntity target,boolean accepted){if(!(target.level() instanceof net.minecraft.server.level.ServerLevel))return;var hits=HITS.get();if(hits.overflow>0){hits.overflow--;return;}var hit=hits.calls.poll();if(hits.calls.isEmpty())HITS.remove();if(hit!=null&&hit.target()==target&&accepted&&Float.isFinite(hit.vitality())&&Float.isFinite(target.getHealth()+target.getAbsorptionAmount())&&target.getHealth()+target.getAbsorptionAmount()<hit.vitality()&&hit.source().getDirectEntity()!=null)combat(hit.source().getDirectEntity(),target);}
 private static final ThreadLocal<Cast> CURRENT=new ThreadLocal<>();public static final String KEY="jemCompanionSpellV4";private CompanionQuestSpell(){}
 public static InteractionResultHolder<ItemStack> use(Item item,Level level,Player user,InteractionHand hand){
  var p=CompanionQuestProof.actor(user);boolean book=item instanceof dev.xylonity.companions.common.item.book.AbstractMagicBook;boolean blood=item instanceof dev.xylonity.companions.common.item.weapon.BloodSwordItem || item instanceof dev.xylonity.companions.common.item.weapon.BloodAxeItem;
  var previous=CURRENT.get();if(p!=null && (book || blood) && !p.getCooldowns().isOnCooldown(item))CURRENT.set(new Cast(p,CompanionQuestProof.id(p.getItemInHand(hand)),UUID.randomUUID(),book && CompanionQuestProof.mage(p),blood,new java.util.ArrayList<>()));else CURRENT.remove();
  try{var result=item.use(level,user,hand);var cast=CURRENT.get();if(cast!=null){boolean committed=!cast.blood()||result!=null&&result.getResult().consumesAction();boolean bloodOutput=false;for(var projectile:cast.created()){var proof=projectile.getPersistentData().getCompound(KEY);proof.putBoolean("committed",committed);if(committed&&!projectile.isRemoved()&&projectile.getOwner()==cast.actor()&&cast.actor().serverLevel().getEntity(projectile.getUUID())==projectile){proof.putBoolean("accepted",true);bloodOutput|=cast.blood();}}if(bloodOutput)CompanionQuestProof.emit(cast.actor(),"v3_pet_blood_and_halo");}return result;}finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
 }
 public static void owner(Projectile projectile,Entity owner){var cast=CURRENT.get();if(cast==null || owner!=cast.actor() || projectile.getOwner()!=owner || !projectile.getType().builtInRegistryHolder().key().location().getNamespace().equals("companions"))return;
  var proof=new CompoundTag();proof.putUUID("actor",owner.getUUID());proof.putUUID("run",cast.run());proof.putString("item",cast.item());proof.putBoolean("mage",cast.mage());proof.putBoolean("blood",cast.blood());projectile.getPersistentData().put(KEY,proof);if(cast.created().size()<128)cast.created().add(projectile);}
 public static void accepted(Entity e,boolean result){if(!result || !(e instanceof Projectile projectile)||e.isRemoved()||!(e.level() instanceof net.minecraft.server.level.ServerLevel level)||level.getEntity(e.getUUID())!=e)return;var proof=e.getPersistentData().getCompound(KEY);var p=CompanionQuestProof.actor(projectile.getOwner());
  if(p!=null && proof.hasUUID("actor") && proof.getUUID("actor").equals(p.getUUID())){if(proof.getBoolean("committed"))proof.putBoolean("accepted",true);}}
 public static void child(Entity parent,Entity child,boolean result){if(!result || child.isRemoved() || !(child.level() instanceof net.minecraft.server.level.ServerLevel level) || level.getEntity(child.getUUID())!=child || !(parent instanceof Projectile from) || !(child instanceof Projectile to) || from.getOwner()!=to.getOwner())return;var proof=parent.getPersistentData().getCompound(KEY);if(proof.getBoolean("accepted") && proof.hasUUID("run")){var next=proof.copy();next.putUUID("parent",parent.getUUID());next.putBoolean("accepted",true);child.getPersistentData().put(KEY,next);}}
 public static ServerPlayer actor(Entity projectile){if(!(projectile instanceof Projectile nativeProjectile))return null;var proof=projectile.getPersistentData().getCompound(KEY);var p=CompanionQuestProof.actor(nativeProjectile.getOwner());return p!=null && p.level()==projectile.level() && p.serverLevel().getServer().getPlayerList().getPlayer(p.getUUID())==p && proof.hasUUID("actor") && proof.getUUID("actor").equals(p.getUUID()) && proof.getBoolean("accepted")?p:null;}
 public static void combat(Entity projectile,Entity target){var p=actor(projectile);var proof=projectile.getPersistentData().getCompound(KEY);if(p!=null && proof.getBoolean("mage") && !proof.getString("item").equals("companions:book_heal_ring") && CompanionQuestProof.mage(p) && CompanionQuestProof.hostile(target,p))CompanionQuestProof.emit(p,"v3_pet_book_and_robes");}
 public static void support(Entity projectile,LivingEntity target){var p=actor(projectile);var proof=projectile.getPersistentData().getCompound(KEY);boolean ally=p!=null && (target==p || target.isAlliedTo(p) || target instanceof OwnableEntity owned && p.getUUID().equals(owned.getOwnerUUID()));
  if(ally && proof.getBoolean("mage") && CompanionQuestProof.mage(p) && proof.getString("item").equals("companions:book_heal_ring"))CompanionQuestProof.emit(p,"v3_pet_book_and_robes_support");}
}
