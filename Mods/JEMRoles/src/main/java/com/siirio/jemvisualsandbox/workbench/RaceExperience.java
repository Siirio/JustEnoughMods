package com.siirio.jemvisualsandbox.workbench;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Final race mechanics and scene lifecycle over the canonical RaceService account record. */
@Mod.EventBusSubscriber(modid="jem_roles")
public final class RaceExperience {
    public record Status(int race,boolean selected,int[] tiers,int[] progress,long signatureRemaining,
                         long secondaryRemaining,boolean developerMode,int bloomUpgrade,
                         long reselectRemaining,boolean testProfile,UUID nonce,boolean healingPenalty) {
        public Status { tiers=tiers.clone();progress=progress.clone(); }
        @Override public int[] tiers(){return tiers.clone();}
        @Override public int[] progress(){return progress.clone();}
    }
    private record Scene(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,Vec3 anchor,long expires,boolean completing) {}
    private static final Map<UUID,Scene> SCENES=new HashMap<>();
    private RaceExperience() {}
    public static boolean allowed(ServerPlayer p){return ProductionRaceBridge.authenticated(p);}
    public static boolean serverActive(MinecraftServer server){return server!=null;}
    public static boolean selected(ServerPlayer p){return allowed(p) && data(p).getBoolean("Selected");}
    public static boolean developerMode(ServerPlayer p){return RaceDevelopment.enabled(p);}
    public static int race(ServerPlayer p){return selected(p)?data(p).getInt("Race"):-1;}
    public static int tier(ServerPlayer p){return RaceDevelopment.tier(p,Math.max(0,race(p)),tierFor(uses(p,Math.max(0,race(p)))));}
    private static int tierFor(int uses){return RacePowerStages.stageForUses(uses);}
    private static int uses(ServerPlayer p,int race){return Math.max(0,Math.min(RacePowerStages.requiredUses(3),data(p).getInt("Uses"+race)));}
    static CompoundTag data(ServerPlayer p){return ProductionRaceBridge.data(p);}
    static void dirty(ServerPlayer p){ProductionRaceBridge.dirty(p);}
    private static String cooldownKey(ServerPlayer p,String key){return "Cooldown"+Math.max(0,race(p))+"_"+key;}
    public static long remaining(ServerPlayer p,String key){return RaceDevelopment.cooldownsDisabled(p)?0:Math.max(0,data(p).getLong(cooldownKey(p,key))-p.server.overworld().getGameTime());}
    public static boolean ready(ServerPlayer p,String key){return remaining(p,key)==0;}
    public static void cooldown(ServerPlayer p,String key,int ticks){
        if(!allowed(p)||!selected(p)||RaceDevelopment.cooldownsDisabled(p))return;
        data(p).putLong(cooldownKey(p,key),p.server.overworld().getGameTime()+Math.max(0,ticks));dirty(p);
    }
    public static int signatureCooldown(int race){return switch(race){case 0->com.siirio.jemroles.race.RaceRules.LIFE_CHORUS_COOLDOWN.get();case 1->com.siirio.jemroles.race.RaceRules.RIFT_AMBUSH_COOLDOWN.get();case 2->com.siirio.jemroles.race.RaceRules.VERDANT_DOMINION_COOLDOWN.get();case 3->com.siirio.jemroles.race.RaceRules.SCULK_VEINS_COOLDOWN.get();case 4->1200;default->0;};}
    public static int bloomUpgrade(ServerPlayer p){return Math.min(tier(p),RaceDevelopment.bloomUpgrade(p,RacePowerStages.clamp(data(p).getInt("BloomUpgrade"))));}
    public static void upgrade(ServerPlayer p,int next){
        if(!allowed(p)||race(p)!=2||next!=bloomUpgrade(p)+1||next>tier(p)||next>3)return;
        RaceDevelopment.storeBloomUpgrade(p,next);dirty(p);
        snapshot(p);
    }
    public static void choose(ServerPlayer p,int next,UUID nonce){
        if(next<0||next>4||!allowed(p)||!p.isAlive()||p.isSpectator()||nonce==null)return;
        String result=ProductionRaceBridge.choose(p,next,nonce);
        if(result.equals("CHOSEN")||result.equals("CHANGED")||result.equals("UNCHANGED"))completeSelectionScene(p);
        mirror(p);SandboxRaceTraits.refresh(p);snapshot(p);
    }
    public static void selectionChanged(ServerPlayer p){
        SandboxUtilities.selectionChanged(p);SandboxRaceTraits.clear(p);
        ElementalAbilities.selectionChanged(p);LifeChorusAbility.selectionChanged(p);
        SonicWaveAbility.selectionChanged(p);FierySlash.selectionChanged(p);
        SandboxHostileControl.selectionChanged(p);
    }
    private static void completeSelectionScene(ServerPlayer p){
        Scene scene=SCENES.get(p.getUUID());
        boolean confirmed=selected(p);
        // Selection acknowledgements may repeat; they must never extend protection.
        if(scene!=null&&confirmed&&!scene.completing)SCENES.put(p.getUUID(),new Scene(scene.dimension,scene.anchor,p.server.overworld().getGameTime()+100,true));
    }
    private static void mirror(ServerPlayer p){p.getPersistentData().putInt("JemSandboxRace",race(p));}
    public static boolean cast(ServerPlayer p){
        if(!canAct(p)||!requireReady(p,"signature"))return false;
        boolean success=switch(race(p)){
            case 0->LifeChorusAbility.cast(p);
            case 1->ElementalAbilities.rift(p);
            case 2->SandboxHostileControl.cast(p);
            case 3->SonicWaveAbility.cast(p);
            case 4->ElementalAbilities.eruption(p);
            default->false;
        };
        // Rift resolves against the live target at its impact marker, not at windup.
        if(success&&race(p)!=1)finishSignature(p);
        return success;
    }
    static void finishSignature(ServerPlayer p){
        if(!allowed(p)||!selected(p))return;
        cooldown(p,"signature",signatureCooldown(race(p)));
        finishUtility(p);
    }
    /** Award only after a real utility result, never input, preview or charge windup. */
    static void finishUtility(ServerPlayer p){
        if(!allowed(p)||!selected(p))return;
        data(p).putInt("Uses"+race(p),Math.min(RacePowerStages.requiredUses(3),uses(p,race(p))+1));dirty(p);
        snapshot(p);
    }
    public static boolean requireReady(ServerPlayer p,String key){
        long time=remaining(p,key);if(time==0)return true;
        p.displayClientMessage(Component.translatable("jem.sandbox.cooldown",com.siirio.jemidentity.api.MinecraftDays.format(time)),true);snapshot(p);return false;
    }
    public static boolean canAct(ServerPlayer p){return allowed(p)&&selected(p)&&p.isAlive()&&!p.isSpectator()&&!locked(p);}
    public static void utility(ServerPlayer p,boolean pressed){
        if(!canAct(p))return;if(pressed)SandboxUtilities.press(p);else SandboxUtilities.release(p);snapshot(p);
    }
    public static void cinematic(ServerPlayer p,boolean enabled){
        if(!allowed(p))return;
        if(enabled){
            // Reopening the progression menu must not grant gameplay immunity.
            if(selected(p))return;
            SCENES.putIfAbsent(p.getUUID(),new Scene(p.serverLevel().dimension(),p.position(),p.server.overworld().getGameTime()+600,false));
        }
        else SCENES.remove(p.getUUID());
    }
    private static boolean locked(ServerPlayer p){
        Scene s=SCENES.get(p.getUUID());return s!=null&&(s.expires>p.server.overworld().getGameTime()||selectionPending(p));
    }
    private static boolean selectionPending(ServerPlayer p){
        if(allowed(p))return !selected(p);
        return false;
    }
    public static void openScreen(ServerPlayer p){if(allowed(p)){snapshot(p);SandboxNetwork.open(p);}}
    public static void snapshot(ServerPlayer p){
        if(!allowed(p))return;
        int[] tiers=new int[5],progress=new int[5];for(int i=0;i<5;i++){progress[i]=uses(p,i);tiers[i]=RaceDevelopment.tier(p,i,tierFor(progress[i]));}
        String secondary=switch(race(p)){case 1->p.isShiftKeyDown()?"gravity":"void_projectile";case 2->"leaf_bridge";case 3->"echo";case 4->"fire_slash";default->"guardian";};
        boolean test=ProductionRaceBridge.testProfile(p);
        long reselect=!test?ProductionRaceBridge.remainingReselectTicks(p):0L;
        UUID nonce=ProductionRaceBridge.nonce(p);
        SandboxNetwork.sendStatus(p,new Status(Math.max(0,race(p)),selected(p),tiers,progress,remaining(p,"signature"),remaining(p,secondary),developerMode(p),bloomUpgrade(p),reselect,test,nonce,SandboxRaceTraits.healingPenalty(p)));
        SandboxOptionalIrons.sync(p);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p&&(allowed(p))){mirrorIfLocal(p);snapshot(p);}}
    private static void mirrorIfLocal(ServerPlayer p){if(allowed(p))mirror(p);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p){mirrorIfLocal(p);snapshot(p);}}
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){SCENES.remove(p.getUUID());if(allowed(p))selectionChanged(p);mirrorIfLocal(p);snapshot(p);}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){SCENES.remove(p.getUUID());com.siirio.jemroles.race.RaceService.clearNonce(p);if(allowed(p))selectionChanged(p);}}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||(!allowed(p)))return;
        if(allowed(p)){
            mirror(p);
            if(selected(p)&&!developerMode(p)&&p.tickCount%20==0){ProductionRaceBridge.tick(p);}
        }
        Scene scene=SCENES.get(p.getUUID());
        if(scene!=null){
            if(scene.expires<=p.server.overworld().getGameTime()&&!selectionPending(p)||scene.dimension!=p.serverLevel().dimension())SCENES.remove(p.getUUID());
            else {p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;if(p.position().distanceToSqr(scene.anchor)>.0025)p.connection.teleport(scene.anchor.x,scene.anchor.y,scene.anchor.z,p.getYRot(),p.getXRot());}
        }
        if(p.tickCount%20==0)snapshot(p);
    }
    @SubscribeEvent public static void protect(LivingAttackEvent e){if(e.getEntity() instanceof ServerPlayer p&&locked(p))e.setCanceled(true);}
    @SubscribeEvent public static void interact(PlayerInteractEvent e){if(e.getEntity() instanceof ServerPlayer p&&locked(p)&&e.isCancelable())e.setCanceled(true);}
    @SubscribeEvent public static void attack(AttackEntityEvent e){if(e.getEntity() instanceof ServerPlayer p&&locked(p))e.setCanceled(true);}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){SCENES.clear();RaceOwnedEffects.clearAll();}

}
