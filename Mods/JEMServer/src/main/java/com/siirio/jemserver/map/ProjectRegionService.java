package com.siirio.jemserver.map;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemserver.ClaimBoundaryGeometry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class ProjectRegionService {
    public enum Result { ACCEPTED, UNAUTHENTICATED, LIMIT, INVALID, DENIED, RATE_LIMITED }
    private static final Map<UUID, Long> LAST_MUTATION = new HashMap<>();
    private ProjectRegionService() { }

    public static Result create(ServerPlayer player, String rawName) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return Result.UNAUTHENTICATED;
        if (!allowMutation(player, principal.get().accountId())) return Result.RATE_LIMITED;
        String name = rawName == null ? "" : rawName.strip().replaceAll("\\s+", " ");
        if (name.length() < 3 || name.length() > 48) return Result.INVALID;
        ProjectRegionData data = ProjectRegionData.get(player.server);
        if (data.all().stream().filter(project -> project.owner().equals(principal.get().accountId())).count() >= 20) return Result.LIMIT;
        UUID id = UUID.randomUUID();
        data.put(new ProjectRegion(id, principal.get().accountId(), player.serverLevel().dimension().location(), name, RegionPalette.forProject(id)));
        return Result.ACCEPTED;
    }

    public static Result createWithRectangle(ServerPlayer player, String rawName, int firstX, int firstZ, int secondX, int secondZ) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return Result.UNAUTHENTICATED;
        if (!allowMutation(player, principal.get().accountId())) return Result.RATE_LIMITED;
        String name = rawName == null ? "" : rawName.strip().replaceAll("\\s+", " ");
        if (name.length() < 3 || name.length() > 48) return Result.INVALID;
        ProjectRegionData data = ProjectRegionData.get(player.server);
        if (data.all().stream().filter(project -> project.owner().equals(principal.get().accountId())).count() >= 20) return Result.LIMIT;
        int minX = Math.min(firstX, secondX), minZ = Math.min(firstZ, secondZ);
        int maxX = Math.max(firstX, secondX) + 1, maxZ = Math.max(firstZ, secondZ) + 1;
        if (maxX <= minX || maxZ <= minZ || (long) (maxX - minX) * (maxZ - minZ) > 1_000_000L) return Result.INVALID;
        UUID id = UUID.randomUUID();
        ProjectRegion project = new ProjectRegion(id, principal.get().accountId(), player.serverLevel().dimension().location(), name, RegionPalette.forProject(id));
        project.add(new ClaimBoundaryGeometry.Rectangle(minX, minZ, maxX, maxZ)); data.put(project);
        return Result.ACCEPTED;
    }

    public static Result addRectangle(ServerPlayer player, UUID projectId, int firstX, int firstZ, int secondX, int secondZ) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return Result.UNAUTHENTICATED;
        if (!allowMutation(player, principal.get().accountId())) return Result.RATE_LIMITED;
        ProjectRegionData data = ProjectRegionData.get(player.server);
        ProjectRegion project = data.find(projectId).orElse(null);
        if (project == null || !RegionPermissions.mayEditMetadata(project, principal.get().accountId())) return Result.DENIED;
        if (!project.dimension().equals(player.serverLevel().dimension().location()) || project.rectangles().size() >= 64) return Result.LIMIT;
        int minX = Math.min(firstX, secondX), minZ = Math.min(firstZ, secondZ);
        int maxX = Math.max(firstX, secondX) + 1, maxZ = Math.max(firstZ, secondZ) + 1;
        if (maxX <= minX || maxZ <= minZ) return Result.INVALID;
        var rectangle = new ClaimBoundaryGeometry.Rectangle(minX, minZ, maxX, maxZ);
        var proposed = new ArrayList<>(project.rectangles()); proposed.add(rectangle);
        if (RegionUnion.area(proposed) > 1_000_000L) return Result.LIMIT;
        project.add(rectangle); data.changed();
        return Result.ACCEPTED;
    }

    public static Result removeRectangle(ServerPlayer player, UUID projectId, int index) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return Result.UNAUTHENTICATED;
        if (!allowMutation(player, principal.get().accountId())) return Result.RATE_LIMITED;
        ProjectRegionData data = ProjectRegionData.get(player.server);
        ProjectRegion project = data.find(projectId).orElse(null);
        if (project == null || !RegionPermissions.mayEditMetadata(project, principal.get().accountId())) return Result.DENIED;
        if (!project.remove(index)) return Result.INVALID;
        data.changed();
        return Result.ACCEPTED;
    }

    private static boolean allowMutation(ServerPlayer player, UUID accountId) {
        long now = player.server.getTickCount();
        long prior = LAST_MUTATION.getOrDefault(accountId, Long.MIN_VALUE / 2);
        if (now - prior < 10L) return false;
        LAST_MUTATION.put(accountId, now);
        return true;
    }
}
