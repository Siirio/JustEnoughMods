package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.ClutchBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.util.FakePlayer;

/** Native command scope and one local split-shaft run, never a redstone/network authority. */
public final class CreateControlProofs {
    private static final String RUN = "jemQuestCreateControlV1";
    private static final String TOKEN = "jemQuestCreateControlIdentityV1";
    private static final String WATCH = "jemQuestCreateInputWatchedV1";
    private static final String EPOCH = "jemQuestCreateInputStopsV1";
    private static final ThreadLocal<ServerPlayer> ACTOR = new ThreadLocal<>();
    private CreateControlProofs() {}
    public static ServerPlayer currentActor() { return ACTOR.get(); }

    public static <T> T command(ServerPlayer actor, Supplier<T> nativeCall) {
        if (actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild) return nativeCall.get();
        ServerPlayer previous = ACTOR.get();
        ACTOR.set(actor);
        try { return nativeCall.get(); }
        finally { if (previous == null) ACTOR.remove(); else ACTOR.set(previous); }
    }
    private static UUID identity(BlockEntity owner) {
        if (!owner.getPersistentData().hasUUID(TOKEN)) {
            owner.getPersistentData().putUUID(TOKEN, UUID.randomUUID());
            owner.setChanged();
        }
        return owner.getPersistentData().getUUID(TOKEN);
    }
    private static KineticBlockEntity kinetic(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic ? kinetic : null;
    }
    public static void changing(BlockState before, Level world, BlockPos pos) {
        if (!(world instanceof ServerLevel level) || !before.hasProperty(BlockStateProperties.POWERED)
                || before.getValue(BlockStateProperties.POWERED) == level.hasNeighborSignal(pos)
                || !(level.getBlockEntity(pos) instanceof SplitShaftBlockEntity shaft)) return;
        BlockEntity owner = (BlockEntity)(Object)shaft;
        CompoundTag prior = owner.getPersistentData().getCompound(RUN);
        owner.getPersistentData().remove(RUN);
        owner.setChanged();
        ServerPlayer actor = ACTOR.get();
        if (actor == null || actor.serverLevel() != level || !shaft.hasSource()) return;
        boolean clutch = shaft instanceof ClutchBlockEntity;
        boolean powering = !before.getValue(BlockStateProperties.POWERED);
        if (clutch && !powering) {
            if (prior.getString("phase").equals("stopped") && prior.hasUUID("actor")
                    && actor.getUUID().equals(prior.getUUID("actor")) && valid(level, owner, prior)) {
                prior.putString("phase", "restarting");
                prior.putBoolean("powered", false);
                owner.getPersistentData().put(RUN, prior);
            }
            return;
        }
        BlockPos inputPos = pos.relative(shaft.getSourceFacing());
        BlockPos outputPos = pos.relative(shaft.getSourceFacing().getOpposite());
        KineticBlockEntity input = kinetic(level, inputPos), output = kinetic(level, outputPos);
        if (input == null || output == null || input.getSpeed() == 0 || output.getSpeed() == 0 || !pos.equals(output.source)) return;
        CompoundTag run = new CompoundTag();
        run.putUUID("actor", actor.getUUID());
        run.putLong("input", inputPos.asLong());
        run.putLong("output", outputPos.asLong());
        run.putUUID("input_identity", identity((BlockEntity)(Object)input));
        run.putUUID("output_identity", identity((BlockEntity)(Object)output));
        input.getPersistentData().putBoolean(WATCH, true);
        input.setChanged();
        run.putLong("epoch", input.getPersistentData().getLong(EPOCH));
        run.putFloat("output_speed", output.getSpeed());
        run.putFloat("input_sign", Math.signum(input.getSpeed()));
        run.putBoolean("powered", powering);
        run.putString("phase", clutch ? "stopping" : "reversing");
        owner.getPersistentData().put(RUN, run);
    }
    private static boolean valid(ServerLevel level, BlockEntity owner, CompoundTag run) {
        if (!run.hasUUID("input_identity") || !run.hasUUID("output_identity")) return false;
        KineticBlockEntity input = kinetic(level, BlockPos.of(run.getLong("input")));
        KineticBlockEntity output = kinetic(level, BlockPos.of(run.getLong("output")));
        return input != null && output != null && input.getSpeed() != 0 && Math.signum(input.getSpeed()) == run.getFloat("input_sign")
                && input.getPersistentData().hasUUID(TOKEN) && output.getPersistentData().hasUUID(TOKEN)
                && input.getPersistentData().getUUID(TOKEN).equals(run.getUUID("input_identity"))
                && output.getPersistentData().getUUID(TOKEN).equals(run.getUUID("output_identity"))
                && input.getPersistentData().getLong(EPOCH) == run.getLong("epoch");
    }
    public static void changed(Level world, BlockPos pos, boolean propagated) {
        if (!(world instanceof ServerLevel level) || !(level.getBlockEntity(pos) instanceof SplitShaftBlockEntity shaft)) return;
        BlockEntity owner = (BlockEntity)(Object)shaft;
        CompoundTag run = owner.getPersistentData().getCompound(RUN);
        if (!run.hasUUID("actor")) return;
        if (!valid(level, owner, run) || !owner.getBlockState().hasProperty(BlockStateProperties.POWERED)
                || owner.getBlockState().getValue(BlockStateProperties.POWERED) != run.getBoolean("powered")) {
            owner.getPersistentData().remove(RUN); owner.setChanged(); return;
        }
        KineticBlockEntity output = kinetic(level, BlockPos.of(run.getLong("output")));
        String phase = run.getString("phase");
        if (phase.equals("stopping") && output.getSpeed() == 0) {
            run.putString("phase", "stopped");
            owner.setChanged();
        } else if (propagated && shaft.hasSource() && BlockPos.of(run.getLong("input")).equals(shaft.source)
                && pos.equals(output.source) && output.getSpeed() != 0
                && (phase.equals("restarting") || phase.equals("reversing") && output.getSpeed() * run.getFloat("output_speed") < 0)) {
            CreateRunProofs.emit(level, run.getUUID("actor"), phase.equals("restarting") ? "create_clutch_stop_restart" : "create_gearshift_reverse");
            owner.getPersistentData().remove(RUN);
            owner.setChanged();
        } else if (phase.equals("stopping") || propagated && (phase.equals("restarting") || phase.equals("reversing"))) {
            owner.getPersistentData().remove(RUN);
            owner.setChanged();
        }
    }
    public static void speedChanged(KineticBlockEntity input, float oldSpeed) {
        if (input.getLevel() instanceof ServerLevel && input.getPersistentData().getBoolean(WATCH)
                && oldSpeed != 0 && input.getSpeed() == 0) {
            input.getPersistentData().putLong(EPOCH, input.getPersistentData().getLong(EPOCH) + 1);
            input.setChanged();
        }
    }
}
