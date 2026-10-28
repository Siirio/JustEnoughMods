package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.satisfy.brewery.core.item.DrinkBlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DrinkBlockItem.class, remap = false)
public abstract class BreweryDrinkFoodQuestMixin {
    @Unique private static final String jem$runKey = "jemNativeBreweryDrinkRunV1";
    @Inject(method = "m_5922_", at = @At("HEAD"))
    private void jem$begin(ItemStack drink, Level level, LivingEntity actor, CallbackInfoReturnable<ItemStack> callback) {
        if (actor instanceof ServerPlayer player) {
            CompoundTag run = new CompoundTag();
            run.put("before", drink.save(new CompoundTag()));
            player.getPersistentData().put(jem$runKey, run);
        }
    }
    @Inject(method = "m_5922_", at = @At("RETURN"))
    private void jem$finished(ItemStack drink, Level level, LivingEntity actor, CallbackInfoReturnable<ItemStack> callback) {
        if (!(actor instanceof ServerPlayer player)) return;
        CompoundTag run = player.getPersistentData().getCompound(jem$runKey).copy();
        player.getPersistentData().remove(jem$runKey);
        ItemStack before = ItemStack.of(run.getCompound("before"));
        if (!before.isEmpty() && before.getItem() == (Object)this && drink.getCount() == before.getCount() - 1)
            QuestActionTrigger.INSTANCE.emit(player, "food_brewery_drink_finished");
    }
}
