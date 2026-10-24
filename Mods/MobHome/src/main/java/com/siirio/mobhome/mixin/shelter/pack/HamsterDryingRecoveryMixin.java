package com.siirio.mobhome.mixin.shelter.pack;

import com.siirio.mobhome.feature.shelter.CompanionAdapters;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.ninni.spawn.server.entity.mob.Hamster", remap = false)
public abstract class HamsterDryingRecoveryMixin {
    @Inject(method = "convertToDriedBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemOneRecoveryOwnerForDrying(CallbackInfo callback) {
        LivingEntity hamster = (LivingEntity) (Object) this;
        if (hamster.level().isClientSide || !CompanionAdapters.bound(hamster)) return;
        var key = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("spawn", "dry_up"));
        var type = hamster.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
        // Same native death source, without also materializing a second serialized pet/inventory in a block.
        hamster.hurt(new DamageSource(type), Float.MAX_VALUE);
        callback.cancel();
    }
}
