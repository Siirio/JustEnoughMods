package com.siirio.jemserver.map;

import com.siirio.jemserver.ClaimBoundaryGeometry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public final class ProjectRegion {
    private final UUID id;
    private final UUID owner;
    private final ResourceLocation dimension;
    private String name;
    private final int color;
    private final List<ClaimBoundaryGeometry.Rectangle> rectangles = new ArrayList<>();
    private final Set<UUID> collaborators = new LinkedHashSet<>();
    private long revision;

    public ProjectRegion(UUID id, UUID owner, ResourceLocation dimension, String name, int color) {
        this.id = id; this.owner = owner; this.dimension = dimension; this.name = name; this.color = color & 0xFFFFFF;
    }

    public UUID id() { return id; }
    public UUID owner() { return owner; }
    public ResourceLocation dimension() { return dimension; }
    public String name() { return name; }
    public int color() { return color; }
    public List<ClaimBoundaryGeometry.Rectangle> rectangles() { return List.copyOf(rectangles); }
    public Set<UUID> collaborators() { return Set.copyOf(collaborators); }
    public long revision() { return revision; }
    void add(ClaimBoundaryGeometry.Rectangle rectangle) { rectangles.add(rectangle); revision++; }
    boolean remove(int index) {
        if (index < 0 || index >= rectangles.size()) return false;
        rectangles.remove(index);
        revision++;
        return true;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id); tag.putUUID("Owner", owner); tag.putString("Dimension", dimension.toString());
        tag.putString("Name", name); tag.putInt("Color", color); tag.putLong("Revision", revision);
        ListTag source = new ListTag();
        for (var rectangle : rectangles) {
            CompoundTag row = new CompoundTag();
            row.putIntArray("Bounds", new int[]{rectangle.minX(), rectangle.minZ(), rectangle.maxX(), rectangle.maxZ()});
            source.add(row);
        }
        tag.put("Rectangles", source);
        ListTag people = new ListTag();
        collaborators.forEach(value -> { CompoundTag row = new CompoundTag(); row.putUUID("Id", value); people.add(row); });
        tag.put("Collaborators", people);
        return tag;
    }

    static ProjectRegion load(CompoundTag tag) {
        ProjectRegion value = new ProjectRegion(tag.getUUID("Id"), tag.getUUID("Owner"),
                ResourceLocation.parse(tag.getString("Dimension")), tag.getString("Name"), tag.getInt("Color"));
        for (Tag raw : tag.getList("Rectangles", Tag.TAG_COMPOUND)) {
            int[] bounds = ((CompoundTag) raw).getIntArray("Bounds");
            if (bounds.length == 4 && bounds[2] > bounds[0] && bounds[3] > bounds[1])
                value.rectangles.add(new ClaimBoundaryGeometry.Rectangle(bounds[0], bounds[1], bounds[2], bounds[3]));
        }
        for (Tag raw : tag.getList("Collaborators", Tag.TAG_COMPOUND)) value.collaborators.add(((CompoundTag) raw).getUUID("Id"));
        value.revision = Math.max(0L, tag.getLong("Revision"));
        return value;
    }
}
