package com.siirio.jemvisualsandbox.workbench;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="jem_visual_sandbox")
public final class SandboxHostileControl {
    private static final String UNTIL="JemSandboxRootUntil";
    private record Squeeze(java.util.UUID owner,java.util.UUID target,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,long start) {}
    private static final java.util.Map<java.util.UUID,Squeeze> SQUEEZES=new java.util.HashMap<>();
    private SandboxHostileControl() {}
    public static void bind(ServerPlayer owner, LivingEntity target) {
        int upgrade=MainSkillProgression.level(owner,2);
        SandboxNetwork.effect(owner,upgrade==0?34:10,2,target.position(),0,target.getId());
        if(upgrade==0)return;
        if(!(target instanceof Mob mob) || !(mob instanceof Enemy || mob.isAggressive() || mob.getTarget() instanceof net.minecraft.world.entity.player.Player))return;
        var tag=mob.getPersistentData();tag.putLong(UNTIL,owner.server.overworld().getGameTime()+90);
        tag.putDouble("JemRootX",mob.getX());tag.putDouble("JemRootY",mob.getY());tag.putDouble("JemRootZ",mob.getZ());
        mob.getNavigation().stop();mob.stopUsingItem();mob.setDeltaMovement(Vec3.ZERO);
        if(upgrade>=2)SQUEEZES.put(mob.getUUID(),new Squeeze(owner.getUUID(),mob.getUUID(),owner.serverLevel().dimension(),owner.server.overworld().getGameTime()));
        if(upgrade>=3)mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,90,0));
    }
    public static void cast(ServerPlayer owner) {
        if(owner.server.isDedicatedServer() || !owner.server.isSingleplayerOwner(owner.getGameProfile()) || !owner.isAlive() || owner.isSpectator())return;
        for(Mob mob:owner.serverLevel().getEntitiesOfClass(Mob.class,owner.getBoundingBox().inflate(10),m->m.isAlive() && (m instanceof Enemy || m.getTarget()==owner))) {
            mob.hurt(owner.damageSources().playerAttack(owner),6);if(mob.isAlive())bind(owner,mob);
        }
    }
    @SubscribeEvent public static void squeeze(net.minecraftforge.event.TickEvent.ServerTickEvent e) {
        if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END || e.getServer().isDedicatedServer())return;
        long now=e.getServer().overworld().getGameTime();
        SQUEEZES.entrySet().removeIf(entry->{
            Squeeze s=entry.getValue();var level=e.getServer().getLevel(s.dimension);ServerPlayer owner=e.getServer().getPlayerList().getPlayer(s.owner);
            var target=level==null?null:level.getEntity(s.target);long age=now-s.start;
            if(owner==null || !(target instanceof LivingEntity mob) || !mob.isAlive() || age<0 || age>=90 || !rooted(mob))return true;
            if(age>0 && age%20==0)mob.hurt(owner.damageSources().magic(),3);
            return false;
        });
    }
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent e) { SQUEEZES.clear(); }
    public static boolean rooted(Entity entity) {
        if(entity==null || entity.level().isClientSide || entity.getServer()==null || entity.getServer().isDedicatedServer() || !entity.isAlive())return false;
        var tag=entity.getPersistentData();long until=tag.getLong(UNTIL);
        if(until<=0)return false;
        if(entity.getServer().overworld().getGameTime()<until)return true;
        tag.remove(UNTIL);tag.remove("JemRootX");tag.remove("JemRootY");tag.remove("JemRootZ");return false;
    }
    public static boolean pause(Entity entity) {
        if(!rooted(entity))return false;
        // Native health, hurt cooldown, fire and potion clocks continue; AI and
        // subclass tick logic (fuses, spells, shots, teleport decisions) do not.
        entity.setOldPosAndRot();entity.tickCount++;entity.baseTick();
        var tag=entity.getPersistentData();entity.setDeltaMovement(Vec3.ZERO);
        entity.setPos(tag.getDouble("JemRootX"),tag.getDouble("JemRootY"),tag.getDouble("JemRootZ"));
        return true;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent e) { if(rooted(e.getSource().getEntity()))e.setCanceled(true); }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void teleport(EntityTeleportEvent e) { if(rooted(e.getEntity()) && e.isCancelable())e.setCanceled(true); }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void dimension(EntityTravelToDimensionEvent e) { if(rooted(e.getEntity()))e.setCanceled(true); }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void projectile(EntityJoinLevelEvent e) { if(e.getEntity() instanceof Projectile p && rooted(p.getOwner()))e.setCanceled(true); }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void explosion(ExplosionEvent.Start e) { if(rooted(e.getExplosion().getDirectSourceEntity()))e.setCanceled(true); }
    public static void refreshTargets(ServerPlayer player) {
        for(Mob mob:player.serverLevel().getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(40),Mob::isAlive)) {
            var old=mob.getTarget();
            if(old!=player && (old==null || !old.getPersistentData().contains("JemVoidEchoExpiry")))continue;
            mob.getNavigation().stop();mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            mob.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            mob.setTarget(player);mob.getLookControl().setLookAt(player,180,180);
            if(!rooted(mob))mob.getNavigation().moveTo(player,1.0);
            if(old!=player && old!=null)old.discard();
        }
    }
}
