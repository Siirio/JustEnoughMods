package com.justenoughmods.achievementguide.integration.battlefinal;
import java.util.ArrayDeque;
import net.minecraft.world.entity.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.entitypatch.player.*;
import yesman.epicfight.world.entity.DeathHarvestOrb;
import yesman.epicfight.world.entity.eventlistener.PlayerKilledEvent;
public final class BattleFinalQuestPassive {
    public interface OrbAccess { net.minecraft.world.entity.player.Player jem$harvestDestination(); }
    private static final String HARVEST="jem_quest_native_death_harvest";
    private static final ThreadLocal<Events> EVENTS=ThreadLocal.withInitial(Events::new);
    private static final class Events {final ArrayDeque<LivingDeathEvent> frames=new ArrayDeque<>();int overflow;}
    private record Harvest(SkillContainer container,PlayerKilledEvent event){}
    private static final ThreadLocal<Harvests> HARVESTS=ThreadLocal.withInitial(Harvests::new);
    private static final class Harvests {final ArrayDeque<Harvest> frames=new ArrayDeque<>();int overflow;}
    private BattleFinalQuestPassive(){}
    private static boolean equipped(ServerPlayerPatch patch,Skill skill){return skill!=null&&BattleFinalQuestProof.real(patch.getOriginal())&&patch.isEpicFightMode()&&patch.getSkillCapability().hasLearned(skill)&&patch.getSkillCapability().listSkillContainers().anyMatch(container->container.getSkill()==skill);}
    public static void eventBegin(LivingDeathEvent event){Events calls=EVENTS.get();if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;}calls.frames.push(event);}
    public static void eventEnd(){Events calls=EVENTS.get();if(calls.overflow>0){calls.overflow--;return;}if(!calls.frames.isEmpty())calls.frames.pop();if(calls.frames.isEmpty())EVENTS.remove();}
    private static LivingDeathEvent event(PlayerKilledEvent killed){Events calls=EVENTS.get();if(calls.overflow!=0||calls.frames.isEmpty())return null;LivingDeathEvent event=calls.frames.peek();return event.getEntity()==killed.getKilledEntity()&&event.getSource()==killed.getDamageSource()?event:null;}
    public static void pillager(ServerPlayerPatch patch,Skill skill,PlayerKilledEvent killed,float before){LivingDeathEvent event=event(killed);
        if(event!=null&&equipped(patch,skill)&&patch.getStamina()>before&&event.getSource().getEntity()==patch.getOriginal()&&BattleFinalQuestProof.target(event.getEntity()))
            BattleFinalQuestLife.event(event,()->BattleFinalQuestProof.emit(patch.getOriginal(),"v3_combat_skill_synergy"));
    }
    public static void harvestBegin(SkillContainer container,PlayerKilledEvent event){Harvests calls=HARVESTS.get();if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;}calls.frames.push(new Harvest(container,event));}
    public static void harvestEnd(){Harvests calls=HARVESTS.get();if(calls.overflow>0){calls.overflow--;return;}if(!calls.frames.isEmpty())calls.frames.pop();if(calls.frames.isEmpty())HARVESTS.remove();}
    public static void spawned(Entity output,boolean accepted){
        Harvests calls=HARVESTS.get();if(!accepted||calls.overflow!=0||calls.frames.isEmpty()||!(output instanceof DeathHarvestOrb orb))return;
        Harvest harvest=calls.frames.peek();if(!(harvest.container.getExecutor() instanceof ServerPlayerPatch patch)||!equipped(patch,harvest.container.getSkill())||orb.level()!=patch.getOriginal().level()||orb.isRemoved()||patch.getOriginal().serverLevel().getEntity(orb.getUUID())!=orb)return;
        LivingDeathEvent event=event(harvest.event);if(!(orb instanceof OrbAccess access)||access.jem$harvestDestination()!=patch.getOriginal()||event==null||event.getSource().getEntity()!=patch.getOriginal()||!BattleFinalQuestProof.target(event.getEntity()))return;
        BattleFinalQuestLife.event(event,()->{if(!orb.isRemoved()&&patch.getOriginal().serverLevel().getEntity(orb.getUUID())==orb)orb.getPersistentData().putUUID(HARVEST,patch.getOriginal().getUUID());});
    }
    public static void collected(Entity orb,SkillContainer innate,float before){
        if(!(innate.getExecutor() instanceof ServerPlayerPatch patch)||!(orb instanceof OrbAccess access)||access.jem$harvestDestination()!=patch.getOriginal()||!BattleFinalQuestProof.real(patch.getOriginal())||!orb.getPersistentData().hasUUID(HARVEST)||!orb.getPersistentData().getUUID(HARVEST).equals(patch.getOriginal().getUUID())||innate.getResource()<=before)return;
        orb.getPersistentData().remove(HARVEST);BattleFinalQuestProof.emit(patch.getOriginal(),"v3_combat_skill_synergy");
    }
    public static void clear(){EVENTS.remove();HARVESTS.remove();}
}
