package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateGaugeProofs;
import com.justenoughmods.achievementguide.integration.create.CreateRequesterProofs;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket", remap = false)
public abstract class CreateLogisticsConfigurationQuestMixin {
    @Unique private CompoundTag jem$requesterBefore;
    @Unique private Map<String, CompoundTag> jem$panelsBefore;
    @Inject(method = "applySettings(Lnet/minecraft/server/level/ServerPlayer;Lcom/simibubi/create/foundation/blockEntity/SyncedBlockEntity;)V", at = @At("HEAD"), remap = false)
    private void jem$before(ServerPlayer actor, SyncedBlockEntity owner, CallbackInfo callback) {
        jem$requesterBefore = owner instanceof RedstoneRequesterBlockEntity requester ? CreateRequesterProofs.configuration(requester) : null;
        jem$panelsBefore = null;
        if (owner instanceof FactoryPanelBlockEntity board) {
            jem$panelsBefore = new HashMap<>();
            board.panels.forEach((slot, panel) -> jem$panelsBefore.put(slot.name(), CreateGaugeProofs.configuration(panel)));
        }
    }
    @Inject(method = "applySettings(Lnet/minecraft/server/level/ServerPlayer;Lcom/simibubi/create/foundation/blockEntity/SyncedBlockEntity;)V", at = @At("RETURN"), remap = false)
    private void jem$after(ServerPlayer actor, SyncedBlockEntity owner, CallbackInfo callback) {
        if (jem$requesterBefore != null && owner instanceof RedstoneRequesterBlockEntity requester)
            CreateRequesterProofs.configured(requester, actor, jem$requesterBefore);
        if (jem$panelsBefore != null && owner instanceof FactoryPanelBlockEntity board)
            board.panels.forEach((slot, panel) -> CreateGaugeProofs.configured(panel, actor, jem$panelsBefore.get(slot.name())));
        jem$requesterBefore = null; jem$panelsBefore = null;
    }
}
