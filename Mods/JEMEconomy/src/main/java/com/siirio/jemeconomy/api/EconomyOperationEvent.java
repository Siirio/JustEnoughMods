package com.siirio.jemeconomy.api;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

/** Notification of a committed operation; never a request to move money or deliver goods. */
public final class EconomyOperationEvent extends Event {
    public enum Kind { SHIPPING_SALE, WITHDRAWAL, VILLAGER_PURCHASE, MARKET_PURCHASE }
    private final MinecraftServer server;
    private final UUID player;
    private final UUID operation;
    private final Kind kind;
    private final ItemStack item;

    public EconomyOperationEvent(MinecraftServer server, UUID player, UUID operation, Kind kind, ItemStack item) {
        this.server = server;
        this.player = player;
        this.operation = operation;
        this.kind = kind;
        this.item = item.copy();
    }
    public MinecraftServer server() { return server; }
    public UUID player() { return player; }
    public UUID operation() { return operation; }
    public Kind kind() { return kind; }
    public ItemStack item() { return item.copy(); }
}
