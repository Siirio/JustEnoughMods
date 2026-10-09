package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.SmpData;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class ArenaSurfaceRules {
    private static final int MIN_ICE_LIFETIME_TICKS = 20;
    private static final int ICE_LIFETIME_VARIATION_TICKS = 20;
    private static final int FROST_WALKER_RADIUS = 4;
    private static final Map<ServerLevel, Map<BlockPos, Long>> MELTING_ICE = new HashMap<>();

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !insideArena(level, event.getPos())) return;
        schedule(level, event.getPos(), level.getGameTime());
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if((now&3)==0) for (var player : level.players()) if (insideArena(level, player.blockPosition()))
            for (int x = -FROST_WALKER_RADIUS; x <= FROST_WALKER_RADIUS; x++)
                for (int z = -FROST_WALKER_RADIUS; z <= FROST_WALKER_RADIUS; z++)
                    for (int y = -1; y <= 0; y++) scheduleFrosted(level, player.blockPosition().offset(x, y, z), now);
        Iterator<Map.Entry<BlockPos, Long>> iterator = MELTING_ICE.computeIfAbsent(level, ignored -> new HashMap<>()).entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue() > now) continue;
            if (isIce(level, entry.getKey()) && insideArena(level, entry.getKey())) evaporate(level,entry.getKey());
            iterator.remove();
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        MELTING_ICE.clear();
    }

    private static void schedule(ServerLevel level, BlockPos pos, long now) {
        if (isIce(level, pos)) MELTING_ICE.computeIfAbsent(level, ignored -> new HashMap<>()).putIfAbsent(pos.immutable(),
                now+MIN_ICE_LIFETIME_TICKS+level.random.nextInt(ICE_LIFETIME_VARIATION_TICKS+1));
    }

    private static void scheduleFrosted(ServerLevel level, BlockPos pos, long now) {
        if (level.getBlockState(pos).is(Blocks.FROSTED_ICE)) schedule(level, pos, now);
    }

    private static boolean isIce(ServerLevel level, BlockPos pos) {
        String path = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath();
        return path.contains("ice") || path.contains("frost") || path.contains("glacier") || path.contains("glacial")
                || path.contains("frozen") || path.contains("rime") || path.contains("icicle") || path.contains("permafrost");
    }

    private static void evaporate(ServerLevel level,BlockPos pos) {
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        level.playSound(null,pos,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.7F,1.8F+level.random.nextFloat()*.4F);
        level.sendParticles(ParticleTypes.CLOUD,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,12,.35,.25,.35,.03);
    }

    private static boolean insideArena(ServerLevel level, BlockPos pos) {
        return EventScheduler.active(level.getServer()).stream()
                .filter(row -> row.contains("radius"))
                .anyMatch(row -> EventRegions.contains(row, level, pos));
    }

    private ArenaSurfaceRules() {
    }
}
