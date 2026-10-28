package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class RemainingFoodInventorySourceMixin {
    @Unique private List<NativeFoodSources.Claim> jem$incoming = List.of();
    @Unique private List<NativeFoodSources.Claim> jem$destination = List.of();
    @Unique private int jem$incomingCount;
    @Unique private ItemStack jem$source;
    @Unique private int jem$slot;
    @Inject(method = "addResource(ILnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"))
    private void jem$before(int slot, ItemStack source, CallbackInfoReturnable<Integer> callback) {
        jem$incoming = NativeFoodSources.claims(source); jem$incomingCount = source.getCount();
        jem$destination = NativeFoodSources.claims(((Inventory)(Object)this).getItem(slot));
        jem$source = source; jem$slot = slot;
    }
    @Inject(method = "addResource(ILnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"))
    private void jem$after(int slot, ItemStack source, CallbackInfoReturnable<Integer> callback) {
        ItemStack destination = ((Inventory)(Object)this).getItem(slot);
        int moved = jem$incomingCount - callback.getReturnValue();
        if (source == jem$source && slot == jem$slot && moved > 0 && !jem$incoming.isEmpty()) {
            NativeFoodSources.claims(destination, jem$destination);
            NativeFoodSources.transfer(destination, jem$incoming, jem$incomingCount, moved);
        }
        jem$incoming = jem$destination = List.of();
        jem$source = null;
    }
}
