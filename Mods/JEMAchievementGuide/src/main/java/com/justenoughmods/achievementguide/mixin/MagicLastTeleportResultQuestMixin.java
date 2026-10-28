package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastWorldProof;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class MagicLastTeleportResultQuestMixin {
    @Inject(method = "teleportTo(DDD)V", at = @At("HEAD"))
    private void jem$begin(double x, double y, double z, CallbackInfo callback) { MagicLastWorldProof.moveBegin((Entity)(Object)this); }
    @Inject(method = "teleportTo(DDD)V", at = @At("RETURN"))
    private void jem$end(double x, double y, double z, CallbackInfo callback) { MagicLastWorldProof.moveEnd((Entity)(Object)this); }
}
