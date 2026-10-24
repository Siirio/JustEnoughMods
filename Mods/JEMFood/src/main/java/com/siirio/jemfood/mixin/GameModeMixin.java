package com.siirio.jemfood.mixin;

import com.siirio.jemfood.FoodEvents;
import com.siirio.jemfood.PlacedConsumption;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class GameModeMixin {
    @Shadow protected ServerPlayer player;
    @Unique private BlockState jemfood$harvest;
    @Redirect(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult nativeBite(BlockState state, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        return PlacedConsumption.use(state, level, player, hand, hit);
    }
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void beforeHarvest(BlockPos pos, CallbackInfoReturnable<Boolean> callback) { jemfood$harvest = player.level().getBlockState(pos); }
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void afterHarvest(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue() && jemfood$harvest != null) FoodEvents.harvested(player, jemfood$harvest);
        jemfood$harvest = null;
    }
}
