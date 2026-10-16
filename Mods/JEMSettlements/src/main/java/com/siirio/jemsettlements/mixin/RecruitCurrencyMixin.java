package com.siirio.jemsettlements.mixin;

import com.siirio.jemidentity.api.DedicatedSmp;
import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CommandEvents.class, remap = false)
public abstract class RecruitCurrencyMixin {
    @Inject(method = "handleRecruiting", at = @At("HEAD"), cancellable = true)
    private static void jem$coinHire(Player player, RecruitsGroup group, AbstractRecruitEntity recruit, boolean dialogue, CallbackInfoReturnable<Boolean> cir) {
        if (!(player instanceof ServerPlayer serverPlayer) || !DedicatedSmp.active(serverPlayer)) return;
        if (group != null && !player.getUUID().equals(group.getPlayerUUID())) { cir.setReturnValue(false); return; }
        boolean hired = recruit.hire(player, group, dialogue);
        // Preserve the native successful-hire faction bookkeeping after omitting
        // only its emerald inventory scan/removal.
        if (hired && player.getTeam() != null) FactionEvents.addNPCToData(serverPlayer.serverLevel(), player.getTeam().getName(), 1);
        cir.setReturnValue(hired);
    }
}
