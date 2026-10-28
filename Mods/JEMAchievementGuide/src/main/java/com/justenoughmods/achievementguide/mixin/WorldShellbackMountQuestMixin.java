package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldShellGrowthProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.obscuria.aquamirae.common.entity.Shellback", remap = false)
public abstract class WorldShellbackMountQuestMixin {
    @Inject(method = "m_20348_", at = @At("RETURN"))
    private void jem$nativeTamedMount(Entity rider, CallbackInfo ci) { WorldShellGrowthProof.mounted((TamableAnimal)(Object)this, rider); }
}
