package com.justenoughmods.achievementguide.integration.food;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.satisfy.vinery.core.block.entity.StorageBlockEntity;
import net.satisfy.vinery.core.item.DrinkBlockItem;

/** Up to nine physical native rack slots remember real manual insertion, never inventory ownership. */
public final class WineCellarProof {
    private static final String KEY = "jemNativeWineRackV1";
    private static final String RUN = "jemNativeWineRackOperationV1";
    private static final TagKey<Item> RED = TagKey.create(Registries.ITEM, new ResourceLocation("vinery", "red_wine"));
    private static final TagKey<Item> WHITE = TagKey.create(Registries.ITEM, new ResourceLocation("vinery", "white_wine"));
    private WineCellarProof() {}
    private static boolean real(Player actor) {
        return actor instanceof ServerPlayer && !(actor instanceof FakePlayer) && !actor.isSpectator() && !actor.getAbilities().instabuild;
    }
    private static boolean slot(StorageBlockEntity owner, int slot) { return slot >= 0 && slot < Math.min(9, owner.getInventory().size()); }
    private static boolean wine(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof DrinkBlockItem && (stack.is(RED) || stack.is(WHITE)); }
    public static void invalidate(StorageBlockEntity owner, int slot) {
        if (slot(owner, slot)) owner.getPersistentData().getCompound(KEY).remove(Integer.toString(slot));
    }
    public static void clear(StorageBlockEntity owner) { owner.getPersistentData().remove(KEY); }
    public static void beginAdd(StorageBlockEntity owner, Player actor, ItemStack input, int slot) {
        owner.getPersistentData().remove(RUN);
        if (!real(actor) || !slot(owner, slot) || !owner.getInventory().get(slot).isEmpty() || !wine(input)) return;
        CompoundTag run = new CompoundTag();
        run.putUUID("actor", actor.getUUID());
        run.putInt("slot", slot);
        run.put("input", input.save(new CompoundTag()));
        owner.getPersistentData().put(RUN, run);
    }
    public static void added(StorageBlockEntity owner, Player actor, ItemStack input, int slot) {
        CompoundTag run = owner.getPersistentData().getCompound(RUN).copy();
        owner.getPersistentData().remove(RUN);
        if (!real(actor) || !slot(owner, slot) || !run.hasUUID("actor") || !run.getUUID("actor").equals(actor.getUUID()) || run.getInt("slot") != slot) return;
        ItemStack before = ItemStack.of(run.getCompound("input")), expected = before.copyWithCount(1);
        before.shrink(1);
        ItemStack stored = owner.getInventory().get(slot);
        if (!wine(stored) || !ItemStack.matches(expected, stored) || !ItemStack.matches(before, input)) return;
        CompoundTag receipt = new CompoundTag();
        receipt.putUUID("actor", actor.getUUID());
        receipt.put("wine", stored.save(new CompoundTag()));
        CompoundTag rows = owner.getPersistentData().getCompound(KEY);
        rows.put(Integer.toString(slot), receipt);
        owner.getPersistentData().put(KEY, rows);
        owner.setChanged();
    }
    public static void beginRemove(StorageBlockEntity owner, Player actor, int slot) {
        owner.getPersistentData().remove(RUN);
        if (!slot(owner, slot)) return;
        CompoundTag receipt = owner.getPersistentData().getCompound(KEY).getCompound(Integer.toString(slot)).copy();
        invalidate(owner, slot);
        owner.setChanged();
        ItemStack stored = owner.getInventory().get(slot);
        if (!real(actor) || !receipt.hasUUID("actor") || !receipt.getUUID("actor").equals(actor.getUUID())
                || !wine(stored) || !stored.save(new CompoundTag()).equals(receipt.getCompound("wine"))) return;
        receipt.putInt("slot", slot);
        receipt.putInt("held", FoodOutputProof.held((ServerPlayer)actor, ((ServerPlayer)actor).containerMenu, stored));
        owner.getPersistentData().put(RUN, receipt);
    }
    public static void removed(StorageBlockEntity owner, Player actor, int slot) {
        CompoundTag run = owner.getPersistentData().getCompound(RUN).copy();
        owner.getPersistentData().remove(RUN);
        if (!(actor instanceof ServerPlayer player) || !slot(owner, slot) || !owner.getInventory().get(slot).isEmpty()
                || !run.hasUUID("actor") || !run.getUUID("actor").equals(actor.getUUID()) || run.getInt("slot") != slot) return;
        ItemStack expected = ItemStack.of(run.getCompound("wine"));
        if (FoodOutputProof.held(player, player.containerMenu, expected) - run.getInt("held") >= expected.getCount())
            QuestActionTrigger.INSTANCE.emit(player, "v3_food_cellar");
    }
    public static void dropped(StorageBlockEntity owner, Player actor, ItemEntity drop) {
        CompoundTag run = owner.getPersistentData().getCompound(RUN);
        if (drop == null || !real(actor) || !run.hasUUID("actor") || !run.getUUID("actor").equals(actor.getUUID())) return;
        ItemStack expected = ItemStack.of(run.getCompound("wine"));
        if (drop.isAlive() && drop.level() == actor.level() && ItemStack.matches(expected, drop.getItem()))
            FoodDroppedProof.update(drop, "v3_food_cellar", expected.getCount(), actor.getUUID());
    }
    public static void save(StorageBlockEntity owner, CompoundTag tag) { tag.put(KEY, owner.getPersistentData().getCompound(KEY).copy()); }
    public static void load(StorageBlockEntity owner, CompoundTag tag) {
        CompoundTag bounded = new CompoundTag(), saved = tag.getCompound(KEY);
        for (int slot = 0; slot < Math.min(9, owner.getInventory().size()); slot++) {
            String key = Integer.toString(slot);
            if (saved.contains(key)) bounded.put(key, saved.getCompound(key).copy());
        }
        owner.getPersistentData().put(KEY, bounded);
        owner.getPersistentData().remove(RUN);
    }
}
