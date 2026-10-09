package com.siirio.jemcompat.feature.backtobed;

import com.siirio.jemcompat.advancement.AdvancementAwards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.List;

public final class GroupBedTeleport {
    private static final ResourceLocation GROUP_HOMECOMING = new ResourceLocation("jem_guide", "qol/group_homecoming");
    private static final int ITEM_COOLDOWN_TICKS = 40;
    private static final int PARTICLE_COUNT = 85;
    private static final double DESTINATION_CENTER_OFFSET = 0.5;
    private static final double DESTINATION_HEIGHT_OFFSET = 0.6;
    private static final DustColorTransitionOptions DESTINATION_PARTICLES = new DustColorTransitionOptions(
            new Vector3f(0.0F, 1.0F, 1.0F),
            new Vector3f(1.0F, 0.0F, 1.0F),
            1.0F
    );

    private GroupBedTeleport() {
    }

    public static void activate(ServerPlayer host, Item item) {
        MinecraftServer server = host.getServer();
        BlockPos respawnPosition = host.getRespawnPosition();
        ServerLevel destinationLevel = server == null ? null : server.getLevel(host.getRespawnDimension());

        if (respawnPosition == null || destinationLevel == null) {
            finish(host, item, "item.backtobed.magical_returner.condition.no_respawn_point");
            return;
        }
        if (!(destinationLevel.getBlockState(respawnPosition).getBlock() instanceof BedBlock)) {
            finish(host, item, "item.backtobed.magical_returner.condition.no_access_to_bed");
            return;
        }

        long currentDay = GroupTeleportPolicy.day(server.overworld().getDayTime());
        if (!DailyTeleportData.canActivate(host, currentDay)) {
            finish(host, item, "message.jemcompat.backtobed.used_today");
            return;
        }

        ServerLevel sourceLevel = host.serverLevel();
        List<ServerPlayer> group = sourceLevel.getEntitiesOfClass(ServerPlayer.class, formationArea(host), player ->
                player.isAlive() && !player.isSpectator() && GroupTeleportPolicy.isNearby(
                        host.getX(), host.getY(), host.getZ(),
                        player.getX(), player.getY(), player.getZ()
                ));
        Entity hostVehicle = host.getVehicle();

        DailyTeleportData.markActivated(host, currentDay);
        group.forEach(Entity::stopRiding);

        double destinationX = respawnPosition.getX() + DESTINATION_CENTER_OFFSET;
        double destinationY = respawnPosition.getY() + DESTINATION_HEIGHT_OFFSET;
        double destinationZ = respawnPosition.getZ() + DESTINATION_CENTER_OFFSET;
        group.forEach(player -> player.teleportTo(
                destinationLevel,
                destinationX,
                destinationY,
                destinationZ,
                player.getYRot(),
                player.getXRot()
        ));
        if (group.size() > 1) {
            group.forEach(player -> AdvancementAwards.award(player, GROUP_HOMECOMING));
        }

        if (hostVehicle != null && sourceLevel == destinationLevel) {
            hostVehicle.teleportTo(destinationX, destinationY, destinationZ);
            host.startRiding(hostVehicle, true);
        }

        destinationLevel.playSound(null, respawnPosition, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        destinationLevel.sendParticles(
                host,
                DESTINATION_PARTICLES,
                true,
                destinationX,
                destinationY,
                destinationZ,
                PARTICLE_COUNT,
                0.85,
                0.75,
                0.85,
                0.005
        );
        finish(host, item, null);
    }

    private static AABB formationArea(ServerPlayer host) {
        return new AABB(
                host.getX() - GroupTeleportPolicy.HORIZONTAL_RADIUS,
                host.getY() - GroupTeleportPolicy.VERTICAL_TOLERANCE,
                host.getZ() - GroupTeleportPolicy.HORIZONTAL_RADIUS,
                host.getX() + GroupTeleportPolicy.HORIZONTAL_RADIUS,
                host.getY() + GroupTeleportPolicy.VERTICAL_TOLERANCE,
                host.getZ() + GroupTeleportPolicy.HORIZONTAL_RADIUS
        );
    }

    private static void finish(ServerPlayer host, Item item, String message) {
        if (message != null) {
            host.displayClientMessage(Component.translatable(message), true);
        }
        host.stopUsingItem();
        host.getCooldowns().addCooldown(item, ITEM_COOLDOWN_TICKS);
    }
}
