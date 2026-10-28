package com.justenoughmods.achievementguide.integration.combattools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;

/** Exact native Soul Key and hand/grindstone processing transactions. */
public final class CombatRegrowthProof {
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Run> calls = new ArrayDeque<>(); int overflow; }
    private record Run(ServerPlayer actor, LevelAccessor world, String route, Event event, BlockPos pos, BlockState before, int materials, int crystals, List<ItemEntity> drops) {}
    private CombatRegrowthProof() {}
    public static void clear() { CALLS.remove(); }
    private static int count(ServerPlayer actor, String id) {
        int result = 0;
        for (int slot = 0; slot < actor.getInventory().getContainerSize(); slot++) { ItemStack stack = actor.getInventory().getItem(slot); if (CombatToolProof.item(stack).equals(id)) result += stack.getCount(); }
        return result;
    }
    public static void begin(String route, LevelAccessor world, Entity user, Event event, double x, double y, double z) {
        Calls calls = CALLS.get(); if (calls.overflow > 0 || calls.calls.size() >= CombatToolProof.LIMIT) { calls.overflow++; return; }
        ServerPlayer actor = CombatToolProof.real(user) ? (ServerPlayer)user : null; BlockPos pos = BlockPos.containing(x, y, z);
        String material = route.equals("key") ? "deep_dark_regrowth:soul_key" : "deep_dark_regrowth:mossy_fluorite";
        if (actor != null && (actor.level() != world || !actor.level().hasChunkAt(pos) || !CombatToolProof.item(actor.getMainHandItem()).equals(material))) actor = null;
        calls.calls.push(new Run(actor, world, route, event, pos, actor == null ? Blocks.AIR.defaultBlockState() : actor.level().getBlockState(pos),
                actor == null ? 0 : count(actor, material), actor == null ? 0 : count(actor, "deep_dark_regrowth:fluorite_crystal"), new ArrayList<>()));
    }
    public static void spawned(Entity entity, boolean accepted) {
        Calls calls = CALLS.get(); if (!accepted || !(entity instanceof ItemEntity drop) || calls.overflow > 0 || calls.calls.isEmpty()) return;
        Run run = calls.calls.peek();
        if (run.actor != null && run.route.equals("fluorite") && drop.level() == run.actor.level() && CombatToolProof.item(drop.getItem()).equals("deep_dark_regrowth:fluorite_crystal") && run.drops.size() < CombatToolProof.LIMIT) run.drops.add(drop);
    }
    public static void end() {
        Calls calls = CALLS.get(); if (calls.overflow > 0) { calls.overflow--; return; }
        Run run = calls.calls.isEmpty() ? null : calls.calls.pop(); if (calls.calls.isEmpty()) CALLS.remove();
        if (run == null || run.actor == null) return;
        Runnable receipt = () -> {
            if (!CombatToolProof.real(run.actor) || run.actor.level() != run.world) return;
            if (run.route.equals("key") && BuiltInRegistries.BLOCK.getKey(run.before.getBlock()).toString().equals("deep_dark_regrowth:unbreakable_door_keyframe")
                    && run.actor.level().getBlockState(run.pos).isAir() && run.materials - count(run.actor, "deep_dark_regrowth:soul_key") == 1) CombatToolProof.emit(run.actor, "sculk_vault_unlocked");
            if (run.route.equals("fluorite") && run.before.is(Blocks.GRINDSTONE) && run.materials - count(run.actor, "deep_dark_regrowth:mossy_fluorite") == 1
                    && count(run.actor, "deep_dark_regrowth:fluorite_crystal") - run.crystals == 1) CombatToolProof.emit(run.actor, "fluorite_light_processed");
            else if (run.route.equals("fluorite") && run.before.is(Blocks.GRINDSTONE) && run.materials - count(run.actor, "deep_dark_regrowth:mossy_fluorite") == 1
                    && count(run.actor, "deep_dark_regrowth:fluorite_crystal") == run.crystals && run.drops.size() == 1 && run.drops.get(0).getItem().getCount() == 1 && !run.drops.get(0).isRemoved()) {
                CombatDropProof.enter(new CombatDropProof.Context(List.of(run.actor.getUUID()), "fluorite_light_processed"));
                try { CombatDropProof.spawned(run.drops.get(0), true); } finally { CombatDropProof.leave(); }
            }
        };
        if (run.event != null) CombatDeferredProof.defer(run.event, receipt); else receipt.run();
    }
}
