package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only a committed native output decrease plus real personal receipt can consume recipe provenance. */
@Mixin(AbstractContainerMenu.class)
public abstract class FoodMenuCollectionMixin {
    @Unique private List<FoodOutputProof.Collection> jem$food = List.of();
    @Inject(method = "clicked", at = @At("HEAD"))
    private void jem$beforeCollection(int slot, int button, ClickType type, Player player, CallbackInfo callback) {
        jem$food = player instanceof ServerPlayer actor ? FoodOutputProof.capture((AbstractContainerMenu)(Object)this, actor) : List.of();
    }
    @Inject(method = "clicked", at = @At("RETURN"))
    private void jem$committedCollection(int slot, int button, ClickType type, Player player, CallbackInfo callback) {
        if (player instanceof ServerPlayer actor) FoodOutputProof.collected((AbstractContainerMenu)(Object)this, actor, jem$food);
        jem$food = List.of();
    }
}
