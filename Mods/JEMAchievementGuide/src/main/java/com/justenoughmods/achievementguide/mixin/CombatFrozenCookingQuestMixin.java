package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatStationProof;
import com.justenoughmods.achievementguide.integration.combattools.CombatToolProof;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class CombatFrozenCookingQuestMixin {
    @Unique private ItemStack jem$input = ItemStack.EMPTY;
    @Unique private CombatStationProof.Before jem$output;
    @Inject(method = "burn", at = @At("HEAD"))
    private void jem$begin(RegistryAccess registry, Recipe<?> recipe, NonNullList<ItemStack> items, int max, CallbackInfoReturnable<Boolean> callback) {
        jem$input = items.get(0).copy(); jem$output = CombatStationProof.before((AbstractFurnaceBlockEntity)(Object)this, 2);
    }
    @Inject(method = "burn", at = @At("RETURN"))
    private void jem$end(RegistryAccess registry, Recipe<?> recipe, NonNullList<ItemStack> items, int max, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue() && recipe != null && recipe.getId().toString().equals("rottencreatures:rotten_flesh_from_smoking")
                && CombatToolProof.item(jem$input).equals("rottencreatures:frozen_rotten_flesh") && jem$input.getCount() - items.get(0).getCount() == 1
                && CombatToolProof.item(items.get(2)).equals("minecraft:rotten_flesh")) CombatStationProof.completed((AbstractFurnaceBlockEntity)(Object)this, 2, "frozen_brewing_processed", jem$output, false);
        jem$input = ItemStack.EMPTY; jem$output = null;
    }
}
