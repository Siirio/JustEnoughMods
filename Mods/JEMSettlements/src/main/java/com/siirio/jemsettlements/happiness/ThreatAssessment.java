package com.siirio.jemsettlements.happiness;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.npc.Villager;

/** Fear follows native combat and panic state. JEM does not run a second path finder. */
public final class ThreatAssessment {
    public enum Kind { NONE, GLASS, OPEN }
    public record Assessment(Kind kind, boolean complete) { }

    private ThreatAssessment() { }

    public static Assessment request(ServerLevel level, LivingEntity resident, Mob hostile) {
        boolean recentDamage = resident.getLastHurtByMob() == hostile
                && resident.tickCount - resident.getLastHurtByMobTimestamp() <= 200;
        boolean targeted = hostile.getTarget() == resident;
        boolean panic = resident instanceof Villager villager
                && villager.getBrain().getActiveNonCoreActivity().filter(Activity.PANIC::equals).isPresent();
        boolean raid = level.getRaidAt(resident.blockPosition()) != null;
        return new Assessment(targeted || recentDamage || panic || raid ? Kind.OPEN : Kind.NONE, true);
    }

    public static void clear() { }
    public static void invalidate(ServerLevel level, BlockPos changed) { }
}
