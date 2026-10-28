package com.justenoughmods.achievementguide.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.items.IItemHandler;

/** Actual native terminal result, matching stock debit and personal inventory/cursor receipt. */
public final class NativeTerminalExtractionEvent extends Event {
    private final ServerPlayer actor;
    private final IItemHandler network;
    private final ItemStack item;
    private final long quantity;
    private final long stockBefore;
    public NativeTerminalExtractionEvent(ServerPlayer actor, IItemHandler network, ItemStack item, long quantity, long stockBefore) {
        this.actor = actor; this.network = network; this.item = item.copyWithCount(1); this.quantity = quantity;
        this.stockBefore = stockBefore;
    }
    public ServerPlayer actor() { return actor; }
    public IItemHandler network() { return network; }
    public ItemStack item() { return item.copy(); }
    public long quantity() { return quantity; }
    public long stockBefore() { return stockBefore; }
    public long stockAfter() { return stockBefore - quantity; }
}
