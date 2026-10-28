package com.justenoughmods.achievementguide.integration.magiclast;
import java.util.ArrayDeque;
import net.minecraft.server.level.ServerPlayer;
/** Exact native Auto Heal debit and effective healing, no copied recovery formula. */
public final class MagicLastAutoHealProof {
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Run> calls = new ArrayDeque<>(); int overflow; }
    private static final class Run { final ServerPlayer actor; final float health; boolean cost, healing; Run(ServerPlayer actor) { this.actor = actor; health = actor == null ? Float.NaN : actor.getHealth(); } }
    private MagicLastAutoHealProof() {}
    public static void begin(ServerPlayer actor) { Calls calls = CALLS.get(); if (calls.overflow > 0 || calls.calls.size() >= MagicLastProof.LIMIT) calls.overflow++; else calls.calls.push(new Run(MagicLastProof.real(actor) ? actor : null)); }
    public static void cost(ServerPlayer actor, float oldMana, float newMana, float oldHealth) { Calls calls = CALLS.get(); if (calls.overflow == 0 && !calls.calls.isEmpty()) { Run run = calls.calls.peek(); if (run.actor == actor && (newMana < oldMana || actor.getHealth() < oldHealth)) run.cost = true; } }
    public static void healed(ServerPlayer actor, float before) { Calls calls = CALLS.get(); if (calls.overflow == 0 && !calls.calls.isEmpty() && calls.calls.peek().actor == actor && actor.getHealth() > before) calls.calls.peek().healing = true; }
    public static void end() { Calls calls = CALLS.get(); if (calls.overflow > 0) { calls.overflow--; return; } Run run = calls.calls.isEmpty() ? null : calls.calls.pop(); if (calls.calls.isEmpty()) CALLS.remove(); if (run != null && run.actor != null && run.cost && run.healing && run.actor.getHealth() > run.health) MagicLastProof.emit(run.actor, "v3_magic_mana_trade"); }
    public static void clear() { CALLS.remove(); }
}
