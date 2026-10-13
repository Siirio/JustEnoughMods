package com.siirio.jemclaims;

import com.siirio.jemmenus.VanillaMenus;
import com.siirio.jemmenus.Buttons;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

public final class ClaimsMenus {
    private ClaimsMenus() {}

    private static final int PAGE_SIZE = 36;
    private static final int MEMBER_PAGE_SIZE = 20;

    public static void overview(ServerPlayer player, int requestedPage) {
        var groups = ClaimGroups.connected(FlanBridge.list(player.server).stream().filter(claim -> player.getUUID().equals(claim.owner())).toList());
        int page = Math.max(0, Math.min(requestedPage, Math.max(0, (groups.size() - 1) / PAGE_SIZE)));
        VanillaMenus.chest(player, JemClaims.MOD_ID, "claims_overview", text("jem.claims.my_territories"), menu -> {
            int remaining = Math.max(0, ClaimsConfig.MAX_TERRITORIES.get() - groups.size());
            menu.button(4, VanillaMenus.icon(Items.COMPASS, text(remaining == 0 ? "jem.claims.limit_reached" : "jem.claims.remaining", remaining), text("jem.claims.blocks", FlanBridge.used(player), FlanBridge.budget(player))), null);
            menu.button(5, VanillaMenus.icon(Items.COMPARATOR, text("jem.claims.all_territories"), text("jem.claims.shared_permissions")), () -> allTerritories(player));
            for (int index = page * PAGE_SIZE; index < Math.min(groups.size(), (page + 1) * PAGE_SIZE); index++) {
                var group = groups.get(index);
                var first = group.get(0);
                var territoryIcon = VanillaMenus.icon(Items.GRASS_BLOCK, Component.literal(first.name()), text("jem.claims.area", ClaimUnion.area(group.stream().map(value -> new ClaimUnion.Rectangle(value.minX(), value.minZ(), value.maxX(), value.maxZ())).toList())), Component.literal(first.dimension().toString()));
                location(territoryIcon, group, player.blockPosition().getY());
                territoryIcon.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 1);
                territoryIcon.hideTooltipPart(net.minecraft.world.item.ItemStack.TooltipPart.ENCHANTMENTS);
                menu.button(9 + index % PAGE_SIZE, territoryIcon, () -> main(player, first.id()));
            }
            menu.button(49, VanillaMenus.icon(Items.ARROW, text("jem.claims.back")), () -> {
                player.closeContainer();
                player.server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), "smp");
            });
            if (page > 0) menu.button(45, VanillaMenus.icon(Items.ARROW, text("jem.claims.previous")), () -> overview(player, page - 1));
            if ((page + 1) * PAGE_SIZE < groups.size()) menu.button(53, VanillaMenus.icon(Items.ARROW, text("jem.claims.next")), () -> overview(player, page + 1));
        });
    }

    public static void confirmDelete(ServerPlayer player, UUID id) {
        if (!FlanBridge.owner(player, id)) {
            player.sendSystemMessage(text("jem.claims.error.owner_delete"));
            return;
        }
        var claim = FlanBridge.summary(player.server, id);
        VanillaMenus.chest(player, JemClaims.MOD_ID, "confirm", text("jem.claims.delete_question"), menu -> {
            menu.button(4, location(VanillaMenus.icon(Items.GRASS_BLOCK, Component.literal(claim.name()), text("jem.claims.blocks_reclaimed", FlanBridge.reclaimed(player, id)), text("jem.claims.coordinates", claim.minX(), claim.minZ(), claim.maxX(), claim.maxZ())), java.util.List.of(claim), player.blockPosition().getY()), null);
            menu.button(22, VanillaMenus.icon(Items.BARRIER, text("jem.claims.delete"), text("jem.claims.delete_keeps_blocks")), () -> {
                if (FlanBridge.delete(player, id)) player.sendSystemMessage(text("jem.claims.deleted"));
                overview(player, 0);
            });
            menu.button(49, VanillaMenus.icon(Items.ARROW, text("jem.claims.cancel")), () -> main(player, id));
        });
    }

    private static void navigation(Buttons menu, ServerPlayer player, UUID id, String active) {
        navigationButton(menu, 45, Items.GRASS_BLOCK, "territory", active.equals("claim"), () -> main(player, id));
        navigationButton(menu, 46, Items.SHIELD, "permissions", active.equals("permissions"), () -> permissions(player, id, null, false));
        navigationButton(menu, 47, Items.PLAYER_HEAD, "members", active.equals("members"), () -> members(player, id, 0));
        navigationButton(menu, 48, Items.COMPARATOR, "settings", active.equals("settings"), () -> territory(player, id));
    }

    private static void navigationButton(Buttons menu, int slot, net.minecraft.world.item.Item item, String title, boolean active, Runnable action) {
        var icon = VanillaMenus.icon(item, text("jem.claims." + title));
        icon.getOrCreateTag().putString("jem_ui_role", "navigation");
        menu.button(slot, icon, active ? null : action);
    }

    private static boolean authorized(ServerPlayer player, UUID claim) {
        if (FlanBridge.canManage(player, claim)) return true;
        player.closeContainer();
        player.sendSystemMessage(text("jem.claims.error.manage_denied"));
        return false;
    }

    public static void main(ServerPlayer player, UUID id) {
        if (!authorized(player, id)) return;
        FlanBridge.unify(player, id);
        var claim = FlanBridge.summary(player.server, id);
        VanillaMenus.chest(player, JemClaims.MOD_ID, "claim", Component.literal(claim.name()), menu -> {
            var summary = VanillaMenus.icon(Items.GRASS_BLOCK, Component.literal(claim.name()), text("jem.claims.owner", claim.ownerName()),
                    text("jem.claims.area", claim.area()), text("jem.claims.blocks", FlanBridge.used(player), FlanBridge.budget(player)),
                    text("jem.claims.coordinates", claim.minX(), claim.minZ(), claim.maxX(), claim.maxZ()));
            location(summary, java.util.List.of(claim), player.blockPosition().getY());
            menu.button(4, summary, null);
            navigation(menu, player, id, "claim");
            menu.button(50, VanillaMenus.icon(Items.PLAYER_HEAD, text("jem.claims.players"), text("jem.claims.individual_permissions")), () -> players(player, id, 0));
            if (FlanBridge.owner(player, id)) menu.button(52, VanillaMenus.icon(Items.BARRIER, text("jem.claims.delete")), () -> confirmDelete(player, id));
            menu.button(49, VanillaMenus.icon(Items.ARROW, text("jem.claims.my_territories")), () -> overview(player, 0));
        });
    }

    private static net.minecraft.world.item.ItemStack location(net.minecraft.world.item.ItemStack icon, java.util.List<FlanBridge.Territory> territory, int y) {
        int minX = territory.stream().mapToInt(FlanBridge.Territory::minX).min().orElseThrow();
        int minZ = territory.stream().mapToInt(FlanBridge.Territory::minZ).min().orElseThrow();
        int maxX = territory.stream().mapToInt(FlanBridge.Territory::maxX).max().orElseThrow();
        int maxZ = territory.stream().mapToInt(FlanBridge.Territory::maxZ).max().orElseThrow();
        return location(icon, territory.get(0).dimension(), new net.minecraft.core.BlockPos(minX, y, minZ), new net.minecraft.core.BlockPos(maxX, y, maxZ));
    }

    public static net.minecraft.world.item.ItemStack location(net.minecraft.world.item.ItemStack icon, net.minecraft.resources.ResourceLocation dimension,
                                                             net.minecraft.core.BlockPos first, net.minecraft.core.BlockPos second) {
        int minX = Math.min(first.getX(), second.getX()), minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX()), maxZ = Math.max(first.getZ(), second.getZ());
        int centerX = (int) (minX + ((long) maxX - minX) / 2), centerZ = (int) (minZ + ((long) maxZ - minZ) / 2);
        long radius = Math.max((long) maxX - centerX, (long) maxZ - centerZ) + 1;
        icon.getOrCreateTag().putString("jem_ui_dimension", dimension.toString());
        icon.getOrCreateTag().putLong("jem_ui_position", new net.minecraft.core.BlockPos(centerX, first.getY(), centerZ).asLong());
        icon.getOrCreateTag().putInt("jem_ui_radius", (int) Math.min(Integer.MAX_VALUE, radius));
        return icon;
    }

    private static void territory(ServerPlayer player, UUID id) {
        if (!authorized(player, id)) return;
        var metadata = ClaimsData.get(player.server).territory(id);
        VanillaMenus.chest(player, JemClaims.MOD_ID, "claim_settings", text("jem.claims.territory_settings"), menu -> {
            menu.button(20, VanillaMenus.icon(Items.NAME_TAG, text("jem.claims.rename")), () -> {
                if (!authorized(player, id)) return;
                VanillaMenus.input(player, text("jem.claims.territory_name"), FlanBridge.get(player.server, id).name(), name -> {
                if (!authorized(player, id)) return;
                String clean = name.replaceAll("[\\p{Cntrl}§]", "").strip();
                if (clean.isEmpty() || clean.length() > ClaimsConfig.NAME_LENGTH.get()) {
                    player.sendSystemMessage(text("jem.claims.error.name_length", ClaimsConfig.NAME_LENGTH.get()));
                    return;
                }
                FlanBridge.rename(player, id, clean);
                territory(player, id);
                });
            });
            menu.button(22, toggleIcon(Items.NAME_TAG, "show_name", metadata.showName), () -> {
                if (!authorized(player, id)) return;
                metadata.showName = !metadata.showName;
                ClaimsData.get(player.server).setDirty();
                FlanBridge.updateTitle(player.server, id);
                territory(player, id);
            });
            menu.button(24, toggleIcon(Items.PLAYER_HEAD, "show_owner", metadata.showOwner), () -> {
                if (!authorized(player, id)) return;
                metadata.showOwner = !metadata.showOwner;
                ClaimsData.get(player.server).setDirty();
                FlanBridge.updateTitle(player.server, id);
                territory(player, id);
            });
            navigation(menu, player, id, "settings");
            menu.button(49, VanillaMenus.icon(Items.ARROW, text("jem.claims.back")), () -> main(player, id));
        });
    }

    private static net.minecraft.world.item.ItemStack toggleIcon(net.minecraft.world.item.Item item, String label, boolean enabled) {
        var icon = VanillaMenus.icon(item, text("jem.claims.setting." + label), text(enabled ? "jem.claims.allowed" : "jem.claims.denied"));
        if (enabled) {
            icon.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 1);
            icon.hideTooltipPart(net.minecraft.world.item.ItemStack.TooltipPart.ENCHANTMENTS);
        }
        return icon;
    }

    private static void permissions(ServerPlayer player, UUID id, UUID member, boolean defaults) {
        if (!authorized(player, id)) return;
        var data = ClaimsData.get(player.server);
        var metadata = data.territory(id);
        if (member != null && !metadata.members.containsKey(member)) { members(player, id, 0); return; }
        Component title = member != null ? Component.literal(metadata.members.get(member).name()) : text(defaults ? "jem.claims.new_members" : "jem.claims.everyone");
        VanillaMenus.chest(player, JemClaims.MOD_ID, member != null ? "member" : defaults ? "member_defaults" : "permissions", title, menu -> {
            List<ClaimSetting> settings = visibleSettings(member != null || defaults);
            boolean allEnabled = settings.stream().allMatch(setting -> settingEnabled(player, id, member, defaults, setting));
            menu.button(4, toggleIcon(Items.LEVER, allEnabled ? "disable_all" : "enable_all", allEnabled), () -> {
                if (!authorized(player, id)) return;
                setSettings(player, id, member, defaults, settings, !allEnabled);
                permissions(player, id, member, defaults);
            });
            for (int index = 0; index < settings.size(); index++) {
                ClaimSetting setting = settings.get(index);
                boolean enabled = settingEnabled(player, id, member, defaults, setting);
                menu.button(10 + index, toggleIcon(setting.icon, setting.key(), enabled), () -> {
                    if (!authorized(player, id)) return;
                    setSettings(player, id, member, defaults, List.of(setting), !enabled);
                    permissions(player, id, member, defaults);
                });
            }
            navigation(menu, player, id, member != null || defaults ? "members" : "permissions");
            if (member != null) {
                menu.button(40, VanillaMenus.icon(Items.BOOK, text("jem.claims.reset_defaults")), () -> {
                    if (!authorized(player, id)) return;
                    var current = metadata.members.get(member);
                    if (current != null && FlanBridge.syncMember(player, id, member, metadata.defaults)) {
                        metadata.members.put(member, new ClaimsData.Member(current.name(), new EnumMap<>(metadata.defaults)));
                        FlanBridge.syncMetadata(player.server, id);
                    }
                    permissions(player, id, member, false);
                });
                menu.button(52, VanillaMenus.icon(Items.BARRIER, text("jem.claims.remove_member")), () -> {
                    if (authorized(player, id) && FlanBridge.removeMember(player, id, member)) {
                        metadata.members.remove(member);
                        FlanBridge.syncMetadata(player.server, id);
                    }
                    members(player, id, 0);
                });
            }
            menu.button(49, VanillaMenus.icon(Items.ARROW, text("jem.claims.back")), () -> {
                if (member != null || defaults) members(player, id, 0);
                else main(player, id);
            });
        });
    }

    private static List<ClaimSetting> visibleSettings(boolean memberScoped) {
        return java.util.Arrays.stream(ClaimSetting.values()).filter(setting -> !memberScoped || setting.memberScoped()).toList();
    }

    private static boolean settingEnabled(ServerPlayer player, UUID id, UUID member, boolean defaults, ClaimSetting setting) {
        var metadata = ClaimsData.get(player.server).territory(id);
        if (member != null) {
            var permissions = metadata.members.get(member).permissions();
            return setting.permissions.stream().allMatch(permission -> permissions.getOrDefault(permission,
                    metadata.defaults.getOrDefault(permission, permission.memberDefault)));
        }
        if (defaults) return setting.permissions.stream().allMatch(permission -> metadata.defaults.getOrDefault(permission, permission.memberDefault));
        return setting.permissions.stream().allMatch(permission -> FlanBridge.value(player, id, permission));
    }

    private static void setSettings(ServerPlayer player, UUID id, UUID member, boolean defaults, List<ClaimSetting> settings, boolean enabled) {
        var metadata = ClaimsData.get(player.server).territory(id);
        if (member != null) {
            var current = metadata.members.get(member);
            if (current == null) return;
            var permissions = new EnumMap<>(current.permissions());
            settings.forEach(setting -> setting.permissions.forEach(permission -> permissions.put(permission, enabled)));
            if (FlanBridge.syncMember(player, id, member, permissions)) {
                metadata.members.put(member, new ClaimsData.Member(current.name(), permissions));
                FlanBridge.syncMetadata(player.server, id);
            }
            return;
        }
        if (defaults) {
            settings.forEach(setting -> setting.permissions.forEach(permission -> metadata.defaults.put(permission, enabled)));
            FlanBridge.syncMetadata(player.server, id);
            return;
        }
        if (settings.stream().anyMatch(setting -> !FlanBridge.set(player, id, setting, enabled))) return;
        settings.forEach(setting -> setting.permissions.forEach(permission -> metadata.tourists.put(permission, enabled)));
        FlanBridge.syncMetadata(player.server, id);
    }

    private static void members(ServerPlayer player, UUID id, int page) {
        if (!authorized(player, id)) return;
        var data = ClaimsData.get(player.server);
        var metadata = data.territory(id);
        var entries = metadata.members.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getValue().name())).toList();
        VanillaMenus.chest(player, JemClaims.MOD_ID, "members", text("jem.claims.members"), menu -> {
            int first = page * MEMBER_PAGE_SIZE;
            for (int index = first; index < Math.min(first + MEMBER_PAGE_SIZE, entries.size()); index++) {
                var entry = entries.get(index);
                int slot = index - first;
                var head = VanillaMenus.icon(Items.PLAYER_HEAD, entry.getValue().name());
                head.getOrCreateTag().putString("SkullOwner", entry.getValue().name());
                head.getOrCreateTag().putInt("jem_ui_remove_slot", slot + MEMBER_PAGE_SIZE);
                menu.button(slot, head, () -> permissions(player, id, entry.getKey(), false));
                var remove = VanillaMenus.icon(Items.BARRIER, text("jem.claims.remove_member"));
                remove.getOrCreateTag().putString("jem_ui_role", "row_remove");
                menu.button(slot + MEMBER_PAGE_SIZE, remove, () -> {
                    if (authorized(player, id) && FlanBridge.removeMember(player, id, entry.getKey())) {
                        metadata.members.remove(entry.getKey());
                        FlanBridge.syncMetadata(player.server, id);
                    }
                    members(player, id, 0);
                });
            }
            navigation(menu, player, id, "members");
            menu.button(50, VanillaMenus.icon(Items.BOOK, text("jem.claims.new_members")), () -> permissions(player, id, null, true));
            menu.button(42, VanillaMenus.icon(Items.PLAYER_HEAD, text("jem.claims.add_member")), () -> candidates(player, id, 0));
            if (page > 0) menu.button(51, VanillaMenus.icon(Items.ARROW, text("jem.claims.previous")), () -> members(player, id, page - 1));
            if (first + MEMBER_PAGE_SIZE < entries.size()) menu.button(53, VanillaMenus.icon(Items.ARROW, text("jem.claims.next")), () -> members(player, id, page + 1));
            menu.button(49, VanillaMenus.icon(Items.ARROW, text("jem.claims.back")), () -> main(player, id));
        });
    }
    private static void candidates(ServerPlayer player, UUID id, int requestedPage) {
        if (!authorized(player, id)) return;
        var profiles = new java.util.HashMap<UUID, com.mojang.authlib.GameProfile>();
        String[] files = player.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile().list();
        if (files != null) for (String file : files) {
            if (!file.endsWith(".dat")) continue;
            try {
                UUID uuid = UUID.fromString(file.substring(0, file.length() - 4));
                player.server.getProfileCache().get(uuid).ifPresent(profile -> profiles.put(uuid, profile));
            } catch (IllegalArgumentException ignored) {}
        }
        player.server.getPlayerList().getPlayers().forEach(online -> profiles.put(online.getUUID(), online.getGameProfile()));
        var data = ClaimsData.get(player.server);
        var metadata = data.territory(id);
        var owner = FlanBridge.get(player.server, id).owner();
        var entries = profiles.values().stream().filter(profile -> !profile.getId().equals(owner) && !metadata.members.containsKey(profile.getId()))
                .sorted(Comparator.comparing(com.mojang.authlib.GameProfile::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        int page = Math.max(0, Math.min(requestedPage, Math.max(0, (entries.size() - 1) / PAGE_SIZE)));
        VanillaMenus.chest(player, JemClaims.MOD_ID, "member_candidates", text("jem.claims.add_member"), menu -> {
            int first = page * PAGE_SIZE;
            for (int index = first; index < Math.min(first + PAGE_SIZE, entries.size()); index++) {
                var profile = entries.get(index);
                var head = VanillaMenus.icon(Items.PLAYER_HEAD, profile.getName());
                head.getOrCreateTag().putString("SkullOwner", profile.getName());
                menu.button(index - first, head, () -> {
                    if (!authorized(player, id)) return;
                    if (!metadata.members.containsKey(profile.getId()) && FlanBridge.syncMember(player, id, profile.getId(), metadata.defaults)) {
                        metadata.members.put(profile.getId(), new ClaimsData.Member(profile.getName(), new EnumMap<>(metadata.defaults)));
                        FlanBridge.syncMetadata(player.server, id);
                    }
                    members(player, id, 0);
                });
            }
            navigation(menu, player, id, "members");
            if (page > 0) menu.button(51, VanillaMenus.icon(Items.ARROW, text("jem.claims.previous")), () -> candidates(player, id, page - 1));
            if (first + PAGE_SIZE < entries.size()) menu.button(53, VanillaMenus.icon(Items.ARROW, text("jem.claims.next")), () -> candidates(player, id, page + 1));
            menu.button(49, VanillaMenus.icon(Items.ARROW, text("jem.claims.members")), () -> members(player, id, 0));
        });
    }

    public static void profile(ServerPlayer owner, UUID target) {
        var territories = ClaimGroups.connected(FlanBridge.list(owner.server).stream().filter(claim -> owner.getUUID().equals(claim.owner())).toList());
        VanillaMenus.chest(owner, JemClaims.MOD_ID, "profile_claims", text("jem.claims.my_territory_rights"), menu -> {
            for (int index = 0; index < Math.min(PAGE_SIZE, territories.size()); index++) {
                var claim = territories.get(index).get(0);
                var metadata = ClaimsData.get(owner.server).territory(claim.id());
                Component relation = text(metadata.members.containsKey(target) ? "jem.claims.resident" : metadata.overrides.containsKey(target) ? "jem.claims.individual_permissions" : "jem.claims.visitor");
                menu.button(index, location(VanillaMenus.icon(Items.FILLED_MAP, Component.literal(claim.name()), relation), territories.get(index), owner.blockPosition().getY()), () -> playerPermissions(owner, claim.id(), target));
            }
            menu.button(49, VanillaMenus.icon(Items.ARROW, text("jem.claims.back")), () -> {
                owner.closeContainer();
                owner.server.getCommands().performPrefixedCommand(owner.createCommandSourceStack(), "smp profiles");
            });
        });
    }

    private static void players(ServerPlayer player, UUID id, int requestedPage) {
        if (!authorized(player, id)) return;
        var profiles = knownPlayers(player);
        int page = Math.max(0, Math.min(requestedPage, Math.max(0, (profiles.size() - 1) / PAGE_SIZE)));
        VanillaMenus.chest(player, JemClaims.MOD_ID, "claim_players", text("jem.claims.players"), menu -> {
            int first = page * PAGE_SIZE;
            for (int index = first; index < Math.min(first + PAGE_SIZE, profiles.size()); index++) {
                var profile = profiles.get(index);
                var metadata = ClaimsData.get(player.server).territory(id);
                Component relation = text(metadata.members.containsKey(profile.getId()) ? "jem.claims.resident" : metadata.overrides.containsKey(profile.getId()) ? "jem.claims.individual" : "jem.claims.visitor");
                var icon = VanillaMenus.icon(Items.PLAYER_HEAD, Component.literal(profile.getName()), relation);
                icon.getOrCreateTag().putString("SkullOwner", profile.getName());
                menu.button(index - first, icon, () -> playerPermissions(player, id, profile.getId()));
            }
            if (page > 0) menu.button(51, VanillaMenus.icon(Items.ARROW, text("jem.claims.previous")), () -> players(player, id, page - 1));
            if (first + PAGE_SIZE < profiles.size()) menu.button(53, VanillaMenus.icon(Items.ARROW, text("jem.claims.next")), () -> players(player, id, page + 1));
            menu.button(49, VanillaMenus.icon(Items.ARROW, text("jem.claims.back")), () -> main(player, id));
        });
    }

    private static void playerPermissions(ServerPlayer player, UUID id, UUID target) {
        if (!authorized(player, id)) return;
        var metadata = ClaimsData.get(player.server).territory(id);
        String name = player.server.getProfileCache().get(target).map(com.mojang.authlib.GameProfile::getName).orElse(target.toString());
        var stored = metadata.overrides.get(target);
        var override = stored == null ? new ClaimsData.Member(name, new EnumMap<>(ClaimPermission.class)) : stored;
        VanillaMenus.chest(player, JemClaims.MOD_ID, "claim_player", Component.literal(name), menu -> {
            var settings = visibleSettings(true);
            boolean allEnabled = settings.stream().allMatch(setting -> overrideEnabled(player, id, target, override, setting));
            menu.button(4, toggleIcon(Items.LEVER, allEnabled ? "disable_all" : "enable_all", allEnabled), () -> {
                settings.forEach(setting -> setting.permissions.forEach(permission -> override.permissions().put(permission, !allEnabled)));
                applyOverride(player, id, target, override);
                playerPermissions(player, id, target);
            });
            for (int index = 0; index < settings.size(); index++) {
                var setting = settings.get(index);
                boolean enabled = overrideEnabled(player, id, target, override, setting);
                menu.button(10 + index, toggleIcon(setting.icon, setting.key(), enabled), () -> {
                    setting.permissions.forEach(permission -> override.permissions().put(permission, !enabled));
                    applyOverride(player, id, target, override);
                    playerPermissions(player, id, target);
                });
            }
            boolean resident = metadata.members.containsKey(target);
            menu.button(45, VanillaMenus.icon(resident ? Items.BARRIER : Items.OAK_DOOR, text(resident ? "jem.claims.remove_resident" : "jem.claims.add_resident")), () -> {
                if (resident) {
                    metadata.members.remove(target);
                    if (metadata.overrides.containsKey(target)) applyOverride(player, id, target, metadata.overrides.get(target));
                    else FlanBridge.removeMember(player, id, target);
                    FlanBridge.syncMetadata(player.server, id);
                } else {
                    metadata.members.put(target, new ClaimsData.Member(name, new EnumMap<>(metadata.defaults)));
                    if (metadata.overrides.containsKey(target)) applyOverride(player, id, target, metadata.overrides.get(target));
                    else FlanBridge.syncMember(player, id, target, metadata.defaults);
                    FlanBridge.syncMetadata(player.server, id);
                }
                playerPermissions(player, id, target);
            });
            menu.button(52, VanillaMenus.icon(Items.BARRIER, text("jem.claims.remove_individual")), () -> {
                metadata.overrides.remove(target);
                if (metadata.members.containsKey(target)) {
                    FlanBridge.syncMember(player, id, target, metadata.defaults);
                    FlanBridge.syncMetadata(player.server, id);
                } else FlanBridge.removeOverride(player, id, target);
                players(player, id, 0);
            });
            menu.button(49, VanillaMenus.icon(Items.ARROW, text("jem.claims.back")), () -> players(player, id, 0));
        });
    }

    private static boolean overrideEnabled(ServerPlayer player, UUID id, UUID target, ClaimsData.Member override, ClaimSetting setting) {
        var metadata = ClaimsData.get(player.server).territory(id);
        return setting.permissions.stream().allMatch(permission -> override.permissions().getOrDefault(permission,
                metadata.members.containsKey(target)
                        ? metadata.defaults.getOrDefault(permission, permission.memberDefault)
                        : FlanBridge.value(player, id, permission)));
    }

    private static void applyOverride(ServerPlayer player, UUID id, UUID target, ClaimsData.Member override) {
        var metadata = ClaimsData.get(player.server).territory(id);
        var effective = new EnumMap<ClaimPermission, Boolean>(ClaimPermission.class);
        for (var permission : ClaimPermission.values()) {
            if (permission.global) continue;
            boolean inherited = metadata.members.containsKey(target)
                    ? metadata.defaults.getOrDefault(permission, permission.memberDefault)
                    : FlanBridge.value(player, id, permission);
            effective.put(permission, override.permissions().getOrDefault(permission, inherited));
        }
        FlanBridge.syncMember(player, id, target, effective);
        metadata.overrides.put(target, override);
        FlanBridge.syncMetadata(player.server, id);
    }

    private static java.util.List<com.mojang.authlib.GameProfile> knownPlayers(ServerPlayer player) {
        var profiles = new java.util.HashMap<UUID, com.mojang.authlib.GameProfile>();
        player.server.getPlayerList().getPlayers().forEach(online -> profiles.put(online.getUUID(), online.getGameProfile()));
        String[] files = player.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile().list();
        if (files != null) for (String file : files) {
            if (!file.endsWith(".dat")) continue;
            try {
                UUID id = UUID.fromString(file.substring(0, file.length() - 4));
                player.server.getProfileCache().get(id).ifPresent(profile -> profiles.put(id, profile));
            } catch (IllegalArgumentException ignored) {}
        }
        return profiles.values().stream().filter(profile -> !profile.getId().equals(player.getUUID())).sorted(Comparator.comparing(com.mojang.authlib.GameProfile::getName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private static void allTerritories(ServerPlayer player) {
        var defaults = ClaimsData.get(player.server).ownerDefaults(player.getUUID());
        VanillaMenus.chest(player, JemClaims.MOD_ID, "all_claims", text("jem.claims.all_territories"), menu -> {
            var settings = List.of(ClaimSetting.values());
            boolean allEnabled = settings.stream().allMatch(setting -> setting.permissions.stream()
                    .allMatch(permission -> defaults.getOrDefault(permission, false)));
            menu.button(4, toggleIcon(Items.LEVER, allEnabled ? "disable_all" : "enable_all", allEnabled), () -> {
                setAllTerritories(player, settings, !allEnabled);
                allTerritories(player);
            });
            for (int index = 0; index < settings.size(); index++) {
                var setting = settings.get(index);
                boolean enabled = setting.permissions.stream().allMatch(permission -> defaults.getOrDefault(permission, false));
                menu.button(10 + index, toggleIcon(setting.icon, setting.key(), enabled), () -> {
                    setAllTerritories(player, List.of(setting), !enabled);
                    allTerritories(player);
                });
            }
            menu.button(49, VanillaMenus.icon(Items.ARROW, text("jem.claims.back_to_territories")), () -> overview(player, 0));
        });
    }

    private static void setAllTerritories(ServerPlayer player, List<ClaimSetting> settings, boolean enabled) {
        var data = ClaimsData.get(player.server);
        var defaults = data.ownerDefaults(player.getUUID());
        settings.forEach(setting -> setting.permissions.forEach(permission -> defaults.put(permission, enabled)));
        for (var territory : FlanBridge.list(player.server)) {
            if (!player.getUUID().equals(territory.owner())) continue;
            var metadata = data.territory(territory.id());
            for (var setting : settings) {
                boolean effective = setting.permissions.stream().allMatch(permission -> metadata.tourists.getOrDefault(permission,
                        defaults.getOrDefault(permission, false)));
                FlanBridge.set(player, territory.id(), setting, effective);
            }
        }
        data.setDirty();
    }

    private static Component text(String key, Object... arguments) {
        return Component.translatable(key, arguments);
    }

}
