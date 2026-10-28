package com.justenoughmods.achievementguide.mixin;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import dev.ftb.mods.ftbquests.quest.reward.ChoiceReward;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Filter only this editor's presentation copy. Native tables and claim identities remain intact. */
@Mixin(targets = "dev.ftb.mods.ftbquests.client.gui.RewardTablesScreen", remap = false)
public abstract class FtbArchiveRewardTablesMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbquests/client/ClientQuestFile;getRewardTables()Ljava/util/List;"))
    private List<RewardTable> jem$activeTables(ClientQuestFile file) {
        List<RewardTable> nativeTables = file.getRewardTables();
        if (file.getAllChapters().stream().noneMatch(chapter -> chapter.hasTag("jem_visual_v4"))) return nativeTables;
        Set<Long> active = new HashSet<>();
        file.getAllChapters().stream().filter(chapter -> chapter.hasTag("jem_visual_v4"))
                .flatMap(chapter -> chapter.getQuests().stream()).flatMap(quest -> quest.getRewards().stream())
                .filter(reward -> reward instanceof ChoiceReward).map(reward -> ((ChoiceReward)reward).getTable())
                .filter(table -> table != null).forEach(table -> active.add(table.id));
        return nativeTables.stream().filter(table -> active.contains(table.id)).toList();
    }
}
