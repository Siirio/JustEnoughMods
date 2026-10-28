package com.justenoughmods.achievementguide.integration.wildlife;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;

public final class WildlifeQuestHurt {
    public static final class Run {
        public final LivingEntity target; public final DamageSource source; public final ServerPlayer actor; public final float health;
        public final ItemStack used; public ShieldBlockEvent shield; public LivingDamageEvent emu;
        public Run(LivingEntity target, DamageSource source) {
            this.target = target; this.source = source; actor = target instanceof Player player ? WildlifeQuestProof.real(player) : null;
            health = target.getHealth(); used = target.getUseItem().copy();
        }
    }
    private static final class Calls { final Deque<Run> rows = new ArrayDeque<>(); int overflow; }
    private static final ThreadLocal<Calls> CURRENT = ThreadLocal.withInitial(Calls::new);
    private WildlifeQuestHurt() {}
    public static void begin(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Player)) return;
        Calls calls = CURRENT.get();
        if (calls.overflow > 0 || calls.rows.size() >= 16) calls.overflow++;
        else calls.rows.push(new Run(entity, source));
    }
    public static void shield(ShieldBlockEvent event) {
        Calls calls = CURRENT.get(); Run run = calls.overflow > 0 ? null : calls.rows.peek();
        if (run != null && run.target == event.getEntity() && run.source == event.getDamageSource()) run.shield = event;
    }
    public static void emu(LivingDamageEvent event) {
        Calls calls = CURRENT.get(); Run run = calls.overflow > 0 ? null : calls.rows.peek(); if (run != null && run.target == event.getEntity() && run.source == event.getSource()) run.emu = event;
    }
    public static void end(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Player)) return;
        Calls calls = CURRENT.get();
        if (calls.overflow > 0) { calls.overflow--; return; }
        var stack = calls.rows; Run run = stack.peek();
        if (run == null || run.target != entity || run.source != source) return;
        stack.pop(); if (stack.isEmpty()) CURRENT.remove(); if (WildlifeQuestProof.real(run.actor) == null) return;
        if (run.emu != null && run.emu.isCanceled() && entity.getHealth() == run.health) WildlifeQuestProof.emit(run.actor, "v3_fauna_wild_gear_emu");
        if (run.shield == null || run.shield.isCanceled() || run.shield.getBlockedDamage() <= 0) return;
        String item = WildlifeQuestProof.item(run.used);
        if (item.equals("alexsmobs:skelewag_sword")) WildlifeQuestProof.emit(run.actor, "v3_fauna_wild_gear_parry");
        if (item.equals("alexsmobs:shield_of_the_deep")) {
            ItemStack current = entity.getUseItem();
            var tools = WildlifeQuestTools.get(run.actor.serverLevel()); var origin = tools.origin(run.used);
            if (origin == null && current.getItem() == run.used.getItem()) origin = tools.origin(current);
            boolean crafted = WildlifeQuestTools.consume(run.actor, "wildlife_shield", origin);
            if (crafted) WildlifeQuestProof.emit(run.actor, "v3_fauna_ocean_finds");
            if (source.getEntity() instanceof LivingEntity attacker && WildlifeQuestProof.matches(attacker, "deep_shield_actor", run.actor.getUUID())
                    && WildlifeQuestProof.data(attacker).getLong("deep_shield_time") == run.actor.level().getGameTime()) {
                WildlifeQuestProof.emit(run.actor, "v3_fauna_sea_defense");
            }
        }
    }
}
