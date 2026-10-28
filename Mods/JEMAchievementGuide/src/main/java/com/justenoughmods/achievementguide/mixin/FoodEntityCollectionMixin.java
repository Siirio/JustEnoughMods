package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.FoodDropMerge;
import com.justenoughmods.achievementguide.integration.food.FoodDroppedProof;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native entity merges conserve certified portions, actual player receipt consumes them once. */
@Mixin(ItemEntity.class)
public abstract class FoodEntityCollectionMixin implements FoodDropMerge {
    @Unique private ItemStack jem$pickup = ItemStack.EMPTY;
    @Unique private int jem$pickupKnown;
    @Unique private int jem$held;
    @Unique private String jem$pickupAction = "";
    @Unique private UUID jem$pickupActor;
    @Unique private ItemStack jem$mergeDestination = ItemStack.EMPTY;
    @Unique private ItemStack jem$mergeSource = ItemStack.EMPTY;
    @Unique private int jem$destinationKnown;
    @Unique private int jem$sourceKnown;
    @Unique private String jem$destinationAction = "";
    @Unique private String jem$sourceAction = "";
    @Unique private UUID jem$destinationActor;
    @Unique private UUID jem$sourceActor;

    @Inject(method = "setItem", at = @At("HEAD"))
    private void jem$replacement(ItemStack stack, CallbackInfo callback) { ((ItemEntity)(Object)this).getPersistentData().remove(FoodDroppedProof.KEY); }

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void jem$beforePickup(Player player, CallbackInfo callback) {
        ItemEntity drop = (ItemEntity)(Object)this;
        jem$pickupKnown = player instanceof ServerPlayer ? FoodDroppedProof.known(drop) : 0;
        jem$pickup = jem$pickupKnown > 0 ? drop.getItem().copy() : ItemStack.EMPTY;
        jem$pickupActor = jem$pickupKnown > 0 ? FoodDroppedProof.actor(drop) : null;
        jem$pickupAction = jem$pickupKnown > 0 ? drop.getPersistentData().getCompound(FoodDroppedProof.KEY).getString("action") : "";
        jem$held = player instanceof ServerPlayer actor && !jem$pickup.isEmpty() ? FoodOutputProof.held(actor, actor.containerMenu, jem$pickup) : 0;
    }
    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void jem$afterPickup(Player player, CallbackInfo callback) {
        ItemEntity drop = (ItemEntity)(Object)this;
        if (player instanceof ServerPlayer actor && jem$pickupKnown > 0) {
            int taken = jem$pickup.getCount() - drop.getItem().getCount();
            int knownTaken = Math.max(0, taken - (jem$pickup.getCount() - jem$pickupKnown));
            if (taken > 0 && FoodOutputProof.held(actor, actor.containerMenu, jem$pickup) - jem$held >= taken) {
                FoodDroppedProof.update(drop, jem$pickupAction, jem$pickupKnown - knownTaken, jem$pickupActor);
                if (knownTaken > 0 && (jem$pickupActor == null || jem$pickupActor.equals(actor.getUUID()))) FoodDroppedProof.emitCollected(actor, jem$pickupAction);
            }
        }
        jem$pickup = ItemStack.EMPTY;
        jem$pickupKnown = 0;
        jem$pickupAction = "";
        jem$pickupActor = null;
    }

    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private static void jem$mergeStart(ItemEntity destination, ItemStack to, ItemEntity source, ItemStack from, CallbackInfo callback) {
        ((FoodDropMerge)destination).jem$beforeMerge(source);
    }
    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private static void jem$mergeEnd(ItemEntity destination, ItemStack to, ItemEntity source, ItemStack from, CallbackInfo callback) {
        ((FoodDropMerge)destination).jem$afterMerge(source);
    }
    @Override public void jem$beforeMerge(ItemEntity source) {
        ItemEntity destination = (ItemEntity)(Object)this;
        jem$destinationKnown = FoodDroppedProof.known(destination);
        jem$sourceKnown = FoodDroppedProof.known(source);
        if (jem$destinationKnown == 0 && jem$sourceKnown == 0) return;
        jem$mergeDestination = destination.getItem().copy();
        jem$mergeSource = source.getItem().copy();
        jem$destinationAction = destination.getPersistentData().getCompound(FoodDroppedProof.KEY).getString("action");
        jem$sourceAction = source.getPersistentData().getCompound(FoodDroppedProof.KEY).getString("action");
        jem$destinationActor = FoodDroppedProof.actor(destination);
        jem$sourceActor = FoodDroppedProof.actor(source);
    }
    @Override public void jem$afterMerge(ItemEntity source) {
        if (jem$destinationKnown == 0 && jem$sourceKnown == 0) return;
        ItemEntity destination = (ItemEntity)(Object)this;
        int transferred = jem$mergeSource.getCount() - source.getItem().getCount();
        if (transferred > 0 && destination.getItem().getCount() - jem$mergeDestination.getCount() == transferred) {
            int certified = Math.max(0, transferred - (jem$mergeSource.getCount() - jem$sourceKnown));
            String action = jem$destinationKnown > 0 ? jem$destinationAction : jem$sourceAction;
            if (certified > 0 && jem$destinationKnown > 0 && (!jem$destinationAction.equals(jem$sourceAction)
                    || !Objects.equals(jem$destinationActor, jem$sourceActor))) {
                FoodDroppedProof.update(destination, "", 0);
                FoodDroppedProof.update(source, jem$sourceAction, jem$sourceKnown - certified, jem$sourceActor);
            } else {
                FoodDroppedProof.update(destination, action, jem$destinationKnown + certified, jem$destinationKnown > 0 ? jem$destinationActor : jem$sourceActor);
                FoodDroppedProof.update(source, jem$sourceAction, jem$sourceKnown - certified, jem$sourceActor);
            }
        }
        jem$mergeDestination = jem$mergeSource = ItemStack.EMPTY;
        jem$destinationKnown = jem$sourceKnown = 0;
        jem$destinationAction = jem$sourceAction = "";
        jem$destinationActor = jem$sourceActor = null;
    }
}
