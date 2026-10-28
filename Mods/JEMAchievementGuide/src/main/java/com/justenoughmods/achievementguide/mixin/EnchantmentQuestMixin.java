package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentQuestMixin {
    @Shadow @Final private Container enchantSlots;
    @Unique private ItemStack jem$beforeEnchant = ItemStack.EMPTY;
    @Unique private int jem$beforeLapis;
    @Unique private int jem$beforeLevels;

    @Inject(method = "clickMenuButton", at = @At("HEAD"))
    private void jem$beforeEnchant(Player player, int button, CallbackInfoReturnable<Boolean> callback) {
        jem$beforeEnchant = enchantSlots.getItem(0).copy();
        jem$beforeLapis = enchantSlots.getItem(1).getCount();
        jem$beforeLevels = player.experienceLevel;
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void jem$afterEnchant(Player player, int button, CallbackInfoReturnable<Boolean> callback) {
        ItemStack result = enchantSlots.getItem(0);
        if (player instanceof ServerPlayer actor && player.containerMenu == (Object) this
                && Boolean.TRUE.equals(callback.getReturnValue()) && button >= 0 && button < 3
                && !jem$beforeEnchant.isEmpty() && !result.isEmpty()
                && (result.is(jem$beforeEnchant.getItem()) || jem$beforeEnchant.is(Items.BOOK)
                && result.is(Items.ENCHANTED_BOOK))
                && jem$beforeLevels - player.experienceLevel == button + 1
                && jem$beforeLapis - enchantSlots.getItem(1).getCount() == button + 1
                && !EnchantmentHelper.getEnchantments(result).isEmpty()
                && !EnchantmentHelper.getEnchantments(jem$beforeEnchant).equals(EnchantmentHelper.getEnchantments(result))) {
            QuestActionTrigger.INSTANCE.emit(actor, "enchanting_table_success");
        }
        jem$beforeEnchant = ItemStack.EMPTY;
    }
}
