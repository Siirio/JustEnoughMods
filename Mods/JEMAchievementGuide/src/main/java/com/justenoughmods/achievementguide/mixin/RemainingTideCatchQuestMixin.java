package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.TideCatchProof;
import com.li64.tide.registries.entities.misc.fishing.TideFishingHook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TideFishingHook.class, remap = false)
public abstract class RemainingTideCatchQuestMixin {
    @Shadow private int nibble;
    @Inject(method = "retrieve(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/player/Player;)I", at = @At("HEAD"))
    private void jem$begin(ItemStack rod, ServerLevel level, Player player, CallbackInfoReturnable<Integer> callback) {
        TideCatchProof.begin((TideFishingHook)(Object)this, player, nibble);
    }
    @Inject(method = "retrieve(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/player/Player;)I", at = @At("RETURN"))
    private void jem$end(ItemStack rod, ServerLevel level, Player player, CallbackInfoReturnable<Integer> callback) { TideCatchProof.end(); }
}
