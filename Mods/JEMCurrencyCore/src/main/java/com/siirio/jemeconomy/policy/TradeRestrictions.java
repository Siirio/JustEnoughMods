package com.siirio.jemeconomy.policy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.registries.ForgeRegistries;

/** Immutable minimum restriction shared by public supply and every native NPC result path. */
public final class TradeRestrictions {
    private static final JsonObject RULES = load();
    private static final Set<String> ITEMS = strings("all_npc_items");
    private static final Set<String> ITEM_TAGS = strings("all_npc_item_tags");
    private static final Set<String> BLOCK_TAGS = strings("all_npc_block_tags");
    private static final Set<String> SUFFIXES = strings("all_npc_path_suffixes");
    private static final Set<String> PREFIXES = strings("all_npc_path_prefixes");
    private static final Set<String> MARKET_NAMESPACES = strings("market_only_namespaces");
    private TradeRestrictions() { }

    public static boolean npcProhibited(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return true;
        if (ITEMS.contains(id.toString()) || SUFFIXES.stream().anyMatch(id.getPath()::endsWith)
                || PREFIXES.stream().anyMatch(id.getPath()::startsWith)) return true;
        if (ITEM_TAGS.stream().anyMatch(tag -> stack.is(TagKey.create(Registries.ITEM, new ResourceLocation(tag))))) return true;
        return stack.getItem() instanceof BlockItem block && BLOCK_TAGS.stream()
                .anyMatch(tag -> block.getBlock().defaultBlockState().is(TagKey.create(Registries.BLOCK, new ResourceLocation(tag))));
    }

    public static boolean marketProhibited(ItemStack stack) {
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null || MARKET_NAMESPACES.contains(id.getNamespace()) || npcProhibited(stack);
    }

    public static boolean offerProhibited(MerchantOffer offer) { return npcProhibited(offer.getResult()); }

    private static Set<String> strings(String key) {
        return java.util.stream.StreamSupport.stream(RULES.getAsJsonArray(key).spliterator(), false)
                .map(element -> element.getAsString()).collect(Collectors.toUnmodifiableSet());
    }
    private static JsonObject load() {
        try (var stream = TradeRestrictions.class.getResourceAsStream("/data/jem_economy/economy/trade_restrictions.json")) {
            if (stream == null) throw new IllegalStateException("Missing permanent trade restrictions");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot load permanent trade restrictions", failure); }
    }
}
