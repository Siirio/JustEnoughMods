package com.siirio.jemserver.mixin.cooking;

import com.siirio.jemserver.smp.events.CookingStations;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.satisfy.brewery.core.block.entity.BrewstationBlockEntity", remap = false)
public abstract class BrewstationMixin {
    @Shadow private ItemStack beer;

    @Inject(method = "brew", at = @At("RETURN"), remap = false)
    private void jem$brewMade(CallbackInfo ci) {
        CookingStations.produced(this, beer);
    }
}
