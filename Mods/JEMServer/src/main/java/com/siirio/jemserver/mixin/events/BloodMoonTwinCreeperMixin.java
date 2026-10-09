package com.siirio.jemserver.mixin.events;

import com.siirio.jemserver.smp.events.BloodMoonSetPieces;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class BloodMoonTwinCreeperMixin {
    @Shadow private int swell;
    @Shadow private int oldSwell;
    @Shadow public abstract void setSwellDir(int direction);

    @Inject(method="explodeCreeper",at=@At("HEAD"),cancellable=true)
    private void jem$surviveTwinDetonation(CallbackInfo callback) {
        Creeper creeper=(Creeper)(Object)this;
        if(!BloodMoonSetPieces.survivingTwin(creeper)) return;
        creeper.level().explode(creeper,creeper.getX(),BloodMoonSetPieces.twinExplosionY(creeper),creeper.getZ(),BloodMoonSetPieces.twinExplosionPower(),Level.ExplosionInteraction.NONE);
        swell=0;
        oldSwell=0;
        setSwellDir(-1);
        callback.cancel();
    }
}
