package com.justenoughmods.achievementguide.integration.food;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;

/** Bounded provenance for one native station output. This never completes a recipe or grants an item. */
public final class FoodOutputProof {
    public static final String KEY = "jemQuestCompletedFoodV1";
    private FoodOutputProof() {}
    public record Before(ItemStack output, CompoundTag proof) {}
    public record Collection(BlockEntity owner, Container inventory, int slot, ItemStack output, ItemStack receiptVariant, CompoundTag proof, int held) {}

    public static Before before(BlockEntity owner, int slot) {
        Container inventory = (Container)owner;
        return new Before(inventory.getItem(slot).copy(), owner.getPersistentData().getCompound(KEY).copy());
    }

    public static void stoveCompleted(BlockEntity owner, String recipe, Before before) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide) return;
        ItemStack output = ((Container)owner).getItem(0);
        String action = switch (recipe) {
            case "farm_and_charm:stove/bread" -> item(output, "bakery:bread") ? "food/bread_baked" : "";
            case "farm_and_charm:stove/roasted_chicken" -> item(output, "farm_and_charm:roasted_chicken") ? "v3_food_roaster" : "";
            default -> "";
        };
        completed(owner, 0, recipe, action, before);
    }

    public static void completed(BlockEntity owner, int slot, String recipe, String action, Before before) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide) return;
        ItemStack output = ((Container)owner).getItem(slot);
        int oldCount = before.output.isEmpty() ? 0 : before.output.getCount();
        if (action.isEmpty() || output.isEmpty() || !before.output.isEmpty() && !ItemStack.isSameItemSameTags(output, before.output)
                || output.getCount() <= oldCount) return;
        int previous = matches(before.proof, before.output) && action.equals(before.proof.getString("action"))
                ? before.proof.getInt("known") : 0;
        CompoundTag proof = new CompoundTag();
        proof.putLong("generation", owner.getPersistentData().getLong(KEY + "Generation") + 1);
        proof.putInt("slot", slot);
        proof.putString("recipe", recipe);
        proof.putString("action", action);
        proof.putInt("known", Math.min(output.getCount(), previous + output.getCount() - oldCount));
        proof.put("output", output.save(new CompoundTag()));
        owner.getPersistentData().putLong(KEY + "Generation", proof.getLong("generation"));
        owner.getPersistentData().put(KEY, proof);
        owner.setChanged();
    }

    /** Arbitrary replacement, reinsertion and clearing cannot preserve a completed recipe receipt. */
    public static void replaced(BlockEntity owner, int slot) {
        CompoundTag proof = owner.getPersistentData().getCompound(KEY);
        if (!proof.isEmpty() && proof.getInt("slot") == slot) { owner.getPersistentData().remove(KEY); owner.setChanged(); }
    }

    public static List<Collection> capture(AbstractContainerMenu menu, ServerPlayer player) {
        if (RemainingFoodActors.real(player)==null || player.containerMenu!=menu || !menu.stillValid(player) || menu.slots.size() > 256) return List.of();
        List<Collection> rows = new ArrayList<>();
        for (var slot : menu.slots) {
            if (!(slot.container instanceof BlockEntity owner)) continue;
            CompoundTag proof = owner.getPersistentData().getCompound(KEY);
            if (proof.isEmpty() || slot.getContainerSlot() != proof.getInt("slot")) continue;
            ItemStack output = slot.getItem();
            if (!matches(proof, output)) { replaced(owner, proof.getInt("slot")); continue; }
            ItemStack receipt = owner instanceof FoodReceiptVariant adapter ? adapter.jem$nativeReceiptVariant(output, player) : output;
            rows.add(new Collection(owner, slot.container, slot.getContainerSlot(), output.copy(), receipt.copy(), proof.copy(), heldVariants(player, menu, output, receipt)));
        }
        return rows;
    }

    public static void collected(AbstractContainerMenu menu, ServerPlayer player, List<Collection> rows) {
        if (RemainingFoodActors.real(player)==null || player.containerMenu != menu || !menu.stillValid(player)) return;
        for (Collection row : rows) {
            ItemStack after = row.inventory.getItem(row.slot);
            int remaining = after.isEmpty() ? 0 : after.getCount();
            int taken = row.output.getCount() - remaining;
            if (taken < 1 || !after.isEmpty() && !ItemStack.isSameItemSameTags(row.output, after) && !ItemStack.isSameItemSameTags(row.receiptVariant, after)
                    || heldVariants(player, menu, row.output, row.receiptVariant) - row.held < taken
                    || row.owner.getPersistentData().getLong(KEY + "Generation") != row.proof.getLong("generation")) continue;
            // Unknown pre-existing output leaves first. Only a real produced portion can earn credit.
            int known = row.proof.getInt("known");
            int receivedKnown = Math.max(0, taken - (row.output.getCount() - known));
            CompoundTag retained = row.proof.copy();
            retained.putInt("known", Math.max(0, known - receivedKnown));
            retained.put("output", after.save(new CompoundTag()));
            if (retained.getInt("known") == 0 || after.isEmpty()) row.owner.getPersistentData().remove(KEY);
            else row.owner.getPersistentData().put(KEY, retained);
            row.owner.setChanged();
            if (receivedKnown > 0) QuestActionTrigger.INSTANCE.emit(player, row.proof.getString("action"));
        }
    }

    public static int held(ServerPlayer player, AbstractContainerMenu menu, ItemStack matching) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (ItemStack.isSameItemSameTags(stack, matching)) count += stack.getCount();
        }
        if (menu != null && ItemStack.isSameItemSameTags(menu.getCarried(), matching)) count += menu.getCarried().getCount();
        return count;
    }
    private static int heldVariants(ServerPlayer player, AbstractContainerMenu menu, ItemStack raw, ItemStack receipt) {
        return held(player, menu, raw) + (ItemStack.isSameItemSameTags(raw, receipt) ? 0 : held(player, menu, receipt));
    }

    public static boolean matches(CompoundTag proof, ItemStack output) {
        return !proof.isEmpty() && proof.getInt("known") > 0 && proof.getInt("known") <= output.getCount()
                && output.save(new CompoundTag()).equals(proof.getCompound("output"));
    }
    public static boolean item(ItemStack stack, String id) { return !stack.isEmpty() && id.equals(String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()))); }
    public static void certify(BlockEntity owner, int slot, String recipe, String action, ItemStack output) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide || output.isEmpty()) return;
        CompoundTag proof = new CompoundTag();
        proof.putLong("generation", owner.getPersistentData().getLong(KEY + "Generation") + 1);
        proof.putInt("slot", slot);
        proof.putString("recipe", recipe);
        proof.putString("action", action);
        proof.putInt("known", output.getCount());
        proof.put("output", output.save(new CompoundTag()));
        owner.getPersistentData().putLong(KEY + "Generation", proof.getLong("generation"));
        owner.getPersistentData().put(KEY, proof);
        owner.setChanged();
    }
    public static void save(BlockEntity owner, CompoundTag tag) {
        tag.put(KEY, owner.getPersistentData().getCompound(KEY).copy());
        tag.putLong(KEY + "Generation", owner.getPersistentData().getLong(KEY + "Generation"));
    }
    public static void load(BlockEntity owner, CompoundTag tag) {
        owner.getPersistentData().put(KEY, tag.getCompound(KEY).copy());
        owner.getPersistentData().putLong(KEY + "Generation", tag.getLong(KEY + "Generation"));
    }
}
