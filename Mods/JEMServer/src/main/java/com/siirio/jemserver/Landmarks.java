package com.siirio.jemserver;

import com.siirio.jemserver.menus.VanillaMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class Landmarks {
    private static final int PAGE_SIZE = 45;
    private static final UUID SERVER_CREATOR = UUID.nameUUIDFromBytes("jem_server:boss_landmarks".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private record Category(String id, Item icon) {}
    private static final List<Category> CATEGORIES = List.of(
            new Category("scenery", Items.FLOWERING_AZALEA),
            new Category("building", Items.BRICKS),
            new Category("landmark", Items.MAP),
            new Category("shop", Items.EMERALD),
            new Category("danger", Items.SKELETON_SKULL));

    private Landmarks() {}

    public static void open(ServerPlayer player, int requestedPage, boolean management) {
        if (!ServerConfig.LANDMARKS_ENABLED.get() || management && !player.hasPermissions(2)) return;
        ServerData data = ServerData.get(player.server);
        var landmarks = data.landmarks(player.getUUID()).stream().sorted(Comparator.comparing(ServerData.Landmark::name).thenComparing(ServerData.Landmark::id)).toList();
        int page = Math.max(0, Math.min(requestedPage, Math.max(0, (landmarks.size() - 1) / PAGE_SIZE)));
        VanillaMenus.chest(player, JemServer.MOD_ID, "landmarks", Component.translatable("jem.landmarks.title"), menu -> {
            for (int index = page * PAGE_SIZE; index < Math.min(landmarks.size(), (page + 1) * PAGE_SIZE); index++) {
                var landmark = landmarks.get(index);
                var icon = locationIcon(landmark);
                menu.button(index % PAGE_SIZE, icon, () -> manage(player, landmark.id(), management));
            }
            if (page > 0) menu.button(45, VanillaMenus.icon(Items.ARROW, Component.translatable("jem.ui.back")), () -> open(player, page - 1, management));
            menu.button(48, VanillaMenus.icon(Items.WRITABLE_BOOK, Component.translatable("jem.landmarks.save_here")), () -> create(player));
            if ((page + 1) * PAGE_SIZE < landmarks.size()) menu.button(53, VanillaMenus.icon(Items.ARROW, Component.translatable("jem.ui.next")), () -> open(player, page + 1, management));
        });
    }

    public static void create(ServerPlayer player) {
        if (!ServerConfig.LANDMARKS_ENABLED.get()) return;
        VanillaMenus.input(player, Component.translatable("jem.landmarks.name"), "", value -> {
            if (!ServerConfig.LANDMARKS_ENABLED.get()) return;
            String name = value.strip();
            if (name.isEmpty() || name.length() > ServerConfig.NAME_LENGTH.get() || name.codePoints().anyMatch(code -> Character.isISOControl(code) || code == 167)) {
                player.sendSystemMessage(Component.translatable("jem.landmarks.error.name_length", ServerConfig.NAME_LENGTH.get()));
                return;
            }
            VanillaMenus.chest(player, JemServer.MOD_ID, "landmark_category", Component.translatable("jem.landmarks.category"), menu -> {
                for (int index = 0; index < CATEGORIES.size(); index++) {
                    Category category = CATEGORIES.get(index);
                    menu.button(20 + index, VanillaMenus.icon(category.icon(), categoryTitle(category.id())), () -> submit(player, name, category.id()));
                }
            });
        });
    }

    private static void submit(ServerPlayer player, String name, String category) {
        if (publish(player, name, category, player.level().dimension().location(), player.blockPosition())) player.closeContainer();
    }

    public static boolean publish(ServerPlayer player, String name, ResourceLocation dimension, BlockPos position) {
        return publish(player, name, "landmark", dimension, position);
    }

    private static boolean publish(ServerPlayer player, String name, String category, ResourceLocation dimension, BlockPos position) {
        String clean = name.strip();
        if (!ServerConfig.LANDMARKS_ENABLED.get() || !player.isAlive()) return false;
        if (clean.isEmpty() || clean.length() > ServerConfig.NAME_LENGTH.get()
                || clean.codePoints().anyMatch(code -> Character.isISOControl(code) || code == 167)) {
            player.sendSystemMessage(Component.translatable("jem.landmarks.error.invalid_name"));
            return false;
        }
        if (!dimension.equals(player.level().dimension().location()) || !player.serverLevel().getWorldBorder().isWithinBounds(position)
                || position.getY() < player.level().getMinBuildHeight() || position.getY() >= player.level().getMaxBuildHeight()) {
            player.sendSystemMessage(Component.translatable("jem.landmarks.error.location"));
            return false;
        }
        var data = ServerData.get(player.server);
        if (data.landmarks(player.getUUID()).stream().anyMatch(value -> value.creator().equals(player.getUUID()) && value.dimension().equals(dimension)
                && value.position().equals(position) && value.name().equals(clean))) return true;
        var landmark = new ServerData.Landmark(UUID.randomUUID(), clean, "", category, dimension, position.immutable(), player.getUUID(), player.getGameProfile().getName(), System.currentTimeMillis());
        if (!data.publish(landmark)) {
            player.sendSystemMessage(Component.translatable("jem.landmarks.error.limit"));
            return false;
        }
        LandmarkNetwork.changed(player.server, landmark);
        player.sendSystemMessage(Component.translatable("jem.landmarks.saved"));
        return true;
    }

    public static boolean isBoss(ServerData.Landmark landmark) {
        return SERVER_CREATOR.equals(landmark.creator());
    }

    public static void discovered(com.siirio.jemtwelveeyes.BossLocationDiscoveredEvent event) {
        if (!com.siirio.jemserver.smp.SmpEnvironment.active(event.server())) return;
        var location = event.location();
        var arena = com.siirio.jemworldbosstiers.api.RaidArenaApi.arena(event.server(), location.entity(), location.dimension(), location.position()).orElse(null);
        if (arena == null) {
            String key = location.entity() + ":" + location.dimension() + ":" + location.position().asLong();
            UUID id = UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            publishBoss(event.server(), id, location.entity(), location.dimension(), location.position());
        } else {
            UUID id;
            try { id = UUID.fromString(arena.id()); }
            catch (IllegalArgumentException namedIdentity) { id = UUID.nameUUIDFromBytes(arena.id().getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
            publishBoss(event.server(), id, location.entity(), location.dimension(), arena.center(), arena.bounds());
        }
        if (ServerConfig.LANDMARKS_ENABLED.get()) event.markShared();
    }

    public static void restoreDiscoveries(MinecraftServer server) {
        if (!com.siirio.jemserver.smp.SmpEnvironment.active(server)) return;
        for (var location : com.siirio.jemtwelveeyes.api.CampaignApi.locatedBosses(server))
            discovered(new com.siirio.jemtwelveeyes.BossLocationDiscoveredEvent(server, location));
    }

    public static void publishBoss(MinecraftServer server, UUID id, ResourceLocation boss, ResourceLocation dimension, BlockPos position) {
        publishBoss(server, id, boss, dimension, position, null);
    }

    public static void publishBoss(MinecraftServer server, UUID id, ResourceLocation boss, ResourceLocation dimension, BlockPos position,
                                   net.minecraft.world.level.levelgen.structure.BoundingBox bounds) {
        var data = ServerData.get(server);
        var existing = data.landmark(id);
        if (existing != null) {
            if (bounds != null && isBoss(existing)) LandmarkNetwork.consolidated(server, data.consolidateBoss(existing, bounds));
            return;
        }
        String nameKey = EntityType.byString(boss.toString()).map(EntityType::getDescriptionId).orElse("");
        String fallback = nameKey.isEmpty() ? boss.getPath() : Component.translatable(nameKey).getString();
        var landmark = new ServerData.Landmark(id, fallback, nameKey, bounds == null ? ServerData.LOCATED_BOSS_CATEGORY : "danger",
                dimension, position.immutable(), SERVER_CREATOR, "JEM", System.currentTimeMillis());
        if (bounds != null) LandmarkNetwork.consolidated(server, data.consolidateBoss(landmark, bounds));
        if (data.publishSystem(landmark)) LandmarkNetwork.changed(server, landmark);
    }

    private static void manage(ServerPlayer player, UUID id, boolean management) {
        if (!ServerConfig.LANDMARKS_ENABLED.get()) return;
        var landmark = ServerData.get(player.server).landmark(id);
        if (landmark == null || !isBoss(landmark) && !landmark.creator().equals(player.getUUID())) {
            open(player, 0, management);
            return;
        }
        VanillaMenus.chest(player, JemServer.MOD_ID, "confirm", displayName(landmark), menu -> {
            menu.button(4, locationIcon(landmark), null);
            if (!isBoss(landmark) && landmark.creator().equals(player.getUUID()))
                menu.button(22, VanillaMenus.icon(Items.BARRIER, Component.translatable("jem.landmarks.delete"), Component.translatable("jem.landmarks.delete_hint")), () -> remove(player, id));
            menu.button(49, VanillaMenus.icon(Items.ARROW, Component.translatable("jem.ui.cancel")), () -> open(player, 0, management));
        });
    }

    public static int remove(ServerPlayer player, UUID id) {
        int result = delete(player, id);
        open(player, 0, false);
        return result;
    }

    public static int delete(ServerPlayer player, UUID id) {
        if (!ServerConfig.LANDMARKS_ENABLED.get()) return 0;
        var data = ServerData.get(player.server);
        var landmark = data.landmark(id);
        if (landmark == null || isBoss(landmark) || !landmark.creator().equals(player.getUUID())) return 0;
        boolean changed = data.removeLandmark(id);
        if (changed) LandmarkNetwork.removed(player, id);
        player.sendSystemMessage(Component.translatable(changed ? "jem.landmarks.deleted" : "jem.landmarks.already_deleted"));
        return changed ? 1 : 0;
    }

    private static net.minecraft.world.item.ItemStack locationIcon(ServerData.Landmark landmark) {
        var icon = VanillaMenus.icon(Items.COMPASS, displayName(landmark), categoryTitle(landmark.category()), DimensionNames.name(landmark.dimension()),
                Component.translatable("jem.landmarks.coordinates", landmark.position().getX(), landmark.position().getY(), landmark.position().getZ()), Component.translatable("jem.landmarks.author", landmark.creatorName()));
        icon.getOrCreateTag().putString("jem_ui_dimension", landmark.dimension().toString());
        icon.getOrCreateTag().putLong("jem_ui_position", landmark.position().asLong());
        return icon;
    }

    private static Component displayName(ServerData.Landmark landmark) {
        return isBoss(landmark) && !landmark.nameKey().isEmpty()
                ? Component.translatable(landmark.nameKey()) : Component.literal(landmark.name());
    }

    private static Component categoryTitle(String id) {
        return CATEGORIES.stream().anyMatch(category -> category.id().equals(id)) || id.equals(ServerData.LOCATED_BOSS_CATEGORY)
                ? Component.translatable("jem.landmarks.category." + id)
                : Component.literal(id);
    }
}
