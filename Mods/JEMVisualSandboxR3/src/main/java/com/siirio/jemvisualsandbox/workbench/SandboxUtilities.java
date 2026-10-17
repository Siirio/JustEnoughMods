package com.siirio.jemvisualsandbox.workbench;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Standalone adapters for the exact JEMRoles utility contracts; no race/account dependency. */
@Mod.EventBusSubscriber(modid="jem_visual_sandbox")
public final class SandboxUtilities {
    private static final UUID GRAVITY=UUID.fromString("8155c170-e974-4b3e-b247-dccfc767f443");
    private record Charge(long start,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {}
    private static final Map<UUID,Charge> CHARGES=new HashMap<>();
    private SandboxUtilities() {}
    private static boolean allowed(ServerPlayer p) { return RaceExperience.allowed(p) && RaceExperience.selected(p) && p.isAlive() && !p.isSpectator(); }
    private static int race(ServerPlayer p) { return p.getPersistentData().getInt("JemSandboxRace"); }
    private static void cue(ServerPlayer p,int action) { SandboxNetwork.effect(p,action,race(p),p.position(),p.getYRot(),p.getId()); }
    public static boolean tryLavaDrink(net.minecraft.world.entity.player.Player player,InteractionHand hand) {
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null || !RaceExperience.serverActive(server) || player.isShiftKeyDown() || !player.getItemInHand(hand).is(Items.LAVA_BUCKET) || player.getPersistentData().getInt("JemSandboxRace")!=4)return false;
        if(player instanceof ServerPlayer p && (!RaceExperience.canAct(p)||!RaceExperience.requireReady(p,"lava_drink")||p.isUsingItem()))return false;
        player.startUsingItem(hand);player.getPersistentData().putBoolean("JemSandboxDrinking",true);return true;
    }
    public static void press(ServerPlayer p) {
        if(!allowed(p))return;
        switch(race(p)) {
            case 0 -> p.displayClientMessage(Component.literal("Guardian activates below 40% health; V: Life Chorus"),true);
            case 1 -> {
                if(p.isShiftKeyDown()) {
                    if(!RaceExperience.requireReady(p,"gravity"))return;
                    var gravity=p.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
                    if(gravity==null)return;
                    gravity.removeModifier(GRAVITY);gravity.addTransientModifier(new AttributeModifier(GRAVITY,"Sandbox gravity denial",-.75,AttributeModifier.Operation.MULTIPLY_TOTAL));
                    p.getPersistentData().putLong("JemSandboxGravity",p.server.overworld().getGameTime()+RacePowerStages.gravityTicks(RaceExperience.tier(p)));cue(p,22);
                    RaceExperience.cooldown(p,"gravity",SandboxRaceRules.GRAVITY_COOLDOWN.get());RaceExperience.finishUtility(p);
                } else if(RaceExperience.requireReady(p,"void_projectile"))CHARGES.putIfAbsent(p.getUUID(),new Charge(p.level().getGameTime(),p.serverLevel().dimension()));
            }
            case 2 -> {
                boolean on=!p.getPersistentData().getBoolean("JemSandboxLivingPath");if(on&&!RaceExperience.requireReady(p,"leaf_bridge"))return;
                p.getPersistentData().putBoolean("JemSandboxLivingPath",on);p.getPersistentData().remove("JemSandboxLivingPathAwarded");if(on)RaceExperience.cooldown(p,"leaf_bridge",300);
                p.displayClientMessage(Component.literal("Living Path: "+(on?"ON":"OFF")),true);if(on)cue(p,23);
            }
            case 3 -> {
                int resonance=p.getPersistentData().getInt("JemSandboxResonance");
                if(resonance>=25) { p.getPersistentData().putInt("JemSandboxResonance",0);p.heal(RacePowerStages.recoveryHeal(RaceExperience.tier(p)));RaceOwnedEffects.grant(p,new MobEffectInstance(MobEffects.ABSORPTION,RacePowerStages.absorptionTicks(RaceExperience.tier(p)),1));cue(p,25);RaceExperience.finishUtility(p); }
                else if(RaceExperience.requireReady(p,"echo")) {
                    if(p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,p.getBoundingBox().inflate(RacePowerStages.echoRadius(RaceExperience.tier(p))),e->e!=p&&e.isAlive()).isEmpty()){p.displayClientMessage(Component.literal("No living creatures nearby"),true);return;}
                    SandboxSkills.echoVision(p);RaceExperience.cooldown(p,"echo",160);RaceExperience.finishUtility(p);
                }
            }
            case 4 -> {if(RaceExperience.requireReady(p,"fire_slash")&&FierySlash.cast(p)){RaceExperience.cooldown(p,"fire_slash",1200);RaceExperience.finishUtility(p);}}
            default -> { }
        }
    }
    public static void release(ServerPlayer p) {
        Charge charge=CHARGES.remove(p.getUUID());
        if(charge==null || !RaceExperience.canAct(p) || race(p)!=1 || charge.dimension!=p.serverLevel().dimension() || !RaceExperience.requireReady(p,"void_projectile"))return;
        if(p.level().getGameTime()-charge.start<10) { p.displayClientMessage(Component.literal("Hold И/B for at least half a second"),true);return; }
        ThrownEnderpearl pearl=new ThrownEnderpearl(p.level(),p);pearl.getPersistentData().putBoolean("JemSandboxVoidProjectile",true);ProjectileEstimation.launch(pearl,p);
        if(p.level().addFreshEntity(pearl)){RaceExperience.cooldown(p,"void_projectile",SandboxRaceRules.VOID_PROJECTILE_COOLDOWN.get());SandboxNetwork.effect(p,28,1,pearl.position(),p.getYRot(),pearl.getId());}
    }
    public static void selectionChanged(ServerPlayer p) {
        var state=p.getPersistentData();
        if(state.hasUUID("JemSandboxGuardian"))for(var level:p.server.getAllLevels())
            if(level.getEntity(state.getUUID("JemSandboxGuardian")) instanceof IronGolem guardian)guardian.discard();
        state.remove("JemSandboxGuardian");state.remove("JemSandboxGuardianTriggered");
        for(var level:p.server.getAllLevels())for(var entity:level.getAllEntities())
            if(entity instanceof ThrownEnderpearl pearl&&pearl.getOwner()==p&&pearl.getPersistentData().getBoolean("JemSandboxVoidProjectile"))pearl.discard();
        RaceOwnedEffects.clear(p);
        p.getPersistentData().remove("JemSandboxResonance");
        CHARGES.remove(p.getUUID());if(p.getPersistentData().getBoolean("JemSandboxDrinking"))p.stopUsingItem();p.getPersistentData().remove("JemSandboxDrinking");p.getPersistentData().remove("JemSandboxLivingPath");p.getPersistentData().remove("JemSandboxLivingPathAwarded");p.getPersistentData().remove("JemSandboxGravity");
        var gravity=p.getAttribute(ForgeMod.ENTITY_GRAVITY.get());if(gravity!=null)gravity.removeModifier(GRAVITY);
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || !allowed(p))return;
        long now=p.server.overworld().getGameTime();
        if(p.getPersistentData().getLong("JemSandboxGravity")<=now) { var gravity=p.getAttribute(ForgeMod.ENTITY_GRAVITY.get());if(gravity!=null)gravity.removeModifier(GRAVITY); }
        else if(p.tickCount%20==0)cue(p,22);
        Charge charge=CHARGES.get(p.getUUID());
        if(charge!=null && (race(p)!=1 || !RaceExperience.canAct(p) || charge.dimension!=p.serverLevel().dimension()))CHARGES.remove(p.getUUID());
        if(race(p)==2 && p.getPersistentData().getBoolean("JemSandboxLivingPath") && p.tickCount%4==0)livingPath(p);
        if(race(p)==0) {
            if(p.getHealth()>=p.getMaxHealth()*.4F)p.getPersistentData().remove("JemSandboxGuardianTriggered");
            else if(!p.getPersistentData().getBoolean("JemSandboxGuardianTriggered"))guardian(p);
        }
        if(p.tickCount%20==0)SandboxPathData.get(p.server).tick(p.server);
    }
    @SubscribeEvent public static void pearl(ProjectileImpactEvent e) {
        if(!(e.getProjectile() instanceof ThrownEnderpearl pearl) || !pearl.getPersistentData().getBoolean("JemSandboxVoidProjectile") || !(pearl.getOwner() instanceof ServerPlayer p) || !RaceExperience.serverActive(p.server))return;
        e.setCanceled(true);Vec3 hit=e.getRayTraceResult().getLocation();
        SandboxNetwork.effect(p,29,1,hit,0,-1);
        if(allowed(p) && race(p)==1) {
            BlockPos safe=SandboxEnvironment.nearestSafe(p,p.serverLevel(),BlockPos.containing(hit),5,true).orElse(null);
            if(safe!=null) { p.teleportTo(p.serverLevel(),safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),p.getXRot());SandboxHostileControl.refreshTargets(p);RaceExperience.finishUtility(p); }
            else p.displayClientMessage(Component.literal("Void pearl: no safe destination"),true);
        }
        pearl.discard();
    }
    private static void livingPath(ServerPlayer p) {
        Vec3 direction=p.getDeltaMovement().multiply(1,0,1);if(direction.lengthSqr()<.0025)direction=p.getLookAngle().multiply(1,0,1);
        direction=direction.normalize();Vec3 side=new Vec3(-direction.z,0,direction.x);
        for(int forward=0;forward<=RacePowerStages.bridgeForward(RaceExperience.tier(p));forward++)for(int lateral=-1;lateral<=1;lateral++) {
            Vec3 point=p.position().add(direction.scale(forward)).add(side.scale(lateral));BlockPos below=BlockPos.containing(point.x,p.getY(),point.z).below();
            if(!p.serverLevel().hasChunkAt(below) || !p.serverLevel().getWorldBorder().isWithinBounds(below) || !p.level().getBlockState(below).isAir() || !p.level().getFluidState(below).isEmpty())continue;
            if(!p.level().setBlock(below,Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),3))continue;
            SandboxPathData.get(p.server).add(p.level().dimension(),below,p.server.overworld().getGameTime()+RacePowerStages.bridgeTicks(RaceExperience.tier(p)));
            SandboxNetwork.effect(p,23,2,Vec3.atBottomCenterOf(below.above()).add(0,.04,0),0,-1);
            if(!p.getPersistentData().getBoolean("JemSandboxLivingPathAwarded")){
                p.getPersistentData().putBoolean("JemSandboxLivingPathAwarded",true);RaceExperience.finishUtility(p);
            }
        }
    }
    private static void guardian(ServerPlayer p) {
        if(!RaceExperience.ready(p,"guardian"))return;
        var tag=p.getPersistentData();
        if(tag.hasUUID("JemSandboxGuardian") && p.serverLevel().getEntity(tag.getUUID("JemSandboxGuardian")) instanceof IronGolem g && g.isAlive())return;
        IronGolem golem=new IronGolem(EntityType.IRON_GOLEM,p.level());
        boolean placed=false;
        for(int radius=1;radius<=3 && !placed;radius++)for(int dy:new int[]{0,1,-1}) {
            for(int[] offset:new int[][]{{radius,0},{-radius,0},{0,radius},{0,-radius},{radius,radius},{-radius,radius},{radius,-radius},{-radius,-radius}}) {
                BlockPos at=p.blockPosition().offset(offset[0],dy,offset[1]);
                if(!p.serverLevel().hasChunkAt(at) || !p.serverLevel().getWorldBorder().isWithinBounds(at) || p.level().getBlockState(at.below()).getCollisionShape(p.level(),at.below()).isEmpty())continue;
                golem.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),0);
                if(p.serverLevel().noCollision(golem)) { placed=true;break; }
            }
            if(placed)break;
        }
        if(!placed)return;
        golem.setPlayerCreated(true);golem.setInvulnerable(true);golem.setCustomName(Component.literal("Ancestral Guardian"));
        golem.getPersistentData().putLong("JemSandboxGuardianExpiry",p.server.overworld().getGameTime()+RacePowerStages.guardianTicks(RaceExperience.tier(p)));
        golem.getPersistentData().putUUID("JemSandboxGuardianOwner",p.getUUID());
        if(!p.level().addFreshEntity(golem))return;
        RaceExperience.cooldown(p,"guardian",SandboxRaceRules.GUARDIAN_COOLDOWN.get());
        RaceExperience.finishUtility(p);LifeChorusAbility.guardianEntry(p);
        tag.putUUID("JemSandboxGuardian",golem.getUUID());tag.putBoolean("JemSandboxGuardianTriggered",true);
        RaceOwnedEffects.grant(p,new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,30,4));
        Mob threat=p.serverLevel().getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(16),m->m instanceof Enemy && m.isAlive()).stream().min(java.util.Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        Vec3 direction=threat==null?p.getLookAngle().multiply(1,0,1).normalize():threat.position().subtract(p.position()).multiply(1,0,1).normalize();
        float yaw=(float)Math.toDegrees(Math.atan2(direction.x,direction.z));
        SandboxNetwork.visual(p,0,p.position().add(direction.scale(1.3)),yaw+180);
        SandboxNetwork.effect(p,27,0,golem.position(),0,golem.getId());if(threat!=null)golem.setTarget(threat);
    }
    @SubscribeEvent public static void golem(LivingEvent.LivingTickEvent e) {
        if(!(e.getEntity() instanceof IronGolem g) || g.level().isClientSide || g.getServer()==null || !RaceExperience.serverActive(g.getServer()) || !g.getPersistentData().contains("JemSandboxGuardianExpiry"))return;
        if(g.getServer().overworld().getGameTime()>=g.getPersistentData().getLong("JemSandboxGuardianExpiry") || g.getServer().getPlayerList().getPlayer(g.getPersistentData().getUUID("JemSandboxGuardianOwner"))==null) { g.discard();return; }
        if(g.tickCount%20==0)g.level().getEntitiesOfClass(Mob.class,g.getBoundingBox().inflate(16),m->m instanceof Enemy && m.isAlive()).stream().min(java.util.Comparator.comparingDouble(g::distanceToSqr)).ifPresent(g::setTarget);
    }
    @SubscribeEvent public static void lava(LivingEntityUseItemEvent.Finish e) {
        if(!(e.getEntity() instanceof ServerPlayer p) || !allowed(p) || race(p)!=4 || !p.getPersistentData().getBoolean("JemSandboxDrinking") || !e.getItem().is(Items.LAVA_BUCKET))return;
        if(!RaceExperience.ready(p,"lava_drink")){p.getPersistentData().remove("JemSandboxDrinking");return;}
        p.getPersistentData().remove("JemSandboxDrinking");e.setResultStack(new ItemStack(Items.BUCKET));p.heal(5);
        RaceExperience.cooldown(p,"lava_drink",SandboxRaceRules.LAVA_DRINK_COOLDOWN.get());RaceExperience.snapshot(p);
        p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_DRINK,net.minecraft.sounds.SoundSource.PLAYERS,.8F,.8F);
    }
    @SubscribeEvent public static void death(LivingDeathEvent e) {
        if(e.getSource().getEntity() instanceof ServerPlayer p && allowed(p) && race(p)==3 && e.getEntity() instanceof Enemy) {
            int resonance=Math.min(100,p.getPersistentData().getInt("JemSandboxResonance")+10);p.getPersistentData().putInt("JemSandboxResonance",resonance);
            p.displayClientMessage(Component.literal("Resonance: "+resonance+" / И: consume25 or reveal"),true);cue(p,24);
        }
        if(e.getEntity() instanceof ServerPlayer p && RaceExperience.serverActive(p.server))selectionChanged(p);
    }
    @SubscribeEvent public static void drops(LivingDropsEvent e) { if(e.getEntity().getPersistentData().contains("JemSandboxGuardianExpiry"))e.getDrops().clear(); }
    @SubscribeEvent public static void pathBreak(BlockEvent.BreakEvent e) {
        if(e.getLevel() instanceof net.minecraft.server.level.ServerLevel level && RaceExperience.serverActive(level.getServer())
                && e.getState().is(Blocks.OAK_LEAVES) && SandboxPathData.get(level.getServer()).contains(level.dimension(),e.getPos()))e.setCanceled(true);
    }
    @SubscribeEvent public static void xp(LivingExperienceDropEvent e) { if(e.getEntity().getPersistentData().contains("JemSandboxGuardianExpiry"))e.setDroppedExperience(0); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { if(e.getEntity() instanceof ServerPlayer p && RaceExperience.serverActive(p.server))selectionChanged(p); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if(e.getEntity() instanceof ServerPlayer p && RaceExperience.serverActive(p.server))selectionChanged(p); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { CHARGES.clear(); }
}
