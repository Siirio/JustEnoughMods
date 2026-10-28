package com.justenoughmods.achievementguide.integration.battlefinal;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.Event;
public final class BattleFinalQuestLife {
    public record HurtView(LivingEntity target,DamageSource source){}
    private static final class Frame {
        final LivingEntity target;final DamageSource source;final float vitality;final boolean wasDead;
        final List<Runnable> direct=new ArrayList<>();final List<Pending> pending=new ArrayList<>();
        Frame(LivingEntity target,DamageSource source,boolean dead){this.target=target;this.source=source;vitality=target.getHealth()+target.getAbsorptionAmount();wasDead=dead;}
    }
    private static final class Pending {final Event event;final Runnable action;boolean approved;Pending(Event event,Runnable action){this.event=event;this.action=action;}}
    private static final ThreadLocal<Calls> HURT=ThreadLocal.withInitial(Calls::new),DEATH=ThreadLocal.withInitial(Calls::new);
    private static final class Calls {final ArrayDeque<Frame> frames=new ArrayDeque<>();int overflow;}
    private BattleFinalQuestLife(){}
    private static void begin(ThreadLocal<Calls> local,LivingEntity target,DamageSource source,boolean dead){Calls calls=local.get();if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;}calls.frames.push(new Frame(target,source,dead));}
    public static void hurtBegin(LivingEntity target,DamageSource source){begin(HURT,target,source,false);}
    public static void deathBegin(LivingEntity target,DamageSource source,boolean dead){begin(DEATH,target,source,dead);}
    public static HurtView hurtFor(Entity actor){Calls calls=HURT.get();if(calls.overflow!=0||calls.frames.isEmpty())return null;Frame frame=calls.frames.peek();return frame.source.getEntity()==actor?new HurtView(frame.target,frame.source):null;}
    public static void direct(LivingEntity target,DamageSource source,Runnable action){Calls calls=HURT.get();if(calls.overflow!=0||calls.frames.isEmpty())return;Frame frame=calls.frames.peek();if(frame.target==target&&frame.source==source&&frame.direct.size()<16)frame.direct.add(action);}
    public static void event(Event event,Runnable action){
        ThreadLocal<Calls> local=event instanceof LivingDeathEvent?DEATH:HURT;Calls calls=local.get();if(calls.overflow!=0||calls.frames.isEmpty())return;Frame frame=calls.frames.peek();
        LivingEntity target=event instanceof LivingDeathEvent death?death.getEntity():event instanceof LivingAttackEvent attack?attack.getEntity():null;
        DamageSource source=event instanceof LivingDeathEvent death?death.getSource():event instanceof LivingAttackEvent attack?attack.getSource():null;
        if(target==frame.target&&source==frame.source&&frame.pending.size()<16)frame.pending.add(new Pending(event,action));
    }
    public static void posted(Event event){
        Entity entity=event instanceof LivingDeathEvent death?death.getEntity():event instanceof LivingAttackEvent attack?attack.getEntity():null;
        if(entity==null||!(entity.level() instanceof net.minecraft.server.level.ServerLevel level)||!level.getServer().isSameThread())return;
        for(ThreadLocal<Calls> local:List.of(HURT,DEATH)){Calls calls=local.get();for(Frame frame:calls.frames)for(Pending pending:frame.pending)if(pending.event==event)pending.approved=!event.isCanceled();}
    }
    private static Frame pop(ThreadLocal<Calls> local){Calls calls=local.get();if(calls.overflow>0){calls.overflow--;return null;}Frame frame=calls.frames.isEmpty()?null:calls.frames.pop();if(calls.frames.isEmpty())local.remove();return frame;}
    private static void commit(Frame frame){for(Runnable action:frame.direct)action.run();for(Pending pending:frame.pending)if(pending.approved&&!pending.event.isCanceled())pending.action.run();}
    public static void hurtEnd(LivingEntity target,boolean accepted){Frame frame=pop(HURT);if(frame==null||frame.target!=target||!accepted||!Float.isFinite(frame.vitality)||!Float.isFinite(target.getHealth()+target.getAbsorptionAmount())||target.getHealth()+target.getAbsorptionAmount()>=frame.vitality)return;
        if(net.minecraftforge.fml.ModList.get().isLoaded("efn"))BattleFinalQuestNightfall.vanguardDamage(target,frame.source);
        if(!BattleFinalQuestProof.target(target))return;commit(frame);
        if(net.minecraftforge.fml.ModList.get().isLoaded("epicfight"))BattleFinalQuestMeteor.damage(target,frame.source);
        if(net.minecraftforge.fml.ModList.get().isLoaded("efn"))BattleFinalQuestNightfall.damage(target,frame.source);
    }
    public static void deathEnd(LivingEntity target,boolean dead){Frame frame=pop(DEATH);if(frame!=null&&frame.target==target&&!frame.wasDead&&dead&&BattleFinalQuestProof.target(target))commit(frame);}
    public static void clear(){HURT.remove();DEATH.remove();}
}
