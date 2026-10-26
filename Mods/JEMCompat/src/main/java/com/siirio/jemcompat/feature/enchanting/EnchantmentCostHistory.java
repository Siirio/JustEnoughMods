package com.siirio.jemcompat.feature.enchanting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

/** Actual paid XP follows each enchantment through supported table and anvil operations. */
public final class EnchantmentCostHistory {
    private static final String KEY = "jemcompat_enchantment_costs";
    private static final String CREDITS = "credits";

    public record XpSnapshot(int level, float progress) {}
    public record Credit(UUID token, int paidXp) {}

    public static XpSnapshot xp(ServerPlayer player) {
        return new XpSnapshot(player.experienceLevel, player.experienceProgress);
    }

    public static int spent(XpSnapshot before, ServerPlayer player) {
        if (before == null || player.getAbilities().instabuild) return 0;
        long paid = points(before.level(), before.progress()) - points(player.experienceLevel, player.experienceProgress);
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, paid));
    }

    public static int paidForLevels(ServerPlayer player, int levels) {
        if (player.getAbilities().instabuild || levels <= 0) return 0;
        int afterLevel = Math.max(0, player.experienceLevel - levels);
        long paid = points(player.experienceLevel, player.experienceProgress)
                - points(afterLevel, player.experienceProgress);
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, paid));
    }

    public static int pointsForLevelsFromZero(int levels) {
        return (int) Math.min(Integer.MAX_VALUE, points(levels, 0));
    }

    private static long points(int level, float progress) {
        int safeLevel = Math.max(0, level);
        long complete = 0;
        int first = Math.min(safeLevel, 15);
        complete += (long) first * first + 6L * first;
        int second = Math.min(Math.max(safeLevel - 15, 0), 15);
        complete += 37L * second + 5L * second * (second - 1) / 2;
        int third = Math.max(safeLevel - 30, 0);
        complete += 112L * third + 9L * third * (third - 1) / 2;
        int next = safeLevel >= 30 ? 112 + (safeLevel - 30) * 9
                : safeLevel >= 15 ? 37 + (safeLevel - 15) * 5 : 7 + safeLevel * 2;
        return complete + Math.round(Math.max(0, Math.min(1, progress)) * next);
    }

    public static void record(ItemStack left, ItemStack right, ItemStack output, int paidXp) {
        if (output.isEmpty()) return;
        Map<Enchantment, Integer> old = EnchantmentHelper.getEnchantments(left);
        Map<Enchantment, Integer> current = EnchantmentHelper.getEnchantments(output);
        if (current.isEmpty()) return;

        List<String> changed = new ArrayList<>();
        Map<String, Integer> levels = new LinkedHashMap<>();
        for (var entry : current.entrySet()) {
            var id = ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey());
            if (id == null) continue;
            String key = id.toString();
            levels.put(key, entry.getValue());
            if (!entry.getValue().equals(old.get(entry.getKey()))) changed.add(key);
        }
        if (changed.isEmpty() && right.isEmpty()) return;

        CompoundTag history = new CompoundTag();
        int share = changed.isEmpty() ? 0 : paidXp / changed.size();
        int remainder = changed.isEmpty() ? 0 : paidXp % changed.size();
        for (var entry : levels.entrySet()) {
            String key = entry.getKey();
            int level = entry.getValue();
            ListTag credits = new ListTag();
            Set<UUID> seen = new HashSet<>();
            inherit(left, key, oldLevel(left, key), level, credits, seen);
            inherit(right, key, oldLevel(right, key), level, credits, seen);
            int changedIndex = changed.indexOf(key);
            if (changedIndex >= 0 && paidXp > 0) {
                CompoundTag credit = new CompoundTag();
                credit.putUUID("token", UUID.randomUUID());
                credit.putInt("paidXp", share + (changedIndex < remainder ? 1 : 0));
                credits.add(credit);
            }
            if (!credits.isEmpty()) {
                CompoundTag item = new CompoundTag();
                item.putInt("level", level);
                item.put(CREDITS, credits);
                history.put(key, item);
            }
        }
        if (!history.isEmpty()) output.getOrCreateTag().put(KEY, history);
        else if (output.getTag() != null) output.getTag().remove(KEY);
    }

    private static int oldLevel(ItemStack stack, String key) {
        if (stack.isEmpty()) return 0;
        for (var entry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            var id = ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey());
            if (id != null && key.equals(id.toString())) return entry.getValue();
        }
        return 0;
    }

    private static void inherit(ItemStack source, String key, int sourceLevel, int resultLevel,
                                ListTag target, Set<UUID> seen) {
        if (source.isEmpty() || sourceLevel <= 0 || sourceLevel > resultLevel) return;
        CompoundTag history = source.getTagElement(KEY);
        if (history == null || !history.contains(key, 10)) return;
        CompoundTag entry = history.getCompound(key);
        if (entry.getInt("level") != sourceLevel) return;
        ListTag credits = entry.getList(CREDITS, 10);
        for (int index = 0; index < credits.size(); index++) {
            CompoundTag credit = credits.getCompound(index);
            if (credit.hasUUID("token") && credit.getInt("paidXp") >= 0 && seen.add(credit.getUUID("token")))
                target.add(credit.copy());
        }
    }

    public static List<Credit> credits(ItemStack stack, String key, int level) {
        CompoundTag history = stack.getTagElement(KEY);
        if (history == null || !history.contains(key, 10)) return List.of();
        CompoundTag entry = history.getCompound(key);
        if (entry.getInt("level") != level) return List.of();
        ListTag values = entry.getList(CREDITS, 10);
        if (values.isEmpty()) return List.of();
        List<Credit> result = new ArrayList<>(values.size());
        Set<UUID> seen = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            CompoundTag value = values.getCompound(index);
            if (!value.hasUUID("token") || !value.contains("paidXp", 3) || value.getInt("paidXp") < 0
                    || !seen.add(value.getUUID("token"))) return List.of();
            result.add(new Credit(value.getUUID("token"), value.getInt("paidXp")));
        }
        return List.copyOf(result);
    }

    public static void remove(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(KEY, 10)) return;
        CompoundTag history = tag.getCompound(KEY);
        history.remove(key);
        if (history.isEmpty()) tag.remove(KEY);
    }

    private EnchantmentCostHistory() {}
}
