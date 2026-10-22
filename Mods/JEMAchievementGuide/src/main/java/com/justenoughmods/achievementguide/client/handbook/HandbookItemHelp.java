package com.justenoughmods.achievementguide.client.handbook;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import vazkii.patchouli.common.util.ItemStackUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tooltip and JEI projections of the actual localized Handbook pages. */
public final class HandbookItemHelp extends SimplePreparableReloadListener<Map<String, JsonObject>> {
    private static final String BOOK_ROOT = "patchouli_books/just_enough_guide/";
    private static final Pattern MACRO = Pattern.compile("\\$\\(([^)]*)\\)");
    private static volatile Cache cache = new Cache(Map.of(), List.of());
    private static boolean registered;

    private HandbookItemHelp() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener(HandbookItemHelp::tooltip);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(HandbookItemHelp::reloadListeners);
    }

    private static void reloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new HandbookItemHelp());
    }

    /** JEI registration reads this cache once; hover events never open resources. */
    public static List<Info> informationEntries() {
        return cache.information;
    }

    public static List<Component> informationFor(ItemStack stack) {
        Help help = helpFor(stack);
        return help == null ? List.of() : help.information;
    }

    private static void tooltip(ItemTooltipEvent event) {
        Help selected = helpFor(event.getItemStack());
        if (selected == null) return;
        for (String line : selected.summary.split("\\n")) {
            for (var wrapped : Minecraft.getInstance().font.getSplitter().splitLines(
                    Component.literal(line), 300, net.minecraft.network.chat.Style.EMPTY)) {
                event.getToolTip().add(Component.literal(wrapped.getString()).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static Help helpFor(ItemStack hovered) {
        List<Help> candidates = cache.items.get(hovered.getItem());
        if (candidates == null) return null;
        Help selected = null;
        for (Help candidate : candidates) {
            if (candidate.stack.hasTag()) {
                if (ItemStack.isSameItemSameTags(candidate.stack, hovered)) {
                    selected = candidate;
                    break;
                }
            } else if (selected == null) {
                selected = candidate;
            }
        }
        return selected;
    }

    @Override
    protected Map<String, JsonObject> prepare(ResourceManager resources, ProfilerFiller profiler) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        readLanguage(resources, "en_us", result);
        String selected = Minecraft.getInstance().getLanguageManager().getSelected();
        if (!selected.equals("en_us")) readLanguage(resources, selected, result);
        return Map.copyOf(result);
    }

    private static void readLanguage(ResourceManager resources, String language, Map<String, JsonObject> result) {
        String prefix = BOOK_ROOT + language + "/entries/";
        resources.listResources(prefix, id -> id.getNamespace().equals("jem_advancements") && id.getPath().endsWith(".json"))
                .forEach((id, resource) -> {
                    try (var reader = resource.openAsReader()) {
                        JsonObject entry = JsonParser.parseReader(reader).getAsJsonObject();
                        String key = id.getPath().substring(prefix.length(), id.getPath().length() - 5);
                        // Locale replacement is per entry, exactly as the book's content is authored.
                        result.put(key, entry);
                    } catch (IOException | RuntimeException error) {
                        LogUtils.getLogger().error("Cannot read Handbook item-help source {}", id, error);
                    }
                });
    }

    @Override
    protected void apply(Map<String, JsonObject> entries, ResourceManager resources, ProfilerFiller profiler) {
        Map<Item, List<Help>> items = new LinkedHashMap<>();
        List<Info> information = new ArrayList<>();
        entries.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(source -> {
            JsonObject entry = source.getValue();
            if (!entry.has("jem_summary")) return;
            try {
                if (!entry.has("pages")) throw new IllegalArgumentException("Item-help entry has no native pages");
                String summary = plain(entry.get("jem_summary").getAsString());
                if (summary.isBlank()) return;
                String selector = firstSpotlight(entry);
                if (selector == null) throw new IllegalArgumentException("Item-help entry has no primary spotlight item");
                List<Component> body = information(entry, summary);
                for (ItemStack stack : ItemStackUtil.loadStackListFromString(selector)) {
                    if (stack.isEmpty()) continue;
                    ItemStack exact = stack.copy();
                    exact.setCount(1);
                    List<Help> targets = items.computeIfAbsent(exact.getItem(), ignored -> new ArrayList<>());
                    if (targets.stream().anyMatch(existing -> ItemStack.isSameItemSameTags(existing.stack, exact))) {
                        LogUtils.getLogger().warn("Duplicate Handbook item-help selector in {}: {}", source.getKey(), selector);
                        continue;
                    }
                    targets.add(new Help(exact, summary, body));
                    information.add(new Info(exact, body));
                }
            } catch (RuntimeException error) {
                LogUtils.getLogger().error("Cannot project Handbook item-help entry {}", source.getKey(), error);
            }
        });
        Map<Item, List<Help>> frozen = new LinkedHashMap<>();
        items.forEach((item, values) -> frozen.put(item, List.copyOf(values)));
        cache = new Cache(Map.copyOf(frozen), List.copyOf(information));
        if (information.isEmpty()) LogUtils.getLogger().warn("No JEM item-help summaries with registered spotlight items were loaded");
    }

    private static String firstSpotlight(JsonObject entry) {
        for (var element : entry.getAsJsonArray("pages")) {
            JsonObject page = element.getAsJsonObject();
            if (page.has("type") && (page.get("type").getAsString().equals("patchouli:spotlight")
                    || page.get("type").getAsString().equals("spotlight")) && page.has("item")) {
                return page.get("item").getAsString();
            }
        }
        return null;
    }

    private static List<Component> information(JsonObject entry, String summary) {
        LinkedHashSet<String> paragraphs = new LinkedHashSet<>();
        if (entry.has("name")) paragraphs.add(plain(entry.get("name").getAsString()));
        paragraphs.add(summary);
        for (var element : entry.getAsJsonArray("pages")) {
            JsonObject page = element.getAsJsonObject();
            if (page.has("title")) paragraphs.add(plain(page.get("title").getAsString()));
            if (page.has("text")) paragraphs.add(plain(page.get("text").getAsString()));
        }
        return paragraphs.stream().filter(text -> !text.isBlank()).<Component>map(Component::literal).toList();
    }

    private static String plain(String source) {
        Matcher matcher = MACRO.matcher(source);
        StringBuilder text = new StringBuilder();
        while (matcher.find()) {
            String macro = matcher.group(1);
            String replacement = "";
            if (macro.equals("br")) replacement = "\n";
            else if (macro.equals("br2")) replacement = "\n\n";
            else if (macro.matches("li[0-9]*")) replacement = "\n- ";
            else if (macro.startsWith("jemdrill:") && net.minecraftforge.fml.ModList.get().isLoaded("jem_drill_lifecycle")) {
                replacement = HandbookText.drillPolicy(macro.substring("jemdrill:".length()));
            }
            else if (macro.startsWith("k:") || macro.startsWith("jemkey:")) {
                String keyName = macro.substring(macro.indexOf(':') + 1);
                replacement = Component.translatable("key.keyboard.unknown").getString();
                for (var key : Minecraft.getInstance().options.keyMappings) {
                    if (key.getName().equals(keyName)) {
                        replacement = key.getTranslatedKeyMessage().getString();
                        break;
                    }
                }
            }
            matcher.appendReplacement(text, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(text);
        return text.toString().replaceAll("\u00a7[0-9a-fk-orA-FK-OR]", "").strip();
    }

    public record Info(ItemStack stack, List<Component> information) {
        public Info {
            stack = stack.copy();
            information = List.copyOf(information);
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }

    private record Help(ItemStack stack, String summary, List<Component> information) {}
    private record Cache(Map<Item, List<Help>> items, List<Info> information) {}
}
