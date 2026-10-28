package com.justenoughmods.achievementguide.integration.combattools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

/** Runs quest-only receipts after the actual Forge dispatch has reached its final cancellation state. */
public final class CombatDeferredProof {
    private static final ThreadLocal<Map<Event, List<Runnable>>> PENDING = ThreadLocal.withInitial(LinkedHashMap::new);
    private CombatDeferredProof() {}
    public static void clear() { PENDING.remove(); }
    public static void defer(Event event, Runnable receipt) {
        if (event == null) return;
        Map<Event, List<Runnable>> pending = PENDING.get();
        if (pending.size() >= 64 && !pending.containsKey(event)) pending.remove(pending.keySet().iterator().next());
        List<Runnable> rows = pending.computeIfAbsent(event, ignored -> new ArrayList<>());
        if (rows.size() < CombatToolProof.LIMIT) rows.add(receipt);
    }
    public static void finish(Event event) {
        Map<Event, List<Runnable>> pending = PENDING.get(); List<Runnable> receipts = pending.remove(event);
        if (pending.isEmpty()) PENDING.remove();
        if (receipts == null || event.isCancelable() && event.isCanceled()) return;
        if (event instanceof PlayerInteractEvent.RightClickBlock block && (block.getUseBlock() == Event.Result.DENY || block.getUseItem() == Event.Result.DENY)) return;
        for (Runnable receipt : receipts) receipt.run();
    }
    public static void finishEntity(Player actor, Entity target, InteractionHand hand) {
        List<Event> events = new ArrayList<>();
        for (Event event : PENDING.get().keySet()) if (event instanceof PlayerInteractEvent.EntityInteract interact
                && interact.getEntity() == actor && interact.getTarget() == target && interact.getHand() == hand) events.add(event);
        for (Event event : events) finish(event);
        if (PENDING.get().isEmpty()) PENDING.remove();
    }
}
