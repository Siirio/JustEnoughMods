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

@Mod.EventBusSubscriber(modid="jem_roles")
public final class SandboxHostileControl {
    private static final String UNTIL="JemSandboxRootUntil";
    private record Grip(java.util.UUID owner,java.util.UUID target,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,long start,int stage,int duration) {}
    private static final java.util.Map<java.util.UUID,Grip> GRIPS=new java.util.HashMap<>();
    private record PoisonHit(LivingEntity target,net.minecraft.world.damagesource.DamageSource source) {}
    private static final ThreadLocal<PoisonHit> POISON_HIT=new ThreadLocal<>();
    private SandboxHostileControl() {}
    private static boolean admitted(ServerPlayer owner, LivingEntity target) {
        return owner.isAlive() && !owner.isSpectator() && owner.level()==target.level()
                && ProductionRaceBridge.canInteract(owner,target)
                && !target.isInvulnerableTo(owner.damageSources().indirectMagic(owner,owner));
    }
    private static void bind(ServerPlayer owner, LivingEntity target) {
        if(!admitted(owner,target))return;
        long now=owner.server.overworld().getGameTime();
        boolean boss=ProductionRaceBridge.registeredBoss(target);
        if(boss && target.getPersistentData().getLong("JemSandboxRootProtectedUntil")>now)return;
        int duration=boss?20:BloomMechanics.GRIP_TICKS;
        int upgrade=MainSkillProgression.level(owner,2);
        SandboxNetwork.effect(owner,upgrade==0?34:10,2,target.position(),0,target.getId());
        if(!(target instanceof Mob mob) || !(mob instanceof Enemy || mob.isAggressive() || mob.getTarget() instanceof net.minecraft.world.entity.player.Player))return;
        var tag=mob.getPersistentData();tag.putUUID("JemSandboxRootOwner",owner.getUUID());tag.putLong(UNTIL,now+duration);
        tag.putDouble("JemRootX",mob.getX());tag.putDouble("JemRootY",mob.getY());tag.putDouble("JemRootZ",mob.getZ());
        mob.getNavigation().stop();if(upgrade>=1)mob.stopUsingItem();mob.setDeltaMovement(Vec3.ZERO);
        if(boss)tag.putLong("JemSandboxRootProtectedUntil",now+duration+160);
        GRIPS.put(mob.getUUID(),new Grip(owner.getUUID(),mob.getUUID(),owner.serverLevel().dimension(),now,upgrade,duration));
    }
    public static boolean cast(ServerPlayer owner) {
        if(!RaceExperience.canAct(owner))return false;
        int count=0;
        for(Mob mob:owner.serverLevel().getEntitiesOfClass(Mob.class,owner.getBoundingBox().inflate(10),m->m.isAlive() && (m instanceof Enemy || m.getTarget()==owner))) {
            if(!admitted(owner,mob))continue;
            if(ProductionRaceBridge.registeredBoss(mob) && mob.getPersistentData().getLong("JemSandboxRootProtectedUntil")>owner.server.overworld().getGameTime())continue;
            // Encounter admission precedes damage; native hurt rejection additionally
            // protects phase-specific immunity implemented inside a boss override.
            if(!mob.hurt(owner.damageSources().indirectMagic(owner,owner),BloomMechanics.INITIAL_DAMAGE))continue;
            count++;
            if(mob.isAlive())bind(owner,mob);
        }
        if(count==0)owner.displayClientMessage(net.minecraft.network.chat.Component.translatable("jem_visual_sandbox.action.target_required"),true);
        return count>0;
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent e) {
        if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END || !RaceExperience.serverActive(e.getServer()))return;
        long now=e.getServer().overworld().getGameTime();
        GRIPS.entrySet().removeIf(entry->{
            Grip grip=entry.getValue();var level=e.getServer().getLevel(grip.dimension);ServerPlayer owner=e.getServer().getPlayerList().getPlayer(grip.owner);
            var target=level==null?null:level.getEntity(grip.target);long age=now-grip.start;
            if(!(target instanceof LivingEntity mob))return true;
            if(owner==null || !mob.isAlive() || age<0 || !admitted(owner,mob)) { clearRoot(mob);return true; }
            if(age<grip.duration) {
                if(grip.stage>=2 && age>0 && age%20==0)mob.hurt(owner.damageSources().indirectMagic(owner,owner),BloomMechanics.SQUEEZE_DAMAGE);
                return false;
            }
            clearRoot(mob);
            long poisonAge=age-grip.duration;
            if(grip.stage<3 || poisonAge>=BloomMechanics.POISON_TICKS)return true;
            if(poisonAge>0 && poisonAge%20==0)poison(owner,mob);
            if(poisonAge%5==0)level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT,mob.getX(),mob.getY()+mob.getBbHeight()*.5,mob.getZ(),0,.25,.6,.1,1);
            return false;
        });
    }
    private static void poison(ServerPlayer owner,LivingEntity mob) {
        float before=mob.getHealth();
        if(before<=1)return;
        var source=owner.damageSources().indirectMagic(owner,owner);
        if(mob.isInvulnerableTo(source))return;
        // Native damage/cancellation, phase gates and boss effective-health
        // normalization remain authoritative. Ordinary mobs retain universal poison.
        PoisonHit previous=POISON_HIT.get();
        POISON_HIT.set(new PoisonHit(mob,source));
        try {
            if(mob.hurt(source,Math.min(BloomMechanics.POISON_DAMAGE,before-1)) && mob.isAlive() && mob.getHealth()<before
                    && !ProductionRaceBridge.registeredBoss(mob))
                mob.setHealth(Math.max(1,before-BloomMechanics.POISON_DAMAGE));
        } finally {
            if(previous==null)POISON_HIT.remove();else POISON_HIT.set(previous);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void poisonFloor(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        PoisonHit hit=POISON_HIT.get();
        if(hit!=null && event.getEntity()==hit.target && event.getSource()==hit.source)
            event.setAmount(Math.min(event.getAmount(),Math.max(0,event.getEntity().getHealth()-1)));
    }
    private static void clearRoot(Entity entity) {
        var tag=entity.getPersistentData();
        tag.remove(UNTIL);tag.remove("JemSandboxRootOwner");tag.remove("JemRootX");tag.remove("JemRootY");tag.remove("JemRootZ");
    }
    static void selectionChanged(ServerPlayer owner){
        GRIPS.values().removeIf(s->s.owner.equals(owner.getUUID()));
        for(var level:owner.server.getAllLevels())for(var entity:level.getAllEntities()){
            var tag=entity.getPersistentData();
            if(tag.hasUUID("JemSandboxRootOwner")&&tag.getUUID("JemSandboxRootOwner").equals(owner.getUUID())){
                clearRoot(entity);
            }
        }
    }
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent e) { GRIPS.clear(); }
    public static boolean rooted(Entity entity) {
        if(entity==null || entity.level().isClientSide || entity.getServer()==null || !RaceExperience.serverActive(entity.getServer()) || !entity.isAlive())return false;
        var tag=entity.getPersistentData();long until=tag.getLong(UNTIL);
        if(until<=0)return false;
        Grip grip=GRIPS.get(entity.getUUID());
        var owner=grip==null?null:entity.getServer().getPlayerList().getPlayer(grip.owner);
        if(owner!=null && entity instanceof LivingEntity living && admitted(owner,living) && entity.getServer().overworld().getGameTime()<until)return true;
        clearRoot(entity);return false;
    }
    public static boolean pause(Entity entity) {
        if(!locked(entity))return false;
        if(entity instanceof LivingEntity living && ProductionRaceBridge.registeredBoss(living))return false;
        // Native health, hurt cooldown, fire and potion clocks continue; AI and
        // subclass tick logic (fuses, spells, shots, teleport decisions) do not.
        entity.setOldPosAndRot();entity.tickCount++;entity.baseTick();
        pin(entity);
        return true;
    }
    public static void pin(Entity entity) {
        if(!rooted(entity))return;
        var tag=entity.getPersistentData();entity.setDeltaMovement(Vec3.ZERO);
        entity.setPos(tag.getDouble("JemRootX"),tag.getDouble("JemRootY"),tag.getDouble("JemRootZ"));
    }
    private static boolean locked(Entity entity) {
        if(!rooted(entity))return false;
        Grip grip=GRIPS.get(entity.getUUID());
        return grip!=null && grip.stage>=1;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent e) { if(locked(e.getSource().getEntity()))e.setCanceled(true); }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void teleport(EntityTeleportEvent e) { if(rooted(e.getEntity()) && e.isCancelable())e.setCanceled(true); }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void dimension(EntityTravelToDimensionEvent e) { if(rooted(e.getEntity()))e.setCanceled(true); }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void projectile(EntityJoinLevelEvent e) { if(e.getEntity() instanceof Projectile p && locked(p.getOwner()))e.setCanceled(true); }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void explosion(ExplosionEvent.Start e) { if(locked(e.getExplosion().getDirectSourceEntity()))e.setCanceled(true); }
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
