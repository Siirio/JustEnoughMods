package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.FactoryPanelPacketAccess;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConfigurationPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = FactoryPanelConfigurationPacket.class, remap = false)
public interface FactoryPanelPacketQuestAccess extends FactoryPanelPacketAccess {
    @Override @Accessor("slot") FactoryPanelBlock.PanelSlot jem$factorySlot();
}
