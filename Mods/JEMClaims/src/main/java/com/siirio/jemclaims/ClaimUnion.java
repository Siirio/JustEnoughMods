package com.siirio.jemclaims;

import io.github.flemmli97.flan.claim.Claim;
import io.github.flemmli97.flan.claim.ClaimStorage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class ClaimUnion {
    public record Rectangle(int minX, int minZ, int maxX, int maxZ) {}
    private record Cached(List<Rectangle> rectangles, long area) {}
    private static final java.util.Map<ServerLevel, java.util.Map<UUID, Cached>> CACHE = new java.util.WeakHashMap<>();
    private ClaimUnion() {}

    public static long area(List<Rectangle> rectangles) {
        long[] xs = rectangles.stream().flatMapToLong(rect -> java.util.stream.LongStream.of(rect.minX(), (long) rect.maxX() + 1)).distinct().sorted().toArray();
        long result = 0;
        for (int index = 1; index < xs.length; index++) {
            long left = xs[index - 1], right = xs[index];
            var spans = rectangles.stream().filter(rect -> rect.minX() < right && (long) rect.maxX() + 1 > left)
                    .sorted(Comparator.comparingInt(Rectangle::minZ)).toList();
            long length = 0, end = Long.MIN_VALUE;
            for (Rectangle span : spans) {
                long top = span.minZ(), bottom = (long) span.maxZ() + 1;
                if (top >= end) { length += bottom - top; end = bottom; }
                else if (bottom > end) { length += bottom - end; end = bottom; }
            }
            result += (right - left) * length;
        }
        return result;
    }

    private static List<Rectangle> rectangles(ServerLevel level, UUID owner, Claim excluded) {
        var claims = ClaimStorage.get(level).allClaimsFromPlayer(owner);
        if (claims == null) return new ArrayList<>();
        return new ArrayList<>(claims.stream().filter(claim -> !claim.isRemoved() && !claim.isAdminClaim() && !claim.equals(excluded))
                .map(claim -> { var box = claim.getDimensions(); return new Rectangle(box.minX(), box.minZ(), box.maxX(), box.maxZ()); }).toList());
    }

    public static int used(MinecraftServer server, UUID owner) {
        long total = 0;
        for (ServerLevel level : server.getAllLevels()) {
            var current = rectangles(level, owner, null);
            var values = CACHE.computeIfAbsent(level, ignored -> new java.util.HashMap<>());
            Cached cached = values.get(owner);
            if (cached == null || !cached.rectangles().equals(current)) {
                cached = new Cached(List.copyOf(current), area(current));
                values.put(owner, cached);
            }
            total += cached.area();
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public static long reclaimed(Claim claim) {
        return area(rectangles(claim.getLevel(), claim.getOwner(), null)) - area(rectangles(claim.getLevel(), claim.getOwner(), claim));
    }

    public static int additional(ServerLevel level, UUID owner, BlockPos first, BlockPos second, Claim replaced) {
        long before = area(rectangles(level, owner, null));
        var after = rectangles(level, owner, replaced);
        after.add(new Rectangle(Math.min(first.getX(), second.getX()), Math.min(first.getZ(), second.getZ()),
                Math.max(first.getX(), second.getX()), Math.max(first.getZ(), second.getZ())));
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, area(after) - before));
    }
}
