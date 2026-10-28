package com.justenoughmods.achievementguide.integration.home;

import java.util.ArrayDeque;
import java.util.Deque;
import me.paulf.fairylights.server.connection.Connection;
import me.paulf.fairylights.server.connection.HangingLightsConnection;
import me.paulf.fairylights.server.fastener.BlockFastener;
import me.paulf.fairylights.server.fastener.Fastener;
import me.paulf.fairylights.server.fastener.FenceFastener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class HomeFairyProof {
    private record Actor(ServerPlayer player) {}
    private static final ThreadLocal<Deque<Actor>> ACTORS = ThreadLocal.withInitial(ArrayDeque::new);

    public static void begin(Player player) {
        if (ACTORS.get().size() >= 16) ACTORS.get().clear();
        ServerPlayer actor = HomeInteractionProof.actor(player);
        ACTORS.get().push(new Actor(actor == HomeItemUseProof.actor() ? actor : null));
    }
    public static void end() { ACTORS.get().poll(); }

    public static void connected(Connection connection, boolean accepted) {
        Actor actor = ACTORS.get().peek();
        if (!accepted || actor == null || actor.player == null || !valid(connection)) return;
        HomeItemUseProof.defer(actor.player, () -> valid(connection),
                () -> ((HomeFairyLineage)connection).jem$homeCreator(actor.player.getUUID()));
    }

    public static void slackened(Connection connection, Player player, float before, float after, boolean accepted) {
        ServerPlayer actor = HomeInteractionProof.actor(player);
        if (accepted && before != after && actor != null && valid(connection)
                && actor.getUUID().equals(((HomeFairyLineage)connection).jem$homeCreator()))
            HomeInteractionProof.emit(player, "v3_home_hanging_lights");
    }

    private static boolean fixed(Fastener<?> fastener) {
        return fastener instanceof BlockFastener || fastener instanceof FenceFastener;
    }

    private static boolean valid(Connection connection) {
        if (!(connection instanceof HangingLightsConnection) || connection.isRemoved()
                || !connection.shouldDrop() || !fixed(connection.getFastener())) return false;
        Fastener<?> destination = connection.getDestination().get(connection.getWorld(), false).orElse(null);
        return destination != null && fixed(destination) && destination != connection.getFastener()
                && !connection.getFastener().createAccessor().equals(destination.createAccessor())
                && connection.getFastener().get(connection.getUUID()).orElse(null) == connection;
    }

    private HomeFairyProof() {}
}
