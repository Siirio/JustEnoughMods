package com.siirio.jemeconomy.api;

import net.minecraft.server.level.ServerPlayer;

/** Implemented by a real open container; CurrencyCore retains all money operations. */
public interface CurrencyContainerAccess {
    boolean permitsCurrencyAction(ServerPlayer player);
}
