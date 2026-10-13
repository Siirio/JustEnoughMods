package com.siirio.jemsettlements.network;

import com.siirio.jemsettlements.JemSettlements;
import com.siirio.jemsettlements.api.SettlementApi;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import com.siirio.jemidentity.api.DedicatedSmp;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SettlementNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(JemSettlements.MOD_ID, "town"))
            .networkProtocolVersion(() -> VERSION).clientAcceptedVersions(VERSION::equals).serverAcceptedVersions(VERSION::equals).simpleChannel();
    private SettlementNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(TownFoundPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TownFoundPacket::encode).decoder(TownFoundPacket::decode).consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && DedicatedSmp.active(player)) {
                        SettlementApi.Result result = SettlementApi.create(player, packet.name(), packet.bell());
                        player.sendSystemMessage(Component.translatable(result == SettlementApi.Result.ACCEPTED
                                ? "jem.settlements.created" : "jem.settlements.found_failed", packet.name()), true);
                        sendOwned(player, true);
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(TownViewPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TownViewPacket::encode).decoder(TownViewPacket::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.siirio.jemsettlements.client.SettlementClientState.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(TreasuryActionPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TreasuryActionPacket::encode).decoder(TreasuryActionPacket::decode).consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && DedicatedSmp.active(player)) {
                        SettlementApi.Result result = SettlementApi.treasury(player, packet.deposit(), packet.amount(), packet.requestId());
                        player.sendSystemMessage(Component.translatable("jem.settlements.treasury." + result.name().toLowerCase(java.util.Locale.ROOT), packet.amount()), true);
                        sendOwned(player, true);
                    }
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void found(String name, net.minecraft.core.BlockPos bell) { CHANNEL.sendToServer(new TownFoundPacket(name, bell)); }
    public static void treasury(boolean deposit, long amount) { CHANNEL.sendToServer(new TreasuryActionPacket(deposit, amount, java.util.UUID.randomUUID())); }

    public static void sendFounding(ServerPlayer player, net.minecraft.core.BlockPos bell) {
        CompoundTag view = new CompoundTag(); view.putBoolean("Founding", true); view.putLong("Bell", bell.asLong());
        send(player, new TownViewPacket(true, view));
    }

    public static void sendOwned(ServerPlayer player, boolean open) {
        var town = SettlementApi.ownedTown(player);
        if (town.isEmpty()) { CompoundTag view = new CompoundTag(); view.putBoolean("NoTown", true); send(player, new TownViewPacket(open, view)); return; }
        send(player, new TownViewPacket(open, buildView(player, town.get())));
    }

    private static CompoundTag buildView(ServerPlayer player, TownRecord town) {
        CompoundTag view = new CompoundTag(); view.putUUID("Id", town.id()); view.putString("Name", town.name());
        view.putInt("Happiness", town.happiness()); view.putInt("Tax", town.taxPercent()); view.putLong("Center", town.center().asLong());
        view.putLong("Treasury", SettlementApi.treasuryBalance(player, town));
        view.putString("Dimension", town.dimension().toString()); view.putInt("StorageLinks", town.storage().size());
        var data = SettlementSavedData.get(player.getServer());
        CompoundTag workforce = new CompoundTag();
        data.workforce(town.id()).forEach(record -> workforce.putInt(record.type().name(), workforce.getInt(record.type().name()) + 1));
        view.put("Workforce", workforce);
        Map<String, Long> stock = new LinkedHashMap<>(); int loaded = 0; long food = 0;
        for (var link : town.storage()) {
            ServerLevel level = player.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, link.dimension()));
            if (level == null || !level.hasChunkAt(link.position())) continue;
            var blockEntity = level.getBlockEntity(link.position()); if (blockEntity == null) continue;
            var optional = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve(); if (optional.isEmpty()) continue;
            loaded++;
            var handler = optional.get();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot); if (stack.isEmpty()) continue;
                String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); stock.merge(key, (long) stack.getCount(), Long::sum);
                if (stack.getItem().getFoodProperties(stack, null) != null) food += stack.getCount();
            }
        }
        view.putInt("LoadedStorage", loaded); view.putLong("FoodUnits", food);
        ListTag items = new ListTag(); stock.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(32).forEach(entry -> {
            CompoundTag row = new CompoundTag(); row.putString("Item", entry.getKey()); row.putLong("Count", entry.getValue()); items.add(row);
        }); view.put("Stock", items);
        return view;
    }

    private static void send(ServerPlayer player, TownViewPacket packet) {
        if (DedicatedSmp.active(player) && CHANNEL.isRemotePresent(player.connection.connection)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
