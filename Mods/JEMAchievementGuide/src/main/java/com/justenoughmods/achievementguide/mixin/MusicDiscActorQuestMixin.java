package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.RainFrogMusicProof;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecordItem.class)
public abstract class MusicDiscActorQuestMixin {
    @Unique private static final ThreadLocal<RainFrogMusicProof.Insertion> jem$musicInsertion = new ThreadLocal<>();
    @Inject(method = "useOn", at = @At("HEAD"))
    private void jem$beforeNativeDisc(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        var before = RainFrogMusicProof.before(context);
        if (before == null) jem$musicInsertion.remove(); else jem$musicInsertion.set(before);
    }
    @Inject(method = "useOn", at = @At("RETURN"))
    private void jem$afterNativeDisc(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        var before = jem$musicInsertion.get();
        jem$musicInsertion.remove();
        RainFrogMusicProof.inserted(before, callback.getReturnValue().consumesAction());
    }
}
