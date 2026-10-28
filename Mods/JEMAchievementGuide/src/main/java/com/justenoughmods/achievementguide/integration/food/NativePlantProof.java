package com.justenoughmods.achievementguide.integration.food;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class NativePlantProof {
    private static final java.util.Set<String> TRACKED = java.util.Set.of("primal:thorny_acacia_sapling", "alexscaves:curly_fern",
            "alexscaves:fiddlehead", "snowyspirit:ginger", "primal:litchi_sapling", "primal:litchi_tree",
            "primal:kiwano_sapling", "primal:kiwano_bulk", "primal:starfruit_sapling", "primal:starfruit_tree");
    private NativePlantProof() {}
    public static void placed(ServerPlayer actor, BlockPos pos, BlockState state, ItemStack spent) {
        ServerLevel level = actor.serverLevel(); NativeFoodSources data = NativeFoodSources.get(level);
        String id = NativeFoodSources.block(state); String item = RemainingFoodActors.item(spent);
        if (!TRACKED.contains(id)) return;
        data.plant(level, pos, null);
        String kind = switch (id) {
            case "primal:thorny_acacia_sapling" -> "thorny_acacia";
            case "alexscaves:curly_fern", "alexscaves:fiddlehead" -> "curly_fern";
            case "snowyspirit:ginger" -> "ginger";
            case "primal:litchi_sapling", "primal:kiwano_sapling", "primal:starfruit_sapling" ->
                    data.consume(actor.getUUID(), NativeFoodSources.spent(spent, 1), "cassowary_seed", 1) ? "cassowary_orchard" : "";
            default -> "";
        };
        if (!kind.isEmpty()) data.plant(level, pos, new NativeFoodSources.Plant(actor.getUUID(), kind, id));
    }
    public static void changed(ServerLevel level, BlockPos pos, BlockState before, BlockState after) {
        if (before.getBlock() == after.getBlock() || !TRACKED.contains(NativeFoodSources.block(before)) || level.getServer().overworld() == null) return;
        NativeFoodSources data = NativeFoodSources.get(level);
        if (data.plant(level, pos) != null) data.plant(level, pos, null);
    }
    public static NativeFoodSources.Plant original(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getServer().overworld() == null || !TRACKED.contains(NativeFoodSources.block(state))) return null;
        NativeFoodSources.Plant plant = NativeFoodSources.get(level).plant(level, pos);
        return plant != null && plant.block().equals(NativeFoodSources.block(state)) ? plant : null;
    }
    public static void evolved(ServerLevel level, BlockPos pos, NativeFoodSources.Plant planted, BlockState after) {
        if (planted != null) NativeFoodSources.get(level).plant(level, pos, new NativeFoodSources.Plant(planted.actor(), planted.kind(), NativeFoodSources.block(after)));
    }
    public static void tree(ServerLevel level, BlockPos pos, NativeFoodSources.Plant planted, BlockState before, boolean nativeSuccess) {
        if (!nativeSuccess || planted == null || !planted.kind().equals("thorny_acacia")) return;
        String after = NativeFoodSources.block(level.getBlockState(pos));
        if (!after.equals("primal:thorny_acacia_log") && !after.equals("primal:thorny_acacia_wood")) return;
        var actor = level.getServer().getPlayerList().getPlayer(planted.actor());
        if (RemainingFoodActors.real(actor) != null && actor.level() == level) QuestActionTrigger.INSTANCE.emit(actor, "food_fritter_tree");
    }
}
