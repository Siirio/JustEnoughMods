package com.justenoughmods.achievementguide.integration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;

/** Bounded read-only snapshots of the exact native capabilities participating in one operation. */
public final class FluidTransferObservation {
    public record Kind(Fluid fluid, CompoundTag tag) {}
    private FluidTransferObservation() {}

    public static Map<Kind, Long> snapshot(IFluidHandler handler) {
        if (handler == null || handler.getTanks() > 128) return null;
        Map<Kind, Long> amounts = new HashMap<>();
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            var stack = handler.getFluidInTank(tank);
            if (stack.isEmpty()) continue;
            Kind kind = new Kind(stack.getFluid(), stack.hasTag() ? stack.getTag().copy() : new CompoundTag());
            amounts.merge(kind, (long) stack.getAmount(), Long::sum);
        }
        return amounts;
    }

    public static long experience(Map<Kind, Long> amounts) {
        if (amounts == null) return -1;
        long sum = 0;
        var tag = ForgeRegistries.FLUIDS.tags().getTag(ModFluids.EXPERIENCE_TAG);
        for (var entry : amounts.entrySet()) if (tag.contains(entry.getKey().fluid())) sum += entry.getValue();
        return sum;
    }

    public static boolean conserved(Map<Kind, Long> firstBefore, Map<Kind, Long> firstAfter,
                                     Map<Kind, Long> secondBefore, Map<Kind, Long> secondAfter) {
        if (firstBefore == null || firstAfter == null || secondBefore == null || secondAfter == null) return false;
        var kinds = new HashSet<>(firstBefore.keySet());
        kinds.addAll(firstAfter.keySet()); kinds.addAll(secondBefore.keySet()); kinds.addAll(secondAfter.keySet());
        int direction = 0;
        for (Kind kind : kinds) {
            long first = firstAfter.getOrDefault(kind, 0L) - firstBefore.getOrDefault(kind, 0L);
            long second = secondAfter.getOrDefault(kind, 0L) - secondBefore.getOrDefault(kind, 0L);
            if (first != -second) return false;
            if (first != 0) {
                int sign = Long.signum(first);
                if (direction != 0 && direction != sign) return false;
                direction = sign;
            }
        }
        return direction != 0;
    }
}
