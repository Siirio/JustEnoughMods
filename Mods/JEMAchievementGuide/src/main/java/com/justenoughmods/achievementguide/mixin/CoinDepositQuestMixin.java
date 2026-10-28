package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemeconomy.physical.PhysicalCoinService;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reads the owner's final physical-debit and ledger-credit acknowledgement without another deposit. */
@Pseudo
@Mixin(targets = "com.siirio.jemeconomy.physical.PhysicalCoinService", remap = false)
public abstract class CoinDepositQuestMixin {
    @Inject(method = "depositAll", at = @At("RETURN"))
    private static void jem$depositedAll(ServerPlayer player, UUID request,
                                       CallbackInfoReturnable<PhysicalCoinService.DepositResult> callback) {
        jem$committed(player, callback.getReturnValue());
    }

    @Inject(method = "depositExact", at = @At("RETURN"))
    private static void jem$depositedExact(ServerPlayer player, CoinDenomination denomination, int count, UUID request,
                                         CallbackInfoReturnable<PhysicalCoinService.DepositResult> callback) {
        jem$committed(player, callback.getReturnValue());
    }

    @Unique private static void jem$committed(ServerPlayer player, PhysicalCoinService.DepositResult result) {
        if (result != null && result.status() == PhysicalCoinService.Status.COMMITTED
                && result.value() > 0 && result.acceptedItems() > 0) {
            QuestActionTrigger.INSTANCE.emit(player, "v3_town_coin_deposit");
        }
    }
}
