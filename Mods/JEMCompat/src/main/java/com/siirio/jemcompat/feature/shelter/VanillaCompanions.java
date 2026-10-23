package com.siirio.jemcompat.feature.shelter;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Mule;

/** Explicit audited native classes: inheritance alone is never a recovery contract. */
public final class VanillaCompanions {
    private VanillaCompanions() { }
    public static UUID owner(Entity entity) {
        if (entity instanceof TamableAnimal animal && animal.isTame()) return animal.getOwnerUUID();
        if (entity instanceof AbstractHorse horse && horse.isTamed()) return horse.getOwnerUUID();
        return entity instanceof OwnableEntity ownable ? ownable.getOwnerUUID() : null;
    }
    public static boolean supported(Entity entity) {
        Class<?> type = entity.getClass();
        if (type != Cat.class && type != Wolf.class && type != Horse.class && type != Donkey.class && type != Mule.class) return false;
        // Unknown capability-owned inventories have no verified transfer/drop reconciliation seam.
        CompoundTag nativeTag = entity.saveWithoutId(new CompoundTag());
        return !nativeTag.contains("ForgeCaps") || nativeTag.getCompound("ForgeCaps").isEmpty();
    }
    public static boolean nativeOwnerTeleport(Entity entity) { return entity.getClass() == Cat.class || entity.getClass() == Wolf.class; }
}
