package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.OrchardFoodProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Other native drops pass through unchanged and obtain no orchard receipt. */
@Mixin(Block.class)
public abstract class NativeOrchardDropQuestMixin {
    @Redirect(method = "popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean jem$actualFruitEntity(Level level, Entity entity) { return OrchardFoodProof.insert(level, entity); }
}
