package com.justenoughmods.achievementguide.client.handbook.enchantment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

/** Presentation only. Native enchantments retain all application and gameplay decisions. */
public final class EnchantmentHelp {
    private static final Map<ResourceLocation, List<ItemStack>> APPLICATION_ITEMS = new java.util.HashMap<>();

    public static List<ItemStack> applicationItems(ResourceLocation id) {
        ensureLoaded();
        return APPLICATION_ITEMS.computeIfAbsent(id, key -> {
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(key);
            if (enchantment == null) return List.of();
            List<ItemStack> result = new ArrayList<>();
            for (var item : ForgeRegistries.ITEMS.getValues()) {
                ItemStack stack = item.getDefaultInstance();
                if (!stack.isEmpty() && !(item instanceof EnchantedBookItem)
                        && item != net.minecraft.world.item.Items.BOOK
                        && (enchantment.canEnchant(stack) || enchantment.canApplyAtEnchantingTable(stack))) result.add(stack);
            }
            result.sort(java.util.Comparator.comparing(stack -> ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()));
            return List.copyOf(result);
        });
    }

    public static ItemStack enchantedBook(ResourceLocation id) {
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
        return enchantment == null ? ItemStack.EMPTY : EnchantedBookItem.createForEnchantment(
                new net.minecraft.world.item.enchantment.EnchantmentInstance(enchantment, enchantment.getMaxLevel()));
    }
    private static final ResourceLocation DATA = new ResourceLocation("jem_advancements", "handbook/enchantments.json");
    private static Map<ResourceLocation, JsonObject> records = Map.of();
    private static JsonObject labels = new JsonObject();
    private static final Map<ResourceLocation, List<ResourceLocation>> incompatible = new LinkedHashMap<>();
    private static boolean loaded;

