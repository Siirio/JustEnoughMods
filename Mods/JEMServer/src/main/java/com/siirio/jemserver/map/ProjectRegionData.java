package com.siirio.jemserver.map;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class ProjectRegionData extends SavedData {
    private static final String NAME = "jem_project_regions";
    private final Map<UUID, ProjectRegion> projects = new LinkedHashMap<>();

    public static ProjectRegionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ProjectRegionData::load, ProjectRegionData::new, NAME);
    }

    public Optional<ProjectRegion> find(UUID id) { return Optional.ofNullable(projects.get(id)); }
    public Collection<ProjectRegion> all() { return java.util.List.copyOf(projects.values()); }
    public void put(ProjectRegion project) { projects.put(project.id(), project); setDirty(); }
    public void changed() { setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag rows = new CompoundTag();
        projects.forEach((id, project) -> rows.put(id.toString(), project.save()));
        tag.put("Projects", rows);
        return tag;
    }

    private static ProjectRegionData load(CompoundTag tag) {
        ProjectRegionData data = new ProjectRegionData();
        CompoundTag rows = tag.getCompound("Projects");
        for (String key : rows.getAllKeys()) {
            try { ProjectRegion project = ProjectRegion.load(rows.getCompound(key)); data.projects.put(project.id(), project); }
            catch (RuntimeException ignored) { }
        }
        return data;
    }
}
