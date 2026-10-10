package com.siirio.jemserver;

import com.siirio.jemmenus.VanillaMenus;

import com.siirio.jemserver.claims.Claims;
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
    private record Category(String id, String title, Item icon) {}
    private static final List<Category> CATEGORIES = List.of(
            new Category("scenery", "Красивое место", Items.FLOWERING_AZALEA),
            new Category("building", "Постройка", Items.BRICKS),
            new Category("landmark", "Достопримечательность", Items.MAP),
            new Category("shop", "Магазин", Items.EMERALD),
            new Category("danger", "Опасное место", Items.SKELETON_SKULL));

    private Landmarks() {}

    public static void open(ServerPlayer player, int requestedPage, boolean management) {
        if (!ServerConfig.LANDMARKS_ENABLED.get() || management && !player.hasPermissions(2)) return;
        ServerData data = ServerData.get(player.server);
        var landmarks = data.landmarks().stream().sorted(Comparator.comparing(ServerData.Landmark::name).thenComparing(ServerData.Landmark::id)).toList();
        int page = Math.max(0, Math.min(requestedPage, Math.max(0, (landmarks.size() - 1) / PAGE_SIZE)));
        VanillaMenus.chest(player, JemServer.MOD_ID, "landmarks", "Места сервера", menu -> {
            for (int index = page * PAGE_SIZE; index < Math.min(landmarks.size(), (page + 1) * PAGE_SIZE); index++) {
                var landmark = landmarks.get(index);
                var icon = locationIcon(landmark);
                menu.button(index % PAGE_SIZE, icon, () -> manage(player, landmark.id(), management));
            }
            if (page > 0) menu.button(45, VanillaMenus.icon(Items.ARROW, "Назад"), () -> open(player, page - 1, management));
            menu.button(48, VanillaMenus.icon(Items.WRITABLE_BOOK, "Поделиться этим местом"), () -> create(player));
            if ((page + 1) * PAGE_SIZE < landmarks.size()) menu.button(53, VanillaMenus.icon(Items.ARROW, "Далее"), () -> open(player, page + 1, management));
        });
    }

    public static void create(ServerPlayer player) {
        if (!ServerConfig.LANDMARKS_ENABLED.get()) return;
        VanillaMenus.input(player, "Название места", "Новое место", value -> {
            if (!ServerConfig.LANDMARKS_ENABLED.get()) return;
            String name = value.strip();
            if (name.isEmpty() || name.length() > ServerConfig.NAME_LENGTH.get() || name.codePoints().anyMatch(code -> Character.isISOControl(code) || code == 167)) {
                player.sendSystemMessage(Component.literal("Введите название длиной от 1 до " + ServerConfig.NAME_LENGTH.get() + " символов без управляющих кодов."));
                return;
            }
            VanillaMenus.chest(player, JemServer.MOD_ID, "landmark_category", "Категория места", menu -> {
                for (int index = 0; index < CATEGORIES.size(); index++) {
                    Category category = CATEGORIES.get(index);
                    menu.button(20 + index, VanillaMenus.icon(category.icon(), category.title()), () -> submit(player, name, category.id()));
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
            player.sendSystemMessage(Component.literal("Недопустимое название метки."));
            return false;
        }
        if (!dimension.equals(player.level().dimension().location()) || !player.serverLevel().getWorldBorder().isWithinBounds(position)
                || position.getY() < player.level().getMinBuildHeight() || position.getY() >= player.level().getMaxBuildHeight()
                || !Claims.available(player, new ChunkPos(position))) {
            player.sendSystemMessage(Component.literal("Общую метку можно поставить в текущем измерении на загруженной для вас территории."));
            return false;
        }
        var data = ServerData.get(player.server);
        if (data.landmarks().stream().anyMatch(value -> value.creator().equals(player.getUUID()) && value.dimension().equals(dimension)
                && value.position().equals(position) && value.name().equals(clean))) return true;
        var landmark = new ServerData.Landmark(UUID.randomUUID(), clean, "", category, dimension, position.immutable(), player.getUUID(), player.getGameProfile().getName(), System.currentTimeMillis());
        if (!data.publish(landmark)) {
            player.sendSystemMessage(Component.literal("Достигнут лимит общих меток. Удалите одну из своих старых меток."));
            return false;
        }
        LandmarkNetwork.syncAll(player.server);
        player.sendSystemMessage(Component.literal("Метка опубликована для всех игроков."));
        return true;
    }

    public static void publishBoss(MinecraftServer server, UUID id, ResourceLocation boss, ResourceLocation dimension, BlockPos position) {
        String nameKey = EntityType.byString(boss.toString()).map(EntityType::getDescriptionId).orElse("");
        String fallback = nameKey.isEmpty() ? boss.getPath() : Component.translatable(nameKey).getString();
        var landmark = new ServerData.Landmark(id, fallback, nameKey, "danger", dimension, position.immutable(), SERVER_CREATOR, "JEM", System.currentTimeMillis());
        if (ServerData.get(server).publishSystem(landmark)) LandmarkNetwork.syncAll(server);
    }

    private static void manage(ServerPlayer player, UUID id, boolean management) {
        if (!ServerConfig.LANDMARKS_ENABLED.get()) return;
        var landmark = ServerData.get(player.server).landmarks().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
        if (landmark == null) {
            open(player, 0, management);
            return;
        }
        VanillaMenus.chest(player, JemServer.MOD_ID, "confirm", landmark.name(), menu -> {
            menu.button(4, locationIcon(landmark), null);
            if (player.hasPermissions(2) || landmark.creator().equals(player.getUUID()))
                menu.button(22, VanillaMenus.icon(Items.RED_DYE, "Удалить общую метку", "Исчезнет с карты у всех игроков"), () -> remove(player, id));
            menu.button(49, VanillaMenus.icon(Items.ARROW, "Отмена"), () -> open(player, 0, management));
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
        var landmark = data.landmarks().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
        if (landmark == null || !player.hasPermissions(2) && !landmark.creator().equals(player.getUUID())) return 0;
        boolean changed = data.removeLandmark(id);
        if (changed) LandmarkNetwork.syncAll(player.server);
        player.sendSystemMessage(Component.literal(changed ? "Общая метка удалена." : "Метка уже удалена."));
        return changed ? 1 : 0;
    }

    private static net.minecraft.world.item.ItemStack locationIcon(ServerData.Landmark landmark) {
        var icon = VanillaMenus.icon(Items.COMPASS, landmark.name(), categoryTitle(landmark.category()), landmark.dimension().toString(),
                "X: " + landmark.position().getX() + " Y: " + landmark.position().getY() + " Z: " + landmark.position().getZ(), "Автор: " + landmark.creatorName());
        icon.getOrCreateTag().putString("jem_ui_dimension", landmark.dimension().toString());
        icon.getOrCreateTag().putLong("jem_ui_position", landmark.position().asLong());
        return icon;
    }

    private static String categoryTitle(String id) { return CATEGORIES.stream().filter(category -> category.id().equals(id)).map(Category::title).findFirst().orElse(id); }
}
