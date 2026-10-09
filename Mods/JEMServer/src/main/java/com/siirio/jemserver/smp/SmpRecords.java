package com.siirio.jemserver.smp;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class SmpRecords {
    public static boolean member(CompoundTag row, UUID id) {
        return row.getCompound("members").contains(id.toString());
    }

    public static CompoundTag members(CompoundTag row) {
        if (!row.contains("members", Tag.TAG_COMPOUND)) row.put("members", new CompoundTag());
        return row.getCompound("members");
    }

    public static Set<UUID> memberIds(CompoundTag row) {
        var result = new LinkedHashSet<UUID>();
        for (String key : row.getCompound("members").getAllKeys()) {
            try {
                result.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    public static void owner(ServerPlayer player, CompoundTag row) {
        require(row.getUUID("owner").equals(player.getUUID()), "owner_required");
    }

    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    public static String text(com.google.gson.JsonObject args, String key, int max) {
        require(!args.has(key) || args.get(key).isJsonPrimitive(), "invalid_text");
        String value = args.has(key) ? args.get(key).getAsString().strip() : "";
        require(
                value.length() <= max && value.chars().noneMatch(c -> c < 32 || c == 167),
                "invalid_text");
        return value;
    }

    public static int number(
            com.google.gson.JsonObject args, String key, int fallback, int min, int max) {
        String value = text(args, key, 32);
        try {
            int result = value.isBlank() ? fallback : Integer.parseInt(value);
            require(result >= min && result <= max, "invalid_number");
            return result;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("invalid_number");
        }
    }

    public static long time(com.google.gson.JsonObject args, String key) {
        String value = text(args, key, 40);
        try {
            return value.isBlank() ? 0 : java.time.Instant.parse(value).toEpochMilli();
        } catch (java.time.DateTimeException | ArithmeticException failure) {
            throw new IllegalArgumentException("invalid_deadline");
        }
    }

    public static void locate(CompoundTag row, ServerPlayer player) {
        row.putString("dimension", player.level().dimension().location().toString());
        row.putLong("position", player.blockPosition().asLong());
    }

    public static boolean present(CompoundTag row, ServerPlayer player) {
        var bounds=row.getIntArray("structureBounds");
        if(row.hasUUID("structureId") && bounds.length==6) return row.getString("dimension").equals(player.level().dimension().location().toString()) && player.isAlive() && !player.isSpectator()
                && new net.minecraft.world.level.levelgen.structure.BoundingBox(bounds[0],bounds[1],bounds[2],bounds[3],bounds[4],bounds[5]).isInside(player.blockPosition());
        return row.getString("dimension").equals(player.level().dimension().location().toString())
                && player.isAlive()
                && !player.isSpectator()
                && player.blockPosition().distSqr(BlockPos.of(row.getLong("position")))
                        <= (long) SmpConfig.ENCOUNTER_RADIUS.get()
                                * SmpConfig.ENCOUNTER_RADIUS.get();
    }

    public static void posting(ServerPlayer player, String table) {
        var data = SmpData.get(player.server);
        long count =
                data.all(table).stream()
                        .filter(
                                row ->
                                        row.getUUID("owner").equals(player.getUUID())
                                                && !SmpData.closed(row))
                        .count();
        require(count < SmpConfig.MAX_ACTIVE.get(), "active_limit");
        long newest =
                data.all(table).stream()
                        .filter(row -> row.getUUID("owner").equals(player.getUUID()))
                        .mapToLong(row -> row.getLong("created"))
                        .max()
                        .orElse(0);
        require(
                System.currentTimeMillis() - newest >= SmpConfig.POST_COOLDOWN.get() * 1000L,
                "cooldown");
    }

    private SmpRecords() {}
}
