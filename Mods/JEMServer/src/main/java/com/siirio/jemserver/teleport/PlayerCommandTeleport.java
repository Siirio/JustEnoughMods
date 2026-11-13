package com.siirio.jemserver.teleport;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = "jem_server")
public final class PlayerCommandTeleport {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(2);
    private static final int MAX_PENDING = 8;
    private static final int MAX_CHUNKS = 16;
    private static final TicketType<Integer> TICKET = TicketType.create("jem_command_teleport", Integer::compareTo);
    private static final ThreadLocal<ChatCommand> CHAT_COMMAND = new ThreadLocal<>();
    private static final Map<MinecraftServer, Map<UUID, Pending>> PENDING = new HashMap<>();

    private PlayerCommandTeleport() {}

    public static int dispatch(Commands commands, ParseResults<CommandSourceStack> parsed, String input, ServerPlayer player) {
        ChatCommand previous = CHAT_COMMAND.get();
        CHAT_COMMAND.set(new ChatCommand(parsed, player));
        try {
            return commands.performCommand(parsed, input);
        } finally {
            if (previous == null) CHAT_COMMAND.remove();
            else CHAT_COMMAND.set(previous);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void command(CommandEvent event) {
        ChatCommand chat = CHAT_COMMAND.get();
        var parsed = event.getParseResults();
        if (chat == null || parsed != chat.parsed() || parsed.getReader().canRead()) return;
        var context = parsed.getContext();
        if (context.getNodes().isEmpty()) return;
        CommandNode<CommandSourceStack> root = context.getNodes().get(0).getNode();
        var dispatcherRoot = context.getDispatcher().getRoot();
        var teleport = dispatcherRoot.getChild("teleport");
        if (root != teleport && (root != dispatcherRoot.getChild("tp") || root.getRedirect() != teleport)) return;

        ServerPlayer player = chat.player();
        CommandSourceStack source = context.getSource();
        ServerLevel level = player.serverLevel();
        if (source.getEntity() != player || source.getLevel() != level || !source.hasPermission(2)
                || !source.getServer().isSameThread() || !player.hasPermissions(2)) return;
        var target = root == teleport ? context : context.getChild();
        if (target == null || target.getChild() != null || target.getCommand() == null) return;
        var arguments = target.getArguments();
        if (!arguments.containsKey("location") || arguments.containsKey("facingEntity")
                || arguments.containsKey("facingLocation")) return;
        var command = target.build(parsed.getReader().getString()).copyFor(source);
        if (arguments.containsKey("targets")) {
            try {
                var entities = EntityArgument.getEntities(command, "targets");
                if (entities.size() != 1 || entities.iterator().next() != player) return;
            } catch (CommandSyntaxException exception) {
                return;
            }
        }

        cancel(player);
        event.setCanceled(true);
        Coordinates location = command.getArgument("location", Coordinates.class);
        Vec3 position = location.getPosition(source);
        var teleportEvent = ForgeEventFactory.onEntityTeleportCommand(player, position.x, position.y, position.z);
        if (teleportEvent.isCanceled()) return;
        position = teleportEvent.getTarget();
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)
                || !Level.isInSpawnableBounds(BlockPos.containing(position))) {
            source.sendFailure(Component.translatable("commands.teleport.invalidPosition"));
            return;
        }

        Coordinates rotation = arguments.containsKey("rotation")
                ? command.getArgument("rotation", Coordinates.class) : null;
        Vec2 angles = rotation != null ? rotation.getRotation(source)
                : arguments.containsKey("targets") ? new Vec2(player.getXRot(), player.getYRot()) : source.getRotation();
        Set<RelativeMovement> flags = EnumSet.noneOf(RelativeMovement.class);
        if (location.isXRelative()) flags.add(RelativeMovement.X);
        if (location.isYRelative()) flags.add(RelativeMovement.Y);
        if (location.isZRelative()) flags.add(RelativeMovement.Z);
        if (rotation == null || rotation.isXRelative()) flags.add(RelativeMovement.X_ROT);
        if (rotation == null || rotation.isYRelative()) flags.add(RelativeMovement.Y_ROT);

        ChunkRange chunks = ChunkRange.covering(destinationBox(player, position));
        if (!chunks.bounded()) {
            source.sendFailure(Component.translatable("jem.command.teleport.unavailable"));
            return;
        }
        Pending request = new Pending(player, level, source, position, angles, flags, chunks, System.nanoTime());
        if (!request.active(source.getServer())) return;
        if (chunks.ready(level.getChunkSource())) {
            request.finish();
            return;
        }

        var pending = PENDING.computeIfAbsent(source.getServer(), server -> new HashMap<>());
        if (pending.size() >= MAX_PENDING) {
            source.sendFailure(Component.translatable("jem.command.teleport.unavailable"));
            return;
        }
        chunks.ticket(level.getChunkSource(), player.getId(), true);
        pending.put(player.getUUID(), request);
        source.sendSuccess(() -> Component.translatable("jem.command.teleport.preparing"), false);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var pending = PENDING.get(event.getServer());
        if (pending == null) return;
        for (Pending request : List.copyOf(pending.values())) {
            UUID playerId = request.player().getUUID();
            if (pending.get(playerId) != request) continue;
            try {
                if (!request.active(event.getServer())) {
                    pending.remove(playerId);
                    request.release();
                } else if (System.nanoTime() - request.created() >= TIMEOUT_NANOS) {
                    pending.remove(playerId);
                    request.release();
                    request.source().sendFailure(Component.translatable("jem.command.teleport.timeout"));
                } else if (request.chunks().ready(request.level().getChunkSource())) {
                    pending.remove(playerId);
                    try {
                        request.finish();
                    } finally {
                        request.release();
                    }
                }
            } catch (RuntimeException exception) {
                if (pending.remove(playerId, request)) request.release();
                LOGGER.error("Deferred player teleport failed", exception);
                request.source().sendFailure(Component.translatable("jem.command.teleport.unavailable"));
            }
        }
        if (pending.isEmpty()) PENDING.remove(event.getServer(), pending);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        var pending = PENDING.remove(event.getServer());
        if (pending != null) pending.values().forEach(Pending::release);
    }

