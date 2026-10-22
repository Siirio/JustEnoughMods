package com.siirio.jemvisualsandbox.workbench;

import java.util.UUID;
import com.siirio.jemroles.race.RaceService;
import com.siirio.jemroles.race.RaceRecord;
import com.siirio.jemroles.race.RaceId;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Coordinates active abilities and publishes the canonical race snapshot. */
@Mod.EventBusSubscriber(modid="jem_roles")
public final class RaceExperience {
    public record Status(int race,boolean selected,int[] tiers,int[] progress,long signatureRemaining,
                         long secondaryRemaining,boolean developerMode,int bloomUpgrade,
                         long reselectRemaining,boolean testProfile,UUID nonce,boolean healingPenalty) {
        public Status { tiers=tiers.clone();progress=progress.clone(); }
        @Override public int[] tiers(){return tiers.clone();}
        @Override public int[] progress(){return progress.clone();}
        public int tierAt(int index){return tiers[index];}
        public int progressAt(int index){return progress[index];}
    }
    private RaceExperience() {}
    public static boolean allowed(ServerPlayer p){return p!=null&&com.siirio.jemidentity.api.IdentityPolicy.resolve(p).isPresent();}
    public static boolean serverActive(MinecraftServer server){return server!=null;}
    public static boolean selected(ServerPlayer p){return allowed(p) && RaceService.selectedRace(p).isPresent();}
    public static boolean developerMode(ServerPlayer p){return RaceDevelopment.enabled(p);}
    public static int race(ServerPlayer p){return RaceService.selectedRace(p).map(RaceId::ordinal).orElse(-1);}
    public static int tier(ServerPlayer p){return RaceDevelopment.tier(p,Math.max(0,race(p)),tierFor(uses(p,Math.max(0,race(p)))));}
    private static int tierFor(int uses){return RacePowerStages.stageForUses(uses);}
    private static int uses(ServerPlayer p,int race){return RaceService.uses(p,race);}
    public static long remaining(ServerPlayer p,String key){return RaceDevelopment.cooldownsDisabled(p)?0:Math.max(0,RaceService.mechanicsCooldown(p,key)-p.server.overworld().getGameTime());}
    public static boolean ready(ServerPlayer p,String key){return remaining(p,key)==0;}
    public static void cooldown(ServerPlayer p,String key,int ticks){
        if(!allowed(p)||!selected(p)||RaceDevelopment.cooldownsDisabled(p))return;
        RaceService.mutate(p, record -> record.mechanicsCooldown(key,p.server.overworld().getGameTime()+Math.max(0,ticks)));
    }
    public static int signatureCooldown(int race){return switch(race){case 0->com.siirio.jemroles.race.RaceRules.LIFE_CHORUS_COOLDOWN.get();case 1->com.siirio.jemroles.race.RaceRules.RIFT_AMBUSH_COOLDOWN.get();case 2->com.siirio.jemroles.race.RaceRules.VERDANT_DOMINION_COOLDOWN.get();case 3->com.siirio.jemroles.race.RaceRules.SCULK_VEINS_COOLDOWN.get();case 4->1200;default->0;};}
    public static int bloomUpgrade(ServerPlayer p){return Math.min(tier(p),RaceDevelopment.bloomUpgrade(p,RacePowerStages.clamp(RaceService.bloomUpgrade(p))));}
    public static void upgrade(ServerPlayer p,int next){
        if(!allowed(p)||race(p)!=2||next!=bloomUpgrade(p)+1||next>tier(p)||next>3)return;
        RaceDevelopment.storeBloomUpgrade(p,next);
        snapshot(p);
    }
    public static void choose(ServerPlayer p,int next,UUID nonce){
        if(next<0||next>4||!allowed(p)||!p.isAlive()||p.isSpectator()||nonce==null)return;
        RaceService.Result result=RaceService.choose(p,RaceId.values()[next],nonce);
        if(result==RaceService.Result.CHANGE_LOCKED)p.displayClientMessage(Component.translatable("jem.roles.change_locked"),true);
        if(result==RaceService.Result.CHOSEN||result==RaceService.Result.CHANGED||result==RaceService.Result.UNCHANGED)RaceSelectionScene.complete(p);
        mirror(p);SandboxRaceTraits.refresh(p);snapshot(p);
    }
    public static void selectionChanged(ServerPlayer p){
        SandboxUtilities.selectionChanged(p);SandboxRaceTraits.clear(p);
        ElementalAbilities.selectionChanged(p);LifeChorusAbility.selectionChanged(p);
        SonicWaveAbility.selectionChanged(p);FierySlash.selectionChanged(p);
        SandboxHostileControl.selectionChanged(p);
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
        RaceService.mutate(p, RaceRecord::awardUtilityUse);
        snapshot(p);
    }
    public static boolean requireReady(ServerPlayer p,String key){
        long time=remaining(p,key);if(time==0)return true;
        p.displayClientMessage(Component.translatable("jem.sandbox.cooldown",com.siirio.jemidentity.api.MinecraftDays.format(time)),true);snapshot(p);return false;
    }
    public static boolean canAct(ServerPlayer p){return allowed(p)&&selected(p)&&p.isAlive()&&!p.isSpectator()&&!RaceSelectionScene.locked(p);}
    public static void utility(ServerPlayer p,boolean pressed){
        if(!canAct(p))return;if(pressed)SandboxUtilities.press(p);else SandboxUtilities.release(p);snapshot(p);
    }
    public static void openScreen(ServerPlayer p){if(allowed(p)){snapshot(p);SandboxNetwork.open(p);}}
    public static void snapshot(ServerPlayer p){
        if(!allowed(p))return;
        int[] tiers=new int[5],progress=new int[5];for(int i=0;i<5;i++){progress[i]=uses(p,i);tiers[i]=RaceDevelopment.tier(p,i,tierFor(progress[i]));}
        String secondary=switch(race(p)){case 1->p.isShiftKeyDown()?"gravity":"void_projectile";case 2->"leaf_bridge";case 3->"echo";case 4->"fire_slash";default->"guardian";};
        boolean test=RaceService.testRaceReselect(p.server);
        long reselect=!test?RaceService.remainingReselectTicks(p):0L;
        UUID nonce=RaceService.currentNonce(p);
        SandboxNetwork.sendStatus(p,new Status(Math.max(0,race(p)),selected(p),tiers,progress,remaining(p,"signature"),remaining(p,secondary),developerMode(p),bloomUpgrade(p),reselect,test,nonce,SandboxRaceTraits.healingPenalty(p)));
        SandboxOptionalIrons.sync(p);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p&&(allowed(p))){mirrorIfLocal(p);snapshot(p);}}
    private static void mirrorIfLocal(ServerPlayer p){if(allowed(p))mirror(p);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p){mirrorIfLocal(p);snapshot(p);}}
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){if(allowed(p))selectionChanged(p);mirrorIfLocal(p);snapshot(p);}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){com.siirio.jemroles.race.RaceService.clearNonce(p);if(allowed(p))selectionChanged(p);}}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||(!allowed(p)))return;
        if(allowed(p)){
            mirror(p);
            if(selected(p)&&!developerMode(p)&&p.tickCount%20==0){RaceService.mutate(p, record -> record.addActiveTicks(20L));}
        }
        if(p.tickCount%20==0)snapshot(p);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent e){RaceOwnedEffects.clearAll();}

}
