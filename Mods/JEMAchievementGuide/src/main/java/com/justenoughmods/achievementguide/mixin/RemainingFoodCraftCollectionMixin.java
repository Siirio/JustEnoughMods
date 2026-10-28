package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativePreparedMealProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class RemainingFoodCraftCollectionMixin {
    @Inject(method = "clicked", at = @At("HEAD"))
    private void jem$begin(int slot, int button, ClickType type, Player player, CallbackInfo callback) {
        if (player instanceof ServerPlayer actor) NativePreparedMealProof.begin(actor, (AbstractContainerMenu)(Object)this);
    }
    @Inject(method = "clicked", at = @At("RETURN"))
    private void jem$end(int slot, int button, ClickType type, Player player, CallbackInfo callback) {
        if (player instanceof ServerPlayer actor) NativePreparedMealProof.end(actor, (AbstractContainerMenu)(Object)this);
    }
}
