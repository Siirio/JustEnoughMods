package com.justenoughmods.achievementguide.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.SteamerBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.SteamerRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import com.justenoughmods.achievementguide.integration.food.SteamerFoodProof;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SteamerBlockEntity.class, remap = false)
public abstract class CookerySteamerFoodQuestMixin {
    @Unique private boolean jem$mantou;
    @Inject(method = "cookingTick", at = @At("HEAD"))
    private void jem$begin(CallbackInfo callback) { jem$mantou = false; }
    @Redirect(method = "cookingTick", at = @At(value = "INVOKE", target = "Ljava/util/Optional;map(Ljava/util/function/Function;)Ljava/util/Optional;"))
    private Optional<ItemStack> jem$assemble(Optional<SteamerRecipe> matched, Function<SteamerRecipe, ItemStack> nativeAssembler) {
        jem$mantou = matched.isPresent() && matched.get().getId().toString().equals("kaleidoscope_cookery:steamer/mantou");
        return matched.map(nativeAssembler);
    }
    @Redirect(method = "cookingTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"))
    private Object jem$commit(NonNullList<ItemStack> items, int slot, Object value) {
        SteamerBlockEntity owner = (SteamerBlockEntity)(Object)this;
        SteamerFoodProof.invalidate(owner, slot);
        ItemStack output = (ItemStack)value;
        ItemStack old = items.set(slot, output);
        if (items == owner.getItems() && jem$mantou) SteamerFoodProof.certify(owner, slot, output);
        jem$mantou = false;
        return old;
    }
    @Redirect(method = "placeFood", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"))
    private Object jem$replace(NonNullList<ItemStack> items, int slot, Object value) {
        SteamerFoodProof.invalidate((BlockEntity)(Object)this, slot);
        return items.set(slot, (ItemStack)value);
    }
    @Redirect(method = "takeFood", at = @At(value = "INVOKE", target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/util/ItemUtils;getItemToLivingEntity(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;I)V"))
    private void jem$receive(LivingEntity actor, ItemStack output, int selected) {
        SteamerBlockEntity owner = (SteamerBlockEntity)(Object)this;
        boolean certified = false;
        for (int slot = 0; slot < Math.min(8, owner.getItems().size()); slot++) {
            if (owner.getItems().get(slot) == output) {
                certified = owner.getCookingTime()[slot] == -1 && SteamerFoodProof.consume(owner, slot, output);
                break;
            }
        }
        ItemStack expected = certified ? output.copy() : ItemStack.EMPTY;
        int held = certified && actor instanceof ServerPlayer player ? FoodOutputProof.held(player, player.containerMenu, expected) : 0;
        ItemUtils.getItemToLivingEntity(actor, output, selected);
        if (certified && actor instanceof ServerPlayer player && FoodOutputProof.held(player, player.containerMenu, expected) > held)
            QuestActionTrigger.INSTANCE.emit(player, "food/steamed_mantou");
    }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { SteamerFoodProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { SteamerFoodProof.load((BlockEntity)(Object)this, tag); }
}
