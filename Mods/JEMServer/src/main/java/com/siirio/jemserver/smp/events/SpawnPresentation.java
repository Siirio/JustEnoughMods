package com.siirio.jemserver.smp.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.warden.Warden;

public final class SpawnPresentation {
    public enum Archetype { GROUND_EMERGE, SCULK_EMERGE, LIGHTNING_ENTRY, PORTAL_ENTRY, DESCENT, GROUND_RUPTURE }

    private static final String TYPE = "jem:spawn_presentation";
    private static final String START = "jem:spawn_presentation_start";
    private static final String END = "jem:spawn_presentation_end";
    private static final String ACTIVATED = "jem:spawn_presentation_activated";

    public static void begin(ServerLevel level, Mob boss, Archetype archetype, int duration) {
        CompoundTag data = boss.getPersistentData();
        data.putString(TYPE, archetype.name());
        data.putLong(START, level.getGameTime());
        data.putLong(END, level.getGameTime() + duration);
        data.putBoolean(ACTIVATED, false);
        boss.setNoAi(true);
        boss.setInvulnerable(true);
        if (boss instanceof Warden warden && archetype == Archetype.SCULK_EMERGE)
            warden.finalizeSpawn(level, level.getCurrentDifficultyAt(warden.blockPosition()), MobSpawnType.TRIGGERED, null, null);
    }

    public static boolean active(Mob boss) {
        return boss.getPersistentData().contains(TYPE);
    }

    public static boolean tick(ServerLevel level, Mob boss) {
        CompoundTag data = boss.getPersistentData();
        if (!data.contains(TYPE)) return true;
        long now = level.getGameTime();
        long start = data.getLong(START);
        long end = data.getLong(END);
        Archetype archetype = Archetype.valueOf(data.getString(TYPE));
        BlockPos origin = boss.blockPosition();
        if ((now - start) % 8 == 0) progress(level, boss, archetype, origin, Math.max(0, Math.min(1, (now - start) / (double) Math.max(1, end - start))));
        if (now < end) return false;
        if (!data.getBoolean(ACTIVATED)) {
            data.putBoolean(ACTIVATED, true);
            level.playSound(null, origin, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.8F, .65F);
            level.sendParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY() + boss.getBbHeight() * .25, boss.getZ(), 3, boss.getBbWidth() * .25, .2, boss.getBbWidth() * .25, 0);
        }
        data.remove(TYPE);
        data.remove(START);
        data.remove(END);
        data.remove(ACTIVATED);
        boss.setNoAi(false);
        boss.setInvulnerable(false);
        return true;
    }

    private static void progress(ServerLevel level, Mob boss, Archetype archetype, BlockPos origin, double progress) {
        switch (archetype) {
            case SCULK_EMERGE -> {
                level.sendParticles(ParticleTypes.SCULK_SOUL, boss.getX(), boss.getY() + .2, boss.getZ(), 4, boss.getBbWidth() * .35, .1, boss.getBbWidth() * .35, .01);
                level.playSound(null, origin, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.HOSTILE, .8F, .6F + (float) progress * .25F);
            }
            case LIGHTNING_ENTRY -> {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, boss.getX(), boss.getY() + boss.getBbHeight() * progress, boss.getZ(), 6, boss.getBbWidth() * .3, .2, boss.getBbWidth() * .3, .03);
                level.playSound(null, origin, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, .35F, .65F);
            }
            case PORTAL_ENTRY -> level.sendParticles(ParticleTypes.REVERSE_PORTAL, boss.getX(), boss.getY() + boss.getBbHeight() * .5, boss.getZ(), 8, boss.getBbWidth() * .35, boss.getBbHeight() * .3, boss.getBbWidth() * .35, .02);
            case DESCENT -> level.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY(), boss.getZ(), 5, boss.getBbWidth() * .4, .1, boss.getBbWidth() * .4, .02);
            case GROUND_EMERGE, GROUND_RUPTURE -> {
                level.sendParticles(ParticleTypes.POOF, boss.getX(), boss.getY() + .1, boss.getZ(), 5, boss.getBbWidth() * .4, .1, boss.getBbWidth() * .4, .02);
                level.playSound(null, origin, SoundEvents.STONE_BREAK, SoundSource.HOSTILE, .7F, .7F + (float) progress * .2F);
            }
        }
    }

    private SpawnPresentation() {}
}
