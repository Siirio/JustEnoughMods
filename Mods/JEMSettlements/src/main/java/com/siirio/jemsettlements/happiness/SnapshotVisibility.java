package com.siirio.jemsettlements.happiness;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Server-only projected geometry. Resource states are replaced before palette/facts construction. */
public final class SnapshotVisibility {
    private static final List<BlockPos> HOST_OFFSETS;
    private static final Set<String> HOST_TAGS=Set.of("minecraft:base_stone_overworld","minecraft:base_stone_nether",
            "minecraft:stone_ore_replaceables","minecraft:deepslate_ore_replaceables","forge:stone","forge:stones","c:stones","forge:netherrack","forge:end_stones");
    // Exact natural-rock registrations lacking a common host tag in the installed static evidence.
    private static final Set<String> EXTRA_HOSTS=Set.of("minecraft:end_stone","minecraft:netherrack","minecraft:basalt","minecraft:smooth_basalt",
            "alexscaves:guanostone","alexscaves:coprolith","alexscaves:galena","alexscaves:abyssmarine");
    static {
        var offsets=new ArrayList<BlockPos>();
        for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++)for(int x=-3;x<=3;x++) {
            int distance=x*x+y*y+z*z;if(distance>0&&distance<=9)offsets.add(new BlockPos(x,y,z));
        }
        offsets.sort(Comparator.<BlockPos>comparingInt(pos->pos.getX()*pos.getX()+pos.getY()*pos.getY()+pos.getZ()*pos.getZ())
                .thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ).thenComparingInt(BlockPos::getX));
        HOST_OFFSETS=List.copyOf(offsets);
    }
    private static net.minecraft.server.MinecraftServer budgetServer;
    private static int budgetTick=-1,globalHostReads;
    private SnapshotVisibility() { }
    private static boolean hostReadAvailable(ServerLevel level) {
        var server=level.getServer();int tick=server.getTickCount();
        if(server!=budgetServer||tick!=budgetTick){budgetServer=server;budgetTick=tick;globalHostReads=0;}
        return globalHostReads<256;
    }

    public static boolean resource(BlockState state) {
        var id=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if(id!=null&&(id.getPath().endsWith("_ore")||id.getPath().startsWith("ore_")||state.is(Blocks.ANCIENT_DEBRIS)))return true;
        return state.getTags().anyMatch(tag->{String path=tag.location().getPath();return path.equals("ores")||path.equals("ore")||path.startsWith("ores/")||path.endsWith("_ores");});
    }
    private static boolean host(ServerLevel level,BlockPos pos,BlockState state) {
        if(state.isAir()||resource(state)||state.hasBlockEntity()||!state.getFluidState().isEmpty()||!state.isCollisionShapeFullBlock(level,pos))return false;
        var id=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id!=null&&EXTRA_HOSTS.contains(id.toString())||state.getTags().anyMatch(tag->HOST_TAGS.contains(tag.location().toString()));
    }

    /** One context per snapshot job; cached local samples cost no new world reads.
     * At most4096 host-world reads/job and256 globally/server tick, independent of ore count. */
    public static final class Projection {
        private final Map<BlockPos,BlockState> samples=new HashMap<>();
        private final Map<BlockPos,BlockState> replacements=new HashMap<>();
        private int totalReads,tickReads;
        public void beginTick() { tickReads=0; }
        public BlockState project(ServerLevel level,BlockPos pos,BlockState state) {
            if(state.isAir())return null;
            if(!resource(state))return state;
            var prior=replacements.get(pos);if(prior!=null)return prior;
            BlockState replacement=null;
            for(var offset:HOST_OFFSETS) {
                BlockPos candidate=pos.offset(offset);BlockState observed=samples.get(candidate);
                if(observed==null&&totalReads<4096&&tickReads<256&&hostReadAvailable(level)) {
                    if(level.getChunkSource().getChunkNow(candidate.getX()>>4,candidate.getZ()>>4)==null)continue;
                    observed=level.getBlockState(candidate);samples.put(candidate.immutable(),observed);totalReads++;tickReads++;globalHostReads++;
                }
                if(observed!=null&&host(level,candidate,observed)){replacement=observed;break;}
            }
            if(replacement==null)replacement=fallback(level,pos);
            replacements.put(pos.immutable(),replacement);return replacement;
        }
    }
    private static BlockState fallback(ServerLevel level,BlockPos pos) {
        if(level.dimension().equals(Level.NETHER))return Blocks.NETHERRACK.defaultBlockState();
        if(level.dimension().equals(Level.END))return Blocks.END_STONE.defaultBlockState();
        return pos.getY()<0?Blocks.DEEPSLATE.defaultBlockState():Blocks.STONE.defaultBlockState();
    }
}
