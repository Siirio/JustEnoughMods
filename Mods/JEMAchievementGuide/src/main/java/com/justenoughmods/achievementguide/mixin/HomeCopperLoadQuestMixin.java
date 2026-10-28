package com.justenoughmods.achievementguide.mixin;

import com.github.smallinger.copperagebackport.entity.CopperGolemEntity;
import com.justenoughmods.achievementguide.integration.home.HomeCopperProof;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CopperGolemEntity.class, remap = false)
public abstract class HomeCopperLoadQuestMixin {
    @Inject(method = "m_7378_", at = @At("HEAD"))
    private void jem$before(CompoundTag tag, CallbackInfo callback) { HomeCopperProof.loading((CopperGolemEntity)(Object)this); }
    @Inject(method = "m_7378_", at = @At("RETURN"))
    private void jem$after(CompoundTag tag, CallbackInfo callback) { HomeCopperProof.loaded((CopperGolemEntity)(Object)this); }
}
