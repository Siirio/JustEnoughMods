package com.siirio.jemcompat.mixin.gate;

import com.siirio.jemcompat.gate.CampaignEyeService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class CampaignEyeItemEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void jemcompat$rescueFromVoid(CallbackInfo callback) {
        ItemEntity item = (ItemEntity) (Object) this;
        if (!(item.level() instanceof ServerLevel level)
                || !CampaignEyeService.isCampaignEye(item.getItem())
                || item.getY() >= level.getMinBuildHeight() - 16) {
            return;
        }
        item.setPos(Vec3.atCenterOf(level.getSharedSpawnPos()).add(0.0, 2.0, 0.0));
        item.setDeltaMovement(Vec3.ZERO);
        item.clearFire();
    }
}
