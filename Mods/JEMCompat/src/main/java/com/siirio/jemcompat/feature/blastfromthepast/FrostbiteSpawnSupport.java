package com.siirio.jemcompat.feature.blastfromthepast;

import com.siirio.jemcompat.JEMCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Mod.EventBusSubscriber(modid = JEMCompat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FrostbiteSpawnSupport {
    private static final String MOD_ID = "blastfromthepast";
    private static final List<ResourceLocation> BIOMES = List.of(
            id("frostbite_forest"),
            id("frostbite_river")
    );
    private static final List<ResourceLocation> SPECIES = List.of(
            id("burrel"),
            id("speartooth"),
            id("snowdo"),
            id("glaceros"),
            id("psycho_bear"),
            id("frostomper")
    );
    private static final int CHECK_INTERVAL = 40;
    private static final int LOCAL_TARGET = 18;
    private static final int HORIZONTAL_RANGE = 64;
    private static final int VERTICAL_RANGE = 32;
    private static final int MIN_SPAWN_DISTANCE = 24;
    private static final int SPAWN_DISTANCE_VARIATION = 32;
    private static final int ATTEMPTS_PER_SPECIES = 8;

    private FrostbiteSpawnSupport() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        int tickSlot = (int) (level.getGameTime() % CHECK_INTERVAL);
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator()
                    && Math.floorMod(player.getUUID().hashCode(), CHECK_INTERVAL) == tickSlot
                    && isFrostbiteBiome(level, player.blockPosition())
                    && replenishNear(level, player)) {
                return;
            }
        }
    }

    private static boolean replenishNear(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(HORIZONTAL_RANGE, VERTICAL_RANGE, HORIZONTAL_RANGE);
        List<Mob> nearby = level.getEntitiesOfClass(Mob.class, area, FrostbiteSpawnSupport::isBlastFromThePastMob);
        if (nearby.size() >= LOCAL_TARGET) {
            return false;
        }
        int[] counts = new int[SPECIES.size()];
        for (Mob mob : nearby) {
            int index = SPECIES.indexOf(entityId(mob));
            if (index >= 0) {
                counts[index]++;
            }
        }
        boolean[] attempted = new boolean[SPECIES.size()];
        for (int attempt = 0; attempt < SPECIES.size(); attempt++) {
            int selected = leastPopulated(counts, attempted);
            attempted[selected] = true;
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(SPECIES.get(selected));
            if (type != null && trySpawn(level, player, type)) {
                return true;
            }
        }
        return false;
    }

    private static int leastPopulated(int[] counts, boolean[] attempted) {
        int selected = -1;
        for (int index = 0; index < counts.length; index++) {
            if (!attempted[index] && (selected < 0 || counts[index] < counts[selected])) {
                selected = index;
            }
        }
        return selected;
    }

    private static boolean trySpawn(ServerLevel level, ServerPlayer player, EntityType<?> type) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < ATTEMPTS_PER_SPECIES; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int distance = MIN_SPAWN_DISTANCE + random.nextInt(SPAWN_DISTANCE_VARIATION);
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isLoaded(pos) || !isFrostbiteBiome(level, pos) || !canSpawn(type, level, pos, random)) {
                continue;
            }
            if (!(type.create(level) instanceof Mob mob)) {
                return false;
            }
            mob.moveTo(x + 0.5D, y, z + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(mob)) {
                mob.discard();
                continue;
            }
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
            return level.addFreshEntity(mob);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static boolean canSpawn(EntityType<?> type, ServerLevel level, BlockPos pos, RandomSource random) {
        return SpawnPlacements.checkSpawnRules((EntityType<Mob>) type, level, MobSpawnType.NATURAL, pos, random);
    }

    private static boolean isFrostbiteBiome(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(key -> BIOMES.contains(key.location())).orElse(false);
    }

    private static boolean isBlastFromThePastMob(Mob mob) {
        ResourceLocation id = entityId(mob);
        return id != null && MOD_ID.equals(id.getNamespace());
    }

    private static ResourceLocation entityId(Mob mob) {
        return ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
