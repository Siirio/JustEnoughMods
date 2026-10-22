package com.siirio.jemserver.smp;

import java.io.IOException;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Owns preserved entities and verifies the durable archive before a removal may proceed. */
public final class BossQuarantineRecords {
    private final LinkedHashMap<UUID, BossQuarantineState> saved = new LinkedHashMap<>();
    private LinkedHashMap<UUID, BossQuarantineState> editing;
    private boolean active;
    BossQuarantineRecords(SmpData data) {}
    void beginEdit() { active = true; }
    void endEdit() { active = false; editing = null; }
    private LinkedHashMap<UUID, BossQuarantineState> working() {
        if (!active) throw new IllegalStateException("Quarantine changes require an owning command scope");
        if (editing == null) { editing = new LinkedHashMap<>(); saved.forEach((id, row) -> editing.put(id, row.copy())); }
        return editing;
    }
    public BossQuarantineState current(UUID id) { return working().get(id); }
    public Collection<BossQuarantineState> currentRows() { return List.copyOf(working().values()); }
    public BossQuarantineState snapshot(UUID id) { var row = (editing == null ? saved : editing).get(id); return row == null ? null : row.snapshot(); }
    public List<BossQuarantineState> snapshots() { return (editing == null ? saved : editing).values().stream().map(BossQuarantineState::snapshot).toList(); }
    public BossQuarantineState put(BossQuarantineState row) {
        var owned = row.copy(); working().put(row.id(), owned); return owned;
    }
    int checkpoint() {
        if (editing == null) return 0;
        int changes = 0;
        for (var row : editing.values()) {
            var prior = saved.get(row.id());
            if (prior != null && prior.encode().equals(row.encode())) continue;
            row.revision(prior == null ? Math.max(1, row.revision() + 1) : prior.revision() + 1);
            saved.put(row.id(), row.copy()); changes++;
        }
        return changes;
    }
    void read(ListTag rows) { for (var value : rows) { var row = BossQuarantineState.decode((CompoundTag) value); if (row.id() != null) saved.put(row.id(), row); } }
    ListTag write() { var result = new ListTag(); saved.values().forEach(row -> result.add(row.encode())); return result; }
    public void verifySaved(MinecraftServer server, UUID id) throws IOException {
        var expected = working().get(id);
        if (expected == null) throw new IOException("No owned quarantine record");
        var file = server.getWorldPath(LevelResource.ROOT).resolve("data/jem_smp_v2.dat").toFile();
        var rows = NbtIo.readCompressed(file).getCompound("data").getCompound("tables").getList("boss_quarantine", Tag.TAG_COMPOUND);
        for (var value : rows) if (value.equals(expected.encode())) return;
        throw new IOException("Exact boss quarantine snapshot was not persisted");
    }
}
