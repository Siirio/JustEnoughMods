package com.justenoughmods.achievementguide.integration.combattools;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;

public final class CombatRuneAnvilProof {
    public record Take(ItemStack first, ItemStack second, ItemStack expected, CompoundTag identity, int levels, int cost, boolean bowFirst) {}
    private CombatRuneAnvilProof() {}
    private static ItemStack withoutProof(ItemStack stack) { ItemStack copy = stack.copy(); copy.removeTagKey(CombatRuneProof.BOW); return copy; }
    private static Take recipe(AnvilMenu menu, ServerPlayer actor) {
        ItemStack first = menu.getSlot(0).getItem().copy(), second = menu.getSlot(1).getItem().copy();
        boolean bowFirst = first.getItem() instanceof SimplyBowItem;
        ItemStack bow = bowFirst ? first : second, material = bowFirst ? second : first;
        if (!CombatToolProof.real(actor) || actor.containerMenu != menu || !(bow.getItem() instanceof SimplyBowItem) || bow.getCount() != 1 || !(material.getItem() instanceof BowUpgradeComponentItem component)) return null;
        BowUpgradeData before = BowUpgradeData.from(bow), after = component.applyTo(before);
        if (after.runeEtching() == RuneEtching.NONE || before.runeEtching() == after.runeEtching() || before.stringLevel() != after.stringLevel() || before.frameLevel() != after.frameLevel()
                || menu.getCost() <= 0 || menu.getCost() != component.getAnvilCost(before, after)) return null;
        ItemStack expected = withoutProof(bow); after.write(expected);
        CompoundTag identity = new CompoundTag(); identity.putUUID("actor", actor.getUUID()); identity.putUUID("token", UUID.randomUUID()); identity.putString("bow", CombatToolProof.item(bow)); identity.putString("rune", after.runeEtching().id());
        return new Take(first, second, expected, identity, actor.experienceLevel, menu.getCost(), bowFirst);
    }
    public static void prepare(AnvilMenu menu, ServerPlayer actor) {
        Take take = recipe(menu, actor); ItemStack actual = menu.getSlot(2).getItem();
        if (take != null && actual.getCount() == 1 && ItemStack.isSameItemSameTags(withoutProof(actual), take.expected)) actual.getOrCreateTag().put(CombatRuneProof.BOW, take.identity.copy());
    }
    public static Take begin(AnvilMenu menu, ServerPlayer actor, ItemStack taken) {
        Take take = recipe(menu, actor); CompoundTag identity = taken.getTagElement(CombatRuneProof.BOW);
        if (take == null || identity == null || !identity.hasUUID("token") || !identity.hasUUID("actor") || !identity.getUUID("actor").equals(actor.getUUID()) || !identity.getString("rune").equals(take.identity.getString("rune"))) return null;
        return new Take(take.first, take.second, take.expected, identity.copy(), take.levels, take.cost, take.bowFirst);
    }
    public static void end(AnvilMenu menu, ServerPlayer actor, ItemStack taken, Take take) {
        if (take == null || !CombatToolProof.real(actor) || actor.containerMenu != menu || take.levels - actor.experienceLevel != take.cost || !menu.getSlot(0).getItem().isEmpty()) return;
        ItemStack remaining = menu.getSlot(1).getItem();
        if (take.bowFirst ? take.second.getCount() - remaining.getCount() != 1 || !remaining.isEmpty() && !ItemStack.isSameItemSameTags(take.second, remaining) : !remaining.isEmpty()) return;
        boolean found = commit(taken, take) | commit(menu.getCarried(), take);
        for (int index = 0; index < actor.getInventory().getContainerSize(); index++) found |= commit(actor.getInventory().getItem(index), take);
        if (found) CombatToolProof.emit(actor, "bow_rune_etched");
    }
    private static boolean commit(ItemStack actual, Take take) {
        CompoundTag proof = actual.getTagElement(CombatRuneProof.BOW);
        if (actual.getCount() != 1 || proof == null || !proof.hasUUID("token") || !proof.getUUID("token").equals(take.identity.getUUID("token")) || !ItemStack.isSameItemSameTags(withoutProof(actual), take.expected)) return false;
        proof.putBoolean("committed", true); return true;
    }
}
