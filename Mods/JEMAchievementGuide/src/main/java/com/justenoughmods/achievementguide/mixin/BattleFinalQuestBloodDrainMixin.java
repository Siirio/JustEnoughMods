package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.BattleFinalQuestNightfall;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.hm.efn.mobeffects.BloodlustEnhance$BloodlustEffect",remap=false)
public abstract class BattleFinalQuestBloodDrainMixin {
    @Unique private final ThreadLocal<Float> jem$health=new ThreadLocal<>();
    @Inject(method="m_6742_",at=@At("HEAD"))private void jem$before(LivingEntity actor,int amplifier,CallbackInfo ci){jem$health.set(actor.getHealth());}
    @Inject(method="m_6742_",at=@At("RETURN"))private void jem$after(LivingEntity actor,int amplifier,CallbackInfo ci){Float before=jem$health.get();jem$health.remove();if(before!=null)BattleFinalQuestNightfall.bloodPaid(actor,before);}
}
