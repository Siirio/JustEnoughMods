package com.siirio.jemserver;

import com.siirio.jemserver.menus.VanillaMenus;

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
        if (com.siirio.jemidentity.api.IdentityPolicy.resolve(player).isEmpty()) return;
        var history = ServerData.get(player.server).deaths(player.getUUID());
        VanillaMenus.chest(player, JemServer.MOD_ID, "deaths", Component.translatable("jem.deaths.title"), menu -> {
            if (history.isEmpty()) menu.button(22, VanillaMenus.icon(Items.CLOCK, Component.translatable("jem.deaths.empty")), null);
            for (int index = 0; index < Math.min(3, history.size()); index++) {
                var death = history.get(index);
                menu.button(FIRST_SLOT + index * SLOT_SPACING, VanillaMenus.icon(Items.RECOVERY_COMPASS, age(death.timestamp()),
                        Component.literal(death.dimension().toString()), Component.literal("X: " + (int) Math.floor(death.x())), Component.literal("Y: " + (int) Math.floor(death.y())), Component.literal("Z: " + (int) Math.floor(death.z())),
                        Component.literal(death.cause()), Component.translatable("jem.deaths.return_hint")), () -> teleport(player, death.id()));
            }
        });
    }

    private static Component age(long timestamp) {
        long elapsedTicks = Math.max(0L, (System.currentTimeMillis() - timestamp) / 50L);
        if (elapsedTicks == 0L) return Component.translatable("jem.deaths.just_now");
        return Component.translatable("jem.deaths.days_ago", com.siirio.jemidentity.api.MinecraftDays.format(elapsedTicks));
    }

    public static void teleport(ServerPlayer player, UUID id) {
        if (!player.isAlive() || player.isSpectator() || com.siirio.jemidentity.api.IdentityPolicy.resolve(player).isEmpty()) return;
        var death = ServerData.get(player.server).deaths(player.getUUID()).stream().limit(3).filter(record -> record.id().equals(id)).findFirst().orElse(null);
        if (death == null) {
            message(player, "jem.deaths.error.expired");
            open(player);
            return;
        }
        ServerLevel level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, death.dimension()));
        if (level == null) {
            message(player, "jem.deaths.error.dimension");
            return;
        }
        Vec3 exact = new Vec3(death.x(), death.y(), death.z());
        BlockPos origin = BlockPos.containing(exact);
        Vec3 target = safe(player, level, exact) ? exact : nearestSafe(player, level, origin, ServerConfig.SAFE_RADIUS.get());
        if (target == null) {
            message(player, "jem.deaths.error.no_safe_point");
            return;
        }
        var decision = com.siirio.jemserver.smp.events.BossTeleportSafety.validate(player, level, target);
        if (decision.matched() && !target.equals(decision.destination())) {
            message(player, "jem.home.arena");
            return;
        }
        var encounter = com.siirio.jemserver.smp.events.EventSession.forPlayer(player);
        if (encounter != null && encounter.combatStarted()) { message(player, "jem.home.arena"); return; }
        player.closeContainer();
        player.stopRiding();
        com.siirio.jemserver.smp.events.BossTeleportSafety.apply(player, level, target, death.yaw(), death.pitch());
        player.fallDistance = 0;
    }

    public static Vec3 nearestSafe(ServerPlayer player, ServerLevel level, BlockPos origin, int radius) {
        return BlockPos.betweenClosedStream(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))
                .map(BlockPos::immutable).sorted(Comparator.comparingDouble(origin::distSqr))
                .map(position -> Vec3.atBottomCenterOf(position)).filter(position -> safe(player, level, position)).findFirst().orElse(null);
    }

    public static boolean safe(ServerPlayer player, ServerLevel level, Vec3 position) {
        BlockPos feet = BlockPos.containing(position);
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)
                || feet.getY() < level.getMinBuildHeight() || feet.getY() + player.getBbHeight() >= level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(feet)) return false;
        AABB box = player.getDimensions(Pose.STANDING).makeBoundingBox(position);
        if (!level.getWorldBorder().isWithinBounds(box)) return false;
        for (BlockPos block : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            level.getChunkAt(block);
            var state = level.getBlockState(block);
            if (!state.getFluidState().isEmpty() || dangerous(state)) return false;
        }
        var floor = level.getBlockState(feet.below());
        return !dangerous(floor) && floor.isFaceSturdy(level, feet.below(), Direction.UP) && level.noCollision(player, box);
    }

    public static boolean dangerous(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.NETHER_PORTAL);
    }

    private static void message(ServerPlayer player, String key) { player.sendSystemMessage(Component.translatable(key)); }
}
