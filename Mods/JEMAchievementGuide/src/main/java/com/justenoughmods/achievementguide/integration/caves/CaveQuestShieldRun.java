package com.justenoughmods.achievementguide.integration.caves;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=com.justenoughmods.achievementguide.JemAdvancementGuide.MOD_ID)
public final class CaveQuestShieldRun {
    public record Outcome(boolean result,boolean blocked){}
    public static final class Run {public final LivingEntity target;public final DamageSource source;public ShieldBlockEvent event;Run(LivingEntity target,DamageSource source){this.target=target;this.source=source;}}
    private static final ThreadLocal<Run> CURRENT=new ThreadLocal<>();private CaveQuestShieldRun(){}
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)public static void observed(ShieldBlockEvent event){var run=CURRENT.get();if(run!=null && event.getEntity()==run.target && event.getDamageSource()==run.source)run.event=event;}
    public static Outcome hurt(LivingEntity target,DamageSource source,BooleanSupplier nativeCall){
        var previous=CURRENT.get();var run=new Run(target,source);CURRENT.set(run);boolean result;
        try{result=nativeCall.getAsBoolean();}finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
        return new Outcome(result,run.event!=null && !run.event.isCanceled() && Float.isFinite(run.event.getBlockedDamage()) && run.event.getBlockedDamage()>0);
    }
}
