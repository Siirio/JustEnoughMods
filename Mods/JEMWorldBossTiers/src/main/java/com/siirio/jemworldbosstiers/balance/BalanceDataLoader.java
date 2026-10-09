package com.siirio.jemworldbosstiers.balance;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.server.ServerLifecycleHooks;
import com.siirio.jemworldbosstiers.network.TierNetwork;
import com.siirio.jemworldbosstiers.progression.TierAdvancements;
import com.siirio.jemworldbosstiers.progression.WorldTierData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BalanceDataLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private final Kind kind;

    private BalanceDataLoader(String directory, Kind kind) {
        super(GSON, directory);
        this.kind = kind;
    }

    public static BalanceDataLoader bosses() {
        return new BalanceDataLoader("jem_world_boss_tiers/bosses", Kind.BOSSES);
    }

    public static BalanceDataLoader rewards() {
        return new BalanceDataLoader("jem_world_boss_tiers/rewards", Kind.REWARDS);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        if (kind == Kind.BOSSES) {
            List<BossProfile> profiles = new ArrayList<>();
            List<Integer> thresholds = null;
            for (JsonElement element : resources.values()) {
                JsonObject root = element.getAsJsonObject();
                ParsedBossData parsed = BalanceDataParser.parseBosses(root);
                profiles.addAll(parsed.profiles());
                if (!parsed.thresholds().isEmpty()) {
                    if (thresholds != null) {
                        throw new IllegalArgumentException("Only one boss data file may define tier_thresholds");
                    }
                    thresholds = parsed.thresholds();
                }
            }
            BalanceRegistry.replaceBosses(profiles, thresholds == null ? List.of(0) : thresholds);
            syncProgression();
            return;
        }
        List<RewardProfile> profiles = new ArrayList<>();
        resources.values().forEach(element -> profiles.addAll(BalanceDataParser.parseRewards(element.getAsJsonObject())));
        BalanceRegistry.replaceRewards(profiles);
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            TierNetwork.syncAll(ServerLifecycleHooks.getCurrentServer());
        }
    }

    private static void syncProgression() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        int tier = WorldTierData.get(server).tier();
        server.getPlayerList().getPlayers().forEach(player -> TierAdvancements.awardCurrent(player, tier));
        TierNetwork.syncAll(server);
    }

    private enum Kind {
        BOSSES,
        REWARDS
    }
}
