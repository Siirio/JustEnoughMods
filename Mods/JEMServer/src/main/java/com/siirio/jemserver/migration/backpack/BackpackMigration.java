package com.siirio.jemserver.migration.backpack;

import com.mojang.logging.LogUtils;
import com.siirio.jemrewards.PendingRewardContainer;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** One dedicated-world cutover, before missing legacy registrations can consume saved contents. */
@Mod.EventBusSubscriber(modid = "jem_server")
public final class BackpackMigration {
    private static final String REFUNDS = "jem_backpack_migration_refunds.dat";
    private static final Set<String> ACTIVE_DATA = Set.of("backpacked_recall.dat", "backpacked_farmhand.dat",
            "jem_rewards.dat");
    private static final Set<String> LEGACY_REWARDS = Set.of("jem_hosted_boss_rewards.dat", "jem_smp_v2.dat");
    private static final Set<String> ROOT_DIRECTORIES = Set.of("playerdata", "data", "region", "entities", "DIM-1", "DIM1", "dimensions");

    private BackpackMigration() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeWorldLoad(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        if (!eligible(server)) return;
        try {
            Path world = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toRealPath();
            BackpackMigrationFiles.resume(world);
            if (BackpackMigrationFiles.completed(world)) return;
            requireVersion("sophisticatedbackpacks", "3.26.14.2226");
            requireVersion("sophisticatedcore", "1.5.15.2427");
            migrate(world);
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("Backpacked migration stopped before world loading. Original backups and"
                    + " prepared files are retained under .jem-migrations/backpacked-v1. Resolve the reported cause"
                    + " before restarting. Never remove the migration journal to bypass recovery.", failure);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforePlayers(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        if (!eligible(server)) return;
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(REFUNDS);
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return;
        try {
            if (Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Backpack migration refund source is not a regular file");
            CompoundTag root = NbtIo.readCompressed(file.toFile());
            if (root.getInt("Version") != 1)
                throw new IOException("Unsupported backpack migration refund format");
            ListTag grants = compounds(root, "Grants");
            for (Tag value : grants) {
                CompoundTag grant = (CompoundTag) value;
                if (!grant.hasUUID("Owner") || !grant.hasUUID("Receipt"))
                    throw new IOException("Incomplete backpack migration refund obligation");
                ListTag items = compounds(grant, "Items");
                if (items.isEmpty()) throw new IOException("Empty backpack migration refund obligation");
                PendingRewardContainer.importOffline(server, grant.getUUID("Owner"), grant.getUUID("Receipt"),
                        items.copy());
            }
            // Rewards owns permanent import receipts, including claims made between later server restarts.
            // Retain this original cutover snapshot rather than maintain a second delivery-completion flag.
            LogUtils.getLogger().info("Backpacked migration: {} retained refund obligations acknowledged by Rewards",
                    grants.size());
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("Backpack migration refunds could not be durably acknowledged."
                    + " Player admission is stopped and all source obligations remain saved.", failure);
        }
    }

    private static boolean eligible(MinecraftServer server) {
        return !server.isSingleplayer() && ModList.get().isLoaded("sophisticatedbackpacks")
                && !ModList.get().isLoaded("backpacked");
    }

    private static void requireVersion(String mod, String expected) throws IOException {
        String actual = ModList.get().getModContainerById(mod).orElseThrow(
                () -> new IOException("Missing migration dependency " + mod)).getModInfo().getVersion().toString();
        if (!actual.equals(expected) && !actual.equals("1.20.1-" + expected))
            throw new IOException("Unreviewed native migration format for " + mod + " " + actual);
    }

    private static void migrate(Path world) throws IOException {
        List<Path> inputs = inputs(world, legacyRewardsPending(world));
        BackpackMigrationFiles.Plan plan = BackpackMigrationFiles.begin(world);
        LegacyBackpackConverter.Result result = new LegacyBackpackConverter.Result();
        int changedFiles = 0;
        int scanned = 0;
        LogUtils.getLogger().info("Backpacked migration: inspecting {} player, active SavedData and region files"
                + " before world loading", inputs.size());
        for (Path file : inputs) {
            String source = world.relativize(file).toString().replace('\\', '/');
            if (file.getFileName().toString().endsWith(".mca")) {
                for (LegacyBackpackRegions.StagedFile staged : LegacyBackpackRegions.stage(file,
                        plan.workDirectory(), source, result)) {
                    plan.addStaged(staged.original(), staged.staged());
                    changedFiles++;
                }
            } else {
                CompoundTag original = NbtIo.readCompressed(file.toFile());
                CompoundTag converted = original.copy();
                boolean changed;
                if (file.getParent().getFileName().toString().equals("playerdata")) {
                    String name = file.getFileName().toString();
                    UUID player;
                    try { player = UUID.fromString(name.substring(0, name.length() - 4)); }
                    catch (IllegalArgumentException invalid) { throw new IOException("Invalid player-data filename " + source, invalid); }
                    changed = LegacyBackpackConverter.convertPlayer(converted, player, source, result);
                } else if (file.getFileName().toString().startsWith("backpacked_")) {
                    changed = LegacyBackpackConverter.convertQueue(converted, source, result);
                } else if (LEGACY_REWARDS.contains(file.getFileName().toString())) {
                    changed = LegacyBackpackConverter.convertLegacyRewards(converted, source, result);
                } else {
                    changed = LegacyBackpackConverter.convertTree(converted, source, result);
                }
                if (changed) {
                    plan.stageNbt(file, converted);
                    changedFiles++;
                }
            }
            if (++scanned % 128 == 0) LogUtils.getLogger().info("Backpacked migration: inspected {}/{} files", scanned, inputs.size());
        }
        if (!result.refunds().isEmpty()) {
            Path refunds = world.resolve("data").resolve(REFUNDS);
            if (Files.exists(refunds, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Unjournaled backpack refund file already exists. Preserve it for recovery");
            CompoundTag saved = new CompoundTag();
            saved.putInt("Version", 1);
            ListTag grants = new ListTag();
            for (LegacyBackpackConverter.Refund refund : result.refunds()) {
                CompoundTag row = new CompoundTag();
                row.putUUID("Owner", refund.owner());
                row.putUUID("Receipt", refund.receipt());
                row.put("Items", refund.items().copy());
                grants.add(row);
            }
            saved.put("Grants", grants);
            plan.stageNbt(refunds, saved);
            changedFiles++;
        }
        plan.commit();
        LogUtils.getLogger().info("Backpacked migration COMPLETE: {} backpacks, {} replaced files, {} refund obligations."
                + " Original bytes remain in .jem-migrations/backpacked-v1", result.backpacks(), changedFiles, result.refunds().size());
    }

    private static ListTag compounds(CompoundTag root, String field) throws IOException {
        Tag value = root.get(field);
        if (!(value instanceof ListTag list) || !list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)
            throw new IOException("Invalid backpack migration " + field + " compound list");
        return list;
    }

    private static boolean legacyRewardsPending(Path world) throws IOException {
        Path file = world.resolve("data/jem_rewards.dat");
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return true;
        if (Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Rewards source is not a regular file");
        return !NbtIo.readCompressed(file.toFile()).getCompound("data").getBoolean("LegacyImported");
    }

    private static List<Path> inputs(Path world, boolean legacyPending) throws IOException {
        List<Path> result = new ArrayList<>();
        Files.walkFileTree(world, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
                if (dir.getFileName().toString().equals(".jem-migrations")) return FileVisitResult.SKIP_SUBTREE;
                Path relative = world.relativize(dir);
                if (!dir.equals(world) && relative.getNameCount() == 1
                        && !ROOT_DIRECTORIES.contains(dir.getFileName().toString())) return FileVisitResult.SKIP_SUBTREE;
                return FileVisitResult.CONTINUE;
            }

            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                if (attributes.isSymbolicLink()) throw new IOException("World contains an unreviewed symbolic link: " + world.relativize(file));
                if (!attributes.isRegularFile()) return FileVisitResult.CONTINUE;
                String parent = file.getParent().getFileName().toString();
                String name = file.getFileName().toString();
                // Native storage never opens editor-created copies such as "r.0.0 copy.mca".
                if ((parent.equals("region") || parent.equals("entities")) && name.matches("r\\.-?\\d+\\.-?\\d+\\.mca")
                        || parent.equals("playerdata") && name.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.dat")
                        || parent.equals("data") && (ACTIVE_DATA.contains(name)
                        || legacyPending && file.getParent().equals(world.resolve("data")) && LEGACY_REWARDS.contains(name))) result.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        result.sort(Comparator.comparing(Path::toString));
        return result;
    }
}
