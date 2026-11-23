package com.siirio.jemcompat.mixin.client;

import com.siirio.jemcompat.client.jei.MultilingualSearch;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeSearchMixin
        extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow private EditBox searchBox;
    @Shadow private static CreativeModeTab selectedTab;

    protected CreativeModeSearchMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory,
                                      Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "refreshSearchResults", at = @At("TAIL"))
    private void jemcompat$appendLocalizedAliases(CallbackInfo callback) {
        String query = searchBox.getValue().trim();
        if (query.isEmpty() || query.startsWith("#")) return;
        String needle = query.toLowerCase(Locale.ROOT);
        for (ItemStack candidate : selectedTab.getSearchTabDisplayItems()) {
            if (!MultilingualSearch.matches(candidate, needle)) continue;
            if (menu.items.stream().noneMatch(existing -> ItemStack.matches(existing, candidate))) {
                menu.items.add(candidate);
            }
        }
        menu.scrollTo(0);
    }
}
