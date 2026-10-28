package com.justenoughmods.achievementguide.client.questbook;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterGroup;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import dev.ftb.mods.ftbquests.quest.reward.ChoiceReward;

/** Client navigation only. Native historical claims remain server-owned and intact. */
public final class QuestbookArchives {
    private QuestbookArchives() {}
    public static boolean visualV4(BaseQuestFile file) {
        return file instanceof ClientQuestFile && file.getAllChapters().stream()
                .anyMatch(chapter -> chapter.hasTag("jem_visual_v4") && !legacy(chapter));
    }
    public static boolean legacy(Chapter chapter) {
        return chapter != null && (chapter.getFilename().startsWith("_archive_")
                || chapter.getFilename().equals("_jem_frozen_claims") || chapter.getFilename().equals("_jem_unavailable")
                || chapter.hasTag("jem_archive") || chapter.hasTag("jem_claims_archive"));
    }
    public static boolean archived(QuestObjectBase object) {
        if (object == null || !visualV4(object.getQuestFile())) return false;
        if (object instanceof Chapter chapter) return legacy(chapter);
        if (object instanceof RewardTable table) return object.getQuestFile().getAllChapters().stream()
                .filter(chapter -> chapter.hasTag("jem_visual_v4") && !legacy(chapter))
                .flatMap(chapter -> chapter.getQuests().stream()).flatMap(quest -> quest.getRewards().stream())
                .filter(reward -> reward instanceof ChoiceReward).map(reward -> ((ChoiceReward)reward).getTable())
                .noneMatch(active -> active != null && active.id == table.id);
        if (object instanceof ChapterGroup group) return !group.getChapters().isEmpty() && group.getChapters().stream().allMatch(QuestbookArchives::legacy);
        if (object instanceof QuestLink link) return legacy(link.getChapter()) || link.getQuest().map(quest -> legacy(quest.getChapter())).orElse(false);
        return legacy(object.getQuestChapter());
    }
    public static Chapter firstActive(BaseQuestFile file) {
        return file.getAllChapters().stream().filter(chapter -> chapter.hasTag("jem_visual_v4") && !legacy(chapter)).findFirst().orElse(null);
    }
}
