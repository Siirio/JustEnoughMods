package dev.taste.engine.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Persists before publication. Failed validation or IO never replaces the active snapshot. */
public final class ConfigStore {
    public enum PreviewStage { NONE, WAITING, VISIBLE, REVERTED, FAILED }
    public record PreviewStatus(PreviewStage stage, int seconds, String error) {
        public boolean pending() { return stage == PreviewStage.WAITING || stage == PreviewStage.VISIBLE; }
    }
    private record Preview(ConfigSnapshot candidate, long eligibleAt, long expiresAt) {
        Preview visible(long now) {
            return new Preview(candidate, eligibleAt,
                    now + candidate.value("general.keepPromptSeconds").getAsLong() * 1_000_000_000L);
        }
    }
    public record ApplyResult(ConfigSnapshot snapshot, List<String> changedIds, Set<SettingSpec.ApplyAction> actions) {
        public ApplyResult { changedIds = List.copyOf(changedIds); actions = Set.copyOf(actions); }
    }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final SettingRegistry registry;
    private final PresetCatalog presets;
    private final Path path;
    private volatile ConfigSnapshot current;
    private Preview preview;
    private PreviewStatus finishedPreview = new PreviewStatus(PreviewStage.NONE, 0, "");
    private JsonObject envelope = new JsonObject();
    private final List<String> warnings = new ArrayList<>();

    public ConfigStore(SettingRegistry registry, PresetCatalog presets, Path path) {
        this.registry = registry;
        this.presets = presets;
        this.path = path.toAbsolutePath();
        current = ConfigDraft.defaults(registry, presets);
        load();
    }
    public SettingRegistry registry() { return registry; }
    public ConfigSnapshot current() { return current; }
    public List<String> warnings() { return List.copyOf(warnings); }
    public ConfigDraft draft() { return new ConfigDraft(registry, presets, current); }
    public Path path() { return path; }

    private void load() {
        for (Path candidate : List.of(path, backupPath())) {
            if (!Files.exists(candidate)) continue;
            try {
                JsonObject document = JsonParser.parseString(Files.readString(candidate, StandardCharsets.UTF_8)).getAsJsonObject();
                if (!document.has("schemaVersion") || document.get("schemaVersion").getAsInt() != 1)
                    throw new IllegalArgumentException("Unsupported config schema");
                ConfigDraft draft = draft();
                draft.set("general.style", document.get("style"));
                draft.set("general.quality", document.get("quality"));
                JsonObject values = document.getAsJsonObject("userOverrides");
                if (values != null) values.entrySet().forEach(e -> {
                    if (!registry.settings().containsKey(e.getKey())) return; // Retained in the private envelope.
                    if (e.getKey().equals("general.style") || e.getKey().equals("general.quality"))
                        throw new IllegalArgumentException("Selectors must be stored separately from user overrides");
                    draft.set(e.getKey(), e.getValue());
                });
                current = draft.snapshot(0);
                envelope = document.deepCopy();
                if (!candidate.equals(path)) warnings.add("Recovered last-good backup; original file retained until Apply.");
                return;
            } catch (IOException | RuntimeException failure) {
                warnings.add("Could not load " + candidate.getFileName() + ": " + failure.getMessage());
            }
        }
        if (!warnings.isEmpty()) warnings.add("Using validated defaults; unreadable files were not overwritten.");
    }

    public synchronized ApplyResult apply(ConfigDraft draft) throws IOException {
        if (draft.baseRevision() != current.revision()) throw new IllegalStateException("Configuration changed while this draft was open; reopen the screen.");
        ApplyResult result = persist(draft.snapshot(current.revision() + 1));
        preview = null;
        finishedPreview = new PreviewStatus(PreviewStage.NONE, 0, "");
        return result;
    }

    public synchronized void startPreview(ConfigDraft draft) {
        if (draft.baseRevision() != current.revision()) throw new IllegalStateException("Configuration changed while this draft was open; reopen the screen.");
        ConfigSnapshot candidate = draft.snapshot(current.revision() + 1);
        long eligible = System.nanoTime() + candidate.value("general.previewDelay").getAsLong() * 1_000_000L;
        preview = new Preview(candidate, eligible, 0L);
        finishedPreview = new PreviewStatus(PreviewStage.NONE, 0, "");
    }

