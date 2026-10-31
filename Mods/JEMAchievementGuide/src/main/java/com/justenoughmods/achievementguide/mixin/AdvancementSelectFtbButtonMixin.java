package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.compat.ftb.FtbQuestFrameScreen;
import net.mehvahdjukaar.advframes.blocks.AdvancementFrameBlockTile;
import net.mehvahdjukaar.advframes.client.AdvancementSelectScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementSelectFtbButtonMixin extends Screen {
    protected AdvancementSelectFtbButtonMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void jem$addFtbQuestButton(CallbackInfo callback) {
        if (!((Object) this instanceof AdvancementSelectScreen screen)) return;
        AdvancementFrameBlockTile tile = ((AdvancementSelectScreenAccess) (Object) screen).jem$getTile();
        addRenderableWidget(Button.builder(Component.translatable("jem.quest_frame.button"),
                button -> Minecraft.getInstance().setScreen(new FtbQuestFrameScreen(tile)))
                .bounds(width / 2 - 50, height / 2 + 78, 100, 20).build());
    }
}
