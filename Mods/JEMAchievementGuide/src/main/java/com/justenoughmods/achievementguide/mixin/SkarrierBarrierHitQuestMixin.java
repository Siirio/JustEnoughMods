package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.SkarrierWorkshopProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "net.mcreator.skarriermobs.entity.WretchedToothCleavingEntity", remap = false)
public abstract class SkarrierBarrierHitQuestMixin {
    @Inject(method = "m_5790_", at = @At("HEAD"))
    private void jem$begin(EntityHitResult hit, CallbackInfo callback) {
        SkarrierWorkshopProof.begin("barrier_hit", SkarrierWorkshopProof.barrierOwner((Entity)(Object)this), hit.getEntity(), null);
    }
    @Inject(method = "m_5790_", at = @At("RETURN"))
    private void jem$end(EntityHitResult hit, CallbackInfo callback) { SkarrierWorkshopProof.end(); }
}
