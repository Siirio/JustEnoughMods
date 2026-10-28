package com.justenoughmods.achievementguide.integration.nature;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

/** Observes accepted native item spawns only inside one exact successful native drop owner call. */
public final class NatureDropProof {
    private static final ThreadLocal<Run> CURRENT = new ThreadLocal<>();
    public static final class Run {
        public final ServerPlayer actor;
        public final ServerLevel level;
        public final BlockPos pos;
        public final String item;
        public boolean accepted;
        public Run(ServerPlayer actor, BlockPos pos, String item) {
            this.actor = actor; level = actor.serverLevel(); this.pos = pos.immutable(); this.item = item;
        }
    }
    private NatureDropProof() {}
    public static <T> T nativeCall(Run run, Supplier<T> operation) {
        Run previous = CURRENT.get();
        CURRENT.set(run);
        try { return operation.get(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    public static void spawned(ServerLevel level, Entity entity, boolean accepted) {
        Run run = CURRENT.get();
        if (!accepted || run == null || run.level != level || !(entity instanceof ItemEntity item)
                || !NatureActors.eligible(run.actor) || item.getItem().isEmpty() || item.isRemoved()
                || item.position().distanceToSqr(Vec3.atCenterOf(run.pos)) > 16) return;
        String id = BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString();
        if (run.item.isEmpty() || run.item.equals(id)) run.accepted = true;
    }
}
