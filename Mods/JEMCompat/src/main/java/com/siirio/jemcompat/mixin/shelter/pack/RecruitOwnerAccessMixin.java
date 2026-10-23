package com.siirio.jemcompat.mixin.shelter.pack;

import com.siirio.jemcompat.feature.shelter.compat.PackCompanionAdapter;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "com.talhanation.recruits.entities.AbstractRecruitEntity", remap = false)
public abstract class RecruitOwnerAccessMixin implements PackCompanionAdapter.NativeOwner {
    @Shadow(remap = false) public abstract UUID getOwnerUUID();
    @Override public UUID jemShelterNativeOwner() { return getOwnerUUID(); }
}
