package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.LinkedHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IPickupResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Only the real player ItemEntity pickup route, never block/hand insertion or a simulated preview. */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.common.CommonEventHandler", remap = false)
public abstract class BackpackPickupQuestMixin {
    @Redirect(method = "lambda$onItemPickup$7", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedcore/util/InventoryHelper;runPickupOnPickupResponseUpgrades(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/p3pp3rf1y/sophisticatedcore/upgrades/UpgradeHandler;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jem$committedPickup(Level level, Player player, UpgradeHandler upgrades,
                                                ItemStack input, boolean simulate) {
        ItemStack kind = input.copy();
        int offered = input.getCount();
        var before = new LinkedHashMap<IItemHandler, Long>();
        if (!simulate && player instanceof ServerPlayer && !input.isEmpty()) {
            for (IPickupResponseUpgrade upgrade : upgrades.getWrappersThatImplement(IPickupResponseUpgrade.class)) {
                if (!(upgrade instanceof PickupUpgradeWrapper pickup) || !pickup.isEnabled()) continue;
                var storage = ((UpgradeWrapperQuestAccess) pickup).jem$questStorageWrapper();
                IItemHandler inventory = storage.getInventoryForUpgradeProcessing();
                if (!before.containsKey(inventory)) before.put(inventory, jem$count(inventory, kind));
            }
        }
        ItemStack remaining = InventoryHelper.runPickupOnPickupResponseUpgrades(level, player, upgrades, input, simulate);
        if (player instanceof ServerPlayer actor && !simulate && remaining.getCount() < offered) {
            for (var entry : before.entrySet()) {
                if (jem$count(entry.getKey(), kind) > entry.getValue()) {
                    QuestActionTrigger.INSTANCE.emit(actor, "backpack_pickup");
                    break;
                }
            }
        }
        return remaining;
    }

    @Unique
    private static long jem$count(IItemHandler inventory, ItemStack kind) {
        long count = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(stack, kind)) count += stack.getCount();
        }
        return count;
    }
}
