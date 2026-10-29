package com.siirio.jemsecurity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SecurityAuditLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("JEM Security");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final ArrayBlockingQueue<Write> WRITES = new ArrayBlockingQueue<>(16_384);
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static final AtomicLong DROPPED = new AtomicLong();
    private static final ZoneId AUDIT_ZONE = ZoneId.of("Asia/Yekaterinburg");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(AUDIT_ZONE);
    private static final int MAX_DETAIL_CHARS = 4096;
    private static final int MAX_HISTORY_BYTES = 512 * 1024;
    private static volatile Thread writerThread;

    private SecurityAuditLog() { }

    public static JsonObject details(String key, String value) {
        JsonObject details = new JsonObject();
        details.addProperty(key, bounded(value));
        return details;
    }

    public static void record(ServerPlayer player, String event, JsonObject details) {
        record(player.server, player.getUUID(), player.getGameProfile().getName(), event, details);
    }

    public static void record(MinecraftServer server, UUID playerId, String playerName, String event, JsonObject details) {
        if (!SecurityRuntime.active(server)) return;
        JsonObject line = new JsonObject();
        line.addProperty("timestamp", Instant.now().toString());
        line.addProperty("timezone", AUDIT_ZONE.getId());
        line.addProperty("playerUuid", playerId.toString());
        line.addProperty("playerName", bounded(playerName));
        line.addProperty("event", bounded(event));
        line.add("details", details == null ? new JsonObject() : details);
        long dropped = DROPPED.getAndSet(0L);
        if (dropped > 0) line.addProperty("droppedBefore", dropped);
        enqueue(new Write(auditDirectory(server).resolve(playerId + ".jsonl"),
                GSON.toJson(line) + System.lineSeparator(), true));
    }

    public static List<String> recent(MinecraftServer server, UUID playerId, int limit) {
        Path file = auditDirectory(server).resolve(playerId + ".jsonl");
        if (!Files.isRegularFile(file)) return List.of();
        try (RandomAccessFile input = new RandomAccessFile(file.toFile(), "r")) {
            long length = input.length();
            int size = (int) Math.min(length, MAX_HISTORY_BYTES);
            byte[] bytes = new byte[size];
            input.seek(length - size);
            input.readFully(bytes);
            List<String> lines = new String(bytes, StandardCharsets.UTF_8).lines().toList();
            int from = Math.max(0, lines.size() - Math.max(1, Math.min(limit, 100)));
            return new ArrayList<>(lines.subList(from, lines.size()));
        } catch (java.io.IOException exception) {
            LOGGER.error("Could not read security audit for {}", playerId, exception);
            return List.of();
        }
    }

    public static Path saveClientLog(ServerPlayer player, String text) {
        Path directory = root(player.server).resolve("client-logs").resolve(player.getUUID().toString());
        Path file = directory.resolve(FILE_TIME.format(Instant.now()) + "-latest.log.txt");
        enqueue(new Write(file, text, false));
        return file;
    }

    public static void stop() {
        RUNNING.set(false);
        Thread thread = writerThread;
        if (thread == null) return;
        thread.interrupt();
        try {
            thread.join(2_000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        if (!WRITES.isEmpty()) LOGGER.warn("JEM Security stopped with {} queued audit writes", WRITES.size());
    }

    private static Path auditDirectory(MinecraftServer server) {
        return root(server).resolve("audit");
    }

    private static Path root(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("jem-security");
    }

    private static String bounded(String value) {
        if (value == null) return "";
        return value.length() <= MAX_DETAIL_CHARS ? value : value.substring(0, MAX_DETAIL_CHARS);
    }

    private static void enqueue(Write write) {
        ensureWriter();
        if (WRITES.offer(write)) return;
        long dropped = DROPPED.incrementAndGet();
        if (dropped == 1) LOGGER.error("JEM Security audit queue is full. Events will be counted until the writer catches up");
    }

    private static void ensureWriter() {
        if (RUNNING.get()) return;
        synchronized (SecurityAuditLog.class) {
            if (RUNNING.get()) return;
            RUNNING.set(true);
            writerThread = new Thread(SecurityAuditLog::writeLoop, "JEM-Security-Audit");
            writerThread.setDaemon(true);
            writerThread.start();
        }
    }

    private static void writeLoop() {
        while (RUNNING.get() || !WRITES.isEmpty()) {
            try {
                Write write = WRITES.poll(1, TimeUnit.SECONDS);
                if (write == null) continue;
                Files.createDirectories(write.file().getParent());
                if (write.append()) {
                    Files.writeString(write.file(), write.text(), StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                } else {
                    Files.writeString(write.file(), write.text(), StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                }
            } catch (InterruptedException exception) {
                if (RUNNING.get()) Thread.currentThread().interrupt();
            } catch (java.io.IOException exception) {
                LOGGER.error("Could not persist a JEM Security audit write", exception);
            }
        }
    }

    private record Write(Path file, String text, boolean append) { }
}
