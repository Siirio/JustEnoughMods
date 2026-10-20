package com.justenoughmods.achievementguide.mixin;

import com.siirio.jempackcore.enchantment.ExcavationMining;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DiggerItem.class)
public abstract class ExcavationDiggerItemMixin {
    @Redirect(method = "mineBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"))
    private void excavationWear(ItemStack stack, int amount, LivingEntity entity, Consumer<LivingEntity> onBreak,
                                ItemStack tool, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        ExcavationMining.miningWear(stack, amount, entity, onBreak, pos);
    }
}
