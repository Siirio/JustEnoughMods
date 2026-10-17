package com.siirio.jemvisualsandbox.workbench;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Integrated-server progression owner; native dedicated JEMRoles is delegated, never shadowed. */
@Mod.EventBusSubscriber(modid="jem_visual_sandbox")
public final class RaceExperience {
    public record Status(int race,boolean selected,int[] tiers,int[] progress,long signatureRemaining,
                         long secondaryRemaining,boolean developerMode,int bloomUpgrade) {
        public Status { tiers=tiers.clone();progress=progress.clone(); }
        @Override public int[] tiers(){return tiers.clone();}
        @Override public int[] progress(){return progress.clone();}
    }
    private static final class Developer {
        int race; int tier=3; int bloom=3;
        Developer(int race){this.race=race;}
    }
    private record Scene(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,Vec3 anchor,long expires) {}
    private static final Map<UUID,Developer> DEVELOPERS=new HashMap<>();
    private static final Map<UUID,Scene> SCENES=new HashMap<>();
    private RaceExperience() {}
    public static boolean allowed(ServerPlayer p){return !p.server.isDedicatedServer() && p.server.isSingleplayerOwner(p.getGameProfile());}
    public static boolean selected(ServerPlayer p){return allowed(p) && (developerMode(p)||data(p).getBoolean("Selected"));}
    public static boolean developerMode(ServerPlayer p){return allowed(p)&&DEVELOPERS.containsKey(p.getUUID());}
    public static int race(ServerPlayer p){return developerMode(p)?DEVELOPERS.get(p.getUUID()).race:selected(p)?data(p).getInt("Race"):-1;}
    public static int tier(ServerPlayer p){return developerMode(p)?DEVELOPERS.get(p.getUUID()).tier:tierFor(uses(p,Math.max(0,race(p))));}
    private static int tierFor(int uses){return uses>=30?3:uses>=10?2:1;}
    private static int uses(ServerPlayer p,int race){return Math.max(0,Math.min(30,data(p).getInt("Uses"+race)));}
    private static CompoundTag data(ServerPlayer p){return Store.get(p.server).record(p);}
    private static void dirty(ServerPlayer p){Store.get(p.server).setDirty();}
    private static String cooldownKey(ServerPlayer p,String key){return "Cooldown"+Math.max(0,race(p))+"_"+key;}
    public static long remaining(ServerPlayer p,String key){return developerMode(p)?0:Math.max(0,data(p).getLong(cooldownKey(p,key))-p.server.overworld().getGameTime());}
    public static boolean ready(ServerPlayer p,String key){return remaining(p,key)==0;}
    public static void cooldown(ServerPlayer p,String key,int ticks){
        if(!allowed(p)||!selected(p)||developerMode(p))return;
        data(p).putLong(cooldownKey(p,key),p.server.overworld().getGameTime()+Math.max(0,ticks));dirty(p);
    }
    public static int signatureCooldown(int race){return switch(race){case 0->SandboxRaceRules.LIFE_CHORUS_COOLDOWN.get();case 1->SandboxRaceRules.RIFT_AMBUSH_COOLDOWN.get();case 2->SandboxRaceRules.VERDANT_DOMINION_COOLDOWN.get();case 3->SandboxRaceRules.SCULK_VEINS_COOLDOWN.get();case 4->1200;default->0;};}
    public static int bloomUpgrade(ServerPlayer p){return developerMode(p)?DEVELOPERS.get(p.getUUID()).bloom:Math.max(0,Math.min(3,data(p).getInt("BloomUpgrade")));}
    public static void upgrade(ServerPlayer p,int next){
        if(!allowed(p)||race(p)!=2||next!=bloomUpgrade(p)+1||next>tier(p)||next>3)return;
        if(developerMode(p))DEVELOPERS.get(p.getUUID()).bloom=next;
        else {data(p).putInt("BloomUpgrade",next);dirty(p);}
        snapshot(p);
    }
    public static void developer(ServerPlayer p,boolean enabled){
        if(!allowed(p))return;
        SandboxUtilities.selectionChanged(p);SandboxRaceTraits.clear(p);
        if(enabled)DEVELOPERS.put(p.getUUID(),new Developer(Math.max(0,race(p))));else DEVELOPERS.remove(p.getUUID());
        mirror(p);snapshot(p);
        p.displayClientMessage(Component.literal(enabled?"Developer mode: separate temporary race/tier, no cooldowns; normal save untouched":"Normal race progression restored"),false);
    }
    public static void developerTier(ServerPlayer p,int tier){
        if(!developerMode(p)||tier<1||tier>3)return;
        DEVELOPERS.get(p.getUUID()).tier=tier;DEVELOPERS.get(p.getUUID()).bloom=tier;snapshot(p);
    }
    public static void developerBloom(ServerPlayer p,int upgrade){if(developerMode(p)&&upgrade>=0&&upgrade<=3){DEVELOPERS.get(p.getUUID()).bloom=upgrade;snapshot(p);}}
    public static void choose(ServerPlayer p,int next){
        if(next<0||next>4)return;
        if(ProductionRaceBridge.available(p)){ProductionRaceBridge.choose(p,next);completeSelectionScene(p);snapshot(p);return;}
        if(!allowed(p)||!p.isAlive()||p.isSpectator())return;
        var tag=data(p);
        if(selected(p)&&race(p)==next){completeSelectionScene(p);snapshot(p);return;}
        if(!developerMode(p)&&selected(p)&&race(p)!=next&&(tag.getBoolean("FreeSwitchUsed")||tag.getLong("ActiveTicks")>=36000)){
            p.displayClientMessage(Component.translatable("jem.roles.change_locked"),true);snapshot(p);return;
        }
        SandboxUtilities.selectionChanged(p);SandboxRaceTraits.clear(p);
        if(developerMode(p))DEVELOPERS.get(p.getUUID()).race=next;
        else {
            if(selected(p)&&race(p)!=next)tag.putBoolean("FreeSwitchUsed",true);
            tag.putBoolean("Selected",true);tag.putInt("Race",next);dirty(p);
        }
        completeSelectionScene(p);mirror(p);snapshot(p);
    }
    private static void completeSelectionScene(ServerPlayer p){
        Scene scene=SCENES.get(p.getUUID());
        boolean confirmed=allowed(p)?selected(p):ProductionRaceBridge.available(p)&&ProductionRaceBridge.status(p)!=null&&ProductionRaceBridge.status(p).selected();
        if(scene!=null&&confirmed)SCENES.put(p.getUUID(),new Scene(scene.dimension,scene.anchor,p.server.overworld().getGameTime()+100));
    }
    private static void mirror(ServerPlayer p){p.getPersistentData().putInt("JemSandboxRace",race(p));}
    public static boolean cast(ServerPlayer p){
        if(ProductionRaceBridge.available(p)){boolean result=ProductionRaceBridge.cast(p);snapshot(p);return result;}
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
        if(!developerMode(p)){data(p).putInt("Uses"+race(p),Math.min(30,uses(p,race(p))+1));dirty(p);}
        snapshot(p);
    }
    public static boolean requireReady(ServerPlayer p,String key){
        long time=remaining(p,key);if(time==0)return true;
        p.displayClientMessage(Component.translatable("jem.sandbox.cooldown",String.format(java.util.Locale.ROOT,"%.1f",time/20.)),true);snapshot(p);return false;
    }
    public static boolean canAct(ServerPlayer p){return allowed(p)&&selected(p)&&p.isAlive()&&!p.isSpectator()&&!locked(p);}
    public static void utility(ServerPlayer p,boolean pressed){
        if(ProductionRaceBridge.available(p)){ProductionRaceBridge.utility(p,pressed);snapshot(p);return;}
        if(!canAct(p))return;if(pressed)SandboxUtilities.press(p);else SandboxUtilities.release(p);snapshot(p);
    }
    public static void cinematic(ServerPlayer p,boolean enabled){
        if(!allowed(p)&&!ProductionRaceBridge.available(p))return;
        if(enabled){
            if(ProductionRaceBridge.available(p)){
                Status nativeStatus=ProductionRaceBridge.status(p);
                if(nativeStatus==null||nativeStatus.selected())return;
            }
            SCENES.putIfAbsent(p.getUUID(),new Scene(p.serverLevel().dimension(),p.position(),p.server.overworld().getGameTime()+600));
        }
        else SCENES.remove(p.getUUID());
    }
    private static boolean locked(ServerPlayer p){
        Scene s=SCENES.get(p.getUUID());return s!=null&&(s.expires>p.server.overworld().getGameTime()||selectionPending(p));
    }
    private static boolean selectionPending(ServerPlayer p){
        if(allowed(p))return !selected(p);
        if(ProductionRaceBridge.available(p)){Status status=ProductionRaceBridge.status(p);return status!=null&&!status.selected();}
        return false;
    }
    public static void snapshot(ServerPlayer p){
        if(ProductionRaceBridge.available(p)){Status nativeStatus=ProductionRaceBridge.status(p);if(nativeStatus!=null)SandboxNetwork.sendStatus(p,nativeStatus);return;}
        if(!allowed(p))return;
        int[] tiers=new int[5],progress=new int[5];for(int i=0;i<5;i++){progress[i]=uses(p,i);tiers[i]=developerMode(p)?DEVELOPERS.get(p.getUUID()).tier:tierFor(progress[i]);}
        String secondary=switch(race(p)){case 1->p.isShiftKeyDown()?"gravity":"void_projectile";case 2->"leaf_bridge";case 3->"echo";case 4->"fire_slash";default->"guardian";};
        SandboxNetwork.sendStatus(p,new Status(Math.max(0,race(p)),selected(p),tiers,progress,remaining(p,"signature"),remaining(p,secondary),developerMode(p),bloomUpgrade(p)));
        SandboxOptionalIrons.sync(p);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p&&(allowed(p)||ProductionRaceBridge.available(p))){mirrorIfLocal(p);snapshot(p);}}
    private static void mirrorIfLocal(ServerPlayer p){if(allowed(p))mirror(p);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p){mirrorIfLocal(p);snapshot(p);}}
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){SCENES.remove(p.getUUID());mirrorIfLocal(p);snapshot(p);}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){SCENES.remove(p.getUUID());DEVELOPERS.remove(p.getUUID());}}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||(!allowed(p)&&!ProductionRaceBridge.available(p)))return;
        if(allowed(p)){
            mirror(p);
            if(selected(p)&&!developerMode(p)&&p.tickCount%20==0){data(p).putLong("ActiveTicks",data(p).getLong("ActiveTicks")+20);dirty(p);}
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
    @SubscribeEvent public static void stop(ServerStoppedEvent e){DEVELOPERS.clear();SCENES.clear();}

    /** One per-world record per UUID survives death without copying transient entity state. */
    private static final class Store extends SavedData {
        private final Map<UUID,CompoundTag> records=new HashMap<>();
        static Store get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(Store::load,Store::new,"jem_races_standalone");}
        CompoundTag record(ServerPlayer p){
            return records.computeIfAbsent(p.getUUID(),id->{
                CompoundTag result=new CompoundTag();var old=p.getPersistentData();
                if(old.contains("JemSandboxRace")){int race=old.getInt("JemSandboxRace");if(race>=0&&race<5){result.putBoolean("Selected",true);result.putInt("Race",race);}}
                result.putInt("BloomUpgrade",Math.max(0,Math.min(3,old.getInt("JemSandboxMainUpgrade2"))));setDirty();return result;
            });
        }
        static Store load(CompoundTag root){Store store=new Store();for(String id:root.getAllKeys())store.records.put(UUID.fromString(id),root.getCompound(id));return store;}
        @Override public CompoundTag save(CompoundTag root){records.forEach((id,value)->root.put(id.toString(),value.copy()));return root;}
    }
}
