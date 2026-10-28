package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;
import org.primal.entity.animal.WalrusEntity;
import org.primal.networking.packets.WalrusJumpPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.primal.networking.Primal_HandlePackets$OnServer", remap = false)
public abstract class CreatureCareWalrusCommandMixin {
    @Inject(method = "lambda$handleWalrusJumpPacket$0", at = @At("RETURN"))
    private static void jem$committed(Supplier<NetworkEvent.Context> context, WalrusJumpPacket packet, CallbackInfo callback) {
        var actor = CreatureCareProof.real(context.get().getSender());
        if (actor == null || !Float.isFinite(packet.playerJumpPendingScale()) || packet.playerJumpPendingScale() < 0 || packet.playerJumpPendingScale() > 1) return;
        if (actor.serverLevel().getEntity(packet.entityId()) instanceof WalrusEntity walrus
                && walrus.getControllingPassenger() == actor && actor.getRootVehicle() == walrus && walrus.isTamed()
                && actor.getUUID().equals(walrus.getOwnerUUID()) && walrus.getJumpCooldown() == 40
                && (walrus.getSlamDuration() == 10 || walrus.getWhirlwindDuration() == 20)) {
            CreatureCareProof.mark(walrus, "walrus_command", actor.getUUID());
            CreatureCareProof.data(walrus).putLong("walrus_command_until", walrus.level().getGameTime() + 40);
            CreatureCareProof.data(walrus).putBoolean("walrus_command_water", walrus.isInWater());
        }
    }
}
