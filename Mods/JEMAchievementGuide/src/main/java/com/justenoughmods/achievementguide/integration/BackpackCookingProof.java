package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import com.justenoughmods.achievementguide.mixin.CookingQuestAccess;
import com.justenoughmods.achievementguide.mixin.UpgradeWrapperQuestAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.ICookingUpgrade;

/** Receipts for an actual carried-backpack recipe and a later native output collection. */
public final class BackpackCookingProof {
    private static final String KEY = "jemQuestCookedOutputV1";
    private static final String GENERATION = KEY + "Generation";
    private static final ThreadLocal<Run> RUN = new ThreadLocal<>();
    private record Run(CookingLogic<?> logic, ServerPlayer actor) {}
    public record Before(ItemStack input, ItemStack output, ItemStack expected, CompoundTag prior, ServerPlayer actor) {}
    public record Collection(IUpgradeWrapper wrapper, CookingLogic<?> logic, ItemStack output, CompoundTag proof, int held) {}
    private BackpackCookingProof() {}

    public static boolean tick(Object wrapper, CookingLogic<?> logic, Entity entity, Level level) {
        Run previous = RUN.get();
        RUN.remove();
        if (entity instanceof ServerPlayer actor && eligible(actor) && actor.level() == level
                && ((UpgradeWrapperQuestAccess) wrapper).jem$questStorageWrapper() instanceof IBackpackWrapper
                && ((IUpgradeWrapper) wrapper).isEnabled()) RUN.set(new Run(logic, actor));
        try {
            return logic.tick(level);
        } finally {
            if (previous == null) RUN.remove(); else RUN.set(previous);
        }
    }

    public static Before before(CookingLogic<?> logic, Recipe<?> recipe, Level level) {
        Run run = RUN.get();
        if (run == null || run.logic != logic || level.isClientSide || !logic.isBurning(level)) return null;
        ItemStack expected = recipe.getResultItem(level.registryAccess());
        if (expected.isEmpty() || logic.getCookInput().isEmpty()) return null;
        return new Before(logic.getCookInput().copy(), logic.getCookOutput().copy(), expected.copy(), proof(logic).copy(), run.actor);
    }

    public static void completed(CookingLogic<?> logic, Before before) {
        if (before == null) return;
        ItemStack input = logic.getCookInput();
        ItemStack output = logic.getCookOutput();
        int oldCount = before.output.isEmpty() ? 0 : before.output.getCount();
        if (before.input.getCount() - input.getCount() != 1
                || !input.isEmpty() && !ItemStack.isSameItemSameTags(input, before.input)
                || !ItemStack.isSameItemSameTags(output, before.expected)
                || !before.output.isEmpty() && !ItemStack.isSameItemSameTags(output, before.output)
                || output.getCount() - oldCount != before.expected.getCount()) return;
        int known = matches(before.prior, before.output, before.actor) ? before.prior.getInt("known") : 0;
        CompoundTag receipt = new CompoundTag();
        var tag = upgrade(logic).getOrCreateTag();
        long generation = tag.getLong(GENERATION) + 1;
        receipt.putUUID("actor", before.actor.getUUID());
        receipt.putInt("known", Math.min(output.getCount(), known + before.expected.getCount()));
        receipt.put("output", output.save(new CompoundTag()));
        receipt.putLong("generation", generation);
        tag.putLong(GENERATION, generation);
        tag.put(KEY, receipt);
        save(logic);
    }

    /** Native output mutation invalidates the receipt before the inventory's native save. */
    public static void changed(CookingLogic<?> logic) {
        if (upgrade(logic).hasTag()) upgrade(logic).getTag().remove(KEY);
    }

    public static List<Collection> capture(BackpackContainer menu, ServerPlayer actor) {
        if (!eligible(actor) || !menu.stillValid(actor)) return List.of();
        var wrappers = menu.getStorageWrapper().getUpgradeHandler().getSlotWrappers().values();
        if (wrappers.size() > 128) return List.of();
        List<Collection> result = new ArrayList<>();
        for (IUpgradeWrapper wrapper : wrappers) {
            if (!wrapper.isEnabled() || !(wrapper instanceof ICookingUpgrade<?> cooking)) continue;
            CookingLogic<?> logic = cooking.getCookingLogic();
            ItemStack output = logic.getCookOutput();
            CompoundTag proof = proof(logic);
            if (matches(proof, output, actor)) result.add(new Collection(wrapper, logic, output.copy(), proof.copy(), FoodOutputProof.held(actor, menu, output)));
        }
        return result;
    }

    public static void collected(BackpackContainer menu, ServerPlayer actor, List<Collection> collections) {
        if (actor.containerMenu != menu || !eligible(actor) || !menu.stillValid(actor)) return;
        var wrappers = menu.getStorageWrapper().getUpgradeHandler().getSlotWrappers().values();
        for (Collection row : collections) {
            if (!wrappers.contains(row.wrapper) || !row.wrapper.isEnabled()) continue;
            ItemStack after = row.logic.getCookOutput();
            int taken = row.output.getCount() - after.getCount();
            if (taken < 1 || !after.isEmpty() && !ItemStack.isSameItemSameTags(row.output, after)
                    || FoodOutputProof.held(actor, menu, row.output) - row.held < taken
                    || upgrade(row.logic).getOrCreateTag().getLong(GENERATION) != row.proof.getLong("generation")) continue;
            int received = Math.max(0, taken - (row.output.getCount() - row.proof.getInt("known")));
            int remaining = row.proof.getInt("known") - received;
            CompoundTag retained = row.proof.copy();
            retained.putInt("known", remaining);
            retained.put("output", after.save(new CompoundTag()));
            if (remaining < 1 || after.isEmpty()) changed(row.logic);
            else upgrade(row.logic).getOrCreateTag().put(KEY, retained);
            save(row.logic);
            if (received > 0) QuestActionTrigger.INSTANCE.emit(actor, "v3_store_processing");
        }
    }

    private static boolean matches(CompoundTag proof, ItemStack output, ServerPlayer actor) {
        return proof.hasUUID("actor") && proof.getUUID("actor").equals(actor.getUUID())
                && proof.getInt("known") > 0 && proof.getInt("known") <= output.getCount()
                && output.save(new CompoundTag()).equals(proof.getCompound("output"));
    }
    private static CompoundTag proof(CookingLogic<?> logic) {
        return upgrade(logic).hasTag() ? upgrade(logic).getTag().getCompound(KEY) : new CompoundTag();
    }
    private static ItemStack upgrade(CookingLogic<?> logic) { return ((CookingQuestAccess) logic).jem$questCookingUpgrade(); }
    private static void save(CookingLogic<?> logic) { ((CookingQuestAccess) logic).jem$saveQuestCookingUpgrade(); }
    private static boolean eligible(ServerPlayer actor) { return !(actor instanceof FakePlayer) && !actor.isSpectator() && !actor.getAbilities().instabuild; }
}
