package com.siirio.jemclaims;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class ClaimsData extends SavedData {
    private final Map<UUID, Territory> territories = new HashMap<>();
    private final Map<UUID, EnumMap<ClaimPermission, Boolean>> ownerDefaults = new HashMap<>();

    public static ClaimsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ClaimsData::load, ClaimsData::new, "jem_claims");
    }

    public Territory territory(UUID id) {
        Territory existing = territories.get(id);
        if (existing != null) return existing;
        Territory created = new Territory();
        territories.put(id, created);
        setDirty();
        return created;
    }

    public void prune(MinecraftServer server) {
        if (territories.keySet().removeIf(id -> !FlanBridge.exists(server, id))) setDirty();
    }

    public boolean trusted(UUID id, UUID player) {
        Territory territory = territories.get(id);
        return territory != null && territory.members.containsKey(player);
    }

    public EnumMap<ClaimPermission, Boolean> ownerDefaults(UUID owner) {
        return ownerDefaults.computeIfAbsent(owner, ignored -> {
            var defaults = new EnumMap<ClaimPermission, Boolean>(ClaimPermission.class);
            for (var permission : ClaimPermission.values()) defaults.put(permission, permission == ClaimPermission.CANSTAY);
            setDirty();
            return defaults;
        });
    }

    private static ClaimsData load(CompoundTag root) {
        ClaimsData data = new ClaimsData();
        CompoundTag ownerDefaults = root.getCompound("_ownerDefaults");
        for (String owner : ownerDefaults.getAllKeys()) {
            var permissions = new EnumMap<ClaimPermission, Boolean>(ClaimPermission.class);
            readAllPermissions(ownerDefaults.getCompound(owner), permissions);
            data.ownerDefaults.put(UUID.fromString(owner), permissions);
        }
        for (String key : root.getAllKeys()) {
            if (key.equals("_ownerDefaults")) continue;
            try {
                CompoundTag tag = root.getCompound(key);
                Territory territory = new Territory();
                territory.showName = tag.getBoolean("showName");
                territory.showOwner = tag.getBoolean("showOwner");
                territory.allowHostiles = !tag.contains("allowHostiles") || tag.getBoolean("allowHostiles");
                readPermissions(tag.getCompound("defaults"), territory.defaults);
                readAllPermissions(tag.getCompound("tourists"), territory.tourists);
                CompoundTag members = tag.getCompound("members");
                for (String memberId : members.getAllKeys()) {
                    CompoundTag member = members.getCompound(memberId);
                    EnumMap<ClaimPermission, Boolean> permissions = new EnumMap<>(ClaimPermission.class);
                    readPermissions(member.getCompound("permissions"), permissions);
                    territory.members.put(UUID.fromString(memberId), new Member(member.getString("name"), permissions));
                }
                CompoundTag overrides = tag.getCompound("overrides");
                for (String memberId : overrides.getAllKeys()) {
                    CompoundTag member = overrides.getCompound(memberId);
                    EnumMap<ClaimPermission, Boolean> permissions = new EnumMap<>(ClaimPermission.class);
                    readPermissions(member.getCompound("permissions"), permissions);
                    territory.overrides.put(UUID.fromString(memberId), new Member(member.getString("name"), permissions));
                }
                data.territories.put(UUID.fromString(key), territory);
            } catch (IllegalArgumentException ignored) {
                throw new IllegalStateException("Invalid JEM claim metadata: " + key, ignored);
            }
        }
        return data;
    }

    private static void readPermissions(CompoundTag tag, Map<ClaimPermission, Boolean> target) {
        for (ClaimPermission permission : ClaimPermission.values()) {
            if (!permission.global && tag.contains(permission.name())) target.put(permission, tag.getBoolean(permission.name()));
        }
    }

    private static void readAllPermissions(CompoundTag tag, Map<ClaimPermission, Boolean> target) {
        for (ClaimPermission permission : ClaimPermission.values()) {
            if (tag.contains(permission.name())) target.put(permission, tag.getBoolean(permission.name()));
        }
    }

    private static CompoundTag permissions(Map<ClaimPermission, Boolean> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach((permission, enabled) -> tag.putBoolean(permission.name(), enabled));
        return tag;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        territories.forEach((id, territory) -> {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("showName", territory.showName);
            tag.putBoolean("showOwner", territory.showOwner);
            tag.putBoolean("allowHostiles", territory.allowHostiles);
            tag.put("defaults", permissions(territory.defaults));
            tag.put("tourists", permissions(territory.tourists));
            CompoundTag members = new CompoundTag();
            territory.members.forEach((memberId, member) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("name", member.name());
                entry.put("permissions", permissions(member.permissions()));
                members.put(memberId.toString(), entry);
            });
            tag.put("members", members);
            CompoundTag overrides = new CompoundTag();
            territory.overrides.forEach((memberId, member) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("name", member.name());
                entry.put("permissions", permissions(member.permissions()));
                overrides.put(memberId.toString(), entry);
            });
            tag.put("overrides", overrides);
            root.put(id.toString(), tag);
        });
        CompoundTag defaults = new CompoundTag();
        ownerDefaults.forEach((owner, permissions) -> defaults.put(owner.toString(), permissions(permissions)));
        root.put("_ownerDefaults", defaults);
        return root;
    }

    public static final class Territory {
        public boolean allowHostiles = true;
        public boolean showName = ClaimsConfig.SHOW_NAME.get();
        public boolean showOwner = ClaimsConfig.SHOW_OWNER.get();
        public final EnumMap<ClaimPermission, Boolean> defaults = new EnumMap<>(ClaimPermission.class);
        public final EnumMap<ClaimPermission, Boolean> tourists = new EnumMap<>(ClaimPermission.class);
        public final Map<UUID, Member> members = new HashMap<>();
        public final Map<UUID, Member> overrides = new HashMap<>();

        public Territory() {
            for (ClaimPermission permission : ClaimPermission.values()) {
                if (!permission.global) defaults.put(permission, permission.memberDefault);
            }
        }
    }

    public record Member(String name, EnumMap<ClaimPermission, Boolean> permissions) {}
}
