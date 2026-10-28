package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateCrafterInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock", remap = false)
public abstract class CreateCrafterInputQuestMixin {
    @Redirect(method = {"use", "m_6227_"}, at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/ItemHandlerHelper;insertItem(Lnet/minecraftforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"), remap = false)
    private ItemStack jem$actualIngredient(IItemHandler inventory, ItemStack item, boolean simulate, BlockState state,
                                           Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!simulate && player instanceof ServerPlayer actor) {
            return CreateCrafterInput.insert(actor, item, () -> ItemHandlerHelper.insertItem(inventory, item, false));
        }
        return ItemHandlerHelper.insertItem(inventory, item, simulate);
    }
}
