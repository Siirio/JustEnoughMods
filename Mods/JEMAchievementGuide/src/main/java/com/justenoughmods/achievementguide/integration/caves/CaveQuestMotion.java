package com.justenoughmods.achievementguide.integration.caves;
import com.github.alexmodguy.alexscaves.server.potion.DarknessIncarnateEffect;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
/** Transient state belongs to one native player lifetime and one native movement episode. */
public final class CaveQuestMotion {
    private Vec3 hookStart,bounceStart,flightStart,totemPosition;
    private net.minecraft.world.entity.Mob totemTarget;
    private net.minecraft.world.level.pathfinder.Path totemPath;
    private long hookAt,bounceAt,flightAt,lastFlightTick;
    private int flightTicks;
    private boolean flightMoved;
    public void clear(){hookStart=null;bounceStart=null;flightStart=null;totemPosition=null;totemTarget=null;totemPath=null;flightTicks=0;flightMoved=false;}
    public static CaveQuestMotion of(ServerPlayer actor){return ((CaveQuestMotionCarrier)(Object)actor).jem$caveMotion();}
    public void hook(ServerPlayer actor){hookStart=actor.position();hookAt=actor.level().getGameTime();}
    public void bounce(ServerPlayer actor){bounceStart=actor.position();bounceAt=actor.level().getGameTime();}
    public void flight(ServerPlayer actor){flightStart=actor.position();flightAt=actor.level().getGameTime();lastFlightTick=flightAt-1;flightTicks=0;flightMoved=false;}
    public void totemBefore(ServerPlayer actor,net.minecraft.world.entity.Mob target){
        if(totemTarget==target && totemPath!=null && target.getNavigation().getPath()==totemPath && totemPosition!=null && target.position().distanceToSqr(totemPosition)>1.0e-6)CaveQuestProof.emit(actor,"v3_cave_occult_links");
        totemTarget=null;totemPath=null;totemPosition=null;
    }
    public void totemAfter(net.minecraft.world.entity.Mob target){totemTarget=target;totemPath=target.getNavigation().getPath();totemPosition=target.position();}
    public void effect(ServerPlayer actor){
        if(flightStart==null)return;
        long now=actor.level().getGameTime();
        if(CaveQuestProof.actor(actor)==null || DarknessIncarnateEffect.isInLight(actor,11) || !actor.hasEffect(ACEffectRegistry.DARKNESS_INCARNATE.get())
                || !actor.getAbilities().flying || now<flightAt || now-lastFlightTick>1){flightStart=null;return;}
        if(now!=lastFlightTick){flightTicks++;lastFlightTick=now;}
        if(flightTicks>=20 && flightMoved){CaveQuestProof.emit(actor,"v3_cave_shadow_flight");flightStart=null;}
    }
    public void moved(ServerPlayer actor,Vec3 before){
        if(CaveQuestProof.actor(actor)==null || !Double.isFinite(actor.position().lengthSqr()) || actor.position().distanceToSqr(before)<=1.0e-8)return;
        long now=actor.level().getGameTime();
        if(hookStart!=null){if(now-hookAt>20 || now<hookAt)hookStart=null;else if(actor.position().distanceToSqr(hookStart)>1.0e-6){CaveQuestProof.emit(actor,"v3_cave_candy_traversal");hookStart=null;}}
        if(bounceStart!=null){if(now-bounceAt>20 || now<bounceAt)bounceStart=null;else if(actor.getY()>bounceStart.y+0.01){CaveQuestProof.emit(actor,"v3_cave_candy_traversal_bounce");bounceStart=null;}}
        if(flightStart!=null && actor.hasEffect(ACEffectRegistry.DARKNESS_INCARNATE.get()) && actor.getAbilities().flying
                && !actor.onGround() && !DarknessIncarnateEffect.isInLight(actor,11))flightMoved=true;
    }
}
