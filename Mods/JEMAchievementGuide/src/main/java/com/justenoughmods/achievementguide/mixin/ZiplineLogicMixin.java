package com.justenoughmods.achievementguide.mixin;

import com.evandev.zipline.Cable;
import com.evandev.zipline.duck.ZiplinePlayerDuck;
import com.evandev.zipline.logic.ZiplineLogic;
import com.justenoughmods.achievementguide.criterion.JemCriteria;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ZiplineLogic.class, remap = false)
public abstract class ZiplineLogicMixin {
    private static final ResourceLocation ATTACH = new ResourceLocation("jem_guide", "cool_stuff/zipline_attach");
    private static final ResourceLocation TRANSFER = new ResourceLocation("jem_guide", "cool_stuff/zipline_transfer");
    private static final ResourceLocation RELEASE = new ResourceLocation("jem_guide", "cool_stuff/zipline_release");

    @Inject(method = "enable", at = @At("TAIL"))
    private static void afterAttach(Player player, ZiplinePlayerDuck state, Cable cable, Vec3 position, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            JemCriteria.fire(serverPlayer, ATTACH);
        }
    }

    @Inject(
            method = "handleCableSwitch",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/evandev/zipline/duck/ZiplinePlayerDuck;zipline$setCable(Lcom/evandev/zipline/Cable;)V",
                    shift = At.Shift.AFTER
            )
    )
    private static void afterCableSwitch(Player player, ZiplinePlayerDuck state, Cable cable, int direction, Vec3 movement, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            JemCriteria.fire(serverPlayer, TRANSFER);
        }
    }

    @Inject(method = "release", at = @At("HEAD"))
    private static void beforeRelease(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && ((ZiplinePlayerDuck) player).zipline$isActuallyUsing()) {
            JemCriteria.fire(serverPlayer, RELEASE);
        }
    }
}
