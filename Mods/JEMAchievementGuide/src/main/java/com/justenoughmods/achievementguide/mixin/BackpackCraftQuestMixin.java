package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.advancement.QuestResultSnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the native result take and every native ingredient debit, including refill mode. */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer$2", remap = false)
public abstract class BackpackCraftQuestMixin {
    @Unique private ServerPlayer jem$actor;
    @Unique private CraftingUpgradeContainer jem$craft;
    @Unique private int jem$ingredients;
    @Unique private int jem$consumed;

    @Inject(method = "m_142406_", at = @At("HEAD"))
    private void jem$beforeCraft(Player player, ItemStack result, CallbackInfo callback) {
        jem$actor = null;
        jem$craft = null;
        jem$ingredients = jem$consumed = 0;
        if (!(player instanceof ServerPlayer actor) || !(actor.containerMenu instanceof BackpackContainer menu)
                || !menu.stillValid(actor) || menu.getBackpackContext().getOwnerPlayer(actor).orElse(null) != actor) return;
        var upgrade = menu.getSlotUpgradeContainer((Slot) (Object) this).orElse(null);
        if (!(upgrade instanceof CraftingUpgradeContainer craft) || craft.getPlayer() != actor
                || !craft.getUpgradeWrapper().isEnabled()) return;
        ItemStack taken = result.isEmpty() ? ((QuestResultSnapshot) menu).jem$questQuickResult() : result;
        var recipe = ((BackpackCraftRecipeQuestAccess) craft).jem$questRecipe();
        if (taken.isEmpty() || recipe == null || !(craft.getCraftMatrix() instanceof CraftingContainer matrix)
                || !recipe.matches(matrix, actor.level())) return;
        for (int index = 0; index < matrix.getContainerSize(); index++) {
            if (!matrix.getItem(index).isEmpty()) jem$ingredients++;
        }
        if (jem$ingredients > 0) {
            jem$actor = actor;
            jem$craft = craft;
        }
    }

    @Redirect(method = "m_142406_", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedcore/upgrades/crafting/CraftingItemHandler;m_7407_(II)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$observeGridDebit(CraftingItemHandler matrix, int slot, int count) {
        ItemStack removed = matrix.removeItem(slot, count);
        if (jem$craft != null && matrix == jem$craft.getCraftMatrix() && count == 1 && !removed.isEmpty()) jem$consumed++;
        return removed;
    }

    @Redirect(method = "m_142406_", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedcore/upgrades/crafting/CraftingUpgradeWrapper;extractFromStorageOrPlayer(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean jem$observeRefillDebit(CraftingUpgradeWrapper wrapper, Player player, ItemStack ingredient) {
        boolean consumed = wrapper.extractFromStorageOrPlayer(player, ingredient);
        if (consumed && jem$craft != null && wrapper == jem$craft.getUpgradeWrapper() && player == jem$actor) jem$consumed++;
        return consumed;
    }

    @Inject(method = "m_142406_", at = @At("TAIL"))
    private void jem$afterCraft(Player player, ItemStack result, CallbackInfo callback) {
        ServerPlayer actor = jem$actor;
        int required = jem$ingredients, consumed = jem$consumed;
        jem$actor = null;
        jem$craft = null;
        jem$ingredients = jem$consumed = 0;
        if (actor != null && actor == player && required > 0 && consumed == required) {
            QuestActionTrigger.INSTANCE.emit(actor, "backpack_craft");
        }
    }
}
