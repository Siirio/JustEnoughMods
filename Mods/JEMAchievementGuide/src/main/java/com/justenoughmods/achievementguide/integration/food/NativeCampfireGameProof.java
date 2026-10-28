package com.justenoughmods.achievementguide.integration.food;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;

/** Provisional physical output becomes usable only after accepted spawn and actual source removal. */
public final class NativeCampfireGameProof {
    private static final class Portion {
        final int slot;
        final ItemStack source;
        final UUID actor;
        final UUID batch;
        boolean inserted;
        Portion(int slot, ItemStack source, UUID actor, UUID batch) { this.slot = slot; this.source = source; this.actor = actor; this.batch = batch; }
    }
    private static final class Run {
        final ServerLevel level;
        final CampfireBlockEntity owner;
        final List<Portion> portions = new ArrayList<>();
        ItemStack selected;
        int slot;
        Run(ServerLevel level, CampfireBlockEntity owner) { this.level = level; this.owner = owner; }
    }
    private static final ThreadLocal<Run> CURRENT = new ThreadLocal<>();
    private NativeCampfireGameProof() {}
    public static void begin(ServerLevel level, CampfireBlockEntity owner) {
        Run interrupted = CURRENT.get(); CURRENT.remove();
        if (interrupted != null) for (Portion portion : interrupted.portions) NativeFoodSources.get(interrupted.level).activate(portion.batch, false);
        CURRENT.set(new Run(level, owner));
    }
    public static void selected(CampfireBlockEntity owner, int slot, ItemStack source) {
        Run run = CURRENT.get();
        if (run != null && run.owner == owner) { run.selected = source.copy(); run.slot = slot; }
    }
    public static void output(ItemStack output) {
        Run run = CURRENT.get();
        if (run == null || run.selected == null || run.portions.size() >= 4) return;
        String raw = RemainingFoodActors.item(run.selected); String cooked = RemainingFoodActors.item(output);
        if (!(raw.equals("cnc:raw_venison") && cooked.equals("cnc:cooked_venison"))
                && !(raw.equals("cnc:raw_turkey") && cooked.equals("cnc:cooked_turkey"))) return;
        var data = NativeFoodSources.get(run.level); var spent = NativeFoodSources.spent(run.selected, 1); var actor = data.owner(spent, "native_game");
        if (actor == null) return;
        UUID batch = data.provisional(actor, "prepared_game", output, output.getCount());
        if (batch != null) run.portions.add(new Portion(run.slot, spent, actor, batch));
    }
    public static void inserted(Entity entity, boolean accepted) {
        Run run = CURRENT.get();
        if (!accepted || run == null || entity.level() != run.level || !(entity instanceof ItemEntity output)) return;
        for (var claim : NativeFoodSources.claims(output.getItem())) {
            for (Portion portion : run.portions) if (portion.batch.equals(claim.batch())) portion.inserted = true;
        }
    }
    public static void end() {
        Run run = CURRENT.get(); CURRENT.remove();
        if (run == null) return;
        var data = NativeFoodSources.get(run.level);
        for (Portion portion : run.portions) {
            boolean committed = portion.inserted && portion.slot < run.owner.getItems().size() && run.owner.getItems().get(portion.slot).isEmpty()
                    && data.consume(portion.actor, portion.source, "native_game", 1);
            data.activate(portion.batch, committed);
        }
    }
}
