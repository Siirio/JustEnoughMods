package com.siirio.jemsettlements.mixin;

import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.network.SettlementNetwork;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VillagerNobleEntity.class, remap = false)
public abstract class NobleQuoteMixin {
    @Inject(method = "openTradeGUI", at = @At("TAIL"))
    private void jem$quotes(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && SettlementAuthority.active(serverPlayer))
            SettlementNetwork.sendNobleQuote(serverPlayer, (VillagerNobleEntity) (Object) this);
    }
}
