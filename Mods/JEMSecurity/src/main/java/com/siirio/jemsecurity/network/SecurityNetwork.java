package com.siirio.jemsecurity.network;

import com.google.gson.JsonObject;
import com.siirio.jemsecurity.SecurityAuditLog;
import com.siirio.jemsecurity.SecurityConfig;
import com.siirio.jemsecurity.SecurityRuntime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SecurityNetwork {
    private static final String PROTOCOL = "1";
    private static final int MAX_MODS = 512;
    private static final int MAX_PACKS = 128;
    private static final int MAX_LOG_CHUNKS = 8;
    private static final int MAX_LOG_CHARS = 131_072;
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation("jem_security", "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();
    private static final Map<UUID, Long> EXPECTED = new HashMap<>();
    private static final Map<UUID, AttestationState> ATTESTED = new HashMap<>();
    private static final Map<UUID, UUID> LOG_REQUESTS = new HashMap<>();
    private static final Map<UUID, LogAssembly> LOG_ASSEMBLIES = new HashMap<>();

    private SecurityNetwork() { }

    public record Attestation(List<String> mods, List<String> packs, String clientVersion) { }
    public record LogRequest(UUID token) { }
    public record LogChunk(UUID token, int index, int total, String text) { }
    public record AttestationState(long receivedAt, List<String> mods, List<String> packs) { }

    public static void register() {
        CHANNEL.messageBuilder(Attestation.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {
                    buffer.writeCollection(packet.mods(), (out, value) -> out.writeUtf(value, 128));
                    buffer.writeCollection(packet.packs(), (out, value) -> out.writeUtf(value, 256));
                    buffer.writeUtf(packet.clientVersion(), 32);
                })
                .decoder(buffer -> new Attestation(
                        buffer.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, MAX_MODS), in -> in.readUtf(128)),
                        buffer.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, MAX_PACKS), in -> in.readUtf(256)),
                        buffer.readUtf(32)))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) accept(player, packet);
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(LogRequest.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> buffer.writeUUID(packet.token()))
                .decoder(buffer -> new LogRequest(buffer.readUUID()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SecurityClient.receiveLogRequest(packet.token()));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(LogChunk.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {
                    buffer.writeUUID(packet.token());
                    buffer.writeVarInt(packet.index());
                    buffer.writeVarInt(packet.total());
                    buffer.writeUtf(packet.text(), 20_000);
                })
                .decoder(buffer -> new LogChunk(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(20_000)))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) acceptLogChunk(player, packet);
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void sendAttestation(List<String> mods, List<String> packs, String clientVersion) {
        CHANNEL.sendToServer(new Attestation(List.copyOf(mods), List.copyOf(packs), clientVersion));
    }

    public static void sendLogChunk(UUID token, int index, int total, String text) {
        CHANNEL.sendToServer(new LogChunk(token, index, total, text));
    }

    public static void expect(ServerPlayer player) {
        if (!SecurityConfig.REQUIRE_ATTESTATION.get() || ATTESTED.containsKey(player.getUUID())) return;
        EXPECTED.put(player.getUUID(), (long) player.server.getTickCount() + SecurityConfig.ATTESTATION_TIMEOUT_SECONDS.get() * 20L);
    }

    public static void tick(MinecraftServer server) {
        if (!SecurityRuntime.active(server) || !SecurityConfig.REQUIRE_ATTESTATION.get()) return;
        long now = server.getTickCount();
        for (var entry : Map.copyOf(EXPECTED).entrySet()) {
            if (entry.getValue() > now) continue;
            EXPECTED.remove(entry.getKey());
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || ATTESTED.containsKey(entry.getKey())) continue;
            SecurityAuditLog.record(player, "client_attestation_timeout", new JsonObject());
            player.connection.disconnect(Component.translatable("jem_security.disconnect.attestation_timeout"));
        }
    }

    public static AttestationState state(UUID playerId) {
        return ATTESTED.get(playerId);
    }

    public static void requestLog(ServerPlayer player) {
        UUID token = UUID.randomUUID();
        LOG_REQUESTS.put(player.getUUID(), token);
        LOG_ASSEMBLIES.remove(player.getUUID());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new LogRequest(token));
        SecurityAuditLog.record(player, "client_log_requested", SecurityAuditLog.details("token", token.toString()));
    }

    public static void clear(UUID playerId) {
        EXPECTED.remove(playerId);
        ATTESTED.remove(playerId);
        LOG_REQUESTS.remove(playerId);
        LOG_ASSEMBLIES.remove(playerId);
    }

    public static void reset() {
        EXPECTED.clear();
        ATTESTED.clear();
        LOG_REQUESTS.clear();
        LOG_ASSEMBLIES.clear();
    }

    private static void accept(ServerPlayer player, Attestation packet) {
        if (!SecurityRuntime.active(player.server)) return;
        Set<String> mods = new HashSet<>();
        packet.mods().stream().map(value -> value.toLowerCase(Locale.ROOT)).limit(MAX_MODS).forEach(mods::add);
        List<String> packs = packet.packs().stream().map(value -> value.toLowerCase(Locale.ROOT)).limit(MAX_PACKS).toList();
        String bannedMod = mods.stream().filter(SecurityConfig.bannedMods()::contains).sorted().findFirst().orElse(null);
        if (bannedMod != null && !bannedMod.equals("jem_freecam")) {
            SecurityAuditLog.record(player, "banned_client_mod", SecurityAuditLog.details("modId", bannedMod));
            player.connection.disconnect(Component.translatable("jem_security.disconnect.banned_mod", bannedMod));
            return;
        }
        String bannedPack = packs.stream().filter(pack -> SecurityConfig.bannedPackTokens().stream().anyMatch(pack::contains)).findFirst().orElse(null);
        if (bannedPack != null) {
            SecurityAuditLog.record(player, "banned_resource_pack", SecurityAuditLog.details("packId", bannedPack));
            player.connection.disconnect(Component.translatable("jem_security.disconnect.banned_pack", bannedPack));
            return;
        }
        EXPECTED.remove(player.getUUID());
        ATTESTED.put(player.getUUID(), new AttestationState(System.currentTimeMillis(), List.copyOf(mods), packs));
        JsonObject details = new JsonObject();
        details.addProperty("clientVersion", packet.clientVersion());
        details.addProperty("modCount", mods.size());
        details.addProperty("resourcePackCount", packs.size());
        details.addProperty("jemFreecamAllowed", mods.contains("jem_freecam"));
        SecurityAuditLog.record(player, "client_attested", details);
    }

    private static void acceptLogChunk(ServerPlayer player, LogChunk packet) {
        UUID expected = LOG_REQUESTS.get(player.getUUID());
        if (expected == null || !expected.equals(packet.token()) || packet.total() < 1 || packet.total() > MAX_LOG_CHUNKS
                || packet.index() < 0 || packet.index() >= packet.total() || packet.text().length() > 20_000) return;
        LogAssembly assembly = LOG_ASSEMBLIES.computeIfAbsent(player.getUUID(), ignored -> new LogAssembly(packet.token(), packet.total()));
        if (!assembly.token.equals(packet.token()) || assembly.total != packet.total()) return;
        assembly.chunks.putIfAbsent(packet.index(), packet.text());
        int chars = assembly.chunks.values().stream().mapToInt(String::length).sum();
        if (chars > MAX_LOG_CHARS) {
            LOG_ASSEMBLIES.remove(player.getUUID());
            LOG_REQUESTS.remove(player.getUUID());
            SecurityAuditLog.record(player, "client_log_rejected", SecurityAuditLog.details("reason", "size_limit"));
            return;
        }
        if (assembly.chunks.size() != assembly.total) return;
        StringBuilder joined = new StringBuilder(chars);
        for (int index = 0; index < assembly.total; index++) {
            String chunk = assembly.chunks.get(index);
            if (chunk == null) return;
            joined.append(chunk);
        }
        LOG_ASSEMBLIES.remove(player.getUUID());
        LOG_REQUESTS.remove(player.getUUID());
        var path = SecurityAuditLog.saveClientLog(player, joined.toString());
        SecurityAuditLog.record(player, "client_log_received", SecurityAuditLog.details("file", path.getFileName().toString()));
    }

    private static final class LogAssembly {
        private final UUID token;
        private final int total;
        private final Map<Integer, String> chunks = new HashMap<>();

        private LogAssembly(UUID token, int total) {
            this.token = token;
            this.total = total;
        }
    }
}
