package com.justenoughmods.achievementguide.integration.combattools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

public final class CombatTentacleProof {
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Run> calls = new ArrayDeque<>(); final ArrayDeque<Vec3> pushes = new ArrayDeque<>(); int overflow, pushOverflow; }
    private static final class Run {
        final ServerPlayer actor; final ItemStack tool; final Item item; boolean paid;
        final List<Force> forces = new ArrayList<>();
        Run(ServerPlayer actor, ItemStack tool) { this.actor = actor; this.tool = tool; this.item = tool.getItem(); }
    }
    private record Force(Entity target, Vec3 force) {}
    private CombatTentacleProof() {}
    public static void clear() { CALLS.remove(); }
    public static void begin(Entity actor, ItemStack tool) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0 || calls.calls.size() >= CombatToolProof.LIMIT) { calls.overflow++; return; }
        calls.calls.push(new Run(CombatToolProof.real(actor) ? (ServerPlayer)actor : null, tool));
    }
    private static Run current() { Calls calls = CALLS.get(); return calls.overflow == 0 && !calls.calls.isEmpty() ? calls.calls.peek() : null; }
    public static void paid(ItemStack item) { Run run = current(); if (run != null && run.tool == item) run.paid = true; }
    public static void pushBegin(Entity target) { Calls calls = CALLS.get(); if (calls.pushOverflow > 0 || calls.pushes.size() >= CombatToolProof.LIMIT) calls.pushOverflow++; else calls.pushes.push(target.getDeltaMovement()); }
    public static void pushEnd(Entity target, Vec3 force) { Calls calls = CALLS.get(); if (calls.pushOverflow > 0) calls.pushOverflow--; else if (!calls.pushes.isEmpty()) force(target, calls.pushes.pop(), force); if (calls.pushes.isEmpty() && calls.calls.isEmpty()) CALLS.remove(); }
    public static void pushEndDelta(Entity target) { Calls calls = CALLS.get(); if (calls.pushOverflow > 0) calls.pushOverflow--; else if (!calls.pushes.isEmpty()) { Vec3 before = calls.pushes.pop(); force(target, before, target.getDeltaMovement().subtract(before)); } if (calls.pushes.isEmpty() && calls.calls.isEmpty()) CALLS.remove(); }
    public static void force(Entity target, Vec3 before, Vec3 force) {
        Run run = current();
        if (run != null && run.actor != null && run.forces.size() < CombatToolProof.LIMIT && target.level() == run.actor.level()
                && target.getDeltaMovement().distanceToSqr(before) > 0.000001 && force.lengthSqr() > 0.000001) run.forces.add(new Force(target, force));
    }
    public static void end(boolean accepted) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0) { calls.overflow--; return; }
        Run run = calls.calls.isEmpty() ? null : calls.calls.pop();
        if (calls.calls.isEmpty()) CALLS.remove();
        if (run == null || run.actor == null || !accepted || !run.paid || !run.actor.getCooldowns().isOnCooldown(run.item)) return;
        for (Force force : run.forces) CombatPullProof.applied(run.actor, force.target, force.force);
    }
}
