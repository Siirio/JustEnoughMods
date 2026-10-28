package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.JukeboxQuestReceipt;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IDiscHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper", remap = false)
public abstract class StorageJukeboxQuestMixin implements JukeboxQuestReceipt {
    @Unique private ServerPlayer jem$actor;
    @Unique private ItemStack jem$startedDisc = ItemStack.EMPTY;
    @Unique private boolean jem$wasPlaying;

    @Override public void jem$beginPlayback(ServerPlayer actor) {
        jem$actor = actor;
        jem$startedDisc = ItemStack.EMPTY;
        jem$wasPlaying = ((JukeboxUpgradeWrapper) (Object) this).isPlaying();
    }

    @Inject(method = "lambda$playDisc$2", at = @At("TAIL"))
    private void jem$nativeDiscStarted(ServerLevel serverLevel, UUID storage, ItemStack disc, Level level,
                                       IDiscHandler handler, CallbackInfo callback) {
        if (jem$actor != null && !disc.isEmpty()) jem$startedDisc = disc.copy();
    }

    @Override public void jem$finishPlayback() {
        ServerPlayer actor = jem$actor;
        ItemStack disc = jem$startedDisc;
        jem$actor = null;
        jem$startedDisc = ItemStack.EMPTY;
        JukeboxUpgradeWrapper wrapper = (JukeboxUpgradeWrapper) (Object) this;
        if (actor != null && !jem$wasPlaying && wrapper.isPlaying() && !disc.isEmpty()
                && ItemStack.isSameItemSameTags(disc, wrapper.getDisc())) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_store_portable_music");
        }
    }
}
