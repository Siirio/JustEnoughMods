package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.items.IItemHandler;

/** Native moving actor's one synchronous break and its actual stored harvest output. */
public final class CreateMovingHarvest {
    public static final String ASSEMBLER = "jemQuestCreateAssemblerV1";
    private static final ThreadLocal<Harvest> CURRENT = new ThreadLocal<>();
    private static final class Harvest {
        final MovementContext context;
        boolean output;
        Harvest(MovementContext context) { this.context = context; }
    }
    private CreateMovingHarvest() {}

    public static boolean mature(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state);
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && age.getName().equals("age")) {
                int maximum = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(-1);
                return state.getValue(age) == maximum;
            }
        }
        return false;
    }
    public static void harvested(MovementContext context, BlockPos pos, boolean crop, Runnable nativeCall) {
        BlockState before = context.world.getBlockState(pos);
        Entity entity = context.contraption == null ? null : (Entity)(Object)context.contraption.entity;
        if (!(context.world instanceof ServerLevel level) || entity == null || entity.isRemoved()
                || !entity.getPersistentData().hasUUID(ASSEMBLER) || (crop ? !mature(before) : !before.is(BlockTags.LOGS))) {
            nativeCall.run();
            return;
        }
        Harvest previous = CURRENT.get();
        Harvest harvest = new Harvest(context);
        CURRENT.set(harvest);
        try {
            nativeCall.run();
            if (harvest.output && !before.equals(level.getBlockState(pos))) {
                UUID actor = entity.getPersistentData().getUUID(ASSEMBLER);
                CreateRunProofs.emit(level, actor, crop ? "create_harvester_crop" : "create_moving_saw_harvest");
            }
        } finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    public static void dropped(MovementBehaviour behaviour, MovementContext context, ItemStack item) {
        Harvest harvest = CURRENT.get();
        if (harvest == null || harvest.context != context || item.isEmpty() || harvest.output) {
            behaviour.dropItem(context, item);
            return;
        }
        // This is the exact synchronous native output call, never a later inventory observation.
        ItemStack output = item.copy();
        IItemHandler storage = context.contraption.getStorage().getAllItems();
        long before = count(storage, output);
        boolean spawned = CreateHarvestDropCommit.observe(context.world, output, () -> behaviour.dropItem(context, item));
        long accepted = count(storage, output) - before;
        harvest.output = accepted > 0 && accepted <= output.getCount() || spawned;
    }
    private static long count(IItemHandler storage, ItemStack item) {
        long total = 0;
        for (int slot = 0; slot < storage.getSlots(); slot++) {
            ItemStack actual = storage.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(actual, item)) total += actual.getCount();
        }
        return total;
    }
}
