package com.siirio.jemserver.migration.backpack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

/** Offline, recoverable replacement transaction. Backups and staged bytes are never removed. */
public final class BackpackMigrationFiles {
    private static final Path DIRECTORY = Path.of(".jem-migrations", "backpacked-v1");
    private static final String ABSENT = "absent";

    private BackpackMigrationFiles() { }

    public static boolean completed(Path world) throws IOException {
        Path root = worldRoot(world);
        Path journal = journal(root);
        if (!Files.exists(journal, LinkOption.NOFOLLOW_LINKS)) return false;
        CompoundTag tag = readJournal(journal);
        return tag.getString("Phase").equals("COMPLETE");
    }

    public static void resume(Path world) throws IOException {
        Path root = worldRoot(world);
        Path journal = journal(root);
        if (!Files.exists(journal, LinkOption.NOFOLLOW_LINKS)) return;
        CompoundTag tag = readJournal(journal);
        if (tag.getString("Phase").equals("COMPLETE")) return;
        replay(root, tag);
    }

    public static Plan begin(Path world) throws IOException {
        Path root = worldRoot(world);
        if (Files.exists(journal(root), LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Backpack migration already has a transaction journal");
        }
        Path run = root.resolve(DIRECTORY).resolve("runs").resolve(UUID.randomUUID().toString());
        directories(root, run.getParent());
        Files.createDirectory(run);
        return new Plan(root, run);
    }

    public static final class Plan {
        private final Path root;
        private final Path run;
        private final ListTag entries = new ListTag();
        private final Set<Path> targets = new HashSet<>();
        private boolean committed;

        private Plan(Path root, Path run) { this.root = root; this.run = run; }

        public Path workDirectory() { return run; }

        public void stageNbt(Path original, CompoundTag replacement) throws IOException {
            requireOpen();
            Path staged = run.resolve("nbt-" + UUID.randomUUID() + ".dat");
            safe(root, staged);
            writeNbt(staged, replacement);
            addStaged(original, staged);
        }

        public void addStaged(Path original, Path staged) throws IOException {
            requireOpen();
            Path target = original.isAbsolute() ? original.normalize() : root.resolve(original).normalize();
            safe(root, target);
            if (target.equals(root) || target.startsWith(root.resolve(".jem-migrations"))) {
                throw new IOException("Migration target overlaps transaction storage");
            }
            if (targets.contains(target)) throw new IOException("Duplicate migration target");
            Path source = staged.toAbsolutePath().normalize();
            if (!source.startsWith(run)) throw new IOException("Staged migration input is outside its owned run");
            safe(root, source);
            if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Staged migration input is not a regular file");
            }
            String after = hash(source);
            Path owned = run.resolve("staged").resolve(UUID.randomUUID() + ".bin");
            directories(root, owned.getParent());
            copyVerified(source, owned, after);
            String before = currentHash(target);
            Path backup = run.resolve("backups").resolve(root.relativize(target));
            if (!before.equals(ABSENT)) {
                directories(root, backup.getParent());
                copyVerified(target, backup, before);
                if (!before.equals(currentHash(target))) throw new IOException("Migration source changed during backup");
            }
            CompoundTag entry = new CompoundTag();
            entry.putString("Target", portable(root.relativize(target)));
            entry.putString("Staged", portable(run.relativize(owned)));
            entry.putString("Backup", portable(run.relativize(backup)));
            entry.putString("Before", before);
            entry.putString("After", after);
            entries.add(entry);
            targets.add(target);
        }

        public void commit() throws IOException {
            requireOpen();
            if (Files.exists(journal(root), LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Migration journal appeared while preparing the transaction");
            }
            CompoundTag tag = new CompoundTag();
            tag.putInt("Version", 1);
            tag.putString("Phase", "PLANNED");
            tag.putString("Run", portable(root.relativize(run)));
            tag.put("Entries", entries.copy());
            validate(root, tag);
            committed = true;
            persist(root, tag);
            replay(root, tag);
        }

        private void requireOpen() throws IOException {
            if (committed) throw new IOException("Migration plan has already been committed");
        }
    }

    private static void replay(Path root, CompoundTag tag) throws IOException {
        validate(root, tag);
        Path run = relative(root, tag.getString("Run"));
        ListTag entries = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (Tag value : entries) {
            CompoundTag entry = (CompoundTag) value;
            Path target = relative(root, entry.getString("Target"));
            String after = entry.getString("After");
            String current = currentHash(target);
            if (current.equals(after)) continue;
            if (!current.equals(entry.getString("Before"))) throw new IOException("Migration target changed before replacement");
            directories(root, target.getParent());
            Path temp = target.resolveSibling(".jem-backpack-" + UUID.randomUUID() + ".tmp");
            copyVerified(relative(run, entry.getString("Staged")), temp, after);
            safe(root, target);
            if (!current.equals(currentHash(target))) throw new IOException("Migration target changed during replacement");
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            if (!after.equals(currentHash(target))) throw new IOException("Migration replacement did not persist");
        }
        for (Tag value : entries) {
            CompoundTag entry = (CompoundTag) value;
            if (!entry.getString("After").equals(currentHash(relative(root, entry.getString("Target"))))) {
                throw new IOException("Migration cannot complete with unmatched target bytes");
            }
        }
        tag.putString("Phase", "COMPLETE");
        persist(root, tag);
    }

    private static void validate(Path root, CompoundTag tag) throws IOException {
        checkHeader(tag);
        Path run = relative(root, tag.getString("Run"));
        Path runs = root.resolve(DIRECTORY).resolve("runs");
        if (!runs.equals(run.getParent()) || !Files.isDirectory(run, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Invalid migration run directory");
        }
        Set<Path> targets = new HashSet<>();
        if (!tag.contains("Entries", Tag.TAG_LIST)) throw new IOException("Missing migration entries");
        ListTag entries = tag.getList("Entries", Tag.TAG_COMPOUND);
        if (((ListTag) tag.get("Entries")).size() != entries.size()) throw new IOException("Invalid migration entries");
        for (Tag value : entries) {
            CompoundTag entry = (CompoundTag) value;
            Path target = relative(root, entry.getString("Target"));
            Path stage = relative(run, entry.getString("Staged"));
            Path backup = relative(run, entry.getString("Backup"));
            if (target.startsWith(root.resolve(".jem-migrations")) || !targets.add(target)
                    || !stage.startsWith(run.resolve("staged"))
                    || !backup.equals(run.resolve("backups").resolve(root.relativize(target)))) {
                throw new IOException("Invalid migration entry paths");
            }
            String before = entry.getString("Before");
            String after = entry.getString("After");
            if (!after.matches("[0-9a-f]{64}") || (!before.equals(ABSENT) && !before.matches("[0-9a-f]{64}"))) {
                throw new IOException("Invalid migration digest");
            }
            if (!after.equals(currentHash(stage)) || (!before.equals(ABSENT) && !before.equals(currentHash(backup)))) {
                throw new IOException("Migration staged bytes or backup do not match journal");
            }
            String current = currentHash(target);
            if (!current.equals(before) && !current.equals(after)) throw new IOException("Migration target has unjournaled changes");
        }
    }

    private static CompoundTag readJournal(Path path) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Invalid migration journal file");
        CompoundTag tag = NbtIo.readCompressed(path.toFile());
        checkHeader(tag);
        return tag;
    }

    private static void checkHeader(CompoundTag tag) throws IOException {
        String phase = tag.getString("Phase");
        if (!tag.contains("Version", Tag.TAG_INT) || tag.getInt("Version") != 1
                || (!phase.equals("PLANNED") && !phase.equals("COMPLETE"))) {
            throw new IOException("Unsupported backpack migration journal");
        }
    }

    private static void persist(Path root, CompoundTag tag) throws IOException {
        Path journal = journal(root);
        directories(root, journal.getParent());
        Path temp = journal.resolveSibling("transaction-" + UUID.randomUUID() + ".tmp");
        writeNbt(temp, tag);
        safe(root, journal);
        Files.move(temp, journal, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        if (!tag.equals(readJournal(journal))) throw new IOException("Migration journal did not persist");
    }

    private static void writeNbt(Path path, CompoundTag tag) throws IOException {
        try (var out = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            NbtIo.writeCompressed(tag, out);
        }
        force(path);
        if (!tag.equals(NbtIo.readCompressed(path.toFile()))) throw new IOException("Migration NBT readback differs");
    }

    private static void copyVerified(Path source, Path target, String expected) throws IOException {
        Files.copy(source, target);
        force(target);
        if (!expected.equals(hash(target))) throw new IOException("Migration copy digest differs");
    }

    private static void force(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) { channel.force(true); }
    }

    private static String currentHash(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return ABSENT;
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Migration file is not a regular file");
        return hash(path);
    }

    private static String hash(Path path) throws IOException {
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException exception) { throw new IOException("SHA-256 unavailable", exception); }
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static Path worldRoot(Path world) throws IOException {
        Path root = world.toAbsolutePath().normalize();
        safe(root.getRoot(), root);
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Migration world directory is missing");
        return root;
    }

    private static Path journal(Path root) throws IOException {
        Path path = root.resolve(DIRECTORY).resolve("transaction.dat");
        safe(root, path);
        return path;
    }

    private static Path relative(Path root, String value) throws IOException {
        final Path relative;
        try { relative = Path.of(value); }
        catch (RuntimeException exception) { throw new IOException("Invalid migration relative path", exception); }
        if (relative.isAbsolute() || value.isBlank()) throw new IOException("Invalid migration relative path");
        for (Path part : relative) {
            if (part.toString().equals("..") || part.toString().equals(".")) throw new IOException("Migration path traversal");
        }
        Path path = root.resolve(relative).normalize();
        if (path.equals(root)) throw new IOException("Migration file path equals its root");
        safe(root, path);
        return path;
    }

    private static void safe(Path root, Path path) throws IOException {
        if (!path.startsWith(root)) throw new IOException("Migration path escapes its root");
        Path cursor = root;
        if (Files.isSymbolicLink(cursor)) throw new IOException("Migration path contains a symbolic link");
        for (Path part : root.relativize(path)) {
            cursor = cursor.resolve(part);
            if (Files.isSymbolicLink(cursor)) throw new IOException("Migration path contains a symbolic link");
            if (!cursor.equals(path) && Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isDirectory(cursor, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Migration parent is not a directory");
            }
        }
    }

    private static void directories(Path root, Path path) throws IOException {
        safe(root, path);
        Files.createDirectories(path);
        safe(root, path);
    }

    private static String portable(Path path) { return path.toString().replace('\\', '/'); }
}
