package com.siirio.jemcompat.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public final class CampaignBarrierData extends SavedData {
    private static final String FILE_ID="jemcompat_campaign_barriers";
    private final List<Barrier> barriers=new ArrayList<>();

    public static CampaignBarrierData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CampaignBarrierData::load,CampaignBarrierData::new,FILE_ID);
    }

    public void ensure(ServerLevel level,CampaignBoss boss,BoundingBox bounds) {
        Barrier barrier=barriers.stream().filter(value->value.matches(level,boss,bounds)).findFirst().orElse(null);
        if(barrier==null) {
            barrier=new Barrier(boss,level.dimension().location(),bounds);
            barriers.add(barrier);
            setDirty();
        }
        removeLoaded(level,barrier);
    }

    public Barrier near(ServerPlayerView player) {
        return barriers.stream().filter(value->value.dimension().equals(player.dimension())&&value.distance(player.x(),player.y(),player.z())<=2.5D).findFirst().orElse(null);
    }

    public void reconcile(MinecraftServer server) {
        for(Barrier barrier:barriers) {
            ServerLevel level=level(server,barrier.dimension());
            if(level==null) continue;
            removeLoaded(level,barrier);
        }
    }

    private static void removeLoaded(ServerLevel level,Barrier barrier) { visitShell(level,barrier,CampaignBarrierData::remove); }

    private static void visitShell(ServerLevel level,Barrier barrier,CellAction action) {
        BoundingBox box=barrier.shell();
        for(int x=box.minX();x<=box.maxX();x++) for(int z=box.minZ();z<=box.maxZ();z++) {
            action.apply(level,new BlockPos(x,box.minY(),z));
            action.apply(level,new BlockPos(x,box.maxY(),z));
        }
        for(int y=box.minY()+1;y<box.maxY();y++) {
            for(int x=box.minX();x<=box.maxX();x++) {
                action.apply(level,new BlockPos(x,y,box.minZ()));
                action.apply(level,new BlockPos(x,y,box.maxZ()));
            }
            for(int z=box.minZ()+1;z<box.maxZ();z++) {
                action.apply(level,new BlockPos(box.minX(),y,z));
                action.apply(level,new BlockPos(box.maxX(),y,z));
            }
        }
    }

    private static void remove(ServerLevel level,BlockPos pos) {
        if(level.hasChunkAt(pos)&&level.getBlockState(pos).is(CampaignBlocks.LOCKED_BOSS_BARRIER.get())) level.removeBlock(pos,false);
    }

    private static ServerLevel level(MinecraftServer server,ResourceLocation dimension) {
        return server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimension));
    }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag list=new ListTag();
        for(Barrier barrier:barriers) list.add(barrier.save());
        tag.put("Barriers",list);
        return tag;
    }

    private static CampaignBarrierData load(CompoundTag tag) {
        CampaignBarrierData data=new CampaignBarrierData();
        for(Tag value:tag.getList("Barriers",Tag.TAG_COMPOUND)) {
            Barrier barrier=Barrier.load((CompoundTag)value);
            if(barrier!=null) data.barriers.add(barrier);
        }
        return data;
    }

    public record ServerPlayerView(ResourceLocation dimension,double x,double y,double z) {}

    public record Barrier(CampaignBoss boss,ResourceLocation dimension,BoundingBox bounds) {
        private boolean matches(ServerLevel level,CampaignBoss target,BoundingBox targetBounds) {
            return boss==target&&dimension.equals(level.dimension().location())
                    &&bounds.minX()==targetBounds.minX()&&bounds.minY()==targetBounds.minY()&&bounds.minZ()==targetBounds.minZ()
                    &&bounds.maxX()==targetBounds.maxX()&&bounds.maxY()==targetBounds.maxY()&&bounds.maxZ()==targetBounds.maxZ();
        }
        private BoundingBox shell() { return new BoundingBox(bounds.minX()-1,bounds.minY()-1,bounds.minZ()-1,bounds.maxX()+1,bounds.maxY()+1,bounds.maxZ()+1); }
        private double distance(double x,double y,double z) {
            BoundingBox box=shell();
            double dx=Math.max(Math.max(box.minX()-x,0),x-box.maxX()-1);
            double dy=Math.max(Math.max(box.minY()-y,0),y-box.maxY()-1);
            double dz=Math.max(Math.max(box.minZ()-z,0),z-box.maxZ()-1);
            return Math.max(dx,Math.max(dy,dz));
        }
        private CompoundTag save() {
            CompoundTag tag=new CompoundTag();
            tag.putString("Boss",boss.key());tag.putString("Dimension",dimension.toString());
            tag.putIntArray("Bounds",new int[]{bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ()});
            return tag;
        }
        private static Barrier load(CompoundTag tag) {
            try {
                CampaignBoss boss=java.util.Arrays.stream(CampaignBoss.values())
                        .filter(value->value.key().equals(tag.getString("Boss")))
                        .findFirst().orElse(null);
                ResourceLocation dimension=ResourceLocation.tryParse(tag.getString("Dimension"));
                int[] bounds=tag.getIntArray("Bounds");
                return boss==null||dimension==null||bounds.length!=6?null:new Barrier(boss,dimension,new BoundingBox(bounds[0],bounds[1],bounds[2],bounds[3],bounds[4],bounds[5]));
            } catch(IllegalArgumentException ignored) { return null; }
        }
    }

    @FunctionalInterface private interface CellAction { void apply(ServerLevel level,BlockPos pos); }
}
