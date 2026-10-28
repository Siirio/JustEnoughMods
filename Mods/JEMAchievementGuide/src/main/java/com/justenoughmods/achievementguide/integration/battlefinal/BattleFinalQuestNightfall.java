package com.justenoughmods.achievementguide.integration.battlefinal;
import java.util.*;
import com.hm.efn.gameasset.EFNSKillDataKeys;
import com.hm.efn.gameasset.animations.*;
import com.hm.efn.registries.EFNMobEffectRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.*;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.entity.eventlistener.DealDamageEvent;
public final class BattleFinalQuestNightfall {
    private static final class Action {
        final ServerPlayerPatch patch;final SkillContainer container;final ItemStack weapon;final long started;
        boolean paid,outcome,stage3,inside,installed,released;long epoch;DynamicAnimation animation;
        Action(ServerPlayerPatch patch,SkillContainer container){this.patch=patch;this.container=container;weapon=patch.getOriginal().getMainHandItem();started=patch.getOriginal().level().getGameTime();epoch=epoch(patch);animation=patch.getAnimator().getPlayerFor(null).getRealAnimation().get();}
    }
    private record Guard(Action action,LivingEntity target){}
    private record Verified(SkillContainer container,Object record){}
    private record ReplayHit(Action action,LivingEntity target){}
    private record RecordCall(SkillContainer container,Object before){}
    private record ReplayCall(SkillContainer container,DealDamageEvent.Attack event,Object record,boolean enhanced){}
    private record Cost(PlayerPatch<?> patch,SkillContainer container,int stack,DynamicAnimation animation){}
    private static final Map<UUID,Action> DUSK=new HashMap<>(),VANGUARD=new HashMap<>(),EXILE=new HashMap<>(),REPLAY=new HashMap<>(),BLOOD=new HashMap<>();
    private record Epoch(long id,DynamicAnimation requested){}
    private static final Map<UUID,Epoch> EPOCHS=new HashMap<>();
    private static final Map<UUID,Verified> HISTORY=new HashMap<>();
    private static final Map<DamageSource,Guard> GUARDS=new IdentityHashMap<>();
    private static final Map<DamageSource,ReplayHit> REPLAY_HITS=new IdentityHashMap<>();
    private static final ThreadLocal<Contexts> CONTEXTS=ThreadLocal.withInitial(Contexts::new);
    private static final class Contexts {final ArrayDeque<RecordCall> records=new ArrayDeque<>();final ArrayDeque<ReplayCall> replays=new ArrayDeque<>();final ArrayDeque<Cost> costs=new ArrayDeque<>();int recordOverflow,replayOverflow,costOverflow;}
    private BattleFinalQuestNightfall(){}
    private static boolean valid(ServerPlayerPatch patch){return patch!=null&&BattleFinalQuestProof.real(patch.getOriginal())&&patch.isEpicFightMode();}
    private static boolean valid(Action action){return action!=null&&valid(action.patch)&&action.patch.getOriginal().getMainHandItem()==action.weapon&&action.container.getExecutor()==action.patch;}
    private static void put(Map<UUID,Action> map,ServerPlayerPatch patch,Action action){if(map.size()>=256)map.clear();map.put(patch.getOriginal().getUUID(),action);}
    private static boolean exileWeapon(String item){return item.equals("efn:exsiliumgladius")||item.equals("efn:fire_exsiliumgladius");}
    private static boolean exileLeft(DynamicAnimation animation){return animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_A.get()||animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_AA.get()||animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_AAA.get()||animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_AAAA.get();}
    private static boolean exileB(DynamicAnimation animation){return animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_AB.get()||animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_ABA.get()||animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_ABAA.get()||animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_ABAB.get()||animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_ABB.get()||animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_ABBA.get()||animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_ABBB.get()||animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_AAB.get()||animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_AABA.get()||animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_AABB.get()||animation==EFNExsiliumgladiusAnimations.EXSILIUMGLADIUS_AAAB.get();}
    private static long epoch(ServerPlayerPatch patch){Epoch epoch=EPOCHS.get(patch.getOriginal().getUUID());return epoch==null?0:epoch.id;}
    public static void animation(yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch<?> living,AssetAccessor<? extends DynamicAnimation> next){
        if(!(living instanceof ServerPlayerPatch patch)||!BattleFinalQuestProof.real(patch.getOriginal()))return;
        if(!EPOCHS.containsKey(patch.getOriginal().getUUID())&&EPOCHS.size()>=256){EPOCHS.clear();DUSK.clear();VANGUARD.clear();EXILE.clear();BLOOD.clear();REPLAY.clear();HISTORY.clear();GUARDS.clear();REPLAY_HITS.clear();}
        long old=epoch(patch);EPOCHS.put(patch.getOriginal().getUUID(),new Epoch(old+1,next.get()));
        Action dusk=DUSK.get(patch.getOriginal().getUUID());
        if(valid(dusk)&&dusk.stage3&&dusk.epoch==old&&patch.getAnimator().getPlayerFor(null).getRealAnimation().get()==EFNLanceAnimations.NF_MEEN_CHARGING.get()&&next.get()==EFNLanceAnimations.NF_MEEN_CHARGE3.get()){dusk.released=true;dusk.epoch=old+1;}
    }
    public static void node(SkillContainer container,AssetAccessor<? extends DynamicAnimation> animation){
        if(!(container.getExecutor() instanceof ServerPlayerPatch patch)||!valid(patch)||patch.getAnimator().getPlayerFor(null).getRealAnimation().get()!=animation.get())return;
        String item=BattleFinalQuestProof.item(patch.getOriginal().getMainHandItem());DynamicAnimation actual=animation.get();
        if(item.equals("efn:sword_of_pioneer")){
            Action old=VANGUARD.get(patch.getOriginal().getUUID());
            if(actual==EFNSwordAnimations.NF_SWORD_SKILL_FIRST.get())put(VANGUARD,patch,new Action(patch,container));
            else if(actual==EFNSwordAnimations.NF_SWORD_SKILL_SECOND.get()&&valid(old)&&old.container==container&&old.epoch==epoch(patch)-1){old.epoch=epoch(patch);old.animation=actual;}
            else VANGUARD.remove(patch.getOriginal().getUUID());
        }
        if(exileWeapon(item)){
            Action action=EXILE.get(patch.getOriginal().getUUID());
            if(exileLeft(actual)){action=new Action(patch,container);action.inside=true;put(EXILE,patch,action);}
            else if(exileB(actual)&&valid(action)&&action.container==container&&action.inside&&action.epoch==epoch(patch)-1){action.paid=true;action.epoch=epoch(patch);action.animation=actual;}
            else if(!exileB(actual))EXILE.remove(patch.getOriginal().getUUID());
        }
        if(item.equals("efn:air_tachi")&&actual==EFNTachiAnimations.NF_TACHI_BLOODLUST.get())put(BLOOD,patch,new Action(patch,container));
    }
    public static void costBegin(PlayerPatch<?> patch){Contexts calls=CONTEXTS.get();if(calls.costOverflow>0||calls.costs.size()>=16){calls.costOverflow++;return;}SkillContainer container=patch instanceof ServerPlayerPatch server&&valid(server)?patch.getSkill(SkillSlots.WEAPON_INNATE):null;calls.costs.push(new Cost(patch,container,container==null?0:container.getStack(),container==null?null:patch.getAnimator().getPlayerFor(null).getRealAnimation().get()));}
    public static void costEnd(){Contexts calls=CONTEXTS.get();if(calls.costOverflow>0){calls.costOverflow--;return;}Cost cost=calls.costs.isEmpty()?null:calls.costs.pop();if(cost==null||cost.container==null||!(cost.patch instanceof ServerPlayerPatch patch)||!valid(patch)||cost.container.getStack()>=cost.stack)return;
        Action action=VANGUARD.get(patch.getOriginal().getUUID());if(valid(action)&&action.epoch==epoch(patch)&&action.container==cost.container&&cost.animation==EFNSwordAnimations.NF_SWORD_SKILL_FIRST.get()){action.paid=true;finish(VANGUARD,action,"v3_combat_agile_school");}
    }
    public static void guard(SkillContainer guardContainer,EpicFightDamageSource source){
        if(!(guardContainer.getExecutor() instanceof ServerPlayerPatch defender)||!valid(defender)||!BattleFinalQuestProof.real(source.getEntity())||source.getAnimation()==null)return;
        ServerPlayer actor=(ServerPlayer)source.getEntity();Action action=VANGUARD.get(actor.getUUID());DynamicAnimation animation=source.getAnimation().get();
        if(valid(action)&&action.epoch==epoch(action.patch)&&action.animation==animation&&BattleFinalQuestProof.item(actor.getMainHandItem()).equals("efn:sword_of_pioneer")&&(animation==EFNSwordAnimations.NF_SWORD_SKILL_FIRST.get()||animation==EFNSwordAnimations.NF_SWORD_SKILL_SECOND.get())){if(GUARDS.size()>=128)GUARDS.clear();GUARDS.put(source,new Guard(action,defender.getOriginal()));}
    }
    // Only this explicitly authorized native Guard bypass may use a real surviving PvP target.
    public static void vanguardDamage(LivingEntity target,DamageSource source){if(!BattleFinalQuestProof.real(source.getEntity()))return;Guard guard=GUARDS.remove(source);if(guard==null||guard.target!=target||!BattleFinalQuestProof.real(target)||!valid(guard.action)||VANGUARD.get(guard.action.patch.getOriginal().getUUID())!=guard.action)return;guard.action.outcome=true;finish(VANGUARD,guard.action,"v3_combat_agile_school");}
    public static void duskState(SkillContainer container){
        if(!(container.getExecutor() instanceof ServerPlayerPatch patch)||!valid(patch)||container.getSkill()==null||!BattleFinalQuestProof.item(patch.getOriginal().getMainHandItem()).equals("efn:meen_spear"))return;
        SkillDataManager data=container.getDataManager();boolean held=Boolean.TRUE.equals(data.getDataValue(EFNSKillDataKeys.IS_PRESSING.get()))&&Boolean.TRUE.equals(data.getDataValue(EFNSKillDataKeys.IS_CHARGING.get()));
        if(!held||patch.getAnimator().getPlayerFor(null).getRealAnimation().get()!=EFNLanceAnimations.NF_MEEN_CHARGING.get())return;
        Action action=DUSK.get(patch.getOriginal().getUUID());
        if(!valid(action)||action.container!=container||action.epoch!=epoch(patch)){if(container.getSkill().getMaxStack()<1||container.getStack()<container.getSkill().getMaxStack())return;action=new Action(patch,container);put(DUSK,patch,action);}
        
    }
    public static void duskStage(yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch<?> living){if(living instanceof ServerPlayerPatch patch){Action action=DUSK.get(patch.getOriginal().getUUID());if(valid(action)&&action.epoch==epoch(patch)&&patch.getAnimator().getPlayerFor(null).getRealAnimation().get()==EFNLanceAnimations.NF_MEEN_CHARGING.get()&&action.container.getDataManager().getDataValue(EFNSKillDataKeys.CHARGE_STAGE.get())==3&&Boolean.TRUE.equals(action.container.getDataManager().getDataValue(EFNSKillDataKeys.IS_PRESSING.get()))&&Boolean.TRUE.equals(action.container.getDataManager().getDataValue(EFNSKillDataKeys.IS_CHARGING.get())))action.stage3=true;}}
    public static void duskPaid(SkillContainer container,DynamicAnimation animation,int before){if(!(container.getExecutor() instanceof ServerPlayerPatch patch)||container.getStack()>=before||animation!=EFNLanceAnimations.NF_MEEN_CHARGE3.get())return;Action action=DUSK.get(patch.getOriginal().getUUID());if(valid(action)&&action.container==container&&action.stage3&&action.released&&(action.epoch==epoch(patch)||action.epoch==epoch(patch)-1&&EPOCHS.get(patch.getOriginal().getUUID()).requested!=EFNLanceAnimations.NF_MEEN_CHARGE3.get())){action.paid=true;finish(DUSK,action,"v3_combat_heavy_school");}}
    private static Object last(SkillContainer container){List<?> history=container.getDataManager().getDataValue(EFNSKillDataKeys.DAMAGE_HISTORY.get());return history==null||history.isEmpty()?null:history.get(history.size()-1);}
    public static void recordBegin(SkillContainer container){Contexts calls=CONTEXTS.get();if(calls.recordOverflow>0||calls.records.size()>=16){calls.recordOverflow++;return;}calls.records.push(new RecordCall(container,last(container)));}
    public static void recordEnd(){Contexts calls=CONTEXTS.get();if(calls.recordOverflow>0){calls.recordOverflow--;return;}RecordCall record=calls.records.isEmpty()?null:calls.records.pop();if(record==null||!(record.container.getExecutor() instanceof ServerPlayerPatch patch)||!valid(patch))return;Object actual=last(record.container);if(actual==null||actual==record.before)return;
        BattleFinalQuestLife.HurtView hit=BattleFinalQuestLife.hurtFor(patch.getOriginal());if(hit!=null)BattleFinalQuestLife.direct(hit.target(),hit.source(),()->{if(HISTORY.size()>=256)HISTORY.clear();HISTORY.put(patch.getOriginal().getUUID(),new Verified(record.container,actual));});
    }
    public static int replayBefore(SkillContainer container){return container.getStack();}
    public static void replayActivated(SkillContainer container,int before){if(!(container.getExecutor() instanceof ServerPlayerPatch patch)||!valid(patch)||container.getStack()>=before||!patch.getOriginal().hasEffect(EFNMobEffectRegistry.REPLAY.get()))return;put(REPLAY,patch,new Action(patch,container));}
    public static void replayBegin(SkillContainer container,DealDamageEvent.Attack event){Contexts calls=CONTEXTS.get();if(calls.replayOverflow>0||calls.replays.size()>=16){calls.replayOverflow++;return;}calls.replays.push(new ReplayCall(container,event,last(container),Boolean.TRUE.equals(container.getDataManager().getDataValue(EFNSKillDataKeys.REPLAY_ENHANCED.get()))));}
    public static void replayEnd(){Contexts calls=CONTEXTS.get();if(calls.replayOverflow>0){calls.replayOverflow--;return;}ReplayCall replay=calls.replays.isEmpty()?null:calls.replays.pop();if(replay==null||replay.enhanced||!(replay.container.getExecutor() instanceof ServerPlayerPatch patch)||!valid(patch))return;
        Verified verified=HISTORY.get(patch.getOriginal().getUUID());Action action=REPLAY.get(patch.getOriginal().getUUID());
        if(valid(action)&&action.container==replay.container&&verified!=null&&verified.container==replay.container&&verified.record==replay.record&&Boolean.TRUE.equals(replay.container.getDataManager().getDataValue(EFNSKillDataKeys.REPLAY_ENHANCED.get()))&&Boolean.TRUE.equals(replay.container.getDataManager().getDataValue(EFNSKillDataKeys.IS_REPLAY_DAMAGE.get()))){if(REPLAY_HITS.size()>=128)REPLAY_HITS.clear();REPLAY_HITS.put(replay.event.getDamageSource(),new ReplayHit(action,replay.event.getTarget()));REPLAY.remove(patch.getOriginal().getUUID());}
    }
    public static void damage(LivingEntity target,DamageSource source){
        if(!BattleFinalQuestProof.real(source.getEntity()))return;ReplayHit replay=REPLAY_HITS.remove(source);if(replay!=null&&replay.target==target&&valid(replay.action)){REPLAY.remove(replay.action.patch.getOriginal().getUUID());BattleFinalQuestProof.emit(replay.action.patch.getOriginal(),"v3_combat_agile_school");}
        if(!(source instanceof EpicFightDamageSource epic)||epic.getAnimation()==null||!BattleFinalQuestProof.real(source.getEntity()))return;UUID actor=source.getEntity().getUUID();DynamicAnimation animation=epic.getAnimation().get();
        Action dusk=DUSK.get(actor);if(valid(dusk)&&dusk.stage3&&dusk.released&&dusk.epoch==epoch(dusk.patch)&&animation==EFNLanceAnimations.NF_MEEN_CHARGE3.get()){dusk.outcome=true;finish(DUSK,dusk,"v3_combat_heavy_school");}
        Action exile=EXILE.get(actor);if(valid(exile)&&exile.paid&&exile.epoch==epoch(exile.patch)&&exile.animation==animation&&exileB(animation)){EXILE.remove(actor);BattleFinalQuestProof.emit(source.getEntity(),"v3_combat_agile_school");}
    }
    public static void duskEffect(LivingEntity target,MobEffectInstance effect,boolean accepted,AssetAccessor<? extends DynamicAnimation> nativeAnimation){
        if(!accepted||!target.hasEffect(effect.getEffect())||!BattleFinalQuestProof.real(target)||nativeAnimation.get()!=EFNLanceAnimations.NF_MEEN_CHARGE3.get())return;
        Action dusk=DUSK.get(target.getUUID());if(valid(dusk)&&dusk.stage3&&dusk.released&&dusk.epoch==epoch(dusk.patch)&&(effect.getEffect()==MobEffects.DAMAGE_RESISTANCE||effect.getEffect()==EFNMobEffectRegistry.MEEN_LANCE.get())){dusk.outcome=true;finish(DUSK,dusk,"v3_combat_heavy_school");}
    }
    public static void bloodInstalled(LivingEntity target,MobEffectInstance effect,boolean accepted){
        if(effect.getEffect()!=EFNMobEffectRegistry.BLODDLUST.get()||!BattleFinalQuestProof.real(target))return;
        Action blood=BLOOD.get(target.getUUID());
        if(!accepted||!target.hasEffect(effect.getEffect())){BLOOD.remove(target.getUUID());return;}
        if(valid(blood)&&blood.epoch==epoch(blood.patch)&&blood.patch.getAnimator().getPlayerFor(null).getRealAnimation().get()==EFNTachiAnimations.NF_TACHI_BLOODLUST.get()){blood.installed=true;blood.outcome=true;finish(BLOOD,blood,"v3_combat_risky_school");}
    }
    public static void bloodPaid(LivingEntity actor,float before){if(!BattleFinalQuestProof.real(actor))return;Action action=BLOOD.get(actor.getUUID());if(valid(action)&&(action.installed||action.epoch==epoch(action.patch))&&actor.getHealth()<before&&actor.isAlive()&&actor.getHealth()>0){action.paid=true;if(actor.hasEffect(EFNMobEffectRegistry.BLODDLUST.get())){action.installed=true;action.outcome=true;}if(action.installed)finish(BLOOD,action,"v3_combat_risky_school");}}
    private static void finish(Map<UUID,Action> map,Action action,String quest){if(valid(action)&&action.paid&&action.outcome){map.remove(action.patch.getOriginal().getUUID());BattleFinalQuestProof.emit(action.patch.getOriginal(),quest);}}
    public static void removed(LivingEntity actor,MobEffectInstance effect){
        if(!BattleFinalQuestProof.real(actor))return;
        if(effect.getEffect()==EFNMobEffectRegistry.BLODDLUST.get())BLOOD.remove(actor.getUUID());
        if(effect.getEffect()==EFNMobEffectRegistry.REPLAY.get()){
            Contexts calls=CONTEXTS.get();boolean consuming=!calls.replays.isEmpty()&&calls.replays.peek().container.getExecutor().getOriginal()==actor;
            if(!consuming)REPLAY.remove(actor.getUUID());
        }
    }
    public static void forget(UUID actor){EPOCHS.remove(actor);DUSK.remove(actor);VANGUARD.remove(actor);EXILE.remove(actor);REPLAY.remove(actor);BLOOD.remove(actor);HISTORY.remove(actor);GUARDS.values().removeIf(hit->hit.action.patch.getOriginal().getUUID().equals(actor)||hit.target.getUUID().equals(actor));REPLAY_HITS.values().removeIf(hit->hit.action.patch.getOriginal().getUUID().equals(actor));}
    public static void clear(){EPOCHS.clear();DUSK.clear();VANGUARD.clear();EXILE.clear();REPLAY.clear();BLOOD.clear();HISTORY.clear();GUARDS.clear();REPLAY_HITS.clear();CONTEXTS.remove();}
}
