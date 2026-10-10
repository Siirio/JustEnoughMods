package com.siirio.jemserver;

import com.siirio.jemmenus.VanillaMenus;

import com.siirio.jemserver.claims.Claims;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DeathHistory {
    private static final int FIRST_SLOT = 20;
    private static final int SLOT_SPACING = 2;
    private static final long MINUTE_MILLIS = 60_000;
    private static final long DAY_MINUTES = 1_440;

    private DeathHistory() {}

    public static void open(ServerPlayer player) {
        var history = ServerData.get(player.server).deaths(player.getUUID());
        VanillaMenus.chest(player, JemServer.MOD_ID, "deaths", "Последние смерти", menu -> {
            if (history.isEmpty()) menu.button(22, VanillaMenus.icon(Items.CLOCK, "История пуста"), null);
            for (int index = 0; index < history.size(); index++) {
                var death = history.get(index);
                menu.button(FIRST_SLOT + index * SLOT_SPACING, VanillaMenus.icon(Items.RECOVERY_COMPASS, age(death.timestamp()),
                        death.dimension().toString(), "X: " + (int) Math.floor(death.x()), "Y: " + (int) Math.floor(death.y()), "Z: " + (int) Math.floor(death.z()),
                        death.cause(), "Нажмите, чтобы вернуться"), () -> teleport(player, death.id()));
            }
        });
    }

    private static String age(long timestamp) {
        long minutes = Math.max(0, (System.currentTimeMillis() - timestamp) / MINUTE_MILLIS);
        if (minutes == 0) return "Только что";
        if (minutes >= DAY_MINUTES) return minutes / DAY_MINUTES + " дн. назад";
        return minutes + " мин. назад";
    }

    public static void teleport(ServerPlayer player, UUID id) {
        if (!player.isAlive()) return;
        var death = ServerData.get(player.server).deaths(player.getUUID()).stream().filter(record -> record.id().equals(id)).findFirst().orElse(null);
        if (death == null) {
            message(player, "Эта смерть уже вытеснена из истории.");
            open(player);
            return;
        }
        ServerLevel level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, death.dimension()));
        if (level == null) {
            message(player, "Это измерение недоступно.");
            return;
        }
        Vec3 exact = new Vec3(death.x(), death.y(), death.z());
        BlockPos origin = BlockPos.containing(exact);
        if (!Claims.canStay(player, level, origin)) {
            message(player, "Вход на территорию в месте смерти запрещён.");
            return;
        }
        Vec3 target = safe(player, level, exact) ? exact : nearest(player, level, origin);
        if (target == null) {
            message(player, "Рядом с местом смерти нет безопасной доступной точки.");
            return;
        }
        if (!Claims.canStay(player, level, BlockPos.containing(target))) {
            message(player, "Вход на эту территорию запрещён.");
            return;
        }
        player.closeContainer();
        player.stopRiding();
        player.teleportTo(level, target.x, target.y, target.z, death.yaw(), death.pitch());
        player.fallDistance = 0;
    }

    private static Vec3 nearest(ServerPlayer player, ServerLevel level, BlockPos origin) {
        int radius = ServerConfig.SAFE_RADIUS.get();
        return BlockPos.betweenClosedStream(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))
                .map(BlockPos::immutable).sorted(Comparator.comparingDouble(origin::distSqr))
                .map(position -> Vec3.atBottomCenterOf(position)).filter(position -> safe(player, level, position)).findFirst().orElse(null);
    }

    private static boolean safe(ServerPlayer player, ServerLevel level, Vec3 position) {
        BlockPos feet = BlockPos.containing(position);
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)
                || feet.getY() < level.getMinBuildHeight() || feet.getY() + player.getBbHeight() >= level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(feet)) return false;
        AABB box = player.getDimensions(Pose.STANDING).makeBoundingBox(position);
        if (!level.getWorldBorder().isWithinBounds(box)) return false;
        for (BlockPos block : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            level.getChunkAt(block);
            var state = level.getBlockState(block);
            if (!state.getFluidState().isEmpty() || dangerous(state)
                    || !Claims.canStay(player, level, block)) return false;
        }
        var floor = level.getBlockState(feet.below());
        return !dangerous(floor) && floor.isFaceSturdy(level, feet.below(), Direction.UP) && level.noCollision(player, box);
    }

    private static boolean dangerous(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.NETHER_PORTAL);
    }

    private static void message(ServerPlayer player, String text) { player.sendSystemMessage(Component.literal(text)); }
}
