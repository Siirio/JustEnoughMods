package com.siirio.jemserver.mixin.events;

import com.siirio.jemserver.smp.events.BloodMoon;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=Warden.class,priority=1100)
public abstract class BloodMoonWardenTargetMixin {
    @Inject(method="canTargetEntity",at=@At("HEAD"),cancellable=true)
    private void jem$ignoreBloodMoonSpectators(Entity target,CallbackInfoReturnable<Boolean> callback) {
        if(!BloodMoon.canTarget((Warden)(Object)this,target)) callback.setReturnValue(false);
    }

    @Inject(method="hurt",at=@At("HEAD"),cancellable=true)
    private void jem$protectBloodMoonWardenFromSculksaur(DamageSource source,float amount,CallbackInfoReturnable<Boolean> callback) {
        Warden warden=(Warden)(Object)this;
        Entity attacker=source.getEntity();
        if(!warden.getPersistentData().hasUUID(BloodMoon.EVENT_ID)||attacker==null) return;
        var id=ForgeRegistries.ENTITY_TYPES.getKey(attacker.getType());
        if(id!=null&&id.getNamespace().equals("ec_ad")&&id.getPath().equals("sculksaur")) callback.setReturnValue(false);
    }
}
