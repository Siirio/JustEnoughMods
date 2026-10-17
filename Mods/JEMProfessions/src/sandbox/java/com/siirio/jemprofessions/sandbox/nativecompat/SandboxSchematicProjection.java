package com.siirio.jemprofessions.sandbox.nativecompat;

import com.siirio.jemprofessions.sandbox.SandboxProfessionSupport;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.zip.GZIPInputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Server-created template payloads only; clients can acknowledge a nonce, never submit a filename or file. */
public final class SandboxSchematicProjection {
    static final int MAX_BYTES = 524288;
    private static SimpleChannel channel;
    private static final Map<ServerPlayer, Pending> PENDING = new WeakHashMap<>();
    private SandboxSchematicProjection() {}

    static void register(SimpleChannel nativeChannel) {
        channel = nativeChannel;
        channel.messageBuilder(Projection.class, 40, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Projection::encode).decoder(Projection::decode).consumerMainThread((packet, context) -> {
                    if (ModList.get().isLoaded("create")) DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> com.siirio.jemprofessions.sandbox.nativecompat.client.SandboxSchematicClient.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
        channel.messageBuilder(Reply.class, 41, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Reply::encode).decoder(Reply::decode).consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    Pending pending = PENDING.get(player);
                    if (player != null && pending != null && pending.nonce.equals(packet.nonce)) {
                        PENDING.remove(player);
                        if (ModList.get().isLoaded("create"))
                            SandboxEngineerEffects.projectionReady(player, pending.slot, pending.version,
                                    packet.success && pending.until >= player.serverLevel().getGameTime() && SandboxProfessionSupport.allowed(player));
                    }
                    context.get().setPacketHandled(true);
                }).add();
    }

    static boolean send(ServerPlayer player, int slot, UUID version, CompoundTag data) {
        validate(data);
        if (channel == null || !channel.isRemotePresent(player.connection.connection)) return false;
        Pending existing = PENDING.get(player);
        if (existing != null && existing.until >= player.serverLevel().getGameTime()) return false;
        UUID nonce = UUID.randomUUID();
        PENDING.put(player, new Pending(nonce, slot, version, player.serverLevel().getGameTime() + 200));
        channel.send(PacketDistributor.PLAYER.with(() -> player), new Projection(nonce, player.getUUID(), slot, version, data.copy()));
        return true;
    }

    static void tick(ServerPlayer player) {
        Pending pending = PENDING.get(player);
        if (pending != null && pending.until < player.serverLevel().getGameTime()) {
            PENDING.remove(player);
            if (ModList.get().isLoaded("create")) SandboxEngineerEffects.projectionReady(player, pending.slot, pending.version, false);
        }
    }

    static void logout(ServerPlayer player) { PENDING.remove(player); }

    public static void reply(UUID nonce, boolean success) { channel.sendToServer(new Reply(nonce, success)); }

    public static String filename(UUID owner, int slot, UUID version) {
        if (slot < 1 || slot > 3) throw new IllegalArgumentException("Invalid template slot");
        return "jem-profession-" + owner + "-" + slot + "-" + version + ".nbt";
    }

    static CompoundTag read(Path file) throws IOException {
        if (!Files.isRegularFile(file) || Files.size(file) > MAX_BYTES) throw new IOException("Missing or oversized schematic");
        try (DataInputStream input = new DataInputStream(new GZIPInputStream(Files.newInputStream(file)))) {
            CompoundTag data = NbtIo.read(input, new NbtAccounter(MAX_BYTES));
            validate(data);
            return data;
        }
    }

    public static void write(Path file, CompoundTag data) throws IOException {
        validate(data);
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "jem-template-", ".nbt");
        try {
            try (OutputStream out = Files.newOutputStream(temporary)) { NbtIo.writeCompressed(data, out); }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }

