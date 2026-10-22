package com.siirio.jemserver.smp;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class SmpRecords {
    public static boolean member(PartyState row, UUID id) { return row.members().containsKey(id); }
    public static boolean member(EventState row, UUID id) { return row.members().containsKey(id); }
    public static Set<UUID> memberIds(PartyState row) { return row.members().keySet(); }
    public static Set<UUID> memberIds(EventState row) { return row.members().keySet(); }
    public static void owner(ServerPlayer player, SmpRecord row) {
        require(player.getUUID().equals(row.owner()), "owner_required");
    }

    public static void require(boolean condition, String message) {
        if (!condition) throw new SmpActionFailure(message);
    }

    public static String text(String value, int max) {
        value = value.strip();
        require(value.length() <= max && value.chars().noneMatch(c -> c < 32 || c == 167), "invalid_text");
        return value;
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
            throw new SmpActionFailure("invalid_number");
        }
    }

    public static long time(com.google.gson.JsonObject args, String key) {
        String value = text(args, key, 40);
        try {
            return value.isBlank() ? 0 : java.time.Instant.parse(value).toEpochMilli();
        } catch (java.time.DateTimeException | ArithmeticException failure) {
            throw new SmpActionFailure("invalid_deadline");
        }
    }

    public static void locate(PartyState row, ServerPlayer player) {
        row.dimension(player.level().dimension().location().toString());
        row.position(player.blockPosition().asLong());
    }

    public static boolean present(PartyState row, ServerPlayer player) {
        if (!row.dimension().equals(player.level().dimension().location().toString())
                || !player.isAlive() || player.isSpectator()) return false;
        var bounds = row.structureBounds();
        if (row.hasStructureId() && bounds != null) {
            return new net.minecraft.world.level.levelgen.structure.BoundingBox(
                    bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())
                    .isInside(player.blockPosition());
        }
        return player.blockPosition().distSqr(BlockPos.of(row.position()))
                <= (long) SmpConfig.ENCOUNTER_RADIUS.get() * SmpConfig.ENCOUNTER_RADIUS.get();
    }

    public static void posting(ServerPlayer player, String table) {
        var data = SmpData.get(player.server);
        Collection<? extends SmpRecord> rows = switch (SmpTable.fromKey(table)) {
            case PARTIES -> data.parties().snapshots();
            case EVENTS -> data.events().snapshots();
            default -> throw new SmpActionFailure("unavailable");
        };
        long count = rows.stream().filter(row -> player.getUUID().equals(row.owner()) && !row.closed()).count();
        require(count < SmpConfig.MAX_ACTIVE.get(), "active_limit");
        long newest = rows.stream().filter(row -> player.getUUID().equals(row.owner()))
                .mapToLong(SmpRecord::created).max().orElse(0);
        require(System.currentTimeMillis() - newest >= SmpConfig.POST_COOLDOWN.get() * 1000L, "cooldown");
    }

    private SmpRecords() {}
}
