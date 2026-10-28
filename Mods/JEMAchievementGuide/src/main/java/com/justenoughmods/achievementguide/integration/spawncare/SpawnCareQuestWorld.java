package com.justenoughmods.achievementguide.integration.spawncare;
import com.justenoughmods.achievementguide.integration.food.FoodDroppedProof;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class SpawnCareQuestWorld {
    public record Change(BlockPos pos, BlockState before, BlockState after) {}
    public static final class Run {
        public final SpawnCareQuestRun nativeRun; public final InteractionHand hand; public final BlockPos broken; public final CompoundTag proof; public final BlockState before;
        public final List<Change> changes = new ArrayList<>();
        public Run(ServerPlayer actor, InteractionHand hand, BlockPos broken) { nativeRun = SpawnCareQuestRun.begin(actor, actor, actor.getItemInHand(hand)); this.hand = hand; this.broken = broken; before = broken == null ? null : actor.level().getBlockState(broken); proof = broken == null ? new CompoundTag() : SpawnCareQuestSetups.get(actor.serverLevel()).at(actor.serverLevel(), broken); }
    }
    private static final ThreadLocal<ArrayDeque<Run>> CURRENT = ThreadLocal.withInitial(ArrayDeque::new);
    private SpawnCareQuestWorld() {}
    public static Run begin(ServerPlayer actor, InteractionHand hand, BlockPos broken) { if (SpawnCareQuestProof.real(actor) == null) return null; var run = new Run(actor, hand, broken); CURRENT.get().push(run); return run; }
    public static void changed(ServerLevel level, BlockPos pos, BlockState before, BlockState after) { var run = CURRENT.get().peek(); if (run != null && run.nativeRun.actor.level() == level && run.changes.size() < 256 && before != after) run.changes.add(new Change(pos.immutable(), before, after)); }
    public static void end(Run run, boolean accepted) {
        if (run == null) return; var q = CURRENT.get(); if (q.peek() == run) q.pop(); if (q.isEmpty()) CURRENT.remove(); SpawnCareQuestRun.end(run.nativeRun); if (!accepted) return;
        var actor = run.nativeRun.actor; ServerLevel level = actor.serverLevel(); var setups = SpawnCareQuestSetups.get(level);
        if (SpawnCareQuestProof.debited(run.nativeRun.input, actor.getItemInHand(run.hand), 1)) for (Change change : run.changes) {
            BlockState actual = level.getBlockState(change.pos()); String block = SpawnCareQuestSetups.block(actual);
            if (actual.equals(change.after()) && actual.getBlock() != change.before().getBlock() && SpawnCareQuestProof.item(run.nativeRun.input).equals(block)
                    && Set.of("animal_feeding_trough:feeding_trough", "spawn:mucus_block", "spawn:ghostly_mucus_block", "spawn:ant_farm").contains(block)) {
                var proof = new CompoundTag(); proof.putUUID("actor", actor.getUUID()); proof.putString("block", block);
                proof.putBoolean("after_wet_collection", SpawnCareQuestProof.data(actor).hasUUID("wet_snail_collected")); setups.put(level, change.pos(), proof);
            }
        }
        if (run.broken == null || run.before == null || level.getBlockState(run.broken).getBlock() == run.before.getBlock()) return;
        for (var entity : run.nativeRun.created) if (entity instanceof ItemEntity drop && level.getEntity(drop.getUUID()) == drop) {
            if (run.proof.hasUUID("wet_snail") && SpawnCareQuestProof.item(drop.getItem()).equals("spawn:mucus")) {
                SpawnCareQuestProof.data(drop).putUUID("wet_snail", run.proof.getUUID("wet_snail")); SpawnCareQuestProof.mark(drop, "wet_collector", actor.getUUID());
                FoodDroppedProof.update(drop, "v3_pet_wet_work", drop.getItem().getCount(), actor.getUUID());
            }
            if (run.proof.hasUUID("farm_actor") && actor.getUUID().equals(run.proof.getUUID("farm_actor")) && drop.getItem().getItem() == run.before.getBlock().asItem()) FoodDroppedProof.update(drop, "v3_pet_colony_farm", drop.getItem().getCount(), actor.getUUID());
        }
    }
}
