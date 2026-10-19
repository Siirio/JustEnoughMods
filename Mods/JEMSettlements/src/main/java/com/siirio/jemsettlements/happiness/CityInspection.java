package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.town.TownRecord;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Bounded surface/foundation LOD snapshots. Never loads a chunk or scans a city on a GUI frame. */
public final class CityInspection {
    private static final Map<String, Job> JOBS = new LinkedHashMap<>();
    private static int cursor;
    private CityInspection() { }

    public static CompoundTag capture(ServerPlayer viewer, TownRecord town) {
        String key = viewer.getUUID() + ":" + town.id();
        long now = viewer.getServer().overworld().getGameTime();
        Job job = JOBS.get(key);
        String bounds = town.territoryMinX()+":"+town.territoryMaxX()+":"+town.territoryMinZ()+":"+town.territoryMaxZ();
        if (job == null || !job.bounds.equals(bounds) || now - job.created > 200) {
            if (JOBS.size() >= 8) JOBS.remove(JOBS.keySet().iterator().next());
            job = new Job(viewer, town, bounds, now); JOBS.put(key, job);
        }
        CompoundTag result = job.data.copy();
        ListTag homes=new ListTag();
        for(var resident:com.siirio.jemsettlements.town.SettlementSavedData.get(viewer.getServer()).residents(town.id())) {
            if(homes.size()>=64)break;
            if(!resident.living()||resident.home().isEmpty())continue;
            var home=resident.home().get();
            if(!home.dimension().location().equals(town.dimension())||!town.contains(home.pos()))continue;
            CompoundTag row=new CompoundTag();row.putUUID("Resident",resident.villagerId());row.putLong("Position",home.pos().asLong());homes.add(row);
        }
        result.put("Homes",homes);
        result.putBoolean("Building", !job.done()); result.putInt("Progress", job.total == 0 ? 100 : job.column * 100 / job.total);
        if (job.done()) { result.put("Palette", job.palette.copy()); result.put("Blocks", job.blocks.copy()); }
        return result;
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !SettlementAuthority.active(event.getServer()) || JOBS.isEmpty()) return;
        JOBS.values().removeIf(job -> event.getServer().getPlayerList().getPlayer(job.viewer) == null || event.getServer().overworld().getGameTime() - job.created > 400);
        var pending = JOBS.values().stream().filter(job -> !job.done()).toList();
        if (pending.isEmpty()) return;
        Job job = pending.get(Math.floorMod(cursor++, pending.size()));
        ServerPlayer viewer = event.getServer().getPlayerList().getPlayer(job.viewer);
        var level = event.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, job.dimension));
        if (viewer == null || level == null) { job.column = job.total; job.data.putBoolean("Unloaded", true); return; }
        job.projection.beginTick();
        for (int budget = 0; budget < 32 && !job.done(); budget++, job.column++) {
            boolean coarse=job.column<job.surfaceColumns;
            BlockPos detail=coarse?null:job.details.get(job.column-job.surfaceColumns);
            int x = coarse?job.minX + job.column % job.columns * job.stride:detail.getX();
            int z = coarse?job.minZ + job.column / job.columns * job.stride:detail.getZ();
            BlockPos probe = new BlockPos(x, level.getMinBuildHeight(), z);
            if (!level.hasChunkAt(probe)) { job.data.putBoolean("Partial", true); continue; }
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            for (int depth = 0; depth < (coarse?2:16) && top - depth >= level.getMinBuildHeight(); depth++) {
                BlockPos pos = new BlockPos(x, top - depth, z); BlockState original = level.getBlockState(pos);
                BlockState state=job.projection.project(level,pos,original);if(state==null)continue;
                int id = job.ids.computeIfAbsent(state, value -> { job.palette.add(NbtUtils.writeBlockState(value)); return job.palette.size()-1; });
                CompoundTag row = new CompoundTag(); row.putLong("Position", pos.asLong()); row.putInt("State", id); row.putBoolean("Coarse",coarse);
                if(state==original&&!SnapshotVisibility.resource(original))row.merge(job.facts(level,pos,original)); job.blocks.add(row);
            }
        }
        if (job.done()) {
            var town=com.siirio.jemsettlements.town.SettlementSavedData.get(event.getServer()).town(job.town).orElse(null);
            if(town!=null) {
                var sources=CityComfortService.observedSources(town);
                var present=new java.util.HashSet<Long>();
                for(int i=0;i<job.blocks.size();i++)present.add(job.blocks.getCompound(i).getLong("Position"));
                for(int i=0;i<sources.size()&&job.blocks.size()<16384;i++) {
                    var source=sources.getCompound(i);BlockPos pos=BlockPos.of(source.getLong("Position"));
                    if(present.contains(pos.asLong())||!level.hasChunkAt(pos))continue;
                    var state=level.getBlockState(pos);
                    if(state.isAir()||SnapshotVisibility.resource(state))continue;
                    int id=job.ids.computeIfAbsent(state,value->{job.palette.add(NbtUtils.writeBlockState(value));return job.palette.size()-1;});
                    CompoundTag row=new CompoundTag();row.putLong("Position",pos.asLong());row.putInt("State",id);row.merge(job.facts(level,pos,state));row.putString("Category",source.getString("Category"));row.putBoolean("Coarse",false);job.blocks.add(row);
                }
            }
            job.data.putLong("Captured", level.getGameTime());
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { JOBS.clear(); cursor=0; }

    private static final class Job {
        final UUID viewer,town; final net.minecraft.resources.ResourceLocation dimension; final String bounds;
        final long created; final int minX, minZ, stride, columns, surfaceColumns, total;
        final List<BlockPos> details=new ArrayList<>();
        final CompoundTag data = new CompoundTag(); final ListTag palette = new ListTag(), blocks = new ListTag();
        final SnapshotVisibility.Projection projection=new SnapshotVisibility.Projection();
        final Map<BlockState,Integer> ids = new LinkedHashMap<>(); final Map<String,CompoundTag> contributions;
        int column;
        Job(ServerPlayer player, TownRecord town, String bounds, long now) {
            viewer=player.getUUID(); this.town=town.id(); contributions=CityComfortService.projectionFacts(town); dimension=town.dimension(); this.bounds=bounds; created=now;
            minX=town.territoryMinX(); minZ=town.territoryMinZ();
            int step=Math.max(1,(int)Math.ceil(Math.sqrt(town.territoryArea()/450D)));
            while(((town.territoryMaxX()-minX)/step+1)*((town.territoryMaxZ()-minZ)/step+1)>450)step++;
            stride=step;
            columns=(town.territoryMaxX()-minX)/stride+1;
            surfaceColumns=columns*((town.territoryMaxZ()-minZ)/stride+1);
            var houses=new ArrayList<BlockPos>();
            for(var resident:com.siirio.jemsettlements.town.SettlementSavedData.get(player.getServer()).residents(town.id())) {
                if(!resident.living()||resident.home().isEmpty())continue;
                var home=resident.home().get();if(!home.dimension().location().equals(town.dimension())||!town.contains(home.pos()))continue;
                if(houses.stream().noneMatch(prior->Math.abs(prior.getX()-home.pos().getX())<=6&&Math.abs(prior.getZ()-home.pos().getZ())<=6)&&houses.size()<64)houses.add(home.pos());
            }
            // Every occupied house receives an equal detail budget; no scan-order fragment wins.
            int perHouse=houses.isEmpty()?0:Math.max(1,768/houses.size());
            int side=Math.min(11,Math.max(1,(int)Math.floor(Math.sqrt(perHouse))));
            var seen=new HashSet<Long>();
            for(var house:houses)for(int dz=0;dz<side;dz++)for(int dx=0;dx<side;dx++) {
                int x=house.getX()+(side==1?0:(int)Math.round(dx*10.0/(side-1))-5);
                int z=house.getZ()+(side==1?0:(int)Math.round(dz*10.0/(side-1))-5);
                var at=new BlockPos(x,house.getY(),z);
                if(town.contains(at)&&seen.add(at.asLong()))details.add(at);
            }
            total=surfaceColumns+details.size();
            data.putUUID("Town",town.id()); data.putLong("Anchor",town.center().asLong()); data.putString("Name",town.name());
            data.putInt("Radius",town.territoryRadius()); data.putInt("VerticalRadius",16); data.putInt("Stride",stride); data.putBoolean("City",true);
            data.putInt("MinX",minX); data.putInt("MaxX",town.territoryMaxX()); data.putInt("MinZ",minZ); data.putInt("MaxZ",town.territoryMaxZ());
        }
        CompoundTag facts(net.minecraft.server.level.ServerLevel level,BlockPos pos,BlockState state) {
            var id=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
            var fact=id==null?null:contributions.get(id.toString());
            if(fact==null)return new CompoundTag();
            var result=fact.copy();
            boolean active=AmenityRegistry.get(state).filter(rule->AmenityRegistry.active(level,pos,state,rule)).isPresent();
            result.putBoolean("Active",active);result.putBoolean("Counted",active&&result.getBoolean("Counted"));return result;
        }
        boolean done() { return column >= total; }
    }
}
