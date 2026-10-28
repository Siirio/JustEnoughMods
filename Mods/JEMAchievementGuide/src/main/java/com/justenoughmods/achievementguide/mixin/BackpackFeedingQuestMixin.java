package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeWrapper", remap = false)
public abstract class BackpackFeedingQuestMixin {
    @Unique private ServerPlayer jem$owner;
    @Unique private int jem$hunger;
    @Unique private ItemStack jem$food = ItemStack.EMPTY;

    @Inject(method = "tick", at = @At("HEAD"))
    private void jem$carriedOwner(Entity entity, Level level, BlockPos pos, CallbackInfo callback) {
        jem$owner = entity instanceof ServerPlayer actor && ((IUpgradeWrapper) this).isEnabled()
                && ((UpgradeWrapperQuestAccess) this).jem$questStorageWrapper() instanceof IBackpackWrapper ? actor : null;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void jem$endTick(Entity entity, Level level, BlockPos pos, CallbackInfo callback) { jem$owner = null; }

    @Inject(method = "tryFeedingStack", at = @At("HEAD"))
    private void jem$beforeMeal(Level level, int hunger, Player player, Integer slot, ItemStack stack,
                                ITrackedContentsItemHandler inventory, CallbackInfoReturnable<Boolean> callback) {
        jem$food = ItemStack.EMPTY;
        if (player != jem$owner || inventory != ((UpgradeWrapperQuestAccess) this).jem$questStorageWrapper().getInventoryForUpgradeProcessing()) return;
        jem$hunger = player.getFoodData().getFoodLevel();
        jem$food = stack.copy();
    }

    @Inject(method = "tryFeedingStack", at = @At("RETURN"))
    private void jem$afterMeal(Level level, int hunger, Player player, Integer slot, ItemStack stack,
                               ITrackedContentsItemHandler inventory, CallbackInfoReturnable<Boolean> callback) {
        ItemStack before = jem$food;
        jem$food = ItemStack.EMPTY;
        if (player != jem$owner || before.isEmpty() || !callback.getReturnValue()
                || player.getFoodData().getFoodLevel() <= jem$hunger) return;
        ItemStack after = inventory.getStackInSlot(slot);
        int remaining = ItemStack.isSameItemSameTags(before, after) ? after.getCount() : 0;
        if (before.getCount() - remaining == 1) QuestActionTrigger.INSTANCE.emit(jem$owner, "v3_store_food_medicine");
    }
}
