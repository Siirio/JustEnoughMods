package com.siirio.jemidentity.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Read-only, server-scoped binding. Neither a JVM flag nor a client enables test privileges. */
public final class DedicatedTestProfile {
    private static final String SAVE = "jem-test-2026-10-20/world";
    private static final Map<MinecraftServer, Optional<Binding>> BINDINGS = new WeakHashMap<>();

    public record Binding(UUID saveIdentity, String epoch, boolean bypassQuotas) {}

    private DedicatedTestProfile() {}

    public static synchronized Optional<Binding> binding(MinecraftServer server) {
        if (server == null || !server.isDedicatedServer()) return Optional.empty();
        return BINDINGS.computeIfAbsent(server, DedicatedTestProfile::load);
    }

    public static boolean active(MinecraftServer server) { return binding(server).isPresent(); }

    private static Optional<Binding> load(MinecraftServer server) {
        try {
            Path root = server.getServerDirectory().getCanonicalFile().toPath();
            Path marker = root.resolve("config/jem/test-profile.properties");
            if (!Files.exists(marker)) return Optional.empty();
            Properties config = read(marker);
            if (!"test".equals(config.getProperty("profile"))) return Optional.empty();
            if (!SAVE.equals(config.getProperty("save"))) throw new IOException("Unexpected test save path");
            Path expected = root.resolve(SAVE).normalize();
            Path actual = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            if (!actual.equals(expected) || !actual.toRealPath().equals(expected))
                throw new IOException("Test save path or symlink binding mismatch");
            String epoch = config.getProperty("epoch", "");
            if (!epoch.matches("[A-Za-z0-9._-]{1,64}")) throw new IOException("Invalid test epoch");
            UUID identity = UUID.fromString(config.getProperty("saveIdentity", ""));
            Properties saved = read(actual.resolve("jem-test-profile.properties"));
            if (!identity.toString().equals(saved.getProperty("saveIdentity")) || !epoch.equals(saved.getProperty("epoch")))
                throw new IOException("Test save identity or epoch mismatch");
            return Optional.of(new Binding(identity, epoch, Boolean.parseBoolean(config.getProperty("bypassQuotas", "false"))));
        } catch (IOException | IllegalArgumentException failure) {
            throw new IllegalStateException("Refusing invalid dedicated test profile", failure);
        }
    }

    private static Properties read(Path path) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path)) { properties.load(reader); }
        return properties;
    }
}
