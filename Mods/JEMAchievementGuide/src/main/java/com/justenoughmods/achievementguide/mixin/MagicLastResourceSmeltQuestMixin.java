package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastResourceProof;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastProof;
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
public abstract class MagicLastResourceSmeltQuestMixin {
    @Unique private ItemStack jem$arcaneInput = ItemStack.EMPTY;
    @Unique private MagicLastResourceProof.Before jem$arcaneOutput;
    @Inject(method = "burn", at = @At("HEAD"))
    private void jem$before(RegistryAccess registry, Recipe<?> recipe, NonNullList<ItemStack> items, int max, CallbackInfoReturnable<Boolean> callback) { jem$arcaneInput = items.get(0).copy(); jem$arcaneOutput = MagicLastResourceProof.before((AbstractFurnaceBlockEntity)(Object)this); }
    @Inject(method = "burn", at = @At("RETURN"))
    private void jem$after(RegistryAccess registry, Recipe<?> recipe, NonNullList<ItemStack> items, int max, CallbackInfoReturnable<Boolean> callback) { if (callback.getReturnValue() && recipe != null && recipe.getId().toString().equals("irons_spellbooks:mithril_scrap_from_salvage") && MagicLastProof.item(jem$arcaneInput).equals("irons_spellbooks:arcane_salvage") && jem$arcaneInput.getCount() - items.get(0).getCount() == 1 && MagicLastProof.item(items.get(2)).equals("irons_spellbooks:mithril_scrap")) MagicLastResourceProof.cooked((AbstractFurnaceBlockEntity)(Object)this, jem$arcaneOutput); jem$arcaneInput = ItemStack.EMPTY; jem$arcaneOutput = null; }
}
