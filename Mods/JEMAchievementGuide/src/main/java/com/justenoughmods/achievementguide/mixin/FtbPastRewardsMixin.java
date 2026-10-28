package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.compat.ftb.PastRewardsNetwork;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Right-click the existing native reward control for the personal historical-claims action. */
@Mixin(targets = "dev.ftb.mods.ftbquests.client.gui.quests.CollectRewardsButton", remap = false)
public abstract class FtbPastRewardsMixin {
    private boolean jem$book() {
        return PastRewardsNetwork.enabled() && ClientQuestFile.INSTANCE != null
                && ClientQuestFile.INSTANCE.getAllChapters().stream().anyMatch(chapter -> chapter.hasTag("jem_visual_v4"));
    }
    @Inject(method = "onClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$pastRewards(MouseButton mouse, CallbackInfo callback) {
        if (!mouse.isRight() || !jem$book()) return;
        ((Widget)(Object)this).getGui().openContextMenu(List.of(new ContextMenuItem(Component.translatable("jem.questbook.past_rewards"),
                Icon.getIcon("minecraft:item/chest"), button -> PastRewardsNetwork.request(0))));
        callback.cancel();
    }
    @Inject(method = "addMouseOverText", at = @At("RETURN"), remap = false)
    private void jem$hint(TooltipList list, CallbackInfo callback) {
        if (jem$book()) list.add(Component.translatable("key.mouse.right").append(": ").append(Component.translatable("jem.questbook.past_rewards")));
    }
}
