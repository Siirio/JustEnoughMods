package com.siirio.jemsettlements.api;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** Post-commit observation, with all admission and mutation retained by the native domain owner. */
public final class SettlementOperationEvent extends Event {
    public enum Kind { TERRITORY_EXPANDED, FOOD_STORAGE_ENABLED, RESIDENT_PAT, COMFORT_CONFIRMED,
        ROOF_AND_WORK_CONFIRMED, HAPPINESS_INCOME_COMMITTED, RESIDENT_MEAL }
    private final ServerPlayer player;
    private final UUID town;
    private final Kind kind;

    public SettlementOperationEvent(ServerPlayer player, UUID town, Kind kind) {
        this.player = player;
        this.town = town;
        this.kind = kind;
    }

    public ServerPlayer player() { return player; }
    public UUID town() { return town; }
    public Kind kind() { return kind; }

    public static void publishOwner(net.minecraft.server.MinecraftServer server,
                                    com.siirio.jemsettlements.town.TownRecord town, Kind kind) {
        ServerPlayer owner = server.getPlayerList().getPlayer(town.owner());
        if (owner != null) net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new SettlementOperationEvent(owner, town.id(), kind));
    }
}
