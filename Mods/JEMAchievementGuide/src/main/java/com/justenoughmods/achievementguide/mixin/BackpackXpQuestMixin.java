package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.FluidTransferObservation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.XpHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeWrapper", remap = false)
public abstract class BackpackXpQuestMixin {
    @Unique private ServerPlayer jem$actor;
    @Unique private int jem$experience;
    @Unique private long jem$fluid;

    @Inject(method = {
            "tryGivePlayerExperienceFromTank(Lnet/minecraft/world/entity/player/Player;Lnet/p3pp3rf1y/sophisticatedcore/api/IStorageFluidHandler;IZ)V",
            "tryFillTankWithPlayerExperience(Lnet/minecraft/world/entity/player/Player;Lnet/p3pp3rf1y/sophisticatedcore/api/IStorageFluidHandler;IZ)V"
    }, at = @At("HEAD"))
    private void jem$beforeExchange(Player player, IStorageFluidHandler tank, int target, boolean ignoreLimit, CallbackInfo callback) {
        jem$actor = null;
        if (player instanceof ServerPlayer actor && ((IUpgradeWrapper) this).isEnabled()) {
            jem$fluid = FluidTransferObservation.experience(FluidTransferObservation.snapshot(tank));
            jem$experience = XpHelper.getPlayerTotalExperience(actor);
            if (jem$fluid >= 0) jem$actor = actor;
        }
    }

    @Inject(method = {
            "tryGivePlayerExperienceFromTank(Lnet/minecraft/world/entity/player/Player;Lnet/p3pp3rf1y/sophisticatedcore/api/IStorageFluidHandler;IZ)V",
            "tryFillTankWithPlayerExperience(Lnet/minecraft/world/entity/player/Player;Lnet/p3pp3rf1y/sophisticatedcore/api/IStorageFluidHandler;IZ)V"
    }, at = @At("RETURN"))
    private void jem$afterExchange(Player player, IStorageFluidHandler tank, int target, boolean ignoreLimit, CallbackInfo callback) {
        ServerPlayer actor = jem$actor;
        jem$actor = null;
        if (actor != player || actor == null) return;
        long after = FluidTransferObservation.experience(FluidTransferObservation.snapshot(tank));
        if (after < 0) return;
        long fluidChange = after - jem$fluid;
        int xpChange = XpHelper.getPlayerTotalExperience(actor) - jem$experience;
        long amount = Math.abs(fluidChange);
        if (amount == 0 || amount > Integer.MAX_VALUE || xpChange == 0) return;
        int converted = (int) XpHelper.liquidToExperience((int) amount);
        if (converted > 0 && (fluidChange > 0 && xpChange == -converted || fluidChange < 0 && xpChange == converted)) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_store_xp_power");
        }
    }
}
