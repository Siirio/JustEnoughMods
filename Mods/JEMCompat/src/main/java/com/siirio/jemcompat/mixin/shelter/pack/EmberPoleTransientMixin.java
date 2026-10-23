package com.siirio.jemcompat.mixin.shelter.pack;

import com.siirio.jemcompat.feature.shelter.compat.PackCompanionAdapter;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "dev.xylonity.companions.common.entity.summon.EmberPoleEntity", remap = false)
public abstract class EmberPoleTransientMixin implements PackCompanionAdapter.NativeTransient {
    @Shadow private int explodeCounter;
    @Shadow public abstract boolean isExploding();
    @Shadow public abstract void setIsExploding(boolean value);
    @Override public void jemShelterWriteTransient(CompoundTag tag) {
        tag.putInt("jemShelterEmberCounter", explodeCounter);
        tag.putBoolean("jemShelterEmberExploding", isExploding());
    }
    @Override public void jemShelterReadTransient(CompoundTag tag) {
        explodeCounter = tag.getInt("jemShelterEmberCounter");
        setIsExploding(tag.getBoolean("jemShelterEmberExploding"));
    }
}
