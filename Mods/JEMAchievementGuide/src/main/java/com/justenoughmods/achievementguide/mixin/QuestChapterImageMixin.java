package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.client.questbook.BookInformationScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only authored captions and non-progress reference clicks; native quest UI is unchanged. */
@Mixin(targets = "dev.ftb.mods.ftbquests.client.gui.quests.ChapterImageButton", remap = false)
public abstract class QuestChapterImageMixin {
    @Shadow @Final private ChapterImage chapterImage;

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$caption(GuiGraphics graphics, Theme theme, int x, int y, int w, int h, CallbackInfo callback) {
        String click = chapterImage.getClick();
        if (!click.startsWith("jem-label:")) return;
        var font = Minecraft.getInstance().font;
        String label = click.substring("jem-label:".length());
        var lines = font.split(dev.ftb.mods.ftbquests.util.TextUtils.parseRawText(label), Math.max(1, w));
        int lineY = y + (h - Math.min(lines.size(), 2) * 10) / 2;
        for (int i = 0; i < Math.min(lines.size(), 2); i++) {
            var line = lines.get(i);
            graphics.drawString(font, line, x + (w - font.width(line)) / 2, lineY + i * 10, 0xFFE6BF, true);
        }
        callback.cancel();
    }

    @Inject(method = "onClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$reference(MouseButton button, CallbackInfo callback) {
        if (!button.isLeft() || Screen.hasControlDown() || Screen.hasShiftDown()) return;
        String click = chapterImage.getClick();
        if (click.startsWith("jem-info:")) {
            BookInformationScreen.open(click.substring("jem-info:".length()));
            callback.cancel();
        } else if (click.startsWith("jem-label:")) callback.cancel();
    }
}
