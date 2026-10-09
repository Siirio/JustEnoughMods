package com.siirio.jemclaims;

import com.mojang.authlib.GameProfile;
import io.github.flemmli97.flan.claim.Claim;
import io.github.flemmli97.flan.claim.ClaimStorage;
import io.github.flemmli97.flan.player.ClaimMode;
import io.github.flemmli97.flan.player.PlayerClaimData;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class FlanBridge {
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> PUBLIC_SERVICES = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK, new ResourceLocation("jem_claims", "public_services"));
    private static final ThreadLocal<Boolean> OVERLAP_CHECK = ThreadLocal.withInitial(() -> false);
    private FlanBridge() {}

    public static boolean overlapPermission(Claim selected, ServerPlayer player, ResourceLocation permission, BlockPos target) {
        if (target == null || OVERLAP_CHECK.get() || !selected.insideClaim(target)) return true;
        Claim selectedRoot = selected.isSubclaim() ? selected.parentClaim() : selected;
        OVERLAP_CHECK.set(true);
        try {
            for (Claim claim : ClaimStorage.get(selected.getLevel()).getClaimsAt(target.getX() >> 4, target.getZ() >> 4)) {
                if (claim.equals(selectedRoot) || claim.isRemoved() || !claim.insideClaim(target)) continue;
                Claim subdivision = claim.getSubClaim(target);
                if (!(subdivision == null ? claim : subdivision).canInteract(player, permission, target, false)) return false;
            }
            return true;
        } finally {
            OVERLAP_CHECK.remove();
        }
    }

    public static boolean publicService(ServerLevel level, BlockPos position) { return level.getBlockState(position).is(PUBLIC_SERVICES); }

    public static void registerEvents() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(FlanBridge::permissionOverride);
    }

    private static void permissionOverride(io.github.flemmli97.flan.api.forge.PermissionCheckEvent event) {
        if (event.player != null && java.util.Arrays.stream(ClaimPermission.values())
                .anyMatch(permission -> permission.alwaysAllowed() && event.permission.equals(permissionId(permission)))) {
            event.setResult(net.minecraft.world.InteractionResult.SUCCESS);
            return;
        }
        if (event.player != null && (event.permission.equals(permissionId(ClaimPermission.BREAK))
                || event.permission.equals(permissionId(ClaimPermission.OPENCONTAINER))
                || event.permission.equals(permissionId(ClaimPermission.INTERACTBLOCK)))
                && com.siirio.jemclaims.compat.GraveCompat.isOwner(event.player, event.player.serverLevel(), event.pos)) {
            event.setResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    public record Territory(UUID id, UUID owner, String name, String ownerName, int area, int minX, int minZ, int maxX, int maxZ, ResourceLocation dimension, int color) {}

    public static List<Territory> list(MinecraftServer server) {
        List<Territory> result = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            ClaimStorage.get(level).getClaims().values().forEach(claims -> claims.stream()
                    .filter(claim -> !claim.isRemoved()).map(FlanBridge::view).forEach(result::add));
        }
        return List.copyOf(result);
    }

    public static List<Territory> territory(MinecraftServer server, UUID id) {
        Territory selected = get(server, id);
        if (selected == null) return List.of();
        return ClaimGroups.connected(list(server).stream().filter(value -> java.util.Objects.equals(value.owner(), selected.owner())
                && value.dimension().equals(selected.dimension())).toList()).stream()
                .filter(group -> group.stream().anyMatch(value -> value.id().equals(id))).findFirst().orElse(List.of());
    }

    public static Territory summary(MinecraftServer server, UUID id) {
        var group = territory(server, id);
        if (group.isEmpty()) return null;
        Territory anchor = get(server, id);
        long area = ClaimUnion.area(group.stream().map(value -> new ClaimUnion.Rectangle(value.minX(), value.minZ(), value.maxX(), value.maxZ())).toList());
        return new Territory(id, anchor.owner(), anchor.name(), anchor.ownerName(), (int) Math.min(Integer.MAX_VALUE, area),
                group.stream().mapToInt(Territory::minX).min().orElseThrow(), group.stream().mapToInt(Territory::minZ).min().orElseThrow(),
                group.stream().mapToInt(Territory::maxX).max().orElseThrow(), group.stream().mapToInt(Territory::maxZ).max().orElseThrow(),
                anchor.dimension(), anchor.color());
    }

    public static long reclaimed(ServerPlayer player, UUID id) {
        return ClaimUnion.area(territory(player.server, id).stream()
                .map(value -> new ClaimUnion.Rectangle(value.minX(), value.minZ(), value.maxX(), value.maxZ())).toList());
    }

    public static boolean delete(ServerPlayer player, UUID id) {
        var group = territory(player.server, id);
        if (group.isEmpty() || group.stream().anyMatch(value -> !player.getUUID().equals(value.owner()))) return false;
        for (Territory value : group) {
            Claim claim = claim(player.server, value.id());
            if (!ClaimStorage.get(claim.getLevel()).deleteClaim(claim, true, ClaimMode.DEFAULT, claim.getLevel())) return false;
        }
        ClaimsData.get(player.server).prune(player.server);
        return true;
    }

    public static void unify(ServerPlayer player, UUID id) {
        if (!canManage(player, id)) return;
        var group = territory(player.server, id);
        if (group.size() < 2) return;
        ClaimsData data = ClaimsData.get(player.server);
        var merged = new java.util.HashMap<UUID, ClaimsData.Member>();
        var overrides = new java.util.HashMap<UUID, ClaimsData.Member>();
        for (Territory value : group) merged.putAll(data.territory(value.id()).members);
        for (Territory value : group) overrides.putAll(data.territory(value.id()).overrides);
        merged.replaceAll((member, settings) -> {
            var permissions = new java.util.EnumMap<ClaimPermission, Boolean>(ClaimPermission.class);
            for (ClaimPermission permission : ClaimPermission.values()) {
                if (permission.global) continue;
                permissions.put(permission, group.stream().allMatch(value -> {
                    var existing = data.territory(value.id()).members.get(member);
                    return existing != null && existing.permissions().getOrDefault(permission, false);
                }));
            }
            return new ClaimsData.Member(settings.name(), permissions);
        });
        var defaults = new java.util.EnumMap<ClaimPermission, Boolean>(ClaimPermission.class);
        for (ClaimPermission permission : ClaimPermission.values()) {
            if (!permission.global) defaults.put(permission, group.stream().allMatch(value -> data.territory(value.id()).defaults.getOrDefault(permission, false)));
        }
        for (ClaimSetting setting : ClaimSetting.values())
            set(player, id, setting, setting.permissions.stream().allMatch(permission -> value(player, id, permission)));
        for (var member : merged.entrySet()) syncMember(player, id, member.getKey(), member.getValue().permissions());
        var metadata = data.territory(id);
        metadata.members.clear();
        metadata.members.putAll(merged);
        metadata.overrides.clear();
        metadata.overrides.putAll(overrides);
        metadata.defaults.clear();
        metadata.defaults.putAll(defaults);
        syncMetadata(player.server, id);
        rename(player, id, claim(player.server, id).getClaimName().replace("%%", "%"));
    }

    public static void syncMetadata(MinecraftServer server, UUID id) {
        ClaimsData data = ClaimsData.get(server);
        ClaimsData.Territory source = data.territory(id);
        for (Territory value : territory(server, id)) {
            if (value.id().equals(id)) continue;
            ClaimsData.Territory target = data.territory(value.id());
            target.showName = source.showName;
            target.showOwner = source.showOwner;
            target.defaults.clear();
            target.defaults.putAll(source.defaults);
            target.tourists.clear();
            target.tourists.putAll(source.tourists);
            target.members.clear();
            source.members.forEach((member, settings) -> target.members.put(member,
                    new ClaimsData.Member(settings.name(), new java.util.EnumMap<>(settings.permissions()))));
            target.overrides.clear();
            source.overrides.forEach((member, settings) -> target.overrides.put(member,
                    new ClaimsData.Member(settings.name(), new java.util.EnumMap<>(settings.permissions()))));
        }
        data.setDirty();
    }

    public static List<Territory> nearby(ServerPlayer player) {
        int radius = ClaimsConfig.BORDER_RADIUS.get();
        return ClaimStorage.get(player.serverLevel()).getNearbyClaims(player.serverLevel(), player.blockPosition(), radius, radius)
                .stream().filter(claim -> !claim.isRemoved()).map(FlanBridge::view).toList();
    }

    public static boolean trusted(ServerPlayer player, UUID id) {
        return owner(player, id) || ClaimsData.get(player.server).trusted(id, player.getUUID());
    }

    public static ResourceLocation permissionId(ClaimPermission permission) {
        return new ResourceLocation("flan", permission.path);
    }

    private static Claim claim(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Claim claim = ClaimStorage.get(level).getFromUUID(id);
            if (claim != null && !claim.isRemoved()) return claim;
        }
        return null;
    }

    public static boolean exists(MinecraftServer server, UUID id) { return claim(server, id) != null; }

    private static Territory view(Claim claim) {
        if (claim == null) return null;
        var box = claim.getDimensions();
        String ownerName = claim.getOwner() == null ? "Администратор" : claim.getLevel().getServer().getProfileCache().get(claim.getOwner()).map(GameProfile::getName).orElse(claim.getOwner().toString());
        return new Territory(claim.getClaimID(), claim.getOwner(), claim.getClaimName(), ownerName, claim.getPlane(), box.minX(), box.minZ(), box.maxX(), box.maxZ(), claim.getLevel().dimension().location(), (ClaimsConfig.MAP_COLOR.get() ^ claim.getClaimID().hashCode()) & 0xFFFFFF);
    }

    public static Territory at(ServerLevel level, BlockPos pos) { return view(ClaimStorage.get(level).getClaimAt(pos)); }
    public static boolean claimed(ServerLevel level, BlockPos pos) { return ClaimStorage.get(level).getClaimAt(pos) != null; }

    public static boolean validateResize(Object nativeClaim, BlockPos previous, BlockPos target, ServerPlayer player) {
        Claim claim = (Claim) nativeClaim;
        var box = claim.getDimensions();
        BlockPos opposite = new BlockPos(previous.getX() == box.minX() ? box.maxX() : box.minX(), previous.getY(), previous.getZ() == box.minZ() ? box.maxZ() : box.minZ());
        return claim.getLevel() == player.serverLevel() && ClaimSelectionGuard.validate(player, opposite, target);
    }
    public static Territory get(MinecraftServer server, UUID id) { return view(claim(server, id)); }
    public static boolean owner(ServerPlayer player, UUID id) {
        Claim claim = claim(player.server, id);
        return claim != null && player.getUUID().equals(claim.getOwner());
    }

    public static boolean canManage(ServerPlayer player, UUID id) {
        Claim claim = claim(player.server, id);
        return claim != null && territory(player.server, id).stream().allMatch(value -> owner(player, value.id()) || claim(player.server, value.id()).canInteract(player,
                io.github.flemmli97.flan.api.permission.BuiltinPermission.EDITPERMS, player.blockPosition(), false));
    }

    public static boolean can(ServerPlayer player, ServerLevel level, BlockPos position, ClaimPermission permission) {
        if (permission.alwaysAllowed()) return true;
        return ClaimStorage.get(level).getForPermissionCheck(position).canInteract(player, permissionId(permission), position, false);
    }

    public static boolean canAutomate(ServerLevel level, BlockPos source, BlockPos target, ClaimPermission permission) {
        return canAutomate(level, source, target, permissionId(permission));
    }

    private static boolean canAutomate(ServerLevel level, BlockPos source, BlockPos target, ResourceLocation permission) {
        Claim origin = ClaimStorage.get(level).getClaimAt(source);
        Claim destination = ClaimStorage.get(level).getClaimAt(target);
        if (destination == null || origin != null && origin.getClaimID().equals(destination.getClaimID())) return true;
        if (!destination.canInteract(null, permissionId(ClaimPermission.CREATE), target, false)) return false;
        if (origin == null) return destination.canInteract(null, permission, target, false);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(origin.getOwner());
        return owner != null && ClaimStorage.get(level).getForPermissionCheck(target).canInteract(owner, permission, target, false);
    }

    public static net.minecraft.world.InteractionResult machinePermission(ServerPlayer player, ResourceLocation permission, BlockPos target) {
        BlockPos source = com.siirio.jemclaims.compat.MachineContext.source();
        if (!(player instanceof net.minecraftforge.common.util.FakePlayer)) return net.minecraft.world.InteractionResult.PASS;
        if (source == null) return claimed(player.serverLevel(), target) ? net.minecraft.world.InteractionResult.FAIL : net.minecraft.world.InteractionResult.PASS;
        return canAutomate(player.serverLevel(), source, target, permission)
                ? net.minecraft.world.InteractionResult.SUCCESS : net.minecraft.world.InteractionResult.FAIL;
    }

    public static boolean environmental(ServerLevel level, BlockPos target, ClaimPermission permission) {
        return ClaimStorage.get(level).getForPermissionCheck(target).canInteract(null, permissionId(permission), target, false);
    }

    public static int used(ServerPlayer player) { return PlayerClaimData.get(player).usedClaimBlocks(); }
    public static int budget(ServerPlayer player) {
        PlayerClaimData data = PlayerClaimData.get(player);
        return data.getClaimBlocks() + data.getAdditionalClaims();
    }

    public static Territory create(ServerPlayer player, BlockPos first, BlockPos second) {
        if (!ClaimSelectionGuard.validate(player, first, second)) return null;
        var owned = list(player.server).stream().filter(value -> player.getUUID().equals(value.owner())).toList();
        int minX = Math.min(first.getX(), second.getX()), minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX()), maxZ = Math.max(first.getZ(), second.getZ());
        boolean joinsExisting = owned.stream().anyMatch(value -> value.dimension().equals(player.level().dimension().location())
                && value.minX() <= (long) maxX + 1 && minX <= (long) value.maxX() + 1
                && value.minZ() <= (long) maxZ + 1 && minZ <= (long) value.maxZ() + 1);
        if (!joinsExisting && ClaimGroups.connected(owned).size() >= ClaimsConfig.MAX_TERRITORIES.get()) {
            player.sendSystemMessage(Component.literal("Достигнут лимит территорий."));
            return null;
        }
        PlayerClaimData data = PlayerClaimData.get(player);
        long area = (Math.abs((long) first.getX() - second.getX()) + 1) * (Math.abs((long) first.getZ() - second.getZ()) + 1);
        int additional = ClaimUnion.additional(player.serverLevel(), player.getUUID(), first, second, null);
        if (area > Integer.MAX_VALUE || !data.canUseClaimBlocks(additional)) {
            player.sendSystemMessage(Component.literal("Недостаточно блоков привата: требуется " + additional + ", доступно " + data.remainingClaimBlocks() + "."));
            return null;
        }
        ClaimMode previous = data.getClaimMode();
        data.setEditMode(ClaimMode.DEFAULT);
        try {
            ClaimStorage storage = ClaimStorage.get(player.serverLevel());
            var existing = storage.allClaimsFromPlayer(player.getUUID());
            var previousIds = existing == null ? java.util.Set.<UUID>of() : existing.stream().map(Claim::getClaimID).collect(java.util.stream.Collectors.toSet());
            if (!storage.createClaim(first.atY(player.level().getMinBuildHeight()), second.atY(player.level().getMinBuildHeight()), player)) return null;
            Claim claim = storage.allClaimsFromPlayer(player.getUUID()).stream().filter(value -> !previousIds.contains(value.getClaimID())).findFirst().orElseThrow();
            var ownerDefaults = ClaimsData.get(player.server).ownerDefaults(player.getUUID());
            for (ClaimPermission permission : ClaimPermission.values()) {
                boolean enabled = permission.alwaysAllowed() || ownerDefaults.getOrDefault(permission, false);
                claim.editGlobalPerms(player, permissionId(permission), enabled ? 1 : 0);
            }
            claim.editGlobalPerms(player, new ResourceLocation("flan", "flight"), 1);
            claim.editGlobalPerms(player, new ResourceLocation("flan", "may_flight"), 0);
            claim.setClaimName("Территория " + player.getGameProfile().getName());
            ClaimsData.get(player.server).territory(claim.getClaimID());
            var original = territory(player.server, claim.getClaimID()).stream().filter(value -> previousIds.contains(value.id())).findFirst();
            if (original.isPresent()) {
                UUID anchor = original.get().id();
                Claim source = claim(player.server, anchor);
                claim.setClaimName(source.getClaimName());
                for (ClaimPermission permission : ClaimPermission.values())
                    claim.editGlobalPerms(player, permissionId(permission), source.permEnabled(permissionId(permission)));
                ClaimsData.Territory sourceMetadata = ClaimsData.get(player.server).territory(anchor);
                ClaimsData.Territory newMetadata = ClaimsData.get(player.server).territory(claim.getClaimID());
                newMetadata.defaults.clear();
                newMetadata.defaults.putAll(sourceMetadata.defaults);
                newMetadata.tourists.clear();
                newMetadata.tourists.putAll(sourceMetadata.tourists);
                sourceMetadata.members.forEach((member, settings) -> newMetadata.members.put(member,
                        new ClaimsData.Member(settings.name(), new java.util.EnumMap<>(settings.permissions()))));
                sourceMetadata.overrides.forEach((member, settings) -> newMetadata.overrides.put(member,
                        new ClaimsData.Member(settings.name(), new java.util.EnumMap<>(settings.permissions()))));
                unify(player, anchor);
                return view(source);
            }
            updateTitle(player.server, claim.getClaimID());
            return view(claim);
        } finally {
            data.setEditMode(previous);
        }
    }

    public static boolean value(ServerPlayer player, UUID id, ClaimPermission permission) {
        Claim claim = claim(player.server, id);
        return claim != null && territory(player.server, id).stream().allMatch(value -> claim(player.server, value.id()).permEnabled(permissionId(permission)) == 1);
    }

    public static boolean set(ServerPlayer player, UUID id, ClaimSetting setting, boolean enabled) {
        if (!canManage(player, id)) return false;
        for (Territory value : territory(player.server, id)) {
            Claim claim = claim(player.server, value.id());
            for (ClaimPermission permission : setting.permissions) {
                if (!claim.editGlobalPerms(player, permissionId(permission), enabled ? 1 : 0)) return false;
            }
        }
        return true;
    }

    public static boolean set(ServerPlayer player, UUID id, ClaimPermission permission, boolean enabled) {
        if (!canManage(player, id)) return false;
        for (Territory value : territory(player.server, id)) {
            if (!claim(player.server, value.id()).editGlobalPerms(player, permissionId(permission), enabled ? 1 : 0)) return false;
        }
        return true;
    }

    public static boolean syncMember(ServerPlayer player, UUID id, UUID member, Map<ClaimPermission, Boolean> permissions) {
        Claim claim = claim(player.server, id);
        if (!canManage(player, id) || member.equals(claim.getOwner())) return false;
        String group = "jem_member_" + member.toString().replace("-", "");
        for (Territory value : territory(player.server, id)) {
            Claim target = claim(player.server, value.id());
            for (var entry : permissions.entrySet()) {
                if (!target.editPerms(player, group, permissionId(entry.getKey()), entry.getValue() ? 1 : 0)) return false;
            }
            if (!target.setPlayerGroup(member, group, true)) return false;
        }
        return true;
    }

    public static boolean removeMember(ServerPlayer player, UUID id, UUID member) {
        if (!canManage(player, id)) return false;
        for (Territory value : territory(player.server, id)) {
            claim(player.server, value.id()).removePermGroup(player, "jem_member_" + member.toString().replace("-", ""));
            ClaimsData.get(player.server).territory(value.id()).members.remove(member);
        }
        ClaimsData.get(player.server).setDirty();
        return true;
    }

    public static boolean removeOverride(ServerPlayer player, UUID id, UUID member) {
        if (!canManage(player, id)) return false;
        for (Territory value : territory(player.server, id)) {
            claim(player.server, value.id()).removePermGroup(player, "jem_member_" + member.toString().replace("-", ""));
            ClaimsData.get(player.server).territory(value.id()).overrides.remove(member);
        }
        ClaimsData.get(player.server).setDirty();
        return true;
    }

    public static boolean rename(ServerPlayer player, UUID id, String name) {
        if (!canManage(player, id)) return false;
        for (Territory value : territory(player.server, id)) claim(player.server, value.id()).setClaimName(name.replace("%", "%%"));
        updateTitle(player.server, id);
        return true;
    }

    public static void updateTitle(MinecraftServer server, UUID id) {
        syncMetadata(server, id);
        ClaimsData.Territory metadata = ClaimsData.get(server).territory(id);
        for (Territory value : territory(server, id)) {
            Claim claim = claim(server, value.id());
            claim.setEnterTitle(metadata.showName ? Component.literal("%2$s") : null,
                    metadata.showName ? Component.literal(metadata.showOwner ? "Территория %1$s" : "") : null);
            claim.setLeaveTitle(null, null);
        }
    }
}
