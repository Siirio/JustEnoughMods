package com.siirio.jemeconomy.merchant;

import com.google.gson.JsonParser;
import com.siirio.jemeconomy.policy.ItemPolicy;
import com.siirio.jemeconomy.policy.ItemPolicyRegistry;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

/** Native-only merchandise pricing. Never broadens shipping or global Market admission. */
public final class NativeItemPricing {
    private record Definition(ItemPolicy policy, int sellUnits) { }
    private static final Map<ResourceLocation, Definition> NATIVE = load();
    private NativeItemPricing() { }

    public static Optional<ItemPolicy> policy(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        var canonical = ItemPolicyRegistry.policy(stack).filter(p -> p.sellable() || p.buyable());
        if (canonical.isPresent()) return canonical;
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        var definition = NATIVE.get(id);
        var policy = definition == null ? null : definition.policy();
        // Exact merchandise allowlist; arbitrary boss equipment and progression items have no fallback.
        if (policy == null) return Optional.empty();
        long premium = 0;
        for (var entry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            int level = Math.max(0, Math.min(255, entry.getValue()));
            long scarcity = entry.getKey().isTreasureOnly() ? 160L : switch (entry.getKey().getRarity()) {
                case COMMON -> 40L; case UNCOMMON -> 60L; case RARE -> 80L; case VERY_RARE -> 120L;
            };
            premium = Math.addExact(premium, Math.multiplyExact(scarcity, Math.multiplyExact((long) level, level)));
        }
        long buy = Math.addExact(policy.buyCopper(), premium);
        if (EnchantmentHelper.getEnchantments(stack).containsKey(net.minecraft.world.item.enchantment.Enchantments.MENDING))
            buy = Math.max(800L, buy);
        // Damage, custom names, maps and enchantments do not increase buyback. Original NBT remains on the offer.
        long sell = policy.sellCopper();
        if (stack.isDamaged() && sell > 0) sell = Math.max(1L, sell / 4L);
        return Optional.of(new ItemPolicy(1, id, policy.outcome(), sell, buy,
                Math.min(policy.dailyPlayerUnits(), stack.getMaxStackSize() == 1 ? 8 : 64),
                Math.min(policy.dailyGlobalUnits(), stack.getMaxStackSize() == 1 ? 128 : 512), policy.demandGroup(), policy.notes()));
    }

    /** Only exact priced native wines whose installed Vinery lifecycle owns these aging fields. */
    public static boolean usesWineAging(ItemStack stack) {
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("vinery")
                && (id.getPath().equals("cherry_wine") || id.getPath().equals("lilitu_wine"))
                && NATIVE.containsKey(id);
    }

    /** Preserve exact tags for every other good; do not price names, arbitrary effects or invented quality. */
    public static boolean matchesGoods(ItemStack expected, ItemStack actual) {
        if (ItemStack.isSameItemSameTags(expected, actual)) return true;
        if (!usesWineAging(expected) || expected.getItem() != actual.getItem()) return false;
        var wanted = expected.getTag();
        String[] aging = {"Year", "EffectAmplifier", "EffectDuration"};
        // An offer explicitly specifying a vintage remains exact, rather than accepting any year.
        if (wanted != null && !wanted.isEmpty()) return false;
        var actualTag = actual.getTag();
        if (actualTag == null) return true;
        for (String key : actualTag.getAllKeys()) {
            if (!java.util.List.of(aging).contains(key) || !actualTag.contains(key, net.minecraft.nbt.Tag.TAG_INT)) return false;
            int value = actualTag.getInt(key);
            // Native config supports at most ten levels and 100000 seconds; no field increases the payout.
            if (value < 0 || (key.equals("EffectAmplifier") && value > 10)
                    || (key.equals("EffectDuration") && value > 2_000_000)) return false;
        }
        return true;
    }

    /** Price metadata for audit/export; canonical Market commodities always have denominator one. */
    public static int sellUnits(ItemStack goods) {
        if (ItemPolicyRegistry.policy(goods).filter(ItemPolicy::sellable).isPresent()) return Math.toIntExact(ItemPolicyRegistry.policy(goods).orElseThrow().sellDenominator());
        var definition = NATIVE.get(ForgeRegistries.ITEMS.getKey(goods.getItem()));
        return definition == null ? 1 : definition.sellUnits();
    }

    /** Shared exact sale arithmetic; native merchandise keeps its real sell_units denominator. */
    public static com.siirio.jemeconomy.policy.RationalPrice salePrice(ItemStack goods,int quantity,int multiplier) {
        if(goods.isEmpty()||quantity<=0||multiplier<=0)throw new ArithmeticException("Invalid native sale");
        var canonical=ItemPolicyRegistry.policy(goods).filter(ItemPolicy::sellable);
        if(canonical.isPresent())return canonical.get().salePrice(quantity,multiplier);
        var policy=policy(goods).orElseThrow(()->new ArithmeticException("Unpriced native sale"));
        if(!policy.sellable())return com.siirio.jemeconomy.policy.RationalPrice.ZERO;
        var definition=NATIVE.get(ForgeRegistries.ITEMS.getKey(goods.getItem()));
        if(definition==null)throw new ArithmeticException("Missing native sale definition");
        return new com.siirio.jemeconomy.policy.RationalPrice(policy.sellCopper(),definition.sellUnits())
                .multiply(quantity).multiply(multiplier).divide(100);
    }
    public static long saleCopper(ItemStack goods,int quantity,int multiplier) { return salePrice(goods,quantity,multiplier).floor(); }

    private static Map<ResourceLocation, Definition> load() {
        Map<ResourceLocation, Definition> result = new LinkedHashMap<>();
        loadResource(result, "native_item_prices.json");
        loadResource(result, "profession_pools.json");
        return Map.copyOf(result);
    }
    private static void loadResource(Map<ResourceLocation, Definition> result, String file) {
        try (var stream = NativeItemPricing.class.getResourceAsStream("/data/jem_economy/economy/" + file)) {
            if (stream == null) throw new IllegalStateException("Missing native merchandise prices: " + file);
            var rows = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            for (var element : rows) {
                var row = element.getAsJsonObject();
                var id = new ResourceLocation(row.get("item").getAsString());
                long buy = row.get("buy_copper").getAsLong(), sell = row.get("sell_copper").getAsLong();
                int sellUnits = row.has("sell_units") ? row.get("sell_units").getAsInt() : 1;
                if (sellUnits <= 0 || sell < 0 || sell == 0 && !row.has("buyback_blocked_reason") || buy <= 0 || Math.multiplyExact(buy, sellUnits) <= Math.multiplyExact(sell, 2L))
                    throw new IllegalStateException("Unsafe native spread: " + id);
                var previous = result.putIfAbsent(id, new Definition(new ItemPolicy(1, id, sell > 0 ? ItemPolicy.Outcome.BUY_AND_SELL : ItemPolicy.Outcome.BUY_ONLY, sell, buy,
                        row.has("daily_player_units") ? row.get("daily_player_units").getAsInt() : 8,
                        row.has("daily_global_units") ? row.get("daily_global_units").getAsInt() : 128,
                        "native_merchandise", row.get("notes").getAsString()), sellUnits));
                if (previous != null && (previous.policy().buyCopper() != buy || previous.policy().sellCopper() != sell || previous.sellUnits() != sellUnits))
                    throw new IllegalStateException("Conflicting native item prices: " + id);
            }
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot load " + file, failure); }
    }
}
