package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestMotion;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntitySpectre", remap = false)
public abstract class WildlifeQuestSpectreTowMixin implements WildlifeQuestMotion.NativeForce {
    @Unique private UUID jem$holder;
    @Unique private Vec3 jem$motion;
    @Inject(method = "m_8119_", at = @At("HEAD"))
    private void jem$before(CallbackInfo callback) {
        Mob self = (Mob)(Object)this; jem$holder = null; jem$motion = null;
        if (self.getLeashHolder() instanceof ServerPlayer actor && WildlifeQuestProof.real(actor) != null && self.distanceTo(actor) > 10) {
            jem$holder = actor.getUUID(); jem$motion = actor.getDeltaMovement();
        }
    }
    @Inject(method = "m_8119_", at = @At("RETURN"))
    private void jem$towed(CallbackInfo callback) {
        Mob self = (Mob)(Object)this;
        if (jem$holder != null && jem$motion != null && self.getLeashHolder() instanceof ServerPlayer actor && jem$holder.equals(actor.getUUID())) WildlifeQuestMotion.force(actor, self, "spectre", actor.getDeltaMovement().subtract(jem$motion), null);
        jem$holder = null; jem$motion = null;
    }
    @Override public boolean jem$wildlifeForceActive(ServerPlayer actor) { Mob self = (Mob)(Object)this; return self.isAlive() && self.getLeashHolder() == actor; }
}