    private EnchantmentHelp() { }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, EnchantmentHelp::tooltip);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(EnchantmentHelp::registerReload);
    }

    private static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) EnchantmentHelp::reload);
    }

    private static void reload(ResourceManager manager) {
        APPLICATION_ITEMS.clear();
        Map<ResourceLocation, JsonObject> replacement = new LinkedHashMap<>();
        try (var reader = manager.getResourceOrThrow(DATA).openAsReader()) {
            JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
            for (var element : data.getAsJsonArray("enchantments")) {
                JsonObject record = element.getAsJsonObject();
                ResourceLocation id = new ResourceLocation(record.get("id").getAsString());
                if (replacement.put(id, record) != null) throw new IllegalStateException("Duplicate enchantment help: " + id);
            }
            records = Map.copyOf(replacement);
            labels = data.getAsJsonObject("labels");
            incompatible.clear();
            loaded = true;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to load JEM enchantment handbook " + DATA, exception);
        }
    }

    private static void ensureLoaded() {
        if (!loaded) reload(Minecraft.getInstance().getResourceManager());
    }

    static String localized(JsonObject object, String field) {
        if (!object.has(field)) return "";
        JsonObject text = object.getAsJsonObject(field);
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        return text.has(language) ? text.get(language).getAsString() : text.get("en_us").getAsString();
    }

    static String label(String field) {
        ensureLoaded();
        return localized(labels, field);
    }

    static JsonObject record(ResourceLocation id) {
        ensureLoaded();
        return records.get(id);
    }

    public static ResourceLocation entryFor(ItemStack stack) {
        if (!enchantmentsVisible(stack)) return null;
        ensureLoaded();
        for (Enchantment enchantment : EnchantmentHelper.getEnchantments(stack).keySet()) {
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            if (records.containsKey(id)) return entryFor(id);
        }
        if (disabledExcavationLevel(stack) > 0 && records.containsKey(new ResourceLocation("jem_pack_core", "excavation"))) {
            return entryFor(new ResourceLocation("jem_pack_core", "excavation"));
        }
        return null;
    }

    public static ResourceLocation entryFor(ResourceLocation id) {
        return new ResourceLocation("jem_advancements", "handbook/enchantments/" + id.getNamespace() + "_" + id.getPath());
    }

    private static int disabledExcavationLevel(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("JEMExcavationDisabledLevel") : 0;
    }

    public static boolean enchantmentsVisible(ItemStack stack) {
        // Native ENCHANTMENTS and ADDITIONAL sections; do not leak hidden effects through help actions.
        return !stack.hasTag() || (stack.getTag().getInt("HideFlags") & 33) == 0;
    }

    private static void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!enchantmentsVisible(stack)) return;
        if (!(stack.getItem() instanceof EnchantedBookItem) && !stack.isEnchanted() && disabledExcavationLevel(stack) <= 0) return;
        ensureLoaded();
        boolean book = stack.getItem() instanceof EnchantedBookItem;
        for (var entry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            Enchantment enchantment = entry.getKey();
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            JsonObject record = records.get(id);
            if (record == null) continue;
            int level = entry.getValue();
            if (!book && id.equals(new ResourceLocation("jem_world_boss_tiers", "progression"))) {
                Component current = NativeProgression.currentEffect(stack);
                if (current != null && event.getToolTip().stream().noneMatch(line -> line.getString().equals(current.getString()))) {
                    event.getToolTip().add(current.copy().withStyle(ChatFormatting.GRAY));
                }
                continue;
            }
            List<Component> additions = new ArrayList<>();
            appendWrapped(additions, localized(record, record.has("brief") ? "brief" : "effect"), ChatFormatting.GRAY);
            if (record.has("levels") || id.equals(new ResourceLocation("create", "capacity"))) {
                appendWrapped(additions, levelEffect(record, level), ChatFormatting.AQUA);
            }
            if (book) appendWrapped(additions, label("applies") + ": " + applicability(record), ChatFormatting.DARK_AQUA);
            if (Screen.hasShiftDown()) {
                if (book) {
                    appendWrapped(additions, label("maximum") + ": " + enchantment.getMaxLevel(), ChatFormatting.GRAY);
                    appendWrapped(additions, localized(record, "acquisition"), ChatFormatting.GRAY);
                }
            }
            String nativeName = enchantment.getFullname(level).getString();
            int insertAt = -1;
            for (int index = 0; index < event.getToolTip().size(); index++) {
                if (event.getToolTip().get(index).getString().equals(nativeName)) { insertAt = index + 1; break; }
            }
            if (insertAt < 0) {
                additions.add(0, Component.literal(localized(record, "name") + " " + level).withStyle(ChatFormatting.LIGHT_PURPLE));
                event.getToolTip().addAll(additions);
            } else event.getToolTip().addAll(insertAt, additions);
        }
        if (disabledExcavationLevel(stack) > 0) {
            appendWrapped(event.getToolTip(), label("excavation_disabled"), ChatFormatting.GOLD);
        }
    }

    private static void appendWrapped(List<Component> tooltip, String text, ChatFormatting color) {
        if (text.isEmpty()) return;
        // Font splitting preserves complete explanations while avoiding a screen-wide tooltip.
        for (var line : Minecraft.getInstance().font.getSplitter().splitLines(Component.literal(text), 300, net.minecraft.network.chat.Style.EMPTY)) {
            tooltip.add(Component.literal(line.getString()).withStyle(color));
        }
    }

    private static String applicability(JsonObject record) {
        if (record.has("targets")) {
            List<String> names = new ArrayList<>();
            for (var target : record.getAsJsonArray("targets")) {
                ResourceLocation id = ResourceLocation.tryParse(target.getAsString());
                if (id != null && ForgeRegistries.ITEMS.containsKey(id)) {
                    names.add(new ItemStack(ForgeRegistries.ITEMS.getValue(id)).getHoverName().getString());
                }
            }
            if (!names.isEmpty()) return String.join(", ", names);
        }
        return localized(record, record.has("applies_brief") ? "applies_brief" : "applies");
    }

    static String levelEffect(JsonObject record, int level) {
        if (record.get("id").getAsString().equals("create:capacity")) {
            Integer capacity = NativeCapacity.maximum(level);
            return capacity == null ? label("capacity_unavailable") : label("capacity") + ": " + capacity;
        }
        if (record.has("levels")) {
            JsonArray levels = record.getAsJsonArray("levels");
            if (level > 0 && level <= levels.size()) return localized(levels.get(level - 1).getAsJsonObject(), "text");
        }
        return localized(record, "effect");
    }

    static String compatibility(Enchantment enchantment) {
        ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        List<ResourceLocation> ids = incompatible.computeIfAbsent(id, ignored -> {
            List<ResourceLocation> result = new ArrayList<>();
            for (Enchantment other : ForgeRegistries.ENCHANTMENTS.getValues()) {
                if (other != enchantment && !enchantment.isCompatibleWith(other)) result.add(ForgeRegistries.ENCHANTMENTS.getKey(other));
            }
            return List.copyOf(result);
        });
        if (ids.isEmpty()) return label("none_incompatible");
        List<String> names = new ArrayList<>();
        for (ResourceLocation other : ids) {
            Enchantment target = ForgeRegistries.ENCHANTMENTS.getValue(other);
            if (target != null) names.add(Component.translatable(target.getDescriptionId()).getString());
        }
        return String.join(", ", names);
    }

    static List<String> pageLines(ResourceLocation id, String section) {
        JsonObject record = record(id);
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
        if (record == null || enchantment == null) return List.of(label("unavailable"));
        List<String> lines = new ArrayList<>();
        if (section.equals("levels")) {
            for (int level = 1; level <= enchantment.getMaxLevel(); level++) lines.add(label("level") + " " + level + ": " + levelEffect(record, level));
        } else if (section.equals("application")) {
            lines.add(label("applies") + ": " + localized(record, "applies"));
        } else if (section.equals("acquisition")) {
            lines.add(localized(record, "acquisition"));
        } else if (section.equals("compatibility")) {
            lines.add(label("incompatible") + ": " + compatibility(enchantment));
        } else if (section.equals("conditions")) {
            lines.add(localized(record, "conditions"));
        } else if (section.equals("details")) {
            for (var detail : record.getAsJsonArray("details")) {
                lines.add(localized(detail.getAsJsonObject(), "text"));
            }
        } else if (section.equals("native")) {
            lines.add(label("maximum") + ": " + enchantment.getMaxLevel());
            lines.add(label("native_flags"));
            lines.add(label("treasure") + ": " + label(enchantment.isTreasureOnly() ? "yes" : "no"));
            lines.add(label("books") + ": " + label(enchantment.isAllowedOnBooks() ? "yes" : "no"));
            lines.add(label("discovery") + ": " + label(enchantment.isDiscoverable() ? "yes" : "no"));
            lines.add(label("trading") + ": " + label(enchantment.isTradeable() ? "yes" : "no"));
        } else {
            lines.add(localized(record, "effect"));
        }
        return lines;
    }
}
