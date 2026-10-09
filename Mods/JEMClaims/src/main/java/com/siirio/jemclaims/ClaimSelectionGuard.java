package com.siirio.jemclaims;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ClaimSelectionGuard {
    private static final Map<UUID, Watched> WATCHED = new HashMap<>();
    private static java.util.function.BiPredicate<ServerPlayer, Area> reservation = (player, area) -> true;
    public record Area(int minX, int minZ, int maxX, int maxZ) {}

    public static void reservation(java.util.function.BiPredicate<ServerPlayer, Area> validator) {
        reservation = java.util.Objects.requireNonNull(validator);
    }
    private record Watched(ResourceKey<Level> dimension, LongSet chunks) {}

    @SubscribeEvent
    public void watch(ChunkWatchEvent.Watch event) {
        Watched watched = WATCHED.compute(event.getPlayer().getUUID(), (id, existing) -> existing != null && existing.dimension().equals(event.getLevel().dimension())
                ? existing : new Watched(event.getLevel().dimension(), new LongOpenHashSet()));
        watched.chunks().add(event.getPos().toLong());
        BossClaimZones.get(event.getLevel().getServer()).observe(event.getLevel(), event.getChunk());
    }

    @SubscribeEvent
    public void unwatch(ChunkWatchEvent.UnWatch event) {
        Watched watched = WATCHED.get(event.getPlayer().getUUID());
        if (watched != null && watched.dimension().equals(event.getLevel().dimension())) watched.chunks().remove(event.getPos().toLong());
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) { WATCHED.remove(event.getEntity().getUUID()); }

    @SubscribeEvent
    public void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Watched watched = WATCHED.get(event.getEntity().getUUID());
        if (watched != null && !watched.dimension().equals(event.getTo())) WATCHED.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void stopped(ServerStoppedEvent event) { WATCHED.clear(); }

    public static boolean click(ServerPlayer player, BlockPos position) {
        if (player.isAlive() && player.canReach(position, 0) && available(player, new ChunkPos(position))) return true;
        return deny(player, "Выберите блок в пределах досягаемости и текущей загруженной области.");
    }

    public static boolean validate(ServerPlayer player, BlockPos first, BlockPos second) {
        if (player == null || !player.isAlive()) return false;
        long area = (Math.abs((long) first.getX() - second.getX()) + 1) * (Math.abs((long) first.getZ() - second.getZ()) + 1);
        if (area < ClaimsConfig.MIN_AREA.get()) return deny(player, "Минимальная площадь территории: " + ClaimsConfig.MIN_AREA.get() + " блока.");
        if (!player.serverLevel().getWorldBorder().isWithinBounds(first) || !player.serverLevel().getWorldBorder().isWithinBounds(second))
            return deny(player, "Территория выходит за границу мира.");
        int minX = Math.min(first.getX(), second.getX()) >> 4;
        int minZ = Math.min(first.getZ(), second.getZ()) >> 4;
        int maxX = Math.max(first.getX(), second.getX()) >> 4;
        int maxZ = Math.max(first.getZ(), second.getZ()) >> 4;
        Watched watched = WATCHED.get(player.getUUID());
        long count = ((long) maxX - minX + 1) * ((long) maxZ - minZ + 1);
        if (watched == null || !watched.dimension().equals(player.level().dimension()) || count > watched.chunks().size())
            return deny(player, "Вся территория должна быть в вашей текущей загруженной области. Подойдите ближе или уменьшите выделение.");
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!available(player, new ChunkPos(x, z))) return deny(player, "Часть территории сейчас не загружена для вас. Подойдите ближе или уменьшите выделение.");
            }
        }
        Area bounds = new Area(Math.min(first.getX(), second.getX()), Math.min(first.getZ(), second.getZ()),
                Math.max(first.getX(), second.getX()), Math.max(first.getZ(), second.getZ()));
        if (!reservation.test(player, bounds)) return deny(player, "Эта область зарезервирована для события.");
        return BossClaimZones.get(player.server).validate(player, first, second);
    }

    public static boolean available(ServerPlayer player, ChunkPos position) {
        Watched watched = WATCHED.get(player.getUUID());
        return watched != null && watched.dimension().equals(player.level().dimension()) && watched.chunks().contains(position.toLong())
                && player.serverLevel().getChunkSource().getChunkNow(position.x, position.z) != null;
    }

    static boolean deny(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
        return false;
    }
}
