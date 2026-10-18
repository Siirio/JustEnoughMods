package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.Painting;

/** Loaded chunk evidence, incremental global work budget, persisted per-chunk contributions.
 * Unloaded chunks retain their last verified evidence and are explicitly stale. */
public final class CityComfortService {
    private static final ArrayDeque<Cursor> QUEUE = new ArrayDeque<>();
    private static final java.util.Set<String> QUEUED = new java.util.HashSet<>();
    private static int townOffset;
    private static final Map<UUID,Integer> CHUNK_OFFSETS = new HashMap<>();
    private record Candidate(int x,int z,int index,boolean invalid) { }
    private CityComfortService() { }
    public static void clear() { QUEUE.clear(); QUEUED.clear(); CHUNK_OFFSETS.clear(); townOffset = 0; }
    public static void invalidate(MinecraftServer server, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos pos) {
        for (var town : SettlementSavedData.get(server).towns()) if (town.dimension().equals(dimension.location()) && town.contains(pos)) {
            String key = key(pos.getX() >> 4, pos.getZ() >> 4);
            if (town.comfortChunks().contains(key)) town.comfortChunks().getCompound(key).putLong("Time", Long.MIN_VALUE);
        }
    }
    public static void tick(MinecraftServer server) {
        var data = SettlementSavedData.get(server);
        if (server.getTickCount() % 20 == 0) {
            var towns = java.util.List.copyOf(data.towns());
            CHUNK_OFFSETS.keySet().retainAll(towns.stream().map(TownRecord::id).collect(java.util.stream.Collectors.toSet()));
            // At most one town's bounded chunk grid admitted per second; all towns rotate fairly.
            if (!towns.isEmpty()) admit(server, towns.get(Math.floorMod(townOffset++, towns.size())));
        }
        int budget = SettlementMetricsConfig.SCAN_BUDGET.get();
        while (budget > 0 && !QUEUE.isEmpty()) {
            Cursor cursor = QUEUE.peekFirst(); int allowance = Math.min(256, budget);
            int used = cursor.step(server, allowance); budget -= Math.max(1, used);
            if (cursor.done) { QUEUE.removeFirst(); QUEUED.remove(cursor.identity()); data.changed(); }
            else { QUEUE.removeFirst(); QUEUE.addLast(cursor); }
        }
    }
    private static void admit(MinecraftServer server, TownRecord town) {
        var level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, town.dimension()));
        if (level == null) return;
        int known = 0, total = 0, stale = 0;
        var validKeys = new java.util.HashSet<String>();
        var candidates = new java.util.ArrayList<Candidate>();
        int chunkDepth = (town.territoryMaxZ() >> 4) - (town.territoryMinZ() >> 4) + 1;
        int chunkCount = ((town.territoryMaxX() >> 4) - (town.territoryMinX() >> 4) + 1) * chunkDepth;
        int first = Math.floorMod(CHUNK_OFFSETS.getOrDefault(town.id(),0), chunkCount);
        for (int x = town.territoryMinX() >> 4; x <= town.territoryMaxX() >> 4; x++) for (int z = town.territoryMinZ() >> 4; z <= town.territoryMaxZ() >> 4; z++) {
            total++; String key = key(x,z); validKeys.add(key);
            CompoundTag row = town.comfortChunks().getCompound(key);
            if (town.comfortChunks().contains(key)) known++;
            boolean loaded = level.getChunkSource().getChunkNow(x,z) != null;
            boolean currentBounds = row.getInt("MinX") == town.territoryMinX() && row.getInt("MaxX") == town.territoryMaxX()
                    && row.getInt("MinZ") == town.territoryMinZ() && row.getInt("MaxZ") == town.territoryMaxZ();
            boolean invalid = !currentBounds || !row.contains("Time") || row.getLong("Time") == Long.MIN_VALUE;
            boolean expired = invalid || level.getGameTime() - row.getLong("Time") >= 1200L;
            if (!loaded || expired) stale++;
            if (loaded && expired) {
                int index=(x-(town.territoryMinX()>>4))*chunkDepth+z-(town.territoryMinZ()>>4);
                candidates.add(new Candidate(x,z,index,invalid));
            }
        }
        // Complete new/invalid observations before routine TTL refreshes. Within each
        // priority, rotate past the last admitted chunk so a full queue cannot starve
        // a distant part of a large town whose full scan takes longer than the TTL.
        candidates.sort(java.util.Comparator.comparingInt((Candidate value)->value.invalid()?0:1)
                .thenComparingInt(value->Math.floorMod(value.index()-first,chunkCount)));
        for (var candidate:candidates) {
            if(QUEUE.size()>=128)break;
            var cursor=new Cursor(town,candidate.x(),candidate.z());
            if(QUEUED.add(cursor.identity())) {
                QUEUE.addLast(cursor);
                CHUNK_OFFSETS.put(town.id(),(candidate.index()+1)%chunkCount);
            }
        }
        for (String key : java.util.Set.copyOf(town.comfortChunks().getAllKeys())) if (!validKeys.contains(key)) town.comfortChunks().remove(key);
        Map<String,Integer> sources = aggregateSources(town);
        CompoundTag categories = new CompoundTag();
        sources.forEach((source,count) -> {
            String category = source.substring(0,source.indexOf('|'));
            int points = Math.min(SettlementMetricsConfig.REPEAT_CAP.get(), count);
            categories.putInt(category, Math.min(SettlementMetricsConfig.CATEGORY_CAP.get(), categories.getInt(category) + points));
        });
        int points = categories.getAllKeys().stream().mapToInt(categories::getInt).sum();
        CompoundTag metrics = town.metrics(); metrics.putInt("Comfort", Math.min(100,points)); metrics.put("ComfortCategories", categories);
        metrics.putInt("ComfortKnownChunks", known); metrics.putInt("ComfortTotalChunks", total); metrics.putInt("ComfortStaleChunks", stale);
        town.metrics(metrics); SettlementSavedData.get(server).changed();
    }
    private static Map<String,Integer> aggregateSources(TownRecord town) {
        Map<String,Integer> sources = new HashMap<>();
        for (String key : town.comfortChunks().getAllKeys()) {
            CompoundTag row = town.comfortChunks().getCompound(key);
            if (row.getInt("MinX") != town.territoryMinX() || row.getInt("MaxX") != town.territoryMaxX()
                    || row.getInt("MinZ") != town.territoryMinZ() || row.getInt("MaxZ") != town.territoryMaxZ()) continue;
            CompoundTag counts = row.getCompound("Counts");
            for (String id : counts.getAllKeys()) sources.merge(id, counts.getInt(id), Integer::sum);
        }
        return sources;
    }
    /** Group/category scoring evidence, not a fictitious allocation of aggregate points to individual blocks.
     * Keys are exact registry IDs; callers revalidate current world state/activity and disclosure. */
    public static Map<String,CompoundTag> projectionFacts(TownRecord town) {
        var sources=aggregateSources(town);
        Map<String,Integer> categories=new HashMap<>();
        sources.forEach((source,count)->{
            String category=source.substring(0,source.indexOf('|'));
            categories.merge(category,Math.min(SettlementMetricsConfig.REPEAT_CAP.get(),count),Integer::sum);
        });
        Map<String,CompoundTag> facts=new HashMap<>();
        sources.forEach((source,count)->{
            int separator=source.indexOf('|');String category=source.substring(0,separator),id=source.substring(separator+1);
            var row=new CompoundTag();row.putString("Category",category);row.putBoolean("CityAggregate",true);
            row.putInt("GroupCount",count);row.putBoolean("GroupCountBounded",true);
            row.putInt("GroupPoints",Math.min(SettlementMetricsConfig.REPEAT_CAP.get(),count));
            row.putInt("GroupCap",SettlementMetricsConfig.REPEAT_CAP.get());
            row.putInt("CategoryPoints",Math.min(SettlementMetricsConfig.CATEGORY_CAP.get(),categories.getOrDefault(category,0)));
            row.putInt("CategoryCap",SettlementMetricsConfig.CATEGORY_CAP.get());
            row.putBoolean("Counted",count>0&&row.getInt("CategoryPoints")>0);
            facts.put(id,row);
        });
        return java.util.Collections.unmodifiableMap(facts);
    }
    /** Bounded factual positions for server-sanitized city snapshots; not an exhaustive object registry. */
    public static net.minecraft.nbt.ListTag observedSources(TownRecord town) {
        var result = new net.minecraft.nbt.ListTag();
        for (String key : town.comfortChunks().getAllKeys()) {
            var sources = town.comfortChunks().getCompound(key).getList("Sources", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i=0;i<sources.size()&&result.size()<512;i++) {
                var source=sources.getCompound(i);
                if(town.contains(BlockPos.of(source.getLong("Position")))) result.add(source.copy());
            }
            if(result.size()>=512) break;
        }
        return result;
    }
    private static String key(int x,int z) { return x + "," + z; }
    private static final class Cursor {
        final TownRecord town; final int x,z,minX,maxX,minZ,maxZ; int section,index; boolean done;
        final CompoundTag counts = new CompoundTag();
        final net.minecraft.nbt.ListTag positions = new net.minecraft.nbt.ListTag();
        Cursor(TownRecord town,int x,int z) { this.town=town;this.x=x;this.z=z;minX=town.territoryMinX();maxX=town.territoryMaxX();minZ=town.territoryMinZ();maxZ=town.territoryMaxZ(); }
        String identity() { return town.id()+":"+key(x,z); }
        void add(String category,String id) { String key=category+"|"+id; counts.putInt(key,Math.min(16,counts.getInt(key)+1)); }
        int step(MinecraftServer server,int budget) {
            var level=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,town.dimension()));
            var chunk=level==null?null:level.getChunkSource().getChunkNow(x,z);
            if(chunk==null || town.territoryMinX()!=minX || town.territoryMaxX()!=maxX || town.territoryMinZ()!=minZ || town.territoryMaxZ()!=maxZ) { done=true;return 1; }
            var sections=chunk.getSections(); int used=0;
            while(section<sections.length && used<budget) {
                var current=sections[section];
                if(current.hasOnlyAir() || !current.maybeHas(state -> AmenityRegistry.get(state).isPresent())) {section++;index=0;used++;continue;}
                int lx=index&15,lz=(index>>4)&15,ly=(index>>8)&15;
                int wx=x*16+lx,wz=z*16+lz,wy=level.getMinBuildHeight()+section*16+ly;
                if(wx>=minX&&wx<=maxX&&wz>=minZ&&wz<=maxZ) {
                    var state=current.getBlockState(lx,ly,lz);var amenity=AmenityRegistry.get(state);
                    if(amenity.isPresent()&&AmenityRegistry.active(level,new BlockPos(wx,wy,wz),state,amenity.get()))
                        {
                        add(amenity.get().category(),net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString());
                        if(positions.size()<32) { var source=new CompoundTag();source.putLong("Position",new BlockPos(wx,wy,wz).asLong());source.putString("Category",amenity.get().category());positions.add(source); }
                    }
                }
                used++;if(++index>=4096){index=0;section++;}
            }
            if(section>=sections.length) {
                var box=new net.minecraft.world.phys.AABB(x*16,level.getMinBuildHeight(),z*16,x*16+16,level.getMaxBuildHeight(),z*16+16);
                level.getEntitiesOfClass(Painting.class,box,painting->{
                    if((painting.blockPosition().getX()>>4)==x && (painting.blockPosition().getZ()>>4)==z
                            && town.contains(painting.blockPosition())&&painting.isAlive()) add("culture","painting:"+painting.getVariant().unwrapKey().map(k->k.location().toString()).orElse("custom"));return false;
                });
                CompoundTag row=new CompoundTag();row.put("Counts",counts);row.put("Sources",positions);row.putLong("Time",level.getGameTime());
                row.putInt("MinX",minX);row.putInt("MaxX",maxX);row.putInt("MinZ",minZ);row.putInt("MaxZ",maxZ);
                town.comfortChunks().put(key(x,z),row);done=true;
            }
            return used;
        }
    }
}
