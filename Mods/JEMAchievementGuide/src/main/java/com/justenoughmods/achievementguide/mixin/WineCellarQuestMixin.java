package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.WineCellarProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.satisfy.vinery.core.block.StorageBlock;
import net.satisfy.vinery.core.block.entity.StorageBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StorageBlock.class, remap = false)
public abstract class WineCellarQuestMixin {
    @Inject(method = "add", at = @At("HEAD"))
    private void jem$beginAdd(Level level, BlockPos pos, Player actor, StorageBlockEntity owner, ItemStack input, int slot, CallbackInfo callback) {
        if (!level.isClientSide && owner.getLevel() == level && owner.getBlockPos().equals(pos)) WineCellarProof.beginAdd(owner, actor, input, slot);
    }
    @Inject(method = "add", at = @At("RETURN"))
    private void jem$added(Level level, BlockPos pos, Player actor, StorageBlockEntity owner, ItemStack input, int slot, CallbackInfo callback) {
        if (!level.isClientSide && owner.getLevel() == level && owner.getBlockPos().equals(pos)) WineCellarProof.added(owner, actor, input, slot);
    }
    @Inject(method = "remove", at = @At("HEAD"))
    private void jem$beginRemove(Level level, BlockPos pos, Player actor, StorageBlockEntity owner, int slot, CallbackInfo callback) {
        if (!level.isClientSide && owner.getLevel() == level && owner.getBlockPos().equals(pos)) WineCellarProof.beginRemove(owner, actor, slot);
    }
    @Inject(method = "remove", at = @At("RETURN"))
    private void jem$removed(Level level, BlockPos pos, Player actor, StorageBlockEntity owner, int slot, CallbackInfo callback) {
        if (!level.isClientSide && owner.getLevel() == level && owner.getBlockPos().equals(pos)) WineCellarProof.removed(owner, actor, slot);
    }
    @Redirect(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_36176_(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity jem$nativeDrop(Player actor, ItemStack wine, boolean random, Level level, BlockPos pos,
                                      Player nativeActor, StorageBlockEntity owner, int slot) {
        ItemEntity drop = actor.drop(wine, random);
        if (actor == nativeActor && !level.isClientSide) WineCellarProof.dropped(owner, actor, drop);
        return drop;
    }
}
