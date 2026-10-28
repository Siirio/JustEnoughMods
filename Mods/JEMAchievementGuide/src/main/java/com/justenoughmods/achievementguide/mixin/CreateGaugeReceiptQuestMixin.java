package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateGaugeProofs;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.logistics.packager.PackagerBlockEntity", remap = false)
public abstract class CreateGaugeReceiptQuestMixin {
    @Unique private CreateGaugeProofs.Receipt jem$receipt;
    @Inject(method = "unwrapBox", at = @At("HEAD"), remap = false)
    private void jem$before(ItemStack box, boolean simulate, CallbackInfoReturnable<Boolean> callback) {
        jem$receipt = CreateGaugeProofs.receiving((PackagerBlockEntity)(Object)this, box, simulate);
    }
    @Inject(method = "unwrapBox", at = @At("RETURN"), remap = false)
    private void jem$after(ItemStack box, boolean simulate, CallbackInfoReturnable<Boolean> callback) {
        CreateGaugeProofs.received(jem$receipt, !simulate && callback.getReturnValueZ()); jem$receipt = null;
    }
}
