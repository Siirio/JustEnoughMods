package com.justenoughmods.achievementguide.integration.battlefinal;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.*;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
public final class BattleFinalQuestMeteor {
    private record Attempt(ServerPlayerPatch patch,Skill skill,DynamicAnimation animation,Vec3 origin,long time){}
    private static final Map<UUID,Attempt> ATTEMPTS=new HashMap<>();
    private BattleFinalQuestMeteor(){}
    public static void started(PlayerPatch<?> patch,Skill skill,AssetAccessor<? extends DynamicAnimation> animation){
        if(!(patch instanceof ServerPlayerPatch server)||!BattleFinalQuestProof.real(server.getOriginal())||!server.isEpicFightMode()||!server.getSkillCapability().hasLearned(skill)||!server.getSkillCapability().listSkillContainers().anyMatch(container->container.getSkill()==skill))return;
        if(ATTEMPTS.size()>=256)ATTEMPTS.clear();ATTEMPTS.put(server.getOriginal().getUUID(),new Attempt(server,skill,animation.get(),server.getOriginal().position(),server.getOriginal().level().getGameTime()));
    }
    private static boolean valid(Attempt attempt,DynamicAnimation animation){return attempt!=null&&attempt.animation==animation&&BattleFinalQuestProof.real(attempt.patch.getOriginal())&&attempt.patch.isEpicFightMode()
        &&attempt.patch.getOriginal().level().getGameTime()-attempt.time<200&&attempt.patch.getSkillCapability().listSkillContainers().anyMatch(container->container.getSkill()==attempt.skill);}
    public static void fractured(LivingEntityPatch<?> patch,AssetAccessor<? extends DynamicAnimation> animation,boolean nativeResult){
        if(!nativeResult||!(patch instanceof ServerPlayerPatch server))return;Attempt attempt=ATTEMPTS.get(server.getOriginal().getUUID());
        if(valid(attempt,animation.get())&&attempt.patch==server&&server.getOriginal().position().distanceToSqr(attempt.origin)>0.0001){ATTEMPTS.remove(server.getOriginal().getUUID());BattleFinalQuestProof.emit(server.getOriginal(),"v3_combat_skill_mobility");}
    }
    public static void damage(LivingEntity target,DamageSource source){
        if(!(source instanceof EpicFightDamageSource epic)||epic.getAnimation()==null||!BattleFinalQuestProof.real(source.getEntity()))return;
        Attempt attempt=ATTEMPTS.get(source.getEntity().getUUID());if(valid(attempt,epic.getAnimation().get())&&attempt.patch.getOriginal()==source.getEntity()){ATTEMPTS.remove(source.getEntity().getUUID());BattleFinalQuestProof.emit(source.getEntity(),"v3_combat_skill_mobility");}
    }
    public static void forget(UUID actor){ATTEMPTS.remove(actor);}
    public static void clear(){ATTEMPTS.clear();}
}
