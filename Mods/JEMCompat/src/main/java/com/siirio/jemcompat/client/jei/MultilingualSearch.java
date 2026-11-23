package com.siirio.jemcompat.client.jei;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** One search projection shared by native JEI, the curated overlay and creative tabs. */
public final class MultilingualSearch {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BUNDLED_ENGLISH = "/assets/jemcompat/jei_english_aliases.json";
    private static final Set<String> TOOLTIP_KEY_PREFIXES = Set.of(
            "tooltip.jemfood.", "trait.jemfood.", "effect.jemfood.", "gui.jemcompat.jei.");
    private static Map<String, Set<String>> translations = Map.of();
    private static final Map<String, String> SEARCH_TEXT = new LinkedHashMap<>();

    private MultilingualSearch() {
    }

    public static void reload() {
        SEARCH_TEXT.clear();
        Set<String> relevantKeys = relevantTranslationKeys();
        Map<String, LinkedHashSet<String>> loaded = new LinkedHashMap<>();
        var resources = Minecraft.getInstance().getResourceManager();
        resources.listResourceStacks("lang", location -> isLanguageFile(location.getPath()))
                .forEach((location, stack) -> {
            for (Resource resource : stack) {
                try (var reader = resource.openAsReader()) {
                    var root = JsonParser.parseReader(reader).getAsJsonObject();
                    root.entrySet().forEach(entry -> {
                        if (relevant(entry.getKey(), relevantKeys) && entry.getValue().isJsonPrimitive()
                                && entry.getValue().getAsJsonPrimitive().isString()) {
                            add(loaded, entry.getKey(), entry.getValue().getAsString());
                        }
                    });
                } catch (Exception exception) {
                    LOGGER.warn("Cannot read multilingual search aliases from {} in {}", location,
                            resource.sourcePackId(), exception);
                }
            }
        });
        try (var stream = MultilingualSearch.class.getResourceAsStream(BUNDLED_ENGLISH)) {
            if (stream != null) {
                try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry -> {
                        if (relevant(entry.getKey(), relevantKeys) && entry.getValue().isJsonPrimitive()
                                && entry.getValue().getAsJsonPrimitive().isString()) {
                            add(loaded, entry.getKey(), entry.getValue().getAsString());
                        }
                    });
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load bundled English search aliases", exception);
        }
        Map<String, Set<String>> immutable = new LinkedHashMap<>();
        loaded.forEach((key, values) -> immutable.put(key, Set.copyOf(values)));
        translations = Map.copyOf(immutable);
    }

    public static Collection<String> aliases(ItemStack stack) {
        ensureLoaded();
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        addAll(aliases, stack.getDescriptionId());
        addAll(aliases, stack.getItem().getDescriptionId());
        aliases.add(stack.getHoverName().getString());
        appendTooltipTerms(stack, aliases);
        aliases.removeIf(String::isBlank);
        return aliases;
    }

    public static String searchText(ItemStack stack) {
        String key = cacheKey(stack);
        String cached = SEARCH_TEXT.get(key);
        if (cached != null) return cached;
        LinkedHashSet<String> terms = new LinkedHashSet<>(aliases(stack));
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null) terms.add(id.toString());
        String text = String.join("\n", terms).toLowerCase(Locale.ROOT);
        SEARCH_TEXT.put(key, text);
        return text;
    }

    public static boolean matches(ItemStack stack, String normalizedNeedle) {
        return searchText(stack).contains(normalizedNeedle);
    }

    private static void appendTooltipTerms(ItemStack stack, Set<String> aliases) {
        try {
            for (Component line : stack.getTooltipLines(Minecraft.getInstance().player, TooltipFlag.Default.NORMAL)) {
                if (containsIndexedTooltipKey(line)) {
                    aliases.add(line.getString());
                    appendComponentTerms(line, aliases);
                }
            }
        } catch (RuntimeException exception) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            LOGGER.debug("Cannot index tooltip search terms for {}", id, exception);
        }
    }

    private static void appendComponentTerms(Component component, Set<String> aliases) {
        if (component.getContents() instanceof TranslatableContents translated) {
            if (indexedTooltipKey(translated.getKey())) addAll(aliases, translated.getKey());
            for (Object argument : translated.getArgs()) {
                if (argument instanceof Component child) appendComponentTerms(child, aliases);
            }
        }
        component.getSiblings().forEach(sibling -> appendComponentTerms(sibling, aliases));
    }

    private static void addAll(Set<String> target, String key) {
        target.addAll(translations.getOrDefault(key, Set.of()));
    }

    private static void ensureLoaded() {
        if (translations.isEmpty()) reload();
    }

    private static void add(Map<String, LinkedHashSet<String>> target, String key, String value) {
        if (!value.isBlank()) target.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(value);
    }

    private static Set<String> relevantTranslationKeys() {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        ForgeRegistries.ITEMS.getValues().forEach(item -> {
            ItemStack stack = new ItemStack(item);
            keys.add(item.getDescriptionId());
            keys.add(stack.getDescriptionId());
        });
        return Set.copyOf(keys);
    }

    private static boolean containsIndexedTooltipKey(Component component) {
        if (component.getContents() instanceof TranslatableContents translated) {
            if (indexedTooltipKey(translated.getKey())) return true;
            for (Object argument : translated.getArgs()) {
                if (argument instanceof Component child && containsIndexedTooltipKey(child)) return true;
            }
        }
        return component.getSiblings().stream().anyMatch(MultilingualSearch::containsIndexedTooltipKey);
    }

    private static boolean isLanguageFile(String path) {
        return path.startsWith("lang/") && path.endsWith(".json") && path.length() > "lang/.json".length();
    }

    private static boolean relevant(String key, Set<String> exactKeys) {
        return exactKeys.contains(key) || indexedTooltipKey(key);
    }

    private static boolean indexedTooltipKey(String key) {
        return TOOLTIP_KEY_PREFIXES.stream().anyMatch(key::startsWith);
    }

    private static String cacheKey(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return (id == null ? stack.getDescriptionId() : id.toString()) + '\u0000'
                + (stack.hasTag() ? stack.getTag().toString() : "");
    }
}
