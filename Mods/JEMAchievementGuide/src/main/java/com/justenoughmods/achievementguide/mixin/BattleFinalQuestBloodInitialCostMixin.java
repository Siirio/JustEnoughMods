package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=com.hm.efn.mobeffects.BloodlustEnhance.class,remap=false)
public abstract class BattleFinalQuestBloodInitialCostMixin {
    @Unique private static final ThreadLocal<Float> jem$health=new ThreadLocal<>();
    @Inject(method="onEffectAdded",at=@At("HEAD"))private static void jem$before(net.minecraftforge.event.entity.living.MobEffectEvent.Added event,CallbackInfo ci){jem$health.set(event.getEntity().getHealth());}
    @Inject(method="onEffectAdded",at=@At("RETURN"))private static void jem$after(net.minecraftforge.event.entity.living.MobEffectEvent.Added event,CallbackInfo ci){Float before=jem$health.get();jem$health.remove();if(before!=null&&event.getEffectInstance().getEffect()==com.hm.efn.registries.EFNMobEffectRegistry.BLODDLUST.get())BattleFinalQuestNightfall.bloodPaid(event.getEntity(),before);}
}
