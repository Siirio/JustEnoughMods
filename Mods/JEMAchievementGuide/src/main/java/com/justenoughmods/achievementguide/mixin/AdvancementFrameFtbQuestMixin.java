package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.compat.ftb.FtbQuestFrameAccess;
import com.justenoughmods.achievementguide.compat.ftb.FtbQuestFrameNetwork;
import com.mojang.authlib.GameProfile;
import net.mehvahdjukaar.advframes.blocks.AdvancementFrameBlockTile;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementFrameBlockTile.class)
public abstract class AdvancementFrameFtbQuestMixin implements FtbQuestFrameAccess {
    @Shadow(remap = false) private String advancementId;
    @Shadow(remap = false) private DisplayInfo advancement;
    @Unique private long jem$ftbQuestId;
    @Unique private java.util.UUID jem$ftbCompleter;

    @Override
    public void jem$setFtbQuest(long questId, DisplayInfo display, ServerPlayer owner) {
        jem$ftbQuestId = questId;
        jem$ftbCompleter = owner.getUUID();
        advancementId = null;
        advancement = display;
        ((AdvancementFrameBlockTile) (Object) this).setOwner(new GameProfile(owner.getUUID(), owner.getGameProfile().getName()));
    }

    @Override public long jem$getFtbQuestId() { return jem$ftbQuestId; }

    @Inject(method = "m_183515_", at = @At("TAIL"), remap = false)
    private void jem$saveFtbQuest(CompoundTag tag, CallbackInfo callback) {
        if (jem$ftbQuestId != 0L) tag.putLong("JemFtbQuestId", jem$ftbQuestId);
        if (jem$ftbCompleter != null) tag.putUUID("JemFtbCompleter", jem$ftbCompleter);
    }

    @Inject(method = "m_142466_", at = @At("TAIL"), remap = false)
    private void jem$loadFtbQuest(CompoundTag tag, CallbackInfo callback) {
        jem$ftbQuestId = tag.getLong("JemFtbQuestId");
        jem$ftbCompleter = tag.hasUUID("JemFtbCompleter") ? tag.getUUID("JemFtbCompleter") : null;
        if (jem$ftbQuestId == 0L) return;
        var file = dev.ftb.mods.ftbquests.quest.ServerQuestFile.INSTANCE;
        if (file == null) return;
        var quest = file.getQuest(jem$ftbQuestId);
        if (quest == null) {
            jem$ftbQuestId = 0L;
            jem$ftbCompleter = null;
            advancement = null;
            return;
        }
        advancement = FtbQuestFrameNetwork.displayFor(quest);
    }
}
