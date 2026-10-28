package com.justenoughmods.achievementguide.integration.magiclast;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
/** The native lightning exchange deliberately cancels with a successful interaction result. */
public final class MagicLastFocusProof {
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private static final ThreadLocal<Map<PlayerInteractEvent.EntityInteractSpecific, Run>> PENDING = ThreadLocal.withInitial(IdentityHashMap::new);
    private static final class Calls { final ArrayDeque<Run> calls = new ArrayDeque<>(); int overflow; }
    private static final class Run {
        final PlayerInteractEvent.EntityInteractSpecific event; final ServerPlayer actor; final ItemStack source; final int bottles, output; final List<ItemEntity> drops = new ArrayList<>();
        InteractionResult result;
        Run(PlayerInteractEvent.EntityInteractSpecific event) { this.event = event; actor = MagicLastProof.real(event.getEntity()) && event.getTarget() instanceof Creeper creeper && creeper.isPowered() && event.getEntity().getItemInHand(event.getHand()).is(Items.GLASS_BOTTLE) ? (ServerPlayer)event.getEntity() : null; source = event.getEntity().getItemInHand(event.getHand()); bottles = source.getCount(); output = actor == null ? 0 : MagicLastProof.held(actor, new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get())); }
    }
    private MagicLastFocusProof() {}
    public static void begin(PlayerInteractEvent.EntityInteractSpecific event) { Calls calls = CALLS.get(); if (calls.overflow > 0 || calls.calls.size() >= MagicLastProof.LIMIT) calls.overflow++; else calls.calls.push(new Run(event)); }
    public static void spawned(Entity output, boolean accepted) { Calls calls = CALLS.get(); if (!accepted || !(output instanceof ItemEntity drop) || calls.overflow > 0 || calls.calls.isEmpty()) return; Run run = calls.calls.peek(); if (run.actor != null && drop.level() == run.actor.level() && MagicLastProof.item(drop.getItem()).equals("irons_spellbooks:lightning_bottle") && run.drops.size() < MagicLastProof.LIMIT) run.drops.add(drop); }
    public static void end() { Calls calls = CALLS.get(); if (calls.overflow > 0) { calls.overflow--; return; } Run run = calls.calls.isEmpty() ? null : calls.calls.pop(); if (calls.calls.isEmpty()) CALLS.remove(); if (run != null && run.actor != null && run.event.isCanceled() && run.event.getCancellationResult().consumesAction()) { run.result = run.event.getCancellationResult(); if (PENDING.get().size() >= 64) PENDING.get().clear(); PENDING.get().put(run.event, run); } }
    public static void finish(net.minecraftforge.eventbus.api.Event event) {
        if (!(event instanceof PlayerInteractEvent.EntityInteractSpecific specific)) return; Run run = PENDING.get().remove(specific);
        if (run == null || !MagicLastProof.real(run.actor) || !specific.isCanceled() || specific.getCancellationResult() != run.result || run.bottles - run.source.getCount() != 1) return;
        ItemStack output = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get());
        if (MagicLastProof.held(run.actor, output) - run.output == 1) MagicLastProof.emit(run.actor, "v3_magic_focus_lightning");
        else if (run.drops.size() == 1 && run.drops.get(0).getItem().getCount() == 1) MagicLastOutputProof.certify(run.drops.get(0), run.actor, "v3_magic_focus_lightning");
    }
    public static void clear() { CALLS.remove(); PENDING.remove(); }
}
