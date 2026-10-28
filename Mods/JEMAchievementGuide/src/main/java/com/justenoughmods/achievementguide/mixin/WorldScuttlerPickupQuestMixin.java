package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldShellGrowthProof;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class WorldScuttlerPickupQuestMixin {
    @Unique private WorldShellGrowthProof.Pickup jem$suppliedJelly;
    @Inject(method = "pickUpItem", at = @At("HEAD"))
    private void jem$nativePickup(ItemEntity drop, CallbackInfo ci) { jem$suppliedJelly = WorldShellGrowthProof.pickup((Mob)(Object)this, drop); }
    @Inject(method = "pickUpItem", at = @At("RETURN"))
    private void jem$nativeEquipped(ItemEntity drop, CallbackInfo ci) {
        WorldShellGrowthProof.picked((Mob)(Object)this, jem$suppliedJelly); jem$suppliedJelly = null;
    }
}
