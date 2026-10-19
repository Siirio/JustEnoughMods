package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Native living callbacks retain golem identity without territory-wide entity scans. */
public final class GuardObservationService {
    private GuardObservationService() { }
    @SubscribeEvent public static void joined(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !SettlementAuthority.active(level.getServer()) || event.loadedFromDisk()) return;
        if (!(event.getEntity() instanceof Mob mob) || !guardType(mob)) return;
        for (var town : SettlementSavedData.get(level.getServer()).towns())
            if (town.dimension().equals(level.dimension().location()) && town.contains(mob.blockPosition())) {
                mob.getPersistentData().putUUID("JemGuardSpawnTown", town.id()); break;
            }
    }
    private static boolean guardType(Mob mob) {
        return mob instanceof IronGolem || net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace().equals("recruits");
    }
    @SubscribeEvent public static void observe(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !guardType(mob) || !(mob.level() instanceof ServerLevel level)
                || !SettlementAuthority.active(level.getServer()) || Math.floorMod(mob.tickCount, 100) != 0) return;
        var data=SettlementSavedData.get(level.getServer());String key=mob.getUUID().toString();
        for(var town:data.towns()) {
            var metrics=town.metrics();var rows=metrics.getCompound("GolemObservations").copy();
            boolean inside=town.dimension().equals(level.dimension().location())&&town.contains(mob.blockPosition())&&mob.isAlive();
            boolean belongs=mob instanceof IronGolem||data.workforceEntity(mob.getUUID()).filter(record->record.settlementActive()&&record.townId().equals(town.id())).isPresent();
            if(inside&&belongs&&mob.getPersistentData().hasUUID("JemGuardSpawnTown")&&town.id().equals(mob.getPersistentData().getUUID("JemGuardSpawnTown"))
                    &&!mob.getPersistentData().getBoolean("JemGuardNamed")) {
                if(!mob.hasCustomName()) {
                    int next=metrics.getInt("NextGuardNumber")+1;metrics.putInt("NextGuardNumber",next);
                    mob.setCustomName(net.minecraft.network.chat.Component.translatable("jem.settlements.guard_name",town.name(),next));
                }
                mob.getPersistentData().putBoolean("JemGuardNamed",true);
            }
            if(mob instanceof IronGolem) {
                if(!inside){rows.remove(key);} else if(rows.size()<512||rows.contains(key)) {
                    var row=new CompoundTag();row.putUUID("Id",mob.getUUID());row.putLong("Position",mob.blockPosition().asLong());
                    row.putBoolean("Qualified",!mob.isNoAi());row.putFloat("Health",mob.getHealth());row.putFloat("MaxHealth",mob.getMaxHealth());
                    row.putString("NameComponent",net.minecraft.network.chat.Component.Serializer.toJson(mob.getName()));rows.put(key,row);
                }
                metrics.put("GolemObservations",rows);
            }
            town.metrics(metrics);
        }
        data.changed();
    }
    public static int count(TownRecord town,ServerLevel level,CompoundTag metrics) {
        var rows=metrics.getCompound("GolemObservations").copy();int count=0;
        for(String key:java.util.Set.copyOf(rows.getAllKeys())) {
            var row=rows.getCompound(key);var entity=level.getEntity(java.util.UUID.fromString(key));
            if(entity!=null&&(!(entity instanceof IronGolem)||!entity.isAlive()||!town.contains(entity.blockPosition()))) {rows.remove(key);continue;}
            boolean elsewhere=false;
            if(entity==null)for(var other:level.getServer().getAllLevels())if(other!=level&&other.getEntity(java.util.UUID.fromString(key))!=null)elsewhere=true;
            if(elsewhere){rows.remove(key);continue;}
            // Entity-leave destruction removes confirmed deaths; absent UUIDs may still be loading.
            if(row.getBoolean("Qualified"))count++;
        }
        metrics.put("GolemObservations",rows);return count;
    }
    @SubscribeEvent public static void removed(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
        if(!(event.getEntity() instanceof IronGolem)||!(event.getLevel() instanceof ServerLevel level)||!SettlementAuthority.active(level.getServer()))return;
        var reason=event.getEntity().getRemovalReason();if(reason==null||!reason.shouldDestroy())return;
        var data=SettlementSavedData.get(level.getServer());
        for(var town:data.towns()){var metrics=town.metrics();metrics.getCompound("GolemObservations").remove(event.getEntity().getUUID().toString());town.metrics(metrics);}data.changed();
    }
}
