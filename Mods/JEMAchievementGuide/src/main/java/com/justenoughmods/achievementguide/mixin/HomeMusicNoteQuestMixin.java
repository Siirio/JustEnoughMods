package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeInteractionProof;
import immersive_melodies.item.InstrumentItem;
import immersive_melodies.network.c2s.NoteBroadcastRequest;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NoteBroadcastRequest.class, remap = false)
public abstract class HomeMusicNoteQuestMixin {
    @Inject(method = "handle", at = @At("RETURN"))
    private void jem$accepted(Player player, CallbackInfo callback) {
        NoteBroadcastRequest note = (NoteBroadcastRequest)(Object)this;
        if (HomeInteractionProof.actor(player) != null && note.tone() >= 0 && note.tone() <= 127
                && note.velocity() > 0 && note.velocity() <= 255
                && (player.getMainHandItem().getItem() instanceof InstrumentItem || player.getOffhandItem().getItem() instanceof InstrumentItem))
            HomeInteractionProof.emit(player, "v3_home_play_music_note");
    }
}
