package com.justenoughmods.achievementguide.compat.ftb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Personal, paginated archive proposals. Every issuance revalidates the frozen source on the server. */
public final class PastRewardsNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("jem_advancements", "past_rewards"), () -> "1", "1"::equals, "1"::equals);
    private static final Map<ServerPlayer, Integer> LAST = new WeakHashMap<>();
    private static boolean registered;
    private PastRewardsNetwork() {}

    /** The user confirmed the quest book has never been used. Historical claims are not part of V4. */
    public static boolean enabled() { return false; }

    static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, Request.class, Request::encode, Request::decode, Request::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, Snapshot.class, Snapshot::encode, Snapshot::decode, Snapshot::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void request(int offset) { if (enabled()) CHANNEL.sendToServer(new Request(offset, null, 0, 0, 0)); }
    static void claim(BookMigration.Claim claim, int offset) { if (enabled()) CHANNEL.sendToServer(new Request(offset, claim.origin(), claim.quest(), claim.parent(), claim.option())); }

    private static void send(ServerPlayer player, int offset) {
        List<Row> rows = new ArrayList<>();
        for (BookMigration.Claim claim : BookMigration.pending(player, offset)) {
            ItemStack compensation = ItemStack.EMPTY;
            if (claim.compensation() && claim.available()) {
                var reward = dev.ftb.mods.ftbquests.quest.ServerQuestFile.INSTANCE.getReward(claim.parent());
                if (reward instanceof BookItemReward item) compensation = BookMigration.delivery(player, item).get(0);
            }
            rows.add(new Row(claim, compensation));
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(offset, List.copyOf(rows)));
    }

    public record Row(BookMigration.Claim claim, ItemStack compensation) {}
    public record Snapshot(int offset, List<Row> rows) {
        private static void encode(Snapshot message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.offset);
            buffer.writeVarInt(message.rows.size());
            for (Row row : message.rows) {
                BookMigration.Claim claim = row.claim;
                buffer.writeUUID(claim.origin()); buffer.writeLong(claim.quest()); buffer.writeLong(claim.parent()); buffer.writeLong(claim.option());
                buffer.writeBoolean(claim.available()); buffer.writeBoolean(claim.compensation()); buffer.writeItem(row.compensation);
            }
        }
        private static Snapshot decode(FriendlyByteBuf buffer) {
            int offset = buffer.readVarInt(), count = buffer.readVarInt();
            if (offset < 0 || offset > 100000 || count < 0 || count > 64) throw new IllegalArgumentException("Invalid archive page");
            List<Row> rows = new ArrayList<>(count);
            for (int i = 0; i < count; i++) rows.add(new Row(new BookMigration.Claim(buffer.readUUID(), buffer.readLong(), buffer.readLong(), buffer.readLong(), buffer.readBoolean(), buffer.readBoolean()), buffer.readItem()));
            return new Snapshot(offset, List.copyOf(rows));
        }
        private static void handle(Snapshot message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PastRewardsScreen.open(message)));
            context.setPacketHandled(true);
        }
    }

    private record Request(int offset, UUID origin, long quest, long parent, long option) {
        private static void encode(Request message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.offset); buffer.writeBoolean(message.origin != null);
            if (message.origin != null) { buffer.writeUUID(message.origin); buffer.writeLong(message.quest); buffer.writeLong(message.parent); buffer.writeLong(message.option); }
        }
        private static Request decode(FriendlyByteBuf buffer) {
            int offset = buffer.readVarInt();
            return buffer.readBoolean() ? new Request(offset, buffer.readUUID(), buffer.readLong(), buffer.readLong(), buffer.readLong()) : new Request(offset, null, 0, 0, 0);
        }
        private static void handle(Request message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (!enabled() || player == null || message.offset < 0 || message.offset > 100000) return;
                Integer last = LAST.get(player);
                if (last != null && player.tickCount - last < 5) return;
                LAST.put(player, player.tickCount);
                try {
                    if (message.origin != null) BookMigration.claim(player, message.origin, message.quest, message.parent, message.option);
                } catch (RuntimeException failure) {
                    com.mojang.logging.LogUtils.getLogger().error("Original quest reward remains pending for {}", player.getUUID(), failure);
                }
                send(player, message.offset);
            });
            context.setPacketHandled(true);
        }
    }
}
