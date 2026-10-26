package com.justenoughmods.inchatftb.mixin;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.FTBQuestsNetClient;
import dev.ftb.mods.ftbquests.quest.Quest;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FTBQuestsNetClient.class, remap = false)
public abstract class FTBQuestCompletionMixin {
    @Inject(method = "displayCompletionToast(J)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void inChatFTB$showQuestInChat(long id, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !ClientQuestFile.exists()) return;
        if (!(ClientQuestFile.INSTANCE.get(id) instanceof Quest quest)) return;

        String command = "/inchatftb quest " + Long.toUnsignedString(id, 16);
        Component title = quest.getTitle().copy().withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("message.in_chat_ftb.open"))));
        minecraft.gui.getChat().addMessage(Component.translatable("message.in_chat_ftb.completed").append(title));

        ClientQuestFile.INSTANCE.getQuestScreen().ifPresent(screen -> {
            screen.refreshQuestPanel();
            screen.refreshChapterPanel();
            screen.refreshViewQuestPanel();
        });
        callback.cancel();
    }
}
