package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.food.FoodTransferSnapshots.EntityMerge;
import java.util.List;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class RemainingFoodEntitySourceMixin {
    @Unique private static final ThreadLocal<EntityMerge> jem$merging = new ThreadLocal<>();
    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private static void jem$before(ItemEntity destination, ItemStack to, ItemEntity source, ItemStack from, CallbackInfo callback) {
        jem$merging.remove();
        if (!NativeFoodSources.claims(from).isEmpty()) jem$merging.set(new EntityMerge(destination, source, from.getCount(), NativeFoodSources.claims(from), NativeFoodSources.claims(to)));
    }
    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private static void jem$after(ItemEntity destination, ItemStack to, ItemEntity source, ItemStack from, CallbackInfo callback) {
        EntityMerge run = jem$merging.get(); jem$merging.remove();
        if (run == null || run.destination() != destination || run.source() != source) return;
        int moved = run.sourceCount() - from.getCount();
        if (moved > 0) {
            NativeFoodSources.claims(destination.getItem(), run.to());
            NativeFoodSources.transfer(destination.getItem(), run.from(), run.sourceCount(), moved);
            NativeFoodSources.claims(source.getItem(), NativeFoodSources.remainder(run.from(), NativeFoodSources.portion(run.from(), run.sourceCount(), moved)));
        }
    }
}
