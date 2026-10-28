package com.justenoughmods.achievementguide.integration.combattools;

import java.util.ArrayDeque;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

public final class CombatBrewingProof {
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Run> calls = new ArrayDeque<>(); int overflow; }
    private record Run(BlockEntity owner, ItemStack ingredient, CombatStationProof.Before[] before, ItemStack[] expected) {}
    private CombatBrewingProof() {}
    public static void clear() { CALLS.remove(); }
    private static String potion(ItemStack item) { return BuiltInRegistries.POTION.getKey(PotionUtils.getPotion(item)).toString(); }
    public static void begin(Level level, BlockPos pos) {
        Calls calls = CALLS.get(); if (calls.overflow > 0 || calls.calls.size() >= CombatToolProof.LIMIT) { calls.overflow++; return; }
        BlockEntity owner = !level.isClientSide && level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand ? stand : null;
        CombatStationProof.Before[] before = new CombatStationProof.Before[3]; ItemStack[] expected = new ItemStack[3];
        ItemStack ingredient = owner == null ? ItemStack.EMPTY : ((Container)owner).getItem(3).copy();
        for (int slot = 0; slot < 3; slot++) {
            before[slot] = owner == null ? new CombatStationProof.Before(ItemStack.EMPTY, new CompoundTag()) : CombatStationProof.before(owner, slot);
            expected[slot] = owner == null ? ItemStack.EMPTY : net.minecraftforge.common.brewing.BrewingRecipeRegistry.getOutput(before[slot].item(), ingredient);
        }
        calls.calls.push(new Run(owner, ingredient, before, expected));
    }
    public static void end() {
        Calls calls = CALLS.get(); if (calls.overflow > 0) { calls.overflow--; return; }
        Run run = calls.calls.isEmpty() ? null : calls.calls.pop(); if (calls.calls.isEmpty()) CALLS.remove();
        if (run == null || run.owner == null) return;
        ItemStack afterIngredient = ((Container)run.owner).getItem(3);
        if (run.ingredient.getCount() - afterIngredient.getCount() != 1 || !afterIngredient.isEmpty() && !ItemStack.isSameItemSameTags(run.ingredient, afterIngredient)) return;
        for (int slot = 0; slot < 3; slot++) {
            CombatStationProof.Before before = run.before[slot]; ItemStack actual = ((Container)run.owner).getItem(slot), expected = run.expected[slot];
            if (actual.isEmpty() || expected.isEmpty() || actual.getCount() != before.item().getCount() || actual.getCount() != expected.getCount()
                    || ItemStack.isSameItemSameTags(actual, before.item()) || !ItemStack.isSameItemSameTags(actual, expected)) continue;
            boolean first = CombatToolProof.item(run.ingredient).equals("rottencreatures:frozen_rotten_flesh") && potion(before.item()).equals("rottencreatures:corrupted") && potion(actual).equals("rottencreatures:freeze");
            boolean upgrade = CombatStationProof.matches(before.proof(), before.item()) && before.proof().getString("action").equals("frozen_brewing")
                    && (potion(actual).equals("rottencreatures:long_freeze") || potion(actual).equals("rottencreatures:strong_freeze"));
            if (first || upgrade) CombatStationProof.completed(run.owner, slot, "frozen_brewing", before, true);
        }
    }
}
