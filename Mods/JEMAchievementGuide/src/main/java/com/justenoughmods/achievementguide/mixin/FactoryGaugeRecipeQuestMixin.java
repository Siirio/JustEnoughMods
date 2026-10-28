package com.justenoughmods.achievementguide.mixin;

import com.google.common.collect.Multimap;
import com.justenoughmods.achievementguide.integration.create.FactoryRecipeProof;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FactoryPanelBehaviour.class, remap = false)
public abstract class FactoryGaugeRecipeQuestMixin {
    @Shadow private void tickRequests() { throw new AssertionError(); }
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;tickRequests()V"))
    private void jem$nativeRecipeCycle(FactoryPanelBehaviour panel) { FactoryRecipeProof.cycle(panel, this::tickRequests); }
    @Redirect(method = "tickRequests", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/packagerLink/LogisticsManager;performPackageRequests(Lcom/google/common/collect/Multimap;)V"))
    private void jem$nativeIngredientRequests(Multimap<PackagerBlockEntity, PackagingRequest> requests) { FactoryRecipeProof.perform(requests); }
    @Inject(method = "setValueSettings", at = @At("RETURN"))
    private void jem$actualFactoryTarget(Player actor, ValueSettingsBehaviour.ValueSettings settings, boolean control, CallbackInfo callback) {
        if (actor instanceof ServerPlayer player) FactoryRecipeProof.configured((FactoryPanelBehaviour)(Object)this, player);
    }
}
