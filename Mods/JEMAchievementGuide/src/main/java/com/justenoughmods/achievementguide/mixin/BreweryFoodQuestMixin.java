package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.BreweryReceipt;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.satisfy.brewery.core.block.entity.BrewstationBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Installed Brewery 2.0.6: only brew() can certify a batch, getBeer() consumes one native portion. */
@Mixin(value = BrewstationBlockEntity.class, remap = false)
public abstract class BreweryFoodQuestMixin implements BreweryReceipt {
    @Shadow private ItemStack beer;
    @Unique private ItemStack jem$drawn = ItemStack.EMPTY;
    @Unique private CompoundTag jem$drawing = new CompoundTag();
    @Unique private String jem$action = "";
    @Unique private static final Set<String> jem$netherite = Set.of("brewery:beer_wheat", "brewery:beer_barley", "brewery:beer_hops",
            "brewery:whiskey_highland_hearth", "brewery:whiskey_jamesons_malt", "brewery:whiskey_maggoallan", "brewery:whiskey_smokey_reverie");

    @Inject(method = "brew", at = @At("TAIL"))
    private void jem$completedBatch(Recipe<?> recipe, RegistryAccess registries, CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)this;
        if (owner.getLevel() == null || owner.getLevel().isClientSide || beer.isEmpty()) return;
        String recipeId = recipe.getId().toString();
        String material = owner.getBlockState().getValues().entrySet().stream().filter(entry -> entry.getKey().getName().equals("material"))
                .map(entry -> String.valueOf(entry.getValue())).findFirst().orElse("");
        String item = String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(beer.getItem()));
        String action = "";
        if (material.equals("WOOD") && recipeId.equals("brewery:brewing/wheat_beer") && item.equals("brewery:beer_wheat")) action = "food/first_brew";
        else if (material.equals("COPPER") && recipeId.equals("brewery:brewing/whiskey_highland_hearth") && item.equals("brewery:whiskey_highland_hearth")) action = "food/copper_whiskey";
        else if (material.equals("NETHERITE") && recipeId.startsWith("brewery:brewing/") && jem$netherite.contains(item)) action = "food/netherite_brew";
        owner.getPersistentData().remove(FoodOutputProof.KEY);
        if (action.isEmpty()) return;
        CompoundTag proof = new CompoundTag();
        proof.putString("action", action);
        proof.putString("recipe", recipeId);
        proof.putInt("slot", -1);
        proof.putInt("known", beer.getCount());
        proof.put("output", beer.save(new CompoundTag()));
        owner.getPersistentData().put(FoodOutputProof.KEY, proof);
        owner.setChanged();
    }

    @Inject(method = "getBeer", at = @At("HEAD"))
    private void jem$beforeDraw(CallbackInfoReturnable<ItemStack> callback) {
        jem$clearDraw();
        BlockEntity owner = (BlockEntity)(Object)this;
        CompoundTag proof = owner.getPersistentData().getCompound(FoodOutputProof.KEY);
        if (FoodOutputProof.matches(proof, beer)) jem$drawing = proof.copy();
        else if (!proof.isEmpty()) { owner.getPersistentData().remove(FoodOutputProof.KEY); owner.setChanged(); }
    }

    @Inject(method = "getBeer", at = @At("RETURN"))
    private void jem$nativeDraw(CallbackInfoReturnable<ItemStack> callback) {
        ItemStack taken = callback.getReturnValue();
        if (jem$drawing.isEmpty() || taken == null || taken.isEmpty()) return;
        BlockEntity owner = (BlockEntity)(Object)this;
        ItemStack before = ItemStack.of(jem$drawing.getCompound("output"));
        if (taken.getCount() != 1 || before.getCount() != beer.getCount() + 1 || !ItemStack.isSameItemSameTags(before, taken)) {
            owner.getPersistentData().remove(FoodOutputProof.KEY);
            jem$clearDraw();
            owner.setChanged();
            return;
        }
        jem$drawn = taken.copy();
        jem$action = jem$drawing.getString("action");
        jem$drawing.putInt("known", Math.max(0, jem$drawing.getInt("known") - 1));
        jem$drawing.put("output", beer.save(new CompoundTag()));
        if (beer.isEmpty() || jem$drawing.getInt("known") == 0) owner.getPersistentData().remove(FoodOutputProof.KEY);
        else owner.getPersistentData().put(FoodOutputProof.KEY, jem$drawing.copy());
        jem$drawing = new CompoundTag();
        owner.setChanged();
    }

    @Override public void jem$clearDraw() { jem$drawn = ItemStack.EMPTY; jem$drawing = new CompoundTag(); jem$action = ""; }
    @Override public void jem$received(ServerPlayer player, ItemStack drawn, boolean inserted) {
        if (inserted && !jem$action.isEmpty() && ItemStack.matches(jem$drawn, drawn)) QuestActionTrigger.INSTANCE.emit(player, jem$action);
        jem$clearDraw();
    }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$saveProvenance(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$loadProvenance(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.load((BlockEntity)(Object)this, tag); jem$clearDraw(); }
}
