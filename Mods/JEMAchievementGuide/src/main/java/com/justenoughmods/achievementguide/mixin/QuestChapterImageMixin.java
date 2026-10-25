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
        boolean title = click.startsWith("jem-title:");
        if (!title && !click.startsWith("jem-label:")) return;
        var font = Minecraft.getInstance().font;
        String label = click.substring("jem-label:".length());
        float scale = title ? 1.35F : 1F;
        var text = dev.ftb.mods.ftbquests.util.TextUtils.parseRawText(label);
        // Keep a real phrase together. At most two intentional lines, never narrow fragments.
        scale = Math.min(scale, Math.max(.85F, w / (float)Math.max(1, font.width(text))));
        var lines = font.split(text, Math.max(1, (int)(w / scale)));
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        int lineY = ((int)(h / scale) - Math.min(lines.size(), 2) * 10) / 2;
        for (int i = 0; i < Math.min(lines.size(), 2); i++) {
            var line = lines.get(i);
            graphics.drawString(font, line, ((int)(w / scale) - font.width(line)) / 2, lineY + i * 10, 0xFFE6BF, true);
        }
        graphics.pose().popPose();
        callback.cancel();
    }

    @Inject(method = "onClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$reference(MouseButton button, CallbackInfo callback) {
        if (!button.isLeft() || Screen.hasControlDown() || Screen.hasShiftDown()) return;
        String click = chapterImage.getClick();
        if (click.startsWith("jem-info:")) {
            BookInformationScreen.open(click.substring("jem-info:".length()));
            callback.cancel();
        } else if (click.startsWith("jem-label:") || click.startsWith("jem-title:")) callback.cancel();
    }
}
