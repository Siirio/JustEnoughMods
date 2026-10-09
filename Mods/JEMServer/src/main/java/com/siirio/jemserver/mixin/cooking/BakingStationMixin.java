package com.siirio.jemserver.mixin.cooking;

import com.siirio.jemserver.smp.events.CookingShow;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.satisfy.bakery.core.block.cake.BlankCakeBlock", remap = false)
public abstract class BakingStationMixin {
    @Inject(method = {"m_6227_", "use"}, at = @At("RETURN"), remap = false)
    private void jem$cakeMade(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                              BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockState(pos).getBlock() != state.getBlock())
            CookingShow.progress(serverPlayer, new ItemStack(level.getBlockState(pos).getBlock().asItem()));
    }

    @Redirect(method = {"m_6227_", "use"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"), remap = false)
    private boolean jem$pastryMade(Level world, Entity entity, BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        boolean added = world.addFreshEntity(entity);
        if (added && player instanceof ServerPlayer serverPlayer && entity instanceof ItemEntity item)
            CookingShow.progress(serverPlayer, item.getItem());
        return added;
    }
}
