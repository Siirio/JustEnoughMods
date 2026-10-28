package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class RemainingGameCookingQuestMixin {
    @Unique private ItemStack jem$input;
    @Unique private int jem$outputCount;
    @Inject(method = "burn", at = @At("HEAD"))
    private void jem$begin(RegistryAccess access, Recipe<?> recipe, NonNullList<ItemStack> inventory, int maximum, CallbackInfoReturnable<Boolean> callback) {
        jem$input = null;
        if (inventory.size() == 3 && !NativeFoodSources.claims(inventory.get(0)).isEmpty()) {
            jem$input = inventory.get(0).copy(); jem$outputCount = inventory.get(2).getCount();
        }
    }
    @Inject(method = "burn", at = @At("RETURN"))
    private void jem$finished(RegistryAccess access, Recipe<?> recipe, NonNullList<ItemStack> inventory, int maximum, CallbackInfoReturnable<Boolean> callback) {
        ItemStack before = jem$input; jem$input = null;
        if (before == null || !callback.getReturnValue() || recipe == null || !(((BlockEntity)(Object)this).getLevel() instanceof ServerLevel level)
                || before.getCount() - inventory.get(0).getCount() != 1) return;
        String input = RemainingFoodActors.item(before); String result = RemainingFoodActors.item(inventory.get(2));
        if (!(input.equals("cnc:raw_turkey") && result.equals("cnc:cooked_turkey"))
                && !(input.equals("cnc:raw_venison") && result.equals("cnc:cooked_venison"))) return;
        int made = inventory.get(2).getCount() - jem$outputCount;
        var sources = NativeFoodSources.get(level); var spent = NativeFoodSources.spent(before, 1); var actor = sources.owner(spent, "native_game");
        if (made > 0 && sources.consume(actor, spent, "native_game", 1)) sources.certify(level, actor, "prepared_game", inventory.get(2), made);
    }
}