    /** Fixed 7-cube, palette-only format: never entity/container/machine NBT or a fluid state. */
    public static void validate(CompoundTag data) {
        if (data == null || data.sizeInBytes() > MAX_BYTES
                || !Set.of("DataVersion", "size", "palette", "blocks").containsAll(data.getAllKeys()))
            throw new IllegalArgumentException("Unsafe schematic data");
        ListTag size = data.getList("size", Tag.TAG_INT);
        if (size.size() != 3) throw new IllegalArgumentException("Invalid schematic size");
        for (int axis = 0; axis < 3; axis++) if (size.getInt(axis) < 1 || size.getInt(axis) > 7)
            throw new IllegalArgumentException("Schematic exceeds 7-cube limit");
        ListTag palette = data.getList("palette", Tag.TAG_COMPOUND);
        if (palette.isEmpty() || palette.size() > 343) throw new IllegalArgumentException("Invalid schematic palette");
        for (Tag row : palette) {
            CompoundTag state = (CompoundTag) row;
            if (!Set.of("Name", "Properties").containsAll(state.getAllKeys()) || ResourceLocation.tryParse(state.getString("Name")) == null
                    || state.getString("Name").length() > 256 || state.getCompound("Properties").getAllKeys().size() > 32)
                throw new IllegalArgumentException("Unsafe palette entry");
            ResourceLocation id = ResourceLocation.tryParse(state.getString("Name"));
            if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id)
                    || net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id) instanceof net.minecraft.world.level.block.LiquidBlock)
                throw new IllegalArgumentException("Missing block or fluid in schematic");
            for (String key : state.getCompound("Properties").getAllKeys()) {
                if (key.length() > 64 || !state.getCompound("Properties").contains(key, Tag.TAG_STRING)
                        || state.getCompound("Properties").getString(key).length() > 64
                        || key.equals("waterlogged") && !state.getCompound("Properties").getString(key).equals("false"))
                    throw new IllegalArgumentException("Unsafe block property");
            }
        }
        ListTag blocks = data.getList("blocks", Tag.TAG_COMPOUND);
        if (blocks.size() > 343) throw new IllegalArgumentException("Too many schematic blocks");
        Set<Integer> positions = new java.util.HashSet<>();
        for (Tag row : blocks) {
            CompoundTag block = (CompoundTag) row;
            if (!Set.of("pos", "state").containsAll(block.getAllKeys()) || !block.contains("state", Tag.TAG_INT)
                    || block.getInt("state") < 0 || block.getInt("state") >= palette.size())
                throw new IllegalArgumentException("Unsafe schematic block");
            ListTag pos = block.getList("pos", Tag.TAG_INT);
            if (pos.size() != 3) throw new IllegalArgumentException("Invalid block position");
            for (int axis = 0; axis < 3; axis++) if (pos.getInt(axis) < 0 || pos.getInt(axis) >= size.getInt(axis))
                throw new IllegalArgumentException("Block outside schematic");
            if (!positions.add(pos.getInt(0) + 7 * pos.getInt(1) + 49 * pos.getInt(2)))
                throw new IllegalArgumentException("Duplicate schematic position");
        }
    }

    public record Projection(UUID nonce, UUID owner, int slot, UUID version, CompoundTag data) {
        static void encode(Projection packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.nonce); buffer.writeUUID(packet.owner); buffer.writeVarInt(packet.slot);
            buffer.writeUUID(packet.version); buffer.writeNbt(packet.data);
        }
        static Projection decode(FriendlyByteBuf buffer) {
            UUID nonce = buffer.readUUID(), owner = buffer.readUUID(); int slot = buffer.readVarInt(); UUID version = buffer.readUUID();
            CompoundTag data = buffer.readNbt(new NbtAccounter(MAX_BYTES));
            if (slot < 1 || slot > 3) throw new IllegalArgumentException("Invalid template slot");
            validate(data);
            return new Projection(nonce, owner, slot, version, data);
        }
    }
    private record Reply(UUID nonce, boolean success) {
        static void encode(Reply packet, FriendlyByteBuf buffer) { buffer.writeUUID(packet.nonce); buffer.writeBoolean(packet.success); }
        static Reply decode(FriendlyByteBuf buffer) { return new Reply(buffer.readUUID(), buffer.readBoolean()); }
    }
    private record Pending(UUID nonce, int slot, UUID version, long until) {}
}
