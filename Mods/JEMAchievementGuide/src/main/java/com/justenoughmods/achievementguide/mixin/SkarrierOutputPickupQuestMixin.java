package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.SkarrierWorkshopProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntity.class)
public abstract class SkarrierOutputPickupQuestMixin {
    @Unique private ItemStack jem$before = ItemStack.EMPTY;
    @Unique private int jem$inventoryBefore;
    @Unique private CompoundTag jem$proof = new CompoundTag();
    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void jem$begin(Player actor, CallbackInfo callback) {
        ItemEntity item = (ItemEntity)(Object)this;
        jem$proof = item.getPersistentData().getCompound("JemQuestWorkshopOutputV4").copy();
        jem$before = item.getPersistentData().contains("JemQuestWorkshopOutputV4") ? item.getItem().copy() : ItemStack.EMPTY;
        jem$inventoryBefore = jem$before.isEmpty() ? 0 : SkarrierWorkshopProof.inventoryCount(actor, jem$before);
    }
    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void jem$end(Player actor, CallbackInfo callback) {
        SkarrierWorkshopProof.pickedUp((ItemEntity)(Object)this, actor, jem$before, jem$inventoryBefore, jem$proof);
        jem$before = ItemStack.EMPTY;
        jem$proof = new CompoundTag();
    }
    @Inject(method = "setItem", at = @At("HEAD"))
    private void jem$replacement(ItemStack stack, CallbackInfo callback) {
        ((ItemEntity)(Object)this).getPersistentData().remove("JemQuestWorkshopOutputV4");
    }
    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private static void jem$mergeBegin(ItemEntity destination, ItemStack to, ItemEntity source, ItemStack from, CallbackInfo callback) {
        SkarrierWorkshopProof.mergeBegin(destination, source);
    }
    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private static void jem$mergeEnd(ItemEntity destination, ItemStack to, ItemEntity source, ItemStack from, CallbackInfo callback) {
        SkarrierWorkshopProof.mergeEnd(destination, source);
    }
}

