package com.siirio.jemcompat.mixin.food;

import com.siirio.jemcompat.feature.food.FoodEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Installed Cookery 1.4.1's native eat returns SUCCESS only after a portion is consumed. */
@Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock", remap = false)
public abstract class KaleidoscopeFoodBiteMixin {
    @Inject(method = "eat", at = @At("RETURN"), remap = false)
    private void jemQualityAfterNativeBite(Level level, BlockPos position, BlockState state, Player player,
                                          CallbackInfoReturnable<InteractionResult> callback) {
        if (player instanceof ServerPlayer serverPlayer && callback.getReturnValue().consumesAction())
            FoodEvents.consumedDish(serverPlayer, ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }
}
