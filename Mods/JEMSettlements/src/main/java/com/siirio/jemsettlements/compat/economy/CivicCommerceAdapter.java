package com.siirio.jemsettlements.compat.economy;

import com.siirio.jemeconomy.sandbox.SandboxCommerce;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CivicCommerceAdapter {
    private CivicCommerceAdapter() { }
    public static boolean active(MinecraftServer server) { return com.siirio.jemeconomy.api.EconomyApi.isEnabled(server) && SandboxCommerce.active(server); }
    public static void releaseFunds(ServerPlayer player, UUID town) { SandboxCommerce.releaseFunds(player, town); }
    public static boolean purposeFundsOutstanding(MinecraftServer server, UUID town) { return SandboxCommerce.purposeFundsOutstanding(server, town); }
    public static void flushOwner(MinecraftServer server) { SandboxCommerce.flushOwner(server); }
    public static boolean fund(ServerPlayer player, UUID town, String purpose, long price, UUID request, boolean refund) { return SandboxCommerce.fund(player, town, purpose, price, request, refund); }
    public static boolean buyWithFund(ServerPlayer player, UUID town, String purpose, UUID listing) { return SandboxCommerce.buyWithFund(player, town, purpose, listing); }
    public static List<CompoundTag> fundRows(ServerPlayer player, UUID town) { return SandboxCommerce.fundRows(player, town); }
    public static CompoundTag snapshot(ServerPlayer player, String node) { return SandboxCommerce.snapshot(player, node); }
}
