package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.criterion.JemCriteria;
import immersive_melodies.network.c2s.NoteBroadcastRequest;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NoteBroadcastRequest.class, remap = false)
public abstract class ImmersiveMelodiesMixin {
    private static final ResourceLocation PLAY_A_NOTE = new ResourceLocation("jem_guide", "cool_stuff/play_a_note");

    @Inject(method = "handle", at = @At("TAIL"))
    private void afterNoteBroadcast(Player player, CallbackInfo ci) {
        NoteBroadcastRequest request = (NoteBroadcastRequest) (Object) this;
        if (player instanceof ServerPlayer serverPlayer && request.velocity() > 0) {
            JemCriteria.fire(serverPlayer, PLAY_A_NOTE);
            // The native server has accepted/broadcast this note. Require an actual held instrument,
            // a playable MIDI tone and the native 8-bit positive velocity instead of menu opening.
            if (request.tone() >= 0 && request.tone() <= 127 && request.velocity() <= 255
                    && (player.getMainHandItem().getItem() instanceof immersive_melodies.item.InstrumentItem
                    || player.getOffhandItem().getItem() instanceof immersive_melodies.item.InstrumentItem)) {
                com.justenoughmods.achievementguide.advancement.QuestActionTrigger.INSTANCE.emit(serverPlayer, "play_note");
            }
        }
    }
}
