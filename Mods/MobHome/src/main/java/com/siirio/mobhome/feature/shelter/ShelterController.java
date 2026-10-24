package com.siirio.mobhome.feature.shelter;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Only the origin cell owns data. There is intentionally no block entity ticker. */
public final class ShelterController extends BlockEntity {
    private UUID owner;
    public ShelterController(BlockPos pos, BlockState state) { super(ShelterContent.CONTROLLER.get(), pos, state); }
    public UUID owner() { return owner; }
    public void owner(UUID value) { owner = value; setChanged(); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); if (owner != null) tag.putUUID("Owner", owner);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
