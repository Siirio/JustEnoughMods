package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateGaugeProofs;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour", remap = false)
public abstract class CreateGaugeQuestMixin {
    @Unique private CompoundTag jem$before;
    @Inject(method = "setValueSettings", at = @At("HEAD"), remap = false)
    private void jem$beforeTarget(Player actor, ValueSettingsBehaviour.ValueSettings settings, boolean control, CallbackInfo callback) {
        jem$before = CreateGaugeProofs.configuration((FactoryPanelBehaviour)(Object)this);
    }
    @Inject(method = "setValueSettings", at = @At("RETURN"), remap = false)
    private void jem$afterTarget(Player actor, ValueSettingsBehaviour.ValueSettings settings, boolean control, CallbackInfo callback) {
        if (actor instanceof ServerPlayer player) CreateGaugeProofs.configured((FactoryPanelBehaviour)(Object)this, player, jem$before);
        jem$before = null;
    }
    @Redirect(method = "tryRestock", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/packagerLink/LogisticsManager;broadcastPackageRequest(Ljava/util/UUID;Lcom/simibubi/create/content/logistics/packagerLink/LogisticallyLinkedBehaviour$RequestType;Lcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;Lcom/simibubi/create/content/logistics/packager/IdentifiedInventory;Ljava/lang/String;)Z"), remap = false)
    private boolean jem$nativeRestock(UUID network, LogisticallyLinkedBehaviour.RequestType type, PackageOrderWithCrafts order, IdentifiedInventory ignored, String address) {
        return CreateGaugeProofs.request((FactoryPanelBehaviour)(Object)this, order,
                () -> LogisticsManager.broadcastPackageRequest(network, type, order, ignored, address));
    }
}
