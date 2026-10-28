package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.RainFrogMusicProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityRainFrog", remap = false)
public abstract class RainFrogMusicQuestMixin {
    @Shadow private BlockPos jukeboxPosition;
    @Shadow public abstract int getDanceTime();
    @Unique private int jem$nativeDanceBefore;
    @Inject(method = "m_8119_", at = @At("HEAD"))
    private void jem$beforeNativeReaction(CallbackInfo callback) { jem$nativeDanceBefore = getDanceTime(); }
    @Inject(method = "m_8119_", at = @At("RETURN"))
    private void jem$validatedNativeReaction(CallbackInfo callback) {
        var frog = (Entity) (Object) this;
        if (!(frog.level() instanceof ServerLevel level) || frog.isRemoved() || jem$nativeDanceBefore < 1
                || getDanceTime() < 1 || jukeboxPosition == null || !level.isLoaded(jukeboxPosition)) return;
        // The native tick already enforced its own 15-block jukebox admission and committed its dance state.
        if (level.getBlockEntity(jukeboxPosition) instanceof JukeboxBlockEntity jukebox) RainFrogMusicProof.reacted(jukebox);
    }
}
