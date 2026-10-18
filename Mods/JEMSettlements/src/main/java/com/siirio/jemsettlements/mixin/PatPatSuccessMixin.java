package com.siirio.jemsettlements.mixin;

import net.lopymine.patpat.packet.PatPacket;
import net.lopymine.patpat.server.packet.PatPatServerPacketManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** PatPat 1.3.1 success tail is after native rate/list/visibility validation; early returns never award. */
@Mixin(value = PatPatServerPacketManager.class, remap = false)
public abstract class PatPatSuccessMixin {
    @Inject(method = "handlePacket", at = @At("TAIL"), remap = false)
    private static void jem$successfulPat(ServerPlayer player, PatPacket<ServerLevel, ?> packet, CallbackInfo ci) {
        var server = player.getServer();
        if (server == null) return;
        var target = packet.getPattedEntity(player.serverLevel());
        if (target instanceof net.minecraft.world.entity.npc.Villager villager) {
            java.util.UUID id = villager.getUUID();
            server.execute(() -> com.siirio.jemsettlements.happiness.ResidentPatService.accept(player, id));
        }
    }
}
