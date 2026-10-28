package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.FactoryPanelPacketAccess;
import com.justenoughmods.achievementguide.integration.create.FactoryRecipeProof;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConfigurationPacket;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockEntityConfigurationPacket.class, remap = false)
public abstract class FactoryConfigurationQuestMixin {
    @Inject(method = "applySettings(Lnet/minecraft/server/level/ServerPlayer;Lcom/simibubi/create/foundation/blockEntity/SyncedBlockEntity;)V", at = @At("RETURN"))
    private void jem$actualFactoryConfigurer(ServerPlayer actor, SyncedBlockEntity owner, CallbackInfo callback) {
        if ((Object)this instanceof FactoryPanelConfigurationPacket && owner instanceof FactoryPanelBlockEntity board) {
            var panel = board.panels.get(((FactoryPanelPacketAccess)this).jem$factorySlot());
            if (panel != null) FactoryRecipeProof.configured(panel, actor);
        }
    }
}
