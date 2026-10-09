package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.SmpConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class ResourcePlacements extends SavedData {
    private final Map<String,Set<Long>> positions=new HashMap<>();
    private final Map<String,Set<Long>> chunks=new HashMap<>();
    private boolean full;
    private int size;
    private static ResourcePlacements get(ServerLevel level) { return level.getServer().overworld().getDataStorage().computeIfAbsent(ResourcePlacements::load,ResourcePlacements::new,"jem_resource_placements"); }
    public static void record(ServerLevel level,BlockPos pos) {
        var data=get(level);
        boolean wasFull=data.full;
        data.record(level.dimension().location().toString(),pos.asLong(),SmpConfig.RUSH_TRACKED_BLOCKS.get());
        if(!wasFull && data.full) org.slf4j.LoggerFactory.getLogger(ResourcePlacements.class)
                .warn("Resource placement history reached its {} entry limit after chunk compaction; Resource Rush bonuses are disabled to prevent placed-block duplication",SmpConfig.RUSH_TRACKED_BLOCKS.get());
    }
    void record(String dimension,long pos,int limit) {
        if(excluded(dimension,pos)) return;
        if(size>=limit) compact();
        if(excluded(dimension,pos)) return;
        if(size>=limit) full=true;
        else if(positions.computeIfAbsent(dimension,key->new HashSet<>()).add(pos)) size++;
        setDirty();
    }
    private void compact() {
        positions.forEach((dimension,known)->{
            Map<Long,Integer> counts=new HashMap<>();
            for(long pos:known) counts.merge(chunk(pos),1,Integer::sum);
            var saturated=chunks.computeIfAbsent(dimension,key->new HashSet<>());
            counts.forEach((chunk,count)->{if(count>1 && saturated.add(chunk)) size++;});
            var iterator=known.iterator();
            while(iterator.hasNext()) if(saturated.contains(chunk(iterator.next()))) {iterator.remove();size--;}
        });
        setDirty();
    }
    private static long chunk(long pos) { return ChunkPos.asLong(BlockPos.getX(pos)>>4,BlockPos.getZ(pos)>>4); }
    boolean excluded(String dimension,long pos) {
        return full || chunks.getOrDefault(dimension,Set.of()).contains(chunk(pos))
                || positions.getOrDefault(dimension,Set.of()).contains(pos);
    }
    public static boolean excluded(ServerLevel level,BlockPos pos) { return get(level).excluded(level.dimension().location().toString(),pos.asLong()); }
    static ResourcePlacements load(CompoundTag root) {
        var data=new ResourcePlacements();data.full=root.getBoolean("full");
        var dimensions=root.getCompound("dimensions");
        for(String key:dimensions.getAllKeys()) { var values=new HashSet<Long>();for(long pos:dimensions.getLongArray(key)) values.add(pos);data.positions.put(key,values);data.size+=values.size(); }
        var chunks=root.getCompound("chunks");
        for(String key:chunks.getAllKeys()) { var values=new HashSet<Long>();for(long chunk:chunks.getLongArray(key)) values.add(chunk);data.chunks.put(key,values);data.size+=values.size(); }
        return data;
    }
    @Override public CompoundTag save(CompoundTag root) {
        root.putBoolean("full",full);var dimensions=new CompoundTag();positions.forEach((key,values)->dimensions.putLongArray(key,values.stream().mapToLong(Long::longValue).toArray()));root.put("dimensions",dimensions);
        var chunks=new CompoundTag();this.chunks.forEach((key,values)->chunks.putLongArray(key,values.stream().mapToLong(Long::longValue).toArray()));root.put("chunks",chunks);return root;
    }
}
