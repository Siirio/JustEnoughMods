package com.siirio.mobhome.mixin.shelter.pack;

import com.siirio.mobhome.feature.shelter.compat.PackCompanionAdapter;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "dev.xylonity.companions.common.entity.summon.EmberPoleEntity", remap = false)
public abstract class EmberPoleTransientMixin implements PackCompanionAdapter.NativeTransient {
    @Shadow(remap = false) private int explodeCounter;
    @Shadow(remap = false) public abstract boolean isExploding();
    @Shadow(remap = false) public abstract void setIsExploding(boolean value);
    @Override public void jemShelterWriteTransient(CompoundTag tag) {
        tag.putInt("jemShelterEmberCounter", explodeCounter);
        tag.putBoolean("jemShelterEmberExploding", isExploding());
    }
    @Override public void jemShelterReadTransient(CompoundTag tag) {
        explodeCounter = tag.getInt("jemShelterEmberCounter");
        setIsExploding(tag.getBoolean("jemShelterEmberExploding"));
    }
}
