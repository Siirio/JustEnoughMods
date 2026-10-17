package com.siirio.jemsettlements.collection;

import com.siirio.jemsettlements.runtime.SettlementAuthority;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;

public final class MountCollectionService {
    private static final String INELIGIBLE = "JemCollectionAdminSpawn";
    private MountCollectionService() { }
    public static void finalizedSpawn(MobSpawnEvent.FinalizeSpawn event) { MobSpawnType type = event.getSpawnType(); if (type == MobSpawnType.COMMAND || type == MobSpawnType.SPAWN_EGG || type == MobSpawnType.DISPENSER) event.getEntity().getPersistentData().putBoolean(INELIGIBLE, true); }
    public static boolean tame(AnimalTameEvent event) {
        if (!(event.getTamer() instanceof ServerPlayer player) || player.isCreative() || event.getAnimal().getPersistentData().getBoolean(INELIGIBLE)) return false;
        Entity entity = event.getAnimal(); var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()); if (!MountRoster.contains(id)) return false;
        if (!(entity instanceof TamableAnimal) && !(entity instanceof AbstractHorse)) return false;
        var principal = SettlementAuthority.principal(player);
        return principal.isPresent() && PlayerMountCollectionSavedData.get(player.getServer()).record(principal.get(), entity.getUUID(), id);
    }
}
