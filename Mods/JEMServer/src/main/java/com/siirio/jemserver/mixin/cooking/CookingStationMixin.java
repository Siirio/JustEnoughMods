package com.siirio.jemserver.mixin.cooking;

import com.siirio.jemserver.smp.events.CookingStations;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

@Pseudo
@Mixin(targets = {"net.satisfy.bakery.core.block.entity.SmallCookingPotBlockEntity",
        "net.satisfy.herbalbrews.core.blocks.entity.CauldronBlockEntity",
        "net.satisfy.farm_and_charm.core.block.entity.CookingPotBlockEntity",
        "net.satisfy.farm_and_charm.core.block.entity.RoasterBlockEntity",
        "net.satisfy.farm_and_charm.core.block.entity.StoveBlockEntity",
        "net.satisfy.beachparty.core.block.entity.MiniFridgeBlockEntity",
        "net.satisfy.beachparty.core.block.entity.PalmBarBlockEntity",
        "net.satisfy.herbalbrews.core.blocks.entity.TeaKettleBlockEntity",
        "net.satisfy.vinery.core.block.entity.FermentationBarrelBlockEntity"}, remap = false)
public abstract class CookingStationMixin {
    @Unique private Map<Item, Integer> jem$before;

    @Inject(method = "craft", at = @At("HEAD"), remap = false)
    private void jem$beforeCooking(CallbackInfo ci) {
        jem$before = CookingStations.snapshot(this);
    }

    @Inject(method = "craft", at = @At("RETURN"), remap = false)
    private void jem$afterCooking(CallbackInfo ci) {
        CookingStations.completed(this, jem$before);
    }
}
