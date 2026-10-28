package com.justenoughmods.achievementguide.integration.nature;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Actual native turnip/calcite harvest transactions and one adjacent root trail per actor. */
public final class NatureHarvestProof {
    public record Harvest(ServerPlayer actor, ServerLevel level, BlockPos pos, BlockState state,
                          String action, NatureDropProof.Run drop) {}
    private record Root(ServerPlayer actor, ServerLevel level, BlockPos pos, BlockState target) {}
    private record RootKey(ServerLevel level, BlockPos target) {}
    private static final Map<UUID, Root> roots = new HashMap<>();
    private static final Map<RootKey, UUID> targets = new HashMap<>();
    private NatureHarvestProof() {}

    public static Harvest before(ServerPlayer actor, BlockPos pos) {
        if (!NatureActors.eligible(actor)) return null;
        ServerLevel level = actor.serverLevel();
        BlockState state = level.getBlockState(pos);
        String action = null;
        String item = null;
        if (NatureNativeFacts.block(state, "calcite_cluster") && NatureNativeFacts.validCalcite(level, pos, state)) {
            action = "v3_world_living_calcite"; item = "natures_spirit:calcite_shard";
        } else if (NatureNativeFacts.block(state, "desert_turnip_block")) {
            Root root = roots.get(actor.getUUID());
            boolean rooted = NatureNativeFacts.block(level.getBlockState(pos.above()), "desert_turnip_root_block")
                    || root != null && root.actor == actor && root.level == level && root.pos.equals(pos.above()) && root.target.equals(state);
            if (rooted && state.hasProperty(DirectionalBlock.FACING) && state.getValue(DirectionalBlock.FACING) == Direction.UP) {
                action = "v3_world_root_and_crown_turnip"; item = "natures_spirit:desert_turnip";
            }
        } else if (NatureNativeFacts.block(state, "desert_turnip_root_block")) {
            action = "root_trail"; item = "natures_spirit:desert_turnip_root_block";
        }
        return action == null ? null : new Harvest(actor, level, pos.immutable(), state, action,
                new NatureDropProof.Run(actor, pos, item));
    }

    public static void after(Harvest harvest, boolean nativeResult) {
        if (harvest == null || !nativeResult || !harvest.drop.accepted || !NatureActors.eligible(harvest.actor)
                || harvest.actor.serverLevel() != harvest.level || harvest.level.getBlockState(harvest.pos).equals(harvest.state)) return;
        if (harvest.action.equals("root_trail")) {
            reset(harvest.actor.getUUID());
            BlockState revealed = harvest.level.getBlockState(harvest.pos.below());
            if (NatureNativeFacts.block(revealed, "desert_turnip_block") || NatureNativeFacts.block(revealed, "desert_turnip_root_block")) {
                roots.put(harvest.actor.getUUID(), new Root(harvest.actor, harvest.level, harvest.pos, revealed));
                UUID previous = targets.put(new RootKey(harvest.level, harvest.pos.below()), harvest.actor.getUUID());
                if (previous != null && !previous.equals(harvest.actor.getUUID())) roots.remove(previous);
            }
        } else {
            if (harvest.action.equals("v3_world_root_and_crown_turnip")) reset(harvest.actor.getUUID());
            NatureActors.emit(harvest.actor, harvest.action);
        }
    }

    public static void changed(ServerLevel level, BlockPos pos) {
        UUID actor = targets.remove(new RootKey(level, pos));
        if (actor != null) roots.remove(actor);
    }
    public static void reset(UUID actor) {
        Root root = roots.remove(actor);
        if (root != null) targets.remove(new RootKey(root.level, root.pos.below()), actor);
    }
    public static void clear() { roots.clear(); targets.clear(); }
}
