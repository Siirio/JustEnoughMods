package com.siirio.jemcompat.mixin.client;

import com.siirio.jemcompat.client.jei.JEMJeiPlugin;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.tom.storagemod.gui.AbstractStorageTerminalScreen", remap = false)
public abstract class StorageTerminalFocusMixin {
    @Unique
    private boolean jemcompat$handledAutomaticFocus;

    @Redirect(method = "onPacket", at = @At(value = "INVOKE",
            target = "Lcom/tom/storagemod/platform/PlatformEditBox;m_93692_(Z)V", remap = false), remap = false)
    private void jemcompat$preserveSearchOwnership(@Coerce Object field, boolean focused) {
        if (jemcompat$handledAutomaticFocus) {
            return;
        }
        jemcompat$handledAutomaticFocus = true;
        Screen screen = (Screen) (Object) this;
        boolean jeiFocused = ModList.get().isLoaded("jei") && JEMJeiPlugin.hasSearchFocus();
        if (focused && screen.getFocused() == null && !jeiFocused) {
            screen.setFocused((EditBox) field);
        }
    }
}
