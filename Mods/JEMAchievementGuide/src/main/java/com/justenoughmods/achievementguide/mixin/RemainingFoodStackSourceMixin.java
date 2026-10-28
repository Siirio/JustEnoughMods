package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Provenance stays outside ordinary item tags, recipes, equality and native stacking. */
@Mixin(ItemStack.class)
public abstract class RemainingFoodStackSourceMixin implements NativeFoodSources.StackAccess {
    @Unique private List<NativeFoodSources.Claim> jem$sources = List.of();
    @Unique private List<NativeFoodSources.Claim> jem$splitSources = List.of();
    @Unique private int jem$splitCount;
    @Override public List<NativeFoodSources.Claim> jem$foodSources() { return jem$sources; }
    @Override public void jem$foodSources(List<NativeFoodSources.Claim> claims) { jem$sources = claims; }
    @Inject(method = "copy", at = @At("RETURN"))
    private void jem$copy(CallbackInfoReturnable<ItemStack> callback) { NativeFoodSources.claims(callback.getReturnValue(), jem$sources); }
    @Inject(method = "split", at = @At("HEAD"))
    private void jem$splitStart(int amount, CallbackInfoReturnable<ItemStack> callback) {
        jem$splitSources = jem$sources; jem$splitCount = ((ItemStack)(Object)this).getCount();
    }
    @Inject(method = "split", at = @At("RETURN"))
    private void jem$splitEnd(int amount, CallbackInfoReturnable<ItemStack> callback) {
        ItemStack result = callback.getReturnValue();
        var selected = NativeFoodSources.portion(jem$splitSources, jem$splitCount, result.getCount());
        NativeFoodSources.claims(result, selected);
        jem$sources = NativeFoodSources.remainder(jem$splitSources, selected); jem$splitSources = List.of();
    }
    @Inject(method = "setCount", at = @At("HEAD"))
    private void jem$count(int count, CallbackInfo callback) {
        int before = ((ItemStack)(Object)this).getCount();
        if (count < before && !jem$sources.isEmpty()) jem$sources = NativeFoodSources.remainder(jem$sources,
                NativeFoodSources.portion(jem$sources, before, before - Math.max(0, count)));
    }
    @Inject(method = "save", at = @At("RETURN"))
    private void jem$save(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback) {
        if (!jem$sources.isEmpty()) callback.getReturnValue().put(NativeFoodSources.STACK_KEY, NativeFoodSources.writeClaims(jem$sources));
        else callback.getReturnValue().remove(NativeFoodSources.STACK_KEY);
    }
    @Inject(method = "of", at = @At("RETURN"))
    private static void jem$load(CompoundTag tag, CallbackInfoReturnable<ItemStack> callback) {
        ItemStack stack = callback.getReturnValue(); NativeFoodSources.claims(stack, NativeFoodSources.readClaims(tag, stack.getCount()));
    }
}
