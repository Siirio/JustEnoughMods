package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidStack;

public final class CreateSpoutProofs {
    private static final String PUMP = "jemQuestPumpConfigurationV1";
    public static final String FLUID = "jemQuestSpoutFluidV1";
    private CreateSpoutProofs() {}
    public static CompoundTag snapshot(FluidStack fluid) { return fluid.writeToNBT(new CompoundTag()); }

    public static void configured(BlockState before, UseOnContext context, InteractionResult result) {
        if (context.getPlayer() instanceof ServerPlayer actor && !(actor instanceof FakePlayer)
                && !actor.isSpectator() && !actor.getAbilities().instabuild && result.consumesAction()
                && !before.equals(context.getLevel().getBlockState(context.getClickedPos()))
                && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PumpBlockEntity pump) {
            BlockEntity owner = (BlockEntity)(Object)pump;
            CompoundTag proof = new CompoundTag();
            proof.putUUID("actor", actor.getUUID());
            proof.putString("configuration", owner.getBlockState().toString());
            owner.getPersistentData().put(PUMP, proof);
            owner.setChanged();
        }
    }
    public static UUID operator(Map<BlockPos, WeakReference<FluidTransportBehaviour>> nativeCache, Set<BlockPos> visited) {
        if (nativeCache.size() > 4096 || visited.size() > 4096) return null;
        Set<UUID> actors = new HashSet<>();
        // Reuse Create's cached actual flow graph. Never discover a new world graph or choose a nearby player.
        for (var entry : nativeCache.entrySet()) {
            if (!visited.contains(entry.getKey())) continue;
            FluidTransportBehaviour behaviour = entry.getValue().get();
            if (behaviour == null || !(behaviour.blockEntity instanceof PumpBlockEntity pump) || pump.getSpeed() == 0) continue;
            BlockEntity owner = (BlockEntity)(Object)pump;
            CompoundTag proof = owner.getPersistentData().getCompound(PUMP);
            if (owner.isRemoved() || !proof.hasUUID("actor") || !owner.getBlockState().toString().equals(proof.getString("configuration"))) return null;
            actors.add(proof.getUUID("actor"));
        }
        return actors.size() == 1 ? actors.iterator().next() : null;
    }
}
