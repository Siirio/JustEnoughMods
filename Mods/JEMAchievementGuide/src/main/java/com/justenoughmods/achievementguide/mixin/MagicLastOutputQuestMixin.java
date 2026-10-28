package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastOutputProof;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntity.class)
public abstract class MagicLastOutputQuestMixin {
    @Unique private ItemStack jem$magicBefore = ItemStack.EMPTY;
    @Unique private CompoundTag jem$magicProof = new CompoundTag();
    @Unique private int jem$magicHeld;
    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void jem$before(Player user, CallbackInfo callback) { ItemEntity item = (ItemEntity)(Object)this; jem$magicProof = item.getPersistentData().getCompound(MagicLastOutputProof.KEY).copy(); jem$magicBefore = jem$magicProof.isEmpty() ? ItemStack.EMPTY : item.getItem().copy(); jem$magicHeld = user instanceof ServerPlayer actor ? MagicLastProof.held(actor, jem$magicBefore) : 0; }
    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void jem$after(Player user, CallbackInfo callback) { MagicLastOutputProof.picked((ItemEntity)(Object)this, user, jem$magicBefore, jem$magicHeld, jem$magicProof); jem$magicBefore = ItemStack.EMPTY; jem$magicProof = new CompoundTag(); }
    @Inject(method = "setItem", at = @At("HEAD"))
    private void jem$replaced(ItemStack item, CallbackInfo callback) { ((ItemEntity)(Object)this).getPersistentData().remove(MagicLastOutputProof.KEY); }
    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private static void jem$mergeBegin(ItemEntity a, ItemStack x, ItemEntity b, ItemStack y, CallbackInfo callback) { MagicLastOutputProof.mergeBegin(a, b); }
    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private static void jem$mergeEnd(ItemEntity a, ItemStack x, ItemEntity b, ItemStack y, CallbackInfo callback) { MagicLastOutputProof.mergeEnd(a, b); }
}
