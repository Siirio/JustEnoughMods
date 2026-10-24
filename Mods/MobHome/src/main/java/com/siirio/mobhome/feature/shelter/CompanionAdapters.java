package com.siirio.mobhome.feature.shelter;

import com.siirio.mobhome.feature.shelter.compat.AlexCompanionAdapter;
import com.siirio.mobhome.feature.shelter.compat.PackCompanionAdapter;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;

/** Native ownership stays authoritative. A rider may explicitly claim a native ownerless mount at Bind. */
public final class CompanionAdapters {
    public enum Habitat { GROUND, AMPHIBIOUS, AQUATIC, FLYING, LAVA }
    public static final String CUSTODIAN = "JemShelterCustodian";
    private CompanionAdapters() { }
    public static boolean excluded(String species) { return species.startsWith("recruits:"); }
    public static Entity canonical(Entity entity) { return AlexCompanionAdapter.canonical(entity); }
    public static boolean supported(Entity entity) {
        if (excluded(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString())) return false;
        boolean vanilla = entity.getClass().getName().startsWith("net.minecraft.");
        return entity instanceof LivingEntity && ((vanilla && (entity instanceof TamableAnimal || entity instanceof AbstractHorse
                || entity instanceof Camel || entity instanceof Pig || entity instanceof Strider
                || entity instanceof net.minecraft.world.entity.animal.allay.Allay))
                || AlexCompanionAdapter.handles(entity) || PackCompanionAdapter.handles(entity));
    }
    public static UUID nativeOwner(Entity entity) {
        if (excluded(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString())) return null;
        if (entity instanceof net.minecraft.world.entity.animal.allay.Allay allay)
            return allay.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.LIKED_PLAYER).orElse(null);
        UUID specific = PackCompanionAdapter.owner(entity);
        if (specific != null) return specific;
        specific = AlexCompanionAdapter.owner(entity);
        if (specific != null) return specific;
        if (entity instanceof TamableAnimal animal && animal.isTame()) return animal.getOwnerUUID();
        if (entity instanceof AbstractHorse horse && horse.isTamed()) return horse.getOwnerUUID();
        return null;
    }
    public static UUID rider(Entity entity) {
        UUID rider = AlexCompanionAdapter.rider(entity);
        if (rider != null) return rider;
        return supported(entity) && entity.getControllingPassenger() instanceof Player player ? player.getUUID() : null;
    }
    public static UUID owner(Entity entity) {
        UUID nativeOwner = nativeOwner(entity);
        if (nativeOwner != null) return nativeOwner;
        if (!supported(entity)) return null;
        if (entity.level() instanceof ServerLevel level) {
            CompanionData.Pet pet = CompanionData.get(level).pet(entity.getUUID());
            if (pet != null && pet.custodian) return pet.owner;
        }
        CompoundTag tag = entity.getPersistentData();
        if (tag.hasUUID(CUSTODIAN)) return tag.getUUID(CUSTODIAN);
        return rider(entity);
    }
    public static boolean provisional(Entity entity) {
        if (nativeOwner(entity) != null || entity.getPersistentData().hasUUID(CUSTODIAN)) return false;
        if (entity.level() instanceof ServerLevel level) {
            CompanionData.Pet pet = CompanionData.get(level).pet(entity.getUUID()); if (pet != null && pet.custodian) return false;
        }
        return true;
    }
    public static boolean ready(Entity entity) { return AlexCompanionAdapter.ready(entity) && PackCompanionAdapter.ready(entity); }
    public static String readinessReason(Entity entity) {
        return AlexCompanionAdapter.ready(entity) ? "native_lifetime_unavailable" : "native_parts_unavailable";
    }
    public static net.minecraft.world.phys.AABB bounds(Entity entity) { return AlexCompanionAdapter.bounds(entity); }
    public static Habitat habitat(Entity entity) {
        if (AlexCompanionAdapter.handles(entity)) return AlexCompanionAdapter.habitat(entity);
        if (PackCompanionAdapter.handles(entity)) return PackCompanionAdapter.habitat(entity);
        if (entity instanceof Strider) return Habitat.LAVA;
        if (entity instanceof net.minecraft.world.entity.animal.Parrot || entity instanceof net.minecraft.world.entity.animal.allay.Allay) return Habitat.FLYING;
        return Habitat.GROUND;
    }
    public static boolean nativeOwnerTeleport(Entity entity) {
        if (AlexCompanionAdapter.handles(entity)) return AlexCompanionAdapter.nativeOwnerTeleport(entity);
        if (PackCompanionAdapter.handles(entity)) return PackCompanionAdapter.nativeOwnerTeleport(entity);
        return entity instanceof TamableAnimal;
    }
    public static void snapshot(Entity entity, CompoundTag tag) {
        AlexCompanionAdapter.snapshot(entity, tag); PackCompanionAdapter.snapshot(entity, tag);
    }
    public static void recovered(Entity entity) {
        AlexCompanionAdapter.recovered(entity); PackCompanionAdapter.recovered(entity);
    }
    public static void rollback(Entity entity) {
        try { AlexCompanionAdapter.rollback(entity); }
        finally { PackCompanionAdapter.recoveryAborted(entity); }
    }
    public static void committed(Entity entity) { PackCompanionAdapter.recoveryCommitted(entity); }
    public static java.util.List<Entity> persistenceEntities(Entity entity) { return AlexCompanionAdapter.persistenceEntities(entity); }
    public static boolean bound(Entity entity) {
        entity = canonical(entity);
        if (!(entity.level() instanceof ServerLevel level)) return false;
        CompanionData.Pet pet = CompanionData.get(level).pet(entity.getUUID());
        return pet != null && pet.home != null && pet.owner.equals(owner(entity)) && supported(entity)
                && pet.species.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }
    public static long epoch(UUID id, ServerLevel level) {
        CompanionData.Pet pet = CompanionData.get(level).pet(id); return pet == null ? 0 : pet.epoch;
    }
}
