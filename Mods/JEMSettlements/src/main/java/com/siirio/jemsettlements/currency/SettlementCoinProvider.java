package com.siirio.jemsettlements.currency;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Optional Coin integration expressed entirely in settlement/native Minecraft types. */
public interface SettlementCoinProvider {
    boolean enabled(MinecraftServer server);
    long balance(ServerPlayer player);
    long held(ServerPlayer player);
    long accountBalance(ServerPlayer player, UUID account);
    long townBalance(MinecraftServer server, UUID town);
    boolean empty(MinecraftServer server, UUID account);
    TransferResult deposit(ServerPlayer player, UUID account, long amount, UUID request);
    TransferResult withdraw(ServerPlayer player, UUID account, long amount, UUID request);
    TransferResult townPayroll(MinecraftServer server, UUID town, long amount, long period);
    TransferResult employerPayroll(MinecraftServer server, UUID owner, long amount, long period);
    TransferResult dailyTax(MinecraftServer server, UUID town, long amount, long day);
    ListTag history(MinecraftServer server, UUID town);
    CompoundTag bulletin(ServerPlayer player);
    Component format(long amount);
}
