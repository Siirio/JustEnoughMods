package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.RainFrogMusicProof;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxRecordQuestMixin {
    @Inject(method = "setItem", at = @At("RETURN"))
    private void jem$recordChanged(int slot, ItemStack item, CallbackInfo callback) {
        var jukebox = (JukeboxBlockEntity) (Object) this;
        if (slot == 0 && item.is(ItemTags.MUSIC_DISCS) && jukebox.getFirstItem() == item) RainFrogMusicProof.invalidated(jukebox);
    }
    @Inject(method = "removeItem", at = @At("RETURN"))
    private void jem$recordRemoved(int slot, int count, CallbackInfoReturnable<ItemStack> callback) {
        if (slot == 0 && !callback.getReturnValue().isEmpty()) RainFrogMusicProof.invalidated((JukeboxBlockEntity) (Object) this);
    }
    @Inject(method = "setRecordWithoutPlaying", at = @At("HEAD"))
    private void jem$recordReplaced(ItemStack item, CallbackInfo callback) {
        RainFrogMusicProof.invalidated((JukeboxBlockEntity) (Object) this);
    }
}