    public static void cancel(ServerPlayer player) {
        var pending = PENDING.get(player.server);
        if (pending == null) return;
        Pending previous = pending.remove(player.getUUID());
        if (previous != null) previous.release();
    }

    private static AABB destinationBox(ServerPlayer player, Vec3 position) {
        return player.getDimensions(Pose.STANDING).makeBoundingBox(position)
                .minmax(player.getBoundingBox().move(position.subtract(player.position())));
    }

    private record ChatCommand(ParseResults<CommandSourceStack> parsed, ServerPlayer player) {}

    private record Pending(ServerPlayer player, ServerLevel level, CommandSourceStack source, Vec3 position,
                           Vec2 rotation, Set<RelativeMovement> flags, ChunkRange chunks, long created) {
        boolean active(MinecraftServer server) {
            return player.serverLevel() == level && player.isAlive() && player.connection.isAcceptingMessages()
                    && server.getPlayerList().getPlayer(player.getUUID()) == player && player.hasPermissions(2);
        }

        void release() {
            chunks.ticket(level.getChunkSource(), player.getId(), false);
        }

        void finish() {
            if (!chunks.contains(ChunkRange.covering(destinationBox(player, position)))) {
                source.sendFailure(Component.translatable("jem.command.teleport.unavailable"));
                return;
            }
            if (!player.teleportTo(level, position.x, position.y, position.z, flags,
                    Mth.wrapDegrees(rotation.y), Mth.wrapDegrees(rotation.x))) {
                source.sendFailure(Component.translatable("jem.command.teleport.unavailable"));
                return;
            }
            if (!player.isFallFlying()) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                player.setOnGround(true);
            }
            source.sendSuccess(() -> Component.translatable("commands.teleport.success.location.single",
                    player.getDisplayName(), coordinate(position.x), coordinate(position.y), coordinate(position.z)), true);
        }
    }

    private static String coordinate(double value) {
        return String.format(Locale.ROOT, "%f", value);
    }

    private record ChunkRange(int minX, int maxX, int minZ, int maxZ) {
        static ChunkRange covering(AABB box) {
            // BlockCollisions checks an extra block beyond the entity bounds.
            return new ChunkRange((Mth.floor(box.minX - 1.0E-7) - 1) >> 4,
                    (Mth.floor(box.maxX + 1.0E-7) + 1) >> 4,
                    (Mth.floor(box.minZ - 1.0E-7) - 1) >> 4,
                    (Mth.floor(box.maxZ + 1.0E-7) + 1) >> 4);
        }

        boolean bounded() {
            long count = (maxX - minX + 1L) * (maxZ - minZ + 1L);
            return count > 0 && count <= MAX_CHUNKS;
        }

        boolean contains(ChunkRange other) {
            return minX <= other.minX && maxX >= other.maxX && minZ <= other.minZ && maxZ >= other.maxZ;
        }

        boolean ready(ServerChunkCache cache) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (cache.getChunkNow(x, z) == null) return false;
                }
            }
            return true;
        }

        void ticket(ServerChunkCache cache, int playerId, boolean add) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    ChunkPos chunk = new ChunkPos(x, z);
                    if (add) cache.addRegionTicket(TICKET, chunk, 0, playerId);
                    else cache.removeRegionTicket(TICKET, chunk, 0, playerId);
                }
            }
        }
    }
}
