package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativePreparedMealProof;
import com.justenoughmods.achievementguide.integration.photo.PhotoCraftProof;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NativePreparedMealProof.class, remap = false)
public abstract class PhotoCraftReceiptQuestMixin {
    @Inject(method = "begin", at = @At("RETURN"))
    private static void jem$begin(ServerPlayer player, AbstractContainerMenu menu, CallbackInfo callback) { PhotoCraftProof.begin(player, menu); }
    @Inject(method = "crafted", at = @At("HEAD"))
    private static void jem$crafted(ServerPlayer player, Recipe<?> recipe, ItemStack output, List<ItemStack> removed, CallbackInfo callback) {
        PhotoCraftProof.crafted(player, recipe, output, removed);
    }
    @Inject(method = "end", at = @At("RETURN"))
    private static void jem$end(ServerPlayer player, AbstractContainerMenu menu, CallbackInfo callback) { PhotoCraftProof.end(player, menu); }
}
