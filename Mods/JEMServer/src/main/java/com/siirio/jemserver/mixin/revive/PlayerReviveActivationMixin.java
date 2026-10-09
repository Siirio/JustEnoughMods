package com.siirio.jemserver.mixin.revive;

import com.siirio.jemserver.smp.events.ReviveSupport;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.playerrevive.server.PlayerReviveServer;

@Pseudo
@Mixin(targets = "team.creative.playerrevive.server.ReviveEventServer", remap = false)
public abstract class PlayerReviveActivationMixin {
    @Inject(method = "playerDied", at = @At("HEAD"), cancellable = true)
    private void jemActivation(LivingDeathEvent event, CallbackInfo callback) {
        if (event.getEntity() instanceof ServerPlayer player && ReviveSupport.session(player) == null) callback.cancel();
    }

    @Inject(method = "playerTick", at = @At("HEAD"), cancellable = true)
    private void jemSessionEnded(TickEvent.PlayerTickEvent event, CallbackInfo callback) {
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof ServerPlayer player) || !PlayerReviveServer.isBleeding(player)) return;
        if (ReviveSupport.session(player) == null) {
            PlayerReviveServer.kill(player);
            callback.cancel();
            return;
        }
        for (var helper : java.util.List.copyOf(PlayerReviveServer.getBleeding(player).revivingPlayers()))
            if (!(helper instanceof ServerPlayer serverHelper) || !ReviveSupport.sameSession(player, serverHelper))
                PlayerReviveServer.removePlayerAsHelper(helper);
    }

    @Inject(method = "playerInteract", at = @At("HEAD"), cancellable = true)
    private void jemPartyHelp(PlayerInteractEvent.EntityInteract event, CallbackInfo callback) {
        if (event.getEntity() instanceof ServerPlayer helper && event.getTarget() instanceof ServerPlayer target
                && PlayerReviveServer.isBleeding(target) && !ReviveSupport.sameSession(helper, target)) callback.cancel();
    }
}
