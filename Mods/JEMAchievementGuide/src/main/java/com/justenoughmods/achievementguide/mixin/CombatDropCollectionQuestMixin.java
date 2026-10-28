package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatDropProof;
import com.justenoughmods.achievementguide.integration.combattools.CombatToolProof;
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
public abstract class CombatDropCollectionQuestMixin {
    @Unique private ItemStack jem$before = ItemStack.EMPTY;
    @Unique private CompoundTag jem$proof = new CompoundTag();
    @Unique private int jem$held;
    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void jem$begin(Player player, CallbackInfo callback) {
        ItemEntity item = (ItemEntity)(Object)this; jem$proof = item.getPersistentData().getCompound(CombatDropProof.KEY).copy();
        jem$before = jem$proof.isEmpty() ? ItemStack.EMPTY : item.getItem().copy();
        jem$held = player instanceof ServerPlayer actor && !jem$before.isEmpty() ? CombatToolProof.held(actor, actor.containerMenu, jem$before) : 0;
    }
    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void jem$end(Player player, CallbackInfo callback) {
        CombatDropProof.picked((ItemEntity)(Object)this, player, jem$before, jem$held, jem$proof); jem$before = ItemStack.EMPTY; jem$proof = new CompoundTag();
    }
    @Inject(method = "setItem", at = @At("HEAD"))
    private void jem$replace(ItemStack item, CallbackInfo callback) { ((ItemEntity)(Object)this).getPersistentData().remove(CombatDropProof.KEY); }
    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private static void jem$mergeBegin(ItemEntity to, ItemStack a, ItemEntity from, ItemStack b, CallbackInfo callback) { CombatDropProof.mergeBegin(to, from); }
    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private static void jem$mergeEnd(ItemEntity to, ItemStack a, ItemEntity from, ItemStack b, CallbackInfo callback) { CombatDropProof.mergeEnd(to, from); }
}
