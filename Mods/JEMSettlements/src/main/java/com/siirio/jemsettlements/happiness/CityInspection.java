package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.JemSettlements;
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
import net.minecraftforge.fml.common.Mod;

/** Bounded surface LOD snapshots. Never loads a chunk or scans a city on a GUI frame. */
@Mod.EventBusSubscriber(modid = JemSettlements.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
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
        if (event.phase != TickEvent.Phase.END || !event.getServer().isDedicatedServer() || JOBS.isEmpty()) return;
        JOBS.values().removeIf(job -> event.getServer().getPlayerList().getPlayer(job.viewer) == null || event.getServer().overworld().getGameTime() - job.created > 400);
        var pending = JOBS.values().stream().filter(job -> !job.done()).toList();
        if (pending.isEmpty()) return;
        Job job = pending.get(Math.floorMod(cursor++, pending.size()));
        ServerPlayer viewer = event.getServer().getPlayerList().getPlayer(job.viewer);
        var level = event.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, job.dimension));
        if (viewer == null || level == null) { job.column = job.total; job.data.putBoolean("Unloaded", true); return; }
        for (int budget = 0; budget < 128 && !job.done(); budget++, job.column++) {
            int x = job.minX + job.column % job.columns * job.stride, z = job.minZ + job.column / job.columns * job.stride;
            BlockPos probe = new BlockPos(x, level.getMinBuildHeight(), z);
            if (!level.hasChunkAt(probe)) { job.data.putBoolean("Partial", true); continue; }
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            for (int depth = 0; depth < 4 && top - depth >= level.getMinBuildHeight(); depth++) {
                BlockPos pos = new BlockPos(x, top - depth, z); BlockState state = level.getBlockState(pos);
                if (!SnapshotVisibility.disclose(level, pos, state, viewer)) continue;
                int id = job.ids.computeIfAbsent(state, value -> { job.palette.add(NbtUtils.writeBlockState(value)); return job.palette.size()-1; });
                CompoundTag row = new CompoundTag(); row.putLong("Position", pos.asLong()); row.putInt("State", id);
                row.merge(job.facts(level,pos,state)); job.blocks.add(row);
            }
        }
        if (job.done()) {
            var town=com.siirio.jemsettlements.town.SettlementSavedData.get(event.getServer()).town(job.town).orElse(null);
            if(town!=null) {
                var sources=CityComfortService.observedSources(town);
                var present=new java.util.HashSet<Long>();
                for(int i=0;i<job.blocks.size();i++)present.add(job.blocks.getCompound(i).getLong("Position"));
                for(int i=0;i<sources.size()&&job.blocks.size()<8192;i++) {
                    var source=sources.getCompound(i);BlockPos pos=BlockPos.of(source.getLong("Position"));
                    if(present.contains(pos.asLong())||!level.hasChunkAt(pos))continue;
                    var state=level.getBlockState(pos);
                    if(AmenityRegistry.get(state).isEmpty()||!SnapshotVisibility.disclose(level,pos,state,viewer))continue;
                    int id=job.ids.computeIfAbsent(state,value->{job.palette.add(NbtUtils.writeBlockState(value));return job.palette.size()-1;});
                    CompoundTag row=new CompoundTag();row.putLong("Position",pos.asLong());row.putInt("State",id);row.merge(job.facts(level,pos,state));job.blocks.add(row);
                }
            }
            job.data.putLong("Captured", level.getGameTime());
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { JOBS.clear(); cursor=0; }

    private static final class Job {
        final UUID viewer,town; final net.minecraft.resources.ResourceLocation dimension; final String bounds;
        final long created; final int minX, minZ, stride, columns, total;
        final CompoundTag data = new CompoundTag(); final ListTag palette = new ListTag(), blocks = new ListTag();
        final Map<BlockState,Integer> ids = new LinkedHashMap<>(); final Map<String,CompoundTag> contributions;
        int column;
        Job(ServerPlayer player, TownRecord town, String bounds, long now) {
            viewer=player.getUUID(); this.town=town.id(); contributions=CityComfortService.projectionFacts(town); dimension=town.dimension(); this.bounds=bounds; created=now;
            minX=town.territoryMinX(); minZ=town.territoryMinZ();
            stride=Math.max(1,(int)Math.ceil(Math.sqrt(town.territoryArea()/1800D)));
            columns=(town.territoryMaxX()-minX)/stride+1;
            total=columns*((town.territoryMaxZ()-minZ)/stride+1);
            data.putUUID("Town",town.id()); data.putLong("Anchor",town.center().asLong()); data.putString("Name",town.name());
            data.putInt("Radius",town.territoryRadius()); data.putInt("VerticalRadius",1); data.putInt("Stride",stride); data.putBoolean("City",true);
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
