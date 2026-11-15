package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.saltywater.sittingplus.networking.SittingPlusNetworking;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Grants the teaching proof to the authenticated sender of a real native sit packet. */
@Mixin(SittingPlusNetworking.StartSitPacket.class)
public abstract class SittingPlusQuestMixin {
    @Inject(method = "handle", at = @At("HEAD"), remap = false)
    private static void jem$recordAcceptedSit(SittingPlusNetworking.StartSitPacket packet,
                                              Supplier<NetworkEvent.Context> supplier,
                                              CallbackInfo callback) {
        var context = supplier.get();
        var player = context.getSender();
        if (player != null) context.enqueueWork(() -> QuestActionTrigger.INSTANCE.emit(player, "major_update_4_sitting_plus"));
    }
}
