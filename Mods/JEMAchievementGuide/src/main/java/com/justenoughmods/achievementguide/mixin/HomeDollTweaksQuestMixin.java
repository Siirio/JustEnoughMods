package com.justenoughmods.achievementguide.mixin;

import com.github.ysbbbbbb.kaleidoscopedoll.network.message.DollTweakersMessage;
import com.justenoughmods.achievementguide.integration.home.HomeDollProof;
import com.justenoughmods.achievementguide.integration.home.HomeDollPacketView;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DollTweakersMessage.class, remap = false)
public abstract class HomeDollTweaksQuestMixin implements HomeDollPacketView {
    @Accessor("entityId") public abstract int jem$homeDollId();
    @Inject(method = "lambda$handle$0", at = @At("HEAD"))
    private static void jem$before(NetworkEvent.Context context, DollTweakersMessage message, CallbackInfo callback) {
        HomeDollProof.beforeTransform(context.getSender(), ((HomeDollPacketView)message).jem$homeDollId());
    }
    @Inject(method = "lambda$handle$0", at = @At("RETURN"))
    private static void jem$after(NetworkEvent.Context context, DollTweakersMessage message, CallbackInfo callback) {
        HomeDollProof.afterTransform(context.getSender(), ((HomeDollPacketView)message).jem$homeDollId());
    }
}
