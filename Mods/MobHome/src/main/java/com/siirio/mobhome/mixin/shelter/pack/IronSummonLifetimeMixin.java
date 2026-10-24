package com.siirio.mobhome.mixin.shelter.pack;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mirror native ownership and its absolute expiry, without extending either. */
@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.capabilities.magic.SummonManager", remap = false)
public abstract class IronSummonLifetimeMixin implements com.siirio.mobhome.feature.shelter.compat.PackCompanionAdapter.NativeSummonManager {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private java.util.PriorityQueue<?> summonExpirations;
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private java.util.HashMap<java.util.UUID, java.util.UUID> summonToOwner;
    @Override public Object jemShelterBegin(Entity entity) {
        java.util.Set<Object> before = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        before.addAll(summonExpirations);
        return before;
    }
    @Override public void jemShelterAbort(Entity entity, Object token) {
        io.redspace.ironsspellbooks.capabilities.magic.SummonManager.removeSummon(entity);
        java.util.Set<?> before = (java.util.Set<?>) token;
        summonExpirations.removeIf(entry -> !before.contains(entry)
                && ((com.siirio.mobhome.feature.shelter.compat.PackCompanionAdapter.NativeExpiration) entry)
                .jemShelterId().equals(entity.getUUID()));
    }
    @Override public void jemShelterCapture(Entity entity, net.minecraft.nbt.CompoundTag tag) {
        net.minecraft.nbt.CompoundTag data = entity.getPersistentData();
        java.util.UUID owner = summonToOwner.get(entity.getUUID());
        if (owner != null) data.putUUID("jemShelterNativeSummoner", owner);
        if (entity.getServer() != null) for (Object entry : summonExpirations) {
            com.siirio.mobhome.feature.shelter.compat.PackCompanionAdapter.NativeExpiration expiry =
                    (com.siirio.mobhome.feature.shelter.compat.PackCompanionAdapter.NativeExpiration) entry;
            if (expiry.jemShelterId().equals(entity.getUUID())) {
                long deadline = entity.getServer().overworld().getGameTime()
                        + expiry.jemShelterExpiry() - entity.getServer().getTickCount();
                if (!data.contains("jemShelterNativeSummonDeadline")
                        || deadline < data.getLong("jemShelterNativeSummonDeadline"))
                    data.putLong("jemShelterNativeSummonDeadline", deadline);
            }
        }
        tag.put("ForgeData", data.copy());
    }
    @Inject(method = "setOwner", at = @At("TAIL"), remap = false)
    private static void jemOwner(Entity summon, Entity owner, CallbackInfo ci) {
        if (owner != null) summon.getPersistentData().putUUID("jemShelterNativeSummoner", owner.getUUID());
        else summon.getPersistentData().remove("jemShelterNativeSummoner");
    }
    @Inject(method = "setDuration", at = @At("TAIL"), remap = false)
    private static void jemDuration(Entity summon, int duration, CallbackInfo ci) {
        if (summon.getServer() != null) summon.getPersistentData().putLong("jemShelterNativeSummonDeadline",
                summon.getServer().overworld().getGameTime() + duration);
    }
}
