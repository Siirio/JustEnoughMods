package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeCampfireGameProof;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlockEntity.class)
public abstract class RemainingCampfireGameCookingMixin {
    @Inject(method = "cookTick", at = @At("HEAD"))
    private static void jem$begin(Level level, BlockPos pos, BlockState state, CampfireBlockEntity owner, CallbackInfo callback) {
        if (level instanceof ServerLevel server) NativeCampfireGameProof.begin(server, owner);
    }
    @Redirect(method = "cookTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;", remap = false))
    private static Object jem$selected(NonNullList<ItemStack> items, int slot, Level level, BlockPos pos, BlockState state, CampfireBlockEntity owner) {
        ItemStack source = items.get(slot);
        if (level instanceof ServerLevel && items == owner.getItems()) NativeCampfireGameProof.selected(owner, slot, source);
        return source;
    }
    @Inject(method = "cookTick", at = @At("RETURN"))
    private static void jem$end(Level level, BlockPos pos, BlockState state, CampfireBlockEntity owner, CallbackInfo callback) {
        if (level instanceof ServerLevel) NativeCampfireGameProof.end();
    }
}
