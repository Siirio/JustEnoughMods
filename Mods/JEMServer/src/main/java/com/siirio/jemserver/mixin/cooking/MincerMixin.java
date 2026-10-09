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
@Mixin(targets = {"net.satisfy.farm_and_charm.core.block.entity.MincerBlockEntity"}, remap = false)
public abstract class MincerMixin {
    @Unique private Map<Item, Integer> jem$before;

    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/satisfy/farm_and_charm/core/block/entity/MincerBlockEntity;)V", at = @At("HEAD"), remap = false)
    private void jem$beforeCooking(CallbackInfo ci) {
        jem$before = CookingStations.snapshot(this);
    }

    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/satisfy/farm_and_charm/core/block/entity/MincerBlockEntity;)V", at = @At("RETURN"), remap = false)
    private void jem$afterCooking(CallbackInfo ci) {
        CookingStations.completed(this, jem$before);
    }
}
