package com.justenoughmods.achievementguide.compat.ftb;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import dev.ftb.mods.ftbquests.quest.reward.XPReward;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;

/** XP points keep the native presentation and a player-persistent cross-team receipt. */
public final class BookXpReward extends XPReward {
    static final String RECEIPTS = "jem_quest_xp_receipts";
    // Failed player saves are retryable delivery operations, not another completion ledger.
    private static final Map<ServerPlayer, Set<Long>> UNFLUSHED = new WeakHashMap<>();
    private static final RewardType TYPE = RewardTypes.register(new ResourceLocation("jem_advancements", "quest_xp"),
            BookXpReward::new, () -> Icon.getIcon("minecraft:item/experience_bottle"));
    public BookXpReward(long id, Quest quest) { super(id, quest); }
    static void register() { }
    @Override public RewardType getType() { return TYPE; }
    static boolean issued(ServerPlayer player, long reward) {
        return player.getPersistentData().getCompound(RECEIPTS).getBoolean(Long.toHexString(reward))
                && !UNFLUSHED.getOrDefault(player, Set.of()).contains(reward);
    }
    @Override public void claim(ServerPlayer player, boolean notify) {
        var file = dev.ftb.mods.ftbquests.quest.ServerQuestFile.INSTANCE;
        claimFrom(player, file.getOrCreateTeamData(player), this, notify);
    }
    void claimFrom(ServerPlayer player, TeamData origin, Reward parent, boolean notify) {
        try {
            CompoundTag receipts = player.getPersistentData().getCompound(RECEIPTS);
            String key = Long.toHexString(id);
            if (issued(player, id)) return;
            if (!receipts.getBoolean(key)) {
                super.claim(player, notify);
                receipts.putBoolean(key, true);
                player.getPersistentData().put(RECEIPTS, receipts);
            }
            UNFLUSHED.computeIfAbsent(player, ignored -> new HashSet<>()).add(id);
            persist(player);
            // This save included every pending receipt for this exact player instance.
            UNFLUSHED.remove(player);
        } catch (RuntimeException failure) {
            if (parent != null && !issued(player, id)) {
                origin.resetReward(player.getUUID(), parent);
                if (origin.getFile() instanceof dev.ftb.mods.ftbquests.quest.ServerQuestFile file) file.saveNow();
            }
            throw failure;
        }
    }
    private static void persist(ServerPlayer player) {
        CompoundTag expected = player.saveWithoutId(new CompoundTag());
        player.server.getPlayerList().saveAll();
        boolean host = player.server.isSingleplayerOwner(player.getGameProfile());
        if (host) player.server.saveEverything(false, true, true);
        try {
            var path = host ? player.server.getWorldPath(LevelResource.LEVEL_DATA_FILE)
                    : player.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getUUID() + ".dat");
            CompoundTag actual = NbtIo.readCompressed(path.toFile());
            if (host) actual = actual.getCompound("Data").getCompound("Player");
            for (String key : new String[]{"XpLevel", "XpTotal", "XpP"})
                if (!Objects.equals(expected.get(key), actual.get(key))) throw new IllegalStateException("storage_unavailable");
            if (!Objects.equals(expected.getCompound("ForgeData").get(RECEIPTS), actual.getCompound("ForgeData").get(RECEIPTS)))
                throw new IllegalStateException("storage_unavailable");
        } catch (IOException failure) { throw new IllegalStateException("storage_unavailable", failure); }
    }
    static void cloned(Player original, Player replacement) {
        var receipts = original.getPersistentData().get(RECEIPTS);
        if (receipts != null) replacement.getPersistentData().put(RECEIPTS, receipts.copy());
        if (original instanceof ServerPlayer before && replacement instanceof ServerPlayer after) {
            Set<Long> pending = UNFLUSHED.remove(before);
            if (pending != null) UNFLUSHED.put(after, pending);
        }
    }
}
