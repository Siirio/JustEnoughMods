package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.combattools.CombatPullProof;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CombatPullPlayerMovementQuestMixin {
    @Shadow public ServerPlayer player;
    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void jem$moved(ServerboundMovePlayerPacket packet, CallbackInfo callback) { CombatPullProof.moved(player); com.justenoughmods.achievementguide.integration.combattools.CombatRuneControlProof.moved(player); }
}
