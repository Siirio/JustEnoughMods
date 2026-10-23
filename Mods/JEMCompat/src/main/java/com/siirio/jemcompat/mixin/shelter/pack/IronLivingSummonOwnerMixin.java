package com.siirio.jemcompat.mixin.shelter.pack;

import com.siirio.jemcompat.feature.shelter.compat.PackCompanionAdapter;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = {"io.redspace.ironsspellbooks.entity.mobs.SummonedHorse",
        "io.redspace.ironsspellbooks.entity.mobs.SummonedPolarBear",
        "io.redspace.ironsspellbooks.entity.mobs.SummonedSkeleton",
        "io.redspace.ironsspellbooks.entity.mobs.SummonedVex",
        "io.redspace.ironsspellbooks.entity.mobs.SummonedZombie"}, remap = false)
public abstract class IronLivingSummonOwnerMixin implements PackCompanionAdapter.NativeSummon {
    @org.spongepowered.asm.mixin.Unique private Object jemShelterRegistration;
    @Override public void jemShelterAbortSummon() {
        if (jemShelterRegistration != null) ((PackCompanionAdapter.NativeSummonManager) (Object) SummonManager.INSTANCE)
                .jemShelterAbort((Entity) (Object) this, jemShelterRegistration);
        jemShelterRegistration = null;
    }
    @Override public void jemShelterCommitSummon() { jemShelterRegistration = null; }
    @Override public UUID jemShelterNativeOwner() {
        Entity self = (Entity) (Object) this;
        Entity owner = SummonManager.getOwner(self);
        CompoundTag data = self.getPersistentData();
        return owner != null ? owner.getUUID() : data.hasUUID("jemShelterNativeSummoner")
                ? data.getUUID("jemShelterNativeSummoner") : null;
    }
    @Override public void jemShelterCaptureSummon(CompoundTag tag) {
        ((PackCompanionAdapter.NativeSummonManager) (Object) SummonManager.INSTANCE)
                .jemShelterCapture((Entity) (Object) this, tag);
    }
    @Override public void jemShelterResumeSummon() {
        Entity self = (Entity) (Object) this;
        UUID ownerId = jemShelterNativeOwner();
        if (self.getServer() == null || ownerId == null) return;
        long deadline = self.getPersistentData().getLong("jemShelterNativeSummonDeadline");
        long remaining = deadline - self.getServer().overworld().getGameTime();
        if (deadline > 0 && remaining <= 0) { self.discard(); return; }
        Entity owner = self.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return;
        if (SummonManager.getOwner(self) != null) return;
        jemShelterRegistration = ((PackCompanionAdapter.NativeSummonManager) (Object) SummonManager.INSTANCE).jemShelterBegin(self);
        SummonManager.setOwner(self, owner);
        if (deadline > 0) SummonManager.setDuration(self, (int) Math.min(Integer.MAX_VALUE, remaining));
    }
}
