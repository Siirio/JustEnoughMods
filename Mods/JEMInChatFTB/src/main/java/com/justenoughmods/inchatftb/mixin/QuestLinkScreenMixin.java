package com.justenoughmods.inchatftb.mixin;

import com.justenoughmods.inchatftb.InChatFTB;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class QuestLinkScreenMixin {
    private static final String QUEST_COMMAND = "/inchatftb quest ";

    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void inChatFTB$openLocalQuestLink(Style style, CallbackInfoReturnable<Boolean> callback) {
        if (style == null) return;
        ClickEvent click = style.getClickEvent();
        if (click == null || click.getAction() != ClickEvent.Action.RUN_COMMAND) return;
        String value = click.getValue();
        if (!value.startsWith(QUEST_COMMAND)) return;
        String questId = value.substring(QUEST_COMMAND.length());
        if (questId.isEmpty() || questId.indexOf(' ') >= 0) return;
        InChatFTB.ClientCommands.openQuest(questId);
        callback.setReturnValue(true);
    }
}
