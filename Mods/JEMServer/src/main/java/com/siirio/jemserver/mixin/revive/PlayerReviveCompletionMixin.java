package com.siirio.jemserver.mixin.revive;

import com.siirio.jemserver.smp.events.ReviveSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.playerrevive.server.PlayerReviveServer;

@Pseudo
@Mixin(targets = "team.creative.playerrevive.server.PlayerReviveServer", remap = false)
public abstract class PlayerReviveCompletionMixin {
    @Unique private static final Map<UUID, List<ServerPlayer>> jemHelpers = new HashMap<>();

    @Inject(method = "startBleeding", at = @At("HEAD"), cancellable = true)
    private static void jemDirectActivation(Player player, DamageSource source, CallbackInfo callback) {
        if (player instanceof ServerPlayer serverPlayer && ReviveSupport.session(serverPlayer) == null) callback.cancel();
    }

    @Inject(method = "revive", at = @At("HEAD"))
    private static void jemCaptureHelpers(Player player, CallbackInfo callback) {
        if (!(player instanceof ServerPlayer target) || !PlayerReviveServer.isBleeding(player)) return;
        var helpers = PlayerReviveServer.getBleeding(player).revivingPlayers().stream()
                .filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast)
                .filter(helper -> ReviveSupport.sameSession(target, helper)).toList();
        jemHelpers.put(target.getUUID(), helpers);
    }

    @Inject(method = "revive", at = @At("RETURN"))
    private static void jemContribution(Player player, CallbackInfo callback) {
        var helpers = jemHelpers.remove(player.getUUID());
        if (helpers != null && !PlayerReviveServer.isBleeding(player)) helpers.forEach(ReviveSupport::revived);
    }
}
