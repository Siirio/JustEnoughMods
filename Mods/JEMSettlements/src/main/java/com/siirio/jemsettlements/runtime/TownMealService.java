package com.siirio.jemsettlements.runtime;

import com.siirio.jemsettlements.api.SettlementApi;
import com.siirio.jemsettlements.resident.ResidentMood;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import com.siirio.jemsettlements.workforce.WorkforceType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** Short-lived orders select a real chest; registry, food policy and physical inventory retain authority. */
public final class TownMealService {
    private record Order(UUID town, UUID owner, BlockPos chest, long until) { }
    private static final Map<UUID, Order> ORDERS = new HashMap<>();
    private TownMealService() { }
    public static SettlementApi.Result request(ServerPlayer player, TownRecord town, BlockPos position) {
        if (!SettlementAuthority.active(player) || SettlementApi.ownedTown(player, town.id()).isEmpty()) return SettlementApi.Result.UNAUTHENTICATED;
        var link = town.storage().stream().filter(value -> value.dimension().equals(town.dimension()) && value.contains(position)).findFirst().orElse(null);
        if (link == null) return SettlementApi.Result.INVALID;
        ServerLevel level = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, town.dimension()));
        if (SharedStorageService.handler(level, link).isEmpty()) return SettlementApi.Result.UNAVAILABLE;
        var data = SettlementSavedData.get(player.getServer());
        var ids = new java.util.HashSet<UUID>();
        for (var resident : data.residents(town.id())) if (resident.living()) ids.add(resident.villagerId());
        for (var recruit : data.workforce(town.id())) if (recruit.type() == WorkforceType.RECRUIT && recruit.settlementActive()
                && recruit.needsEnrolled() && recruit.ownerId().equals(town.owner())) ids.add(recruit.entityId());
        long now = player.getServer().overworld().getGameTime();
        ORDERS.entrySet().removeIf(entry -> entry.getValue().until() < now);
        if ((long) ORDERS.size() + ids.stream().filter(id -> !ORDERS.containsKey(id)).count() > 16384L) return SettlementApi.Result.UNAVAILABLE;
        var order = new Order(town.id(), town.owner(), link.position(), now + 2400L);
        for (UUID id : ids) ORDERS.put(id, order);
        return SettlementApi.Result.ACCEPTED;
    }
    public static BlockPos target(ServerLevel level, TownRecord town, Mob mob, long now) {
        if (town == null) return null;
        Order order = ORDERS.get(mob.getUUID()); if (order == null) return null;
        var data = SettlementSavedData.get(level.getServer());
        boolean registered = town != null && order.town().equals(town.id()) && order.owner().equals(town.owner())
                && town.dimension().equals(level.dimension().location())
                && (data.resident(mob.getUUID()).filter(value -> value.living() && value.townId().equals(town.id())).isPresent()
                || data.workforceEntity(mob.getUUID()).filter(value -> value.type() == WorkforceType.RECRUIT && value.settlementActive()
                        && value.needsEnrolled() && value.ownerId().equals(town.owner()) && value.townId().equals(town.id())).isPresent());
        if (now > order.until() || !registered || town.storage().stream().noneMatch(value -> value.contains(order.chest()))) {
            ORDERS.remove(mob.getUUID()); return null;
        }
        return order.chest();
    }
    public static void prepare(ServerLevel level, TownRecord town, Mob mob, ResidentMood needs, long now) {
        if (needs.satiety() >= 100) { ORDERS.remove(mob.getUUID()); return; }
        if (target(level, town, mob, now) != null) needs.requestMeal();
    }
    public static int count(UUID town, BlockPos chest, long now) {
        ORDERS.entrySet().removeIf(entry -> entry.getValue().until() < now);
        return (int) ORDERS.values().stream().filter(order -> order.town().equals(town) && order.chest().equals(chest)).count();
    }
    public static void clear() { ORDERS.clear(); }
}
