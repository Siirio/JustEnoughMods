package com.siirio.jemserver.smp;

import com.siirio.jemserver.smp.events.BloodMoonWaves;
import com.siirio.jemserver.smp.fishing.FishingConfig;
import com.siirio.jemserver.smp.fishing.FishingXp;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

public final class EventDescriptions {
    public static void add(CompoundTag row, CompoundTag view) {
        String activity = row.getString("activity");
        if (!row.contains("planned") && !row.getString("description").isBlank()) return;
        if (!row.contains("planned") && !activity.equals("BOSS")) {
            view.putString("descriptionKey", "jem.smp.description.party");
            view.put("descriptionArgs", new ListTag());
            return;
        }
        String key = "jem.smp.description." + activity;
        Object[] values = switch (activity) {
            case "FISHING" -> new Object[]{range(FishingConfig.OVERWORLD_MIN.get(), FishingConfig.OVERWORLD_MAX.get(), false),
                    range(FishingConfig.NETHER_MIN.get(), FishingConfig.NETHER_MAX.get(), false),
                    range(FishingConfig.END_MIN.get(), FishingConfig.END_MAX.get(), false)};
            case "BLOOD_MOON" -> new Object[]{BloodMoonWaves.WAVES};
            case "COOKING_SHOW" -> {
                var requirements = row.getCompound("cookingRequirements");
                if (requirements.isEmpty()) {
                    key += "_planned";
                    yield new Object[]{SmpConfig.COOKING_MINUTES.get()};
                }
                yield new Object[]{requirements.getAllKeys().stream().mapToInt(requirements::getInt).sum(), requirements.size()};
            }
            default -> new Object[]{};
        };
        view.putString("descriptionKey", key);
        view.put("descriptionArgs", arguments(values));
    }

    public static void fishingRules(ListTag rules) {
        rule(rules, "fishing_tier");
        rule(rules, "fishing_factors");
        rule(rules, "fishing_void", FishingConfig.VOID_MULTIPLIER.get(),
                range(FishingConfig.OVERWORLD_MIN.get(), FishingConfig.OVERWORLD_MAX.get(), true),
                range(FishingConfig.NETHER_MIN.get(), FishingConfig.NETHER_MAX.get(), true),
                range(FishingConfig.END_MIN.get(), FishingConfig.END_MAX.get(), true));
        rule(rules, FishingConfig.CAP_VOID.get() ? "fishing_capped" : "fishing_uncapped");
        rule(rules, "fishing_access");
        rule(rules, "fishing_reward");
    }

    private static String range(int minimum, int maximum, boolean inVoid) {
        int[] bounds = FishingXp.bounds(minimum, maximum, inVoid ? FishingConfig.VOID_MULTIPLIER.get() : 1, FishingConfig.CAP_VOID.get());
        return bounds[0] + "–" + bounds[1];
    }

    private static void rule(ListTag rules, String key, Object... values) {
        var rule = new CompoundTag();
        rule.putString("key", "jem.smp.rule." + key);
        rule.put("args", arguments(values));
        rules.add(rule);
    }

    private static ListTag arguments(Object[] values) {
        var args = new ListTag();
        for (Object value : values) args.add(StringTag.valueOf(value.toString()));
        return args;
    }

    private EventDescriptions() {}
}