    public synchronized ConfigSnapshot renderSnapshot() {
        expirePreview();
        return preview != null && System.nanoTime() - preview.eligibleAt() >= 0 ? preview.candidate() : current;
    }

    public synchronized void markPreviewVisible(ConfigSnapshot rendered) {
        if (preview != null && preview.candidate() == rendered && preview.expiresAt() == 0L)
            preview = preview.visible(System.nanoTime());
    }

    public synchronized void rejectPreview(ConfigSnapshot rendered, String error) {
        if (preview == null || preview.candidate() != rendered) return;
        preview = null;
        finishedPreview = new PreviewStatus(PreviewStage.FAILED, 0, error);
    }

    public synchronized ApplyResult keepPreview() throws IOException {
        expirePreview();
        if (preview == null || preview.expiresAt() == 0L)
            throw new IllegalStateException("No visible configuration preview to keep");
        ConfigSnapshot candidate = preview.candidate();
        try {
            ApplyResult result = persist(candidate);
            preview = null;
            finishedPreview = new PreviewStatus(PreviewStage.NONE, 0, "");
            return result;
        } catch (IOException failure) {
            rejectPreview(candidate, failure.getMessage());
            throw failure;
        }
    }

    public synchronized void revertPreview() {
        if (preview != null) {
            preview = null;
            finishedPreview = new PreviewStatus(PreviewStage.REVERTED, 0, "");
        }
    }

    public synchronized PreviewStatus previewStatus() {
        expirePreview();
        if (preview == null) return finishedPreview;
        if (preview.expiresAt() == 0L) return new PreviewStatus(PreviewStage.WAITING, 0, "");
        long remaining = preview.expiresAt() - System.nanoTime();
        int seconds = (int) Math.max(0, (remaining + 999_999_999L) / 1_000_000_000L);
        return new PreviewStatus(PreviewStage.VISIBLE, seconds, "");
    }

    private void expirePreview() {
        if (preview != null && preview.expiresAt() != 0L && System.nanoTime() - preview.expiresAt() >= 0)
            revertPreview();
    }

    private ApplyResult persist(ConfigSnapshot next) throws IOException {
        JsonObject nextDocument = document(next);
        Files.createDirectories(path.getParent());
        writeAtomically(backupPath(), bytes(document(current)));
        writeAtomically(path, bytes(nextDocument));
        List<String> changed = registry.settings().keySet().stream()
                .filter(id -> !current.value(id).equals(next.value(id))).toList();
        EnumSet<SettingSpec.ApplyAction> actions = EnumSet.noneOf(SettingSpec.ApplyAction.class);
        changed.forEach(id -> actions.addAll(registry.require(id).apply()));
        envelope = nextDocument;
        current = next; // Single volatile publication only after the durable file replacement succeeds.
        return new ApplyResult(next, changed, actions);
    }
    private JsonObject document(ConfigSnapshot snapshot) {
        JsonObject result = envelope.deepCopy();
        result.addProperty("schemaVersion", 1);
        result.addProperty("style", snapshot.style());
        result.addProperty("quality", snapshot.quality());
        JsonObject overrides = result.has("userOverrides") && result.get("userOverrides").isJsonObject()
                ? result.getAsJsonObject("userOverrides").deepCopy() : new JsonObject();
        registry.settings().keySet().forEach(overrides::remove);
        snapshot.userOverrides().forEach(overrides::add);
        result.add("userOverrides", overrides);
        return result;
    }
    private byte[] bytes(JsonObject document) { return (GSON.toJson(document) + "\n").getBytes(StandardCharsets.UTF_8); }
    private Path backupPath() { return path.resolveSibling(path.getFileName() + ".bak"); }
    private static void writeAtomically(Path target, byte[] bytes) throws IOException {
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) {
                // Refuse an unprotected overwrite; the caller retains its active snapshot and the previous file.
                throw new IOException("Atomic replacement is unsupported for " + target, unsupported);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
