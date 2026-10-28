package com.justenoughmods.achievementguide.integration.nature;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Exact physical source placement/release -> accepted falling entity -> native conversion lineage. */
public final class NatureFallingProof {
    private static final String KEY = "JemNatureFallV4";
    private NatureFallingProof() {}

    public static void attributed(ServerPlayer actor, BlockPos pos, BlockState state, String kind) {
        if (!NatureActors.eligible(actor)) return;
        CompoundTag origin = new CompoundTag();
        origin.putUUID("actor", actor.getUUID());
        origin.put("state", NbtUtils.writeBlockState(state));
        NatureChunkOrigins.put(actor.serverLevel(), pos, kind, origin);
    }

    public static void released(ServerPlayer actor, BlockPos support) {
        if (!NatureActors.eligible(actor)) return;
        ServerLevel level = actor.serverLevel();
        BlockPos pos = support.above();
        if (FallingBlock.isFree(level.getBlockState(support)) && level.getBlockState(pos).is(BlockTags.ANVIL)) {
            attributed(actor, pos, level.getBlockState(pos), "Anvil");
        }
    }

    public static FallingBlockEntity fall(Level world, BlockPos pos, BlockState state) {
        String kind = state.is(BlockTags.ANVIL) ? "Anvil" : NatureNativeFacts.block(state, "coconut") ? "Coconut" : "";
        CompoundTag origin = world instanceof ServerLevel level && !kind.isEmpty()
                ? NatureChunkOrigins.get(level, pos, kind) : new CompoundTag();
        boolean sameSource = origin.hasUUID("actor") && origin.getCompound("state").equals(NbtUtils.writeBlockState(state))
                && world.getBlockState(pos).equals(state);
        FallingBlockEntity falling = FallingBlockEntity.fall(world, pos, state);
        if (sameSource && world instanceof ServerLevel level && level.getEntity(falling.getUUID()) == falling
                && falling.getStartPos().equals(pos) && !level.getBlockState(pos).equals(state)) {
            if (kind.equals("Anvil")) {
                CompoundTag provenance = new CompoundTag();
                provenance.putUUID("actor", origin.getUUID("actor"));
                provenance.putUUID("entity", falling.getUUID());
                provenance.putLong("source", pos.asLong());
                falling.getPersistentData().put(KEY, provenance);
            } else NatureActors.emit(level.getServer().getPlayerList().getPlayer(origin.getUUID("actor")), "v3_world_root_and_crown");
        }
        return falling;
    }

    public static void landed(Fallable block, Level world, BlockPos pos, BlockState fallingState,
                              BlockState replaced, FallingBlockEntity falling) {
        boolean nativeSetup = fallingState.is(BlockTags.ANVIL) && world instanceof ServerLevel && NatureNativeFacts.deepslateEnabled()
                && world.getBlockState(pos.below()).is(Blocks.STONE) && world.getBlockState(pos.below(2)).is(Blocks.MAGMA_BLOCK);
        block.onLand(world, pos, fallingState, replaced, falling);
        if (!nativeSetup || !(world instanceof ServerLevel level) || !world.getBlockState(pos.below()).is(Blocks.DEEPSLATE)) return;
        CompoundTag provenance = falling.getPersistentData().getCompound(KEY);
        if (!provenance.hasUUID("actor") || !provenance.hasUUID("entity")
                || !provenance.getUUID("entity").equals(falling.getUUID())
                || !provenance.contains("source", 4) || falling.getStartPos().asLong() != provenance.getLong("source")) return;
        NatureActors.emit(level.getServer().getPlayerList().getPlayer(provenance.getUUID("actor")), "v3_world_stone_press");
        falling.getPersistentData().remove(KEY);
    }
}
