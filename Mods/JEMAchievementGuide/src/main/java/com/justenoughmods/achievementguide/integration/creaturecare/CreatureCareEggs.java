package com.justenoughmods.achievementguide.integration.creaturecare;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public final class CreatureCareEggs {
    public static final String KIND = "creature_eagle_egg";
    public static final class Harvest {
        final ServerPlayer actor; final BlockPos pos; final List<ItemEntity> drops = new ArrayList<>(); ItemStack expected;
        Harvest(ServerPlayer actor, BlockPos pos) { this.actor = actor; this.pos = pos.immutable(); }
    }
    public static final class Hatch {
        final ServerLevel level; final BlockPos pos; final List<CreatureCareLineage.Egg> sources; final List<Entity> babies = new ArrayList<>();
        Hatch(ServerLevel level, BlockPos pos, int count) { this.level = level; this.pos = pos.immutable(); sources = CreatureCareLineage.get(level).eggs(level, pos, count); }
    }
    private static final ThreadLocal<Harvest> HARVEST = new ThreadLocal<>();
    private static final ThreadLocal<Hatch> HATCH = new ThreadLocal<>();
    private CreatureCareEggs() {}
    public static boolean egg(BlockState state) { return "primal:eagle_egg".equals(String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock()))); }
    public static int count(BlockState state) {
        if (!egg(state)) return 0;
        for (var property : state.getProperties()) if (property.getName().equals("eggs")) return (Integer)state.getValue(property);
        return 0;
    }
    public static Harvest harvest(ServerPlayer actor, BlockPos pos) {
        HARVEST.remove(); if (actor == null) return null;
        Harvest run = new Harvest(actor, pos); HARVEST.set(run); return run;
    }
    public static void expected(Level level, BlockPos pos, ItemStack stack) {
        Harvest run = HARVEST.get();
        if (run != null && run.actor.level() == level && run.pos.equals(pos) && run.expected == null) run.expected = stack;
    }
    public static void expectedEnd(ItemStack stack) { Harvest run = HARVEST.get(); if (run != null && run.expected == stack) run.expected = null; }
    public static void harvested(Harvest run) {
        harvested(run, true);
    }
    public static void harvested(Harvest run, boolean nativeSuccess) {
        if (HARVEST.get() == run) HARVEST.remove(); if (run == null || !nativeSuccess) return;
        for (ItemEntity drop : run.drops) {
            if (run.actor.serverLevel().getEntity(drop.getUUID()) == drop && CreatureCareProof.item(drop.getItem()).equals("primal:eagle_egg")) {
                NativeFoodSources.get(run.actor.serverLevel()).certify(run.actor.serverLevel(), run.actor.getUUID(), KIND, drop.getItem(), drop.getItem().getCount());
            }
        }
    }
    public static Hatch hatch(ServerLevel level, BlockPos pos, BlockState state) {
        HATCH.remove(); if (!egg(state)) return null;
        Hatch run = new Hatch(level, pos, count(state)); HATCH.set(run); return run;
    }
    public static void inserted(Entity entity, boolean accepted) {
        if (!accepted) return;
        Harvest harvest = HARVEST.get();
        if (harvest != null && entity instanceof ItemEntity drop && entity.level() == harvest.actor.level()
                && drop.getItem() == harvest.expected && harvest.drops.size() < 2) harvest.drops.add(drop);
        Hatch hatch = HATCH.get();
        if (hatch != null && entity.level() == hatch.level && CreatureCareProof.type(entity).equals("primal:eagle")
                && hatch.babies.size() < 2) hatch.babies.add(entity);
    }
    public static void hatched(Hatch run) {
        finishHatch(run, run != null && !egg(run.level.getBlockState(run.pos)));
    }
    public static void finishHatch(Hatch run, boolean nativeEmpty) {
        if (HATCH.get() == run) HATCH.remove(); if (run == null || !nativeEmpty) return;
        CreatureCareLineage.get(run.level).eggs(run.level, run.pos, List.of());
        if (run.sources.size() != run.babies.size()) return;
        for (int i = 0; i < run.babies.size(); i++) {
            Entity entity = run.babies.get(i); var source = run.sources.get(i);
            if (source.actor() != null && source.source() != null && entity instanceof TamableAnimal baby && baby.isBaby() && run.level.getEntity(baby.getUUID()) == baby) {
                CreatureCareProof.mark(baby, "eagle_hatched", source.actor());
                CreatureCareProof.data(baby).putUUID("eagle_egg_source", source.source());
            }
        }
    }
    public static void placed(ServerPlayer actor, BlockPos pos, ItemStack before, int oldEggCount) {
        ServerLevel level = actor.serverLevel(); BlockState state = level.getBlockState(pos);
        if (!CreatureCareProof.item(before).equals("primal:eagle_egg") || count(state) != oldEggCount + 1) return;
        ItemStack spent = NativeFoodSources.spent(before, 1); var physical = NativeFoodSources.get(level);
        if (!actor.getUUID().equals(physical.owner(spent, KIND))) return;
        var claims = NativeFoodSources.claims(spent); if (claims.size() != 1) return;
        var lineage = CreatureCareLineage.get(level); List<CreatureCareLineage.Egg> sources = new ArrayList<>(lineage.eggs(level, pos, oldEggCount));
        if (sources.size() != oldEggCount || sources.size() >= 2 || !physical.consume(actor.getUUID(), spent, KIND, 1)) return;
        sources.add(new CreatureCareLineage.Egg(actor.getUUID(), claims.get(0).batch())); lineage.eggs(level, pos, sources);
    }
    public static void changed(ServerLevel level, BlockPos pos, BlockState before, BlockState after) {
        if (!level.getServer().isSameThread()) return;
        if (!egg(before)) {
            if (before.getBlock() != after.getBlock() && !CreatureCareLineage.get(level).eggs(level, pos).isEmpty()) {
                CreatureCareLineage.get(level).eggs(level, pos, List.of());
            }
            return;
        }
        // Adding an egg preserves the existing siblings. Any removal, replacement or reset invalidates the stored position.
        boolean reset = false;
        if (egg(after)) for (var property : before.getProperties()) {
            if (property.getName().equals("hatch") && (Integer)after.getValue(property) < (Integer)before.getValue(property)) reset = true;
        }
        if (!egg(after) || count(after) < count(before) || reset) CreatureCareLineage.get(level).eggs(level, pos, List.of());
    }
}
