package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Guard records change on lifecycle, section changes, and damage. */
public final class GuardObservationService {
    private static final String TOWN = "JemGuardObservationTown";
    private GuardObservationService() { }

    private static boolean guardType(Mob mob) {
        return mob instanceof IronGolem
                || net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace().equals("recruits");
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getLevel() instanceof ServerLevel level && guardType(mob))
            observe(level, mob);
    }

    @SubscribeEvent
    public static void moved(EntityEvent.EnteringSection event) {
        if (!event.didChunkChange() || !(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level) || !guardType(mob)) return;
        observe(level, mob);
    }

    @SubscribeEvent
    public static void damaged(LivingDamageEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel level && guardType(mob))
            observe(level, mob);
    }

    @SubscribeEvent
    public static void removed(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getLevel() instanceof ServerLevel level && guardType(mob))
            remove(level, mob);
    }

    private static void observe(ServerLevel level, Mob mob) {
        if (!SettlementAuthority.active(level.getServer())) return;
        SettlementSavedData data = SettlementSavedData.get(level.getServer());
        removePrior(data, mob);
        TownRecord town = data.townAt(level.dimension().location(), mob.blockPosition()).orElse(null);
        if (town == null || !mob.isAlive()) {
            data.changed();
            return;
        }
        if (!mob.getPersistentData().getBoolean("JemGuardNamed") && !mob.hasCustomName()) {
            CompoundTag metrics = town.metrics();
            int next = metrics.getInt("NextGuardNumber") + 1;
            metrics.putInt("NextGuardNumber", next);
            town.metrics(metrics);
            mob.setCustomName(net.minecraft.network.chat.Component.translatable("jem.settlements.guard_name", town.name(), next));
            mob.getPersistentData().putBoolean("JemGuardNamed", true);
        }
        if (mob instanceof IronGolem) {
            CompoundTag metrics = town.metrics();
            CompoundTag rows = metrics.getCompound("GolemObservations").copy();
            CompoundTag row = new CompoundTag();
            row.putUUID("Id", mob.getUUID());
            row.putLong("Position", mob.blockPosition().asLong());
            row.putBoolean("Qualified", !mob.isNoAi());
            row.putFloat("Health", mob.getHealth());
            row.putFloat("MaxHealth", mob.getMaxHealth());
            row.putString("NameComponent", net.minecraft.network.chat.Component.Serializer.toJson(mob.getName()));
            rows.put(mob.getUUID().toString(), row);
            metrics.put("GolemObservations", rows);
            town.metrics(metrics);
        }
        mob.getPersistentData().putUUID(TOWN, town.id());
        data.changed();
    }

    private static void remove(ServerLevel level, Mob mob) {
        if (!SettlementAuthority.active(level.getServer())) return;
        SettlementSavedData data = SettlementSavedData.get(level.getServer());
        removePrior(data, mob);
        data.changed();
    }

    private static void removePrior(SettlementSavedData data, Mob mob) {
        if (!mob.getPersistentData().hasUUID(TOWN)) return;
        data.town(mob.getPersistentData().getUUID(TOWN)).ifPresent(town -> {
            CompoundTag metrics = town.metrics();
            metrics.getCompound("GolemObservations").remove(mob.getUUID().toString());
            town.metrics(metrics);
        });
        mob.getPersistentData().remove(TOWN);
    }

    public static int count(TownRecord town, ServerLevel level, CompoundTag metrics) {
        CompoundTag rows = metrics.getCompound("GolemObservations");
        int count = 0;
        for (String key : rows.getAllKeys()) if (rows.getCompound(key).getBoolean("Qualified")) count++;
        return count;
    }
}
