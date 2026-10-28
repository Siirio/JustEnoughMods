package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.BackpackCookingProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = {
        "net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeWrapper",
        "net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoCookingUpgradeWrapper"
}, remap = false)
public abstract class BackpackCookingTickQuestMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedcore/upgrades/cooking/CookingLogic;tick(Lnet/minecraft/world/level/Level;)Z"))
    private boolean jem$nativeCooking(CookingLogic<?> logic, Level world, Entity entity, Level level, BlockPos position) {
        return BackpackCookingProof.tick(this, logic, entity, world);
    }
}
