package com.justenoughmods.achievementguide.client.handbook.enchantment;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;

/** Loaded only by Patchouli's client template; tooltips do not load its API. */
public final class EnchantmentPageProcessor implements IComponentProcessor {
    private ResourceLocation id;
    private String section;
    private int page;

    @Override
    public void setup(Level level, IVariableProvider variables) {
        id = ResourceLocation.tryParse(variables.get("enchantment").asString());
        section = variables.get("section").asString("effect");
        page = variables.get("page").asNumber(0).intValue();
    }

    @Override
    public IVariable process(Level level, String key) {
        if (id == null) return IVariable.wrap("");
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
        if (key.equals("target_item")) {
            ItemStack target = target();
            return target.isEmpty() ? IVariable.wrapList(List.of())
                    : IVariable.wrapList(List.of(IVariable.wrap(ForgeRegistries.ITEMS.getKey(target.getItem()).toString())));
        }
        if (key.equals("target_name")) {
            ItemStack target = target();
            return IVariable.wrap(target.isEmpty() ? EnchantmentHelp.label("target_unavailable") : target.getHoverName().getString());
        }
        if (key.equals("title")) {
            var record = EnchantmentHelp.record(id);
            return IVariable.wrap(record == null ? "" : "$(bold)" + EnchantmentHelp.localized(record, "name") + "$()");
        }
        if (!key.equals("text")) return null;
        if (enchantment == null) return IVariable.wrap(EnchantmentHelp.label("unavailable"));
        if (section.equals("targets")) {
            ItemStack target = target();
            if (target.isEmpty()) return IVariable.wrap(EnchantmentHelp.label("target_unavailable"));
            return IVariable.wrap(EnchantmentHelp.label("target_table") + ": "
                    + EnchantmentHelp.label(enchantment.canApplyAtEnchantingTable(target) ? "yes" : "no")
                    + "$(br2)" + EnchantmentHelp.label("target_anvil") + ": "
                    + EnchantmentHelp.label(enchantment.canEnchant(target) ? "yes" : "no")
                    + "$(br2)" + EnchantmentHelp.label("target_conditions"));
        }
        List<String> lines = EnchantmentHelp.pageLines(id, section);
        // Each numbered level receives its own page; application pages split at the logical field.
        if (section.equals("levels") || section.equals("details")) return IVariable.wrap(page < lines.size() ? lines.get(page) : "");
        var record = EnchantmentHelp.record(id);
        if (record != null && record.has("page_parts") && record.getAsJsonObject("page_parts").has(section)) {
            int parts = record.getAsJsonObject("page_parts").get(section).getAsInt();
            return IVariable.wrap(part(String.join("$(br2)", lines), page, parts));
        }
        return IVariable.wrap(String.join(section.equals("native") ? "$(br)" : "$(br2)", lines));
    }

    private ItemStack target() {
        var record = EnchantmentHelp.record(id);
        if (record == null || !record.has("targets")) return ItemStack.EMPTY;
        var targets = record.getAsJsonArray("targets");
        if (page < 0 || page >= targets.size()) return ItemStack.EMPTY;
        ResourceLocation itemId = ResourceLocation.tryParse(targets.get(page).getAsString());
        if (itemId == null || !ForgeRegistries.ITEMS.containsKey(itemId)) return ItemStack.EMPTY;
        return new ItemStack(ForgeRegistries.ITEMS.getValue(itemId));
    }

    private static String part(String text, int selected, int count) {
        int start = 0;
        for (int index = 0; index < count; index++) {
            int end = index + 1 == count ? text.length() : text.length() * (index + 1) / count;
            while (end < text.length() && !Character.isWhitespace(text.charAt(end))) end++;
            if (index == selected) return text.substring(start, end).strip();
            start = end;
        }
        return "";
    }
}
