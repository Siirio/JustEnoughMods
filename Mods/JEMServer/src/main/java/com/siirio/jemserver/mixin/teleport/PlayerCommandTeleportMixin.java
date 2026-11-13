package com.siirio.jemserver.mixin.teleport;

import com.mojang.brigadier.ParseResults;
import com.siirio.jemserver.teleport.PlayerCommandTeleport;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerCommandTeleportMixin {
    @Shadow public ServerPlayer player;

    @Redirect(method = "performChatCommand", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/commands/Commands;performCommand(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)I"))
    private int jem$dispatchPlayerCommand(Commands commands, ParseResults<CommandSourceStack> parsed, String input) {
        return PlayerCommandTeleport.dispatch(commands, parsed, input, player);
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void jem$cancelPendingArrival(double x, double y, double z, float yaw, float pitch,
                                          Set<RelativeMovement> relative, CallbackInfo callback) {
        if (player.server.isSameThread() && (x != player.getX() || y != player.getY() || z != player.getZ())) {
            PlayerCommandTeleport.cancel(player);
        }
    }
}
