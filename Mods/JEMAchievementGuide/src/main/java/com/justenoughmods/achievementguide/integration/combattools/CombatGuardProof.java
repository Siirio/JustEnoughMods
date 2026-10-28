package com.justenoughmods.achievementguide.integration.combattools;

import com.justenoughmods.achievementguide.mixin.CombatGuardMenuQuestAccess;
import java.util.ArrayDeque;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import tallestegg.guardvillagers.entities.Guard;

/** Loaded only from guardvillagers-gated native Mixins. */
public final class CombatGuardProof {
    private static final String KEY = "JemQuestRecruitedGuardV4";
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);
    private static final class State { final ArrayDeque<Recruit> recruits = new ArrayDeque<>(); final ArrayDeque<Patrol> patrols = new ArrayDeque<>(); int recruitOverflow, patrolOverflow; }
    private static final class Recruit {
        final PlayerInteractEvent.EntityInteract event; final ServerPlayer actor; final Villager source; final ItemStack tool; final ItemStack before;
        Guard guard;
        Recruit(PlayerInteractEvent.EntityInteract event) {
            this.event = event; actor = CombatToolProof.real(event.getEntity()) ? (ServerPlayer)event.getEntity() : null;
            source = event.getTarget() instanceof Villager villager ? villager : null;
            tool = event.getEntity().getMainHandItem(); before = tool.copy();
        }
    }
    private record Patrol(ServerPlayer actor, Guard guard, BlockPos before, boolean requested) {}
    private CombatGuardProof() {}
    public static void clear() { STATE.remove(); }
    public static void begin(PlayerInteractEvent.EntityInteract event) {
        State state = STATE.get(); if (state.recruitOverflow > 0 || state.recruits.size() >= CombatToolProof.LIMIT) { state.recruitOverflow++; return; }
        state.recruits.push(new Recruit(event));
    }
    public static void spawned(Entity entity, boolean accepted) {
        State state = STATE.get(); if (state.recruitOverflow > 0 || state.recruits.isEmpty()) return;
        Recruit run = state.recruits.peek();
        if (accepted && run.actor != null && entity instanceof Guard guard && !guard.isRemoved() && guard.level() == run.actor.level()) run.guard = guard;
    }
    public static void end() {
        State state = STATE.get(); if (state.recruitOverflow > 0) { state.recruitOverflow--; return; }
        Recruit run = state.recruits.isEmpty() ? null : state.recruits.pop(); clean(state);
        if (run == null || run.actor == null || run.source == null || run.guard == null) return;
        CombatDeferredProof.defer(run.event, () -> {
            if (!CombatToolProof.real(run.actor) || !run.source.isRemoved() || run.source.getRemovalReason() != Entity.RemovalReason.DISCARDED
                    || run.guard.isRemoved() || run.guard.level() != run.actor.level() || run.guard.level().getEntity(run.guard.getId()) != run.guard || run.before.getCount() - run.tool.getCount() != 1
                    || !ItemStack.isSameItemSameTags(run.before, run.guard.getMainHandItem())) return;
            CompoundTag proof = new CompoundTag(); proof.putUUID("actor", run.actor.getUUID()); proof.putUUID("source", run.source.getUUID());
            run.guard.getPersistentData().put(KEY, proof); CombatToolProof.emit(run.actor, "guard_team_recruited");
        });
    }
    public static void patrolBegin(ServerPlayer actor, Guard guard, boolean pressed) {
        State state = STATE.get(); if (state.patrolOverflow > 0 || state.patrols.size() >= CombatToolProof.LIMIT) { state.patrolOverflow++; return; }
        CompoundTag proof = guard == null ? new CompoundTag() : guard.getPersistentData().getCompound(KEY);
        boolean valid = CombatToolProof.real(actor) && guard != null && actor.containerMenu instanceof CombatGuardMenuQuestAccess menu && menu.jem$guard() == guard
                && proof.hasUUID("actor") && actor.getUUID().equals(proof.getUUID("actor"));
        state.patrols.push(new Patrol(valid ? actor : null, guard, guard == null ? null : guard.getPatrolPos(), !pressed));
    }
    public static void patrolEnd() {
        State state = STATE.get(); if (state.patrolOverflow > 0) { state.patrolOverflow--; return; }
        Patrol run = state.patrols.isEmpty() ? null : state.patrols.pop(); clean(state);
        if (run != null && run.actor != null && run.requested && run.guard.isPatrolling() && run.guard.getPatrolPos() != null
                && run.guard.getPatrolPos().equals(run.guard.blockPosition()) && !run.guard.getPatrolPos().equals(run.before)) CombatToolProof.emit(run.actor, "guard_team");
    }
    private static void clean(State state) { if (state.recruits.isEmpty() && state.patrols.isEmpty()) STATE.remove(); }
}
