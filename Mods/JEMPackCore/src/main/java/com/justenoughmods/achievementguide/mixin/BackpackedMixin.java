package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.criterion.JemCriteria;
import com.mrcrayfish.backpacked.BackpackHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BackpackHelper.class, remap = false)
public abstract class BackpackedMixin {
    private static final ResourceLocation FIRST_BACKPACK = new ResourceLocation("jem_guide", "qol/7");
    private static final ResourceLocation TWO_BACKPACKS = new ResourceLocation("jem_guide", "qol/7_3");

    @Inject(method = "equipBackpack", at = @At("RETURN"), remap = false)
    private static void onEquipBackpack(Player player, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        JemCriteria.fire(serverPlayer, FIRST_BACKPACK);
        long equipped = BackpackHelper.getBackpacks(player).stream().filter(item -> !item.isEmpty()).count();
        if (equipped >= 2) {
            JemCriteria.fire(serverPlayer, TWO_BACKPACKS);
        }
    }
}
