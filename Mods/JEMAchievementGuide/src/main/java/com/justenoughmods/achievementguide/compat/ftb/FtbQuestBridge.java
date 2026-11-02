package com.justenoughmods.achievementguide.compat.ftb;

import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.CustomTaskEvent;
import dev.ftb.mods.ftbquests.net.SyncQuestsMessage;
import dev.ftb.mods.ftbquests.net.SyncTeamDataMessage;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.task.CustomTask;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Loaded only with the audited native FTB Quests API; completion stays in FTB Teams. */
public final class FtbQuestBridge {
    private static final Map<Long, String> RACES = Map.of(
            0x1110000000000043L, "ANCESTORS",
            0x1110000000000053L, "VOID_WALKERS",
            0x1110000000000063L, "BORN_IN_BLOOM",
            0x1110000000000073L, "INFECTED_BY_SCULK",
            0x1110000000000083L, "NETHER_HEARTEDS",
            0x1110000000000093L, "*");
    private static final long CAMPAIGN = 0x1110000000000045L;
    private static final Map<UUID, UUID> PENDING_TEAM_UNLOCKS = new HashMap<>();

    private FtbQuestBridge() {}

    public static void register() {
        BookItemReward.register();
        BookXpReward.register();
        PastRewardsNetwork.register();
        MinecraftForge.EVENT_BUS.register(BookEvents.class);
        if (QuestSignals.loaded("jem_economy")) BookEconomyEvents.register();
        CustomTaskEvent.EVENT.register(event -> {
            CustomTask task = event.getTask();
            Long dormantMaximum = FtbQuestProjection.dormantMaximum(task.getQuestFile(), task.id);
            if (dormantMaximum != null) {
                task.setMaxProgress(dormantMaximum);
                task.setEnableButton(false);
                task.setCheckTimer(0);
                return EventResult.pass();
            }
            if (BookRules.bind(task)) return EventResult.pass();
            String race = RACES.get(task.id);
            if (race == null && task.id != CAMPAIGN) return EventResult.pass();
            task.setEnableButton(false);
            task.setCheckTimer(20);
            task.setCheck((data, player) -> {
                if (!FtbQuestProjection.active(task.getQuestFile(), task.getQuest().id)) return;
                boolean confirmed = race == null ? QuestSignals.campaignComplete(player)
                        : race.equals("*") ? !QuestSignals.race(player).isEmpty() : race.equals(QuestSignals.race(player));
                if (confirmed && data.getProgress() < task.getMaxProgress()) data.setProgress(task.getMaxProgress());
            });
            return EventResult.pass();
        });
        TeamEvent.PLAYER_LOGGED_IN.register(event ->
                PENDING_TEAM_UNLOCKS.put(event.getPlayer().getUUID(), event.getTeam().getId()));
        TeamEvent.PLAYER_CHANGED.register(event ->
                PENDING_TEAM_UNLOCKS.put(event.getPlayerId(), event.getTeam().getId()));
        MinecraftForge.EVENT_BUS.register(FtbQuestBridge.class);
    }

    @SubscribeEvent
    public static void refresh(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        reconcileNativeTeamUnlocks(event);
        if (event.getServer().getTickCount() % 20 != 0) return;
        BookMigration.refresh(event.getServer());
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null || file.server != event.getServer() || file.isLoading()
                || !FtbQuestProjection.featuresChanged(event.getServer())) return;
        // Native reload reads its normal team files. Flush first so an enable/disable cannot lose recent progress.
        file.saveNow();
        file.load();
        new SyncQuestsMessage(file).sendToAll(event.getServer());
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            new SyncTeamDataMessage(file.getOrCreateTeamData(player), true).sendTo(player);
        }
    }

    private static void reconcileNativeTeamUnlocks(TickEvent.ServerTickEvent event) {
        if (PENDING_TEAM_UNLOCKS.isEmpty()) return;
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null || file.server != event.getServer() || file.isLoading()) return;
        var iterator = PENDING_TEAM_UNLOCKS.entrySet().iterator();
        while (iterator.hasNext()) {
            var pending = iterator.next();
            var player = event.getServer().getPlayerList().getPlayer(pending.getKey());
            if (player != null) {
                var data = file.getOrCreateTeamData(pending.getValue());
                if (data.setLocked(false)) data.saveIfChanged();
                new SyncTeamDataMessage(data, true).sendTo(player);
            }
            iterator.remove();
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        PENDING_TEAM_UNLOCKS.clear();
        FtbQuestProjection.clear(event.getServer());
        BookMigration.clear(event.getServer());
        BookEvents.clear();
    }
}
