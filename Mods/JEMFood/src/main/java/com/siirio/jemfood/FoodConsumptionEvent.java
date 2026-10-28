package com.siirio.jemfood;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** Read-only consumption result after the native blessing and variety state have been saved. */
public final class FoodConsumptionEvent extends Event {
    private final ServerPlayer player;
    private final ResourceLocation previous;
    private final ResourceLocation current;
    private final boolean admittedBundle;
    private final boolean varietyActive;

    public FoodConsumptionEvent(ServerPlayer player, ResourceLocation previous, ResourceLocation current,
                                boolean admittedBundle, boolean varietyActive) {
        this.player = player;
        this.previous = previous;
        this.current = current;
        this.admittedBundle = admittedBundle;
        this.varietyActive = varietyActive;
    }

    public ServerPlayer player() { return player; }
    public java.util.Optional<ResourceLocation> previous() { return java.util.Optional.ofNullable(previous); }
    public ResourceLocation current() { return current; }
    public boolean admittedBundle() { return admittedBundle; }
    public boolean varietyActive() { return varietyActive; }
}
