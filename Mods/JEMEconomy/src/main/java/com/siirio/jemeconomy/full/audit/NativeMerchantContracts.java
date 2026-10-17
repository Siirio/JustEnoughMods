package com.siirio.jemeconomy.full.audit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.siirio.jemeconomy.merchant.ProfessionTradePools;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.registries.ForgeRegistries;

/** Explicit export-only contracts on unsaved, unticked in-memory NPCs. Never adds
 * entities to a level, opens menus, generates maps or mutates player/world saves. */
public final class NativeMerchantContracts {
    private static final String CHOICES = "jem_economy_profession_choices_v1";
    private static final int SEEDS = 32;
    private static final List<String> PROFESSIONS = List.of("armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman",
            "fletcher", "leatherworker", "librarian", "mason", "shepherd", "toolsmith", "weaponsmith");
    private NativeMerchantContracts() { }

    public static JsonObject snapshot(ServerLevel level) {
        Counter checks = new Counter(); JsonArray professions = new JsonArray(); int varied = 0;
        for (String path : PROFESSIONS) {
            ResourceLocation id = new ResourceLocation("minecraft", path);
            var profession = ForgeRegistries.VILLAGER_PROFESSIONS.getValue(id);
            checks.require(profession != null, "Missing native profession " + id);
            Set<String> selections = new HashSet<>(); int eligibleSelections = 0; boolean varietyExpected = false;
            for (int seed = 0; seed < SEEDS; seed++) {
                Villager villager = new Villager(EntityType.VILLAGER, level);
                villager.setVillagerData(villager.getVillagerData().setProfession(profession).setLevel(1));
                villager.getRandom().setSeed(seed);
                MerchantOffers offers = new MerchantOffers();
                offers.add(new MerchantOffer(new ItemStack(Items.WHEAT, 20), new ItemStack(Items.EMERALD), 16, 2, 0.05F));
                ((com.siirio.jemeconomy.mixin.AbstractVillagerOffersAccessor) villager).jem$setOffers(offers);
                checks.require(villager.getOffers() == offers && villager.getOffers().size() == 1,
                        "Native sentinel list was not installed before extension: " + id);
                CompoundTag sentinel = offersTag(villager).getList("Recipes", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).copy();
                ProfessionTradePools.extend(villager);
                checks.require(villager.getPersistentData().getCompound(CHOICES).isEmpty(), "Novice added a profession selection: " + id);
                checks.require(villager.getOffers().size() <= 2 && villager.getOffers().stream().allMatch(NativeMerchantContracts::wheatOnly),
                        "Novice gained merchandise beyond native wheat and its reverse: " + id + " actualOffers=" + offersTag(villager));
                checks.require(sentinel.equals(offersTag(villager).getList("Recipes", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0)), "Native sentinel changed: " + id);
                assertStable(villager, checks, "novice " + id);
                for (int tier = 2; tier <= 5; tier++) {
                    final int currentTier = tier;
                    final Villager beforeUnlock = villager;
                    List<ProfessionTradePools.Entry> eligible = ProfessionTradePools.entries().stream()
                            .filter(entry -> entry.profession().equals(id) && entry.level() == currentTier && ForgeRegistries.ITEMS.containsKey(entry.item())
                                    && !hasItem(beforeUnlock.getOffers(), entry.item())).toList();
                    if (eligible.stream().map(ProfessionTradePools.Entry::item).distinct().count() > 1L) varietyExpected = true;
                    villager.getRandom().setSeed(seed * 31L + tier);
                    villager.setVillagerData(villager.getVillagerData().setLevel(tier));
                    ProfessionTradePools.extend(villager);
                    CompoundTag choices = villager.getPersistentData().getCompound(CHOICES);
                    String key = id + ":" + tier, selected = choices.getString(key);
                    checks.require(choices.contains(key), "Missing persisted level selection " + key);
                    if (eligible.isEmpty()) checks.require(selected.equals("none"), "Empty pool generated an offer " + key);
                    else {
                        var match = eligible.stream().filter(entry -> entry.item().toString().equals(selected)).findFirst();
                        checks.require(match.isPresent(), "Selection outside eligible native-profession pool " + key + "=" + selected);
                        var entry = match.orElseThrow();
                        checks.require(villager.getOffers().stream().anyMatch(offer -> selected.equals(String.valueOf(ForgeRegistries.ITEMS.getKey(offer.getResult().getItem())))
                                        && offer.getResult().getCount() == entry.count() && offer.getMaxUses() == entry.maxUses()), "Selected merchandise/count/stock missing " + key + " actualOffers=" + offersTag(villager));
                        eligibleSelections++;
                    }
                    checks.require(sentinel.equals(offersTag(villager).getList("Recipes", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0)), "Native sentinel changed after level " + key);
                    assertStable(villager, checks, key);
                    CompoundTag saved = villager.saveWithoutId(new CompoundTag());
                    Villager restored = new Villager(EntityType.VILLAGER, level);
                    restored.load(saved);
                    checks.require(offersTag(villager).equals(offersTag(restored)), "Native NBT changed exact offers " + key);
                    checks.require(choices.equals(restored.getPersistentData().getCompound(CHOICES)), "Native NBT lost selected pool entries " + key);
                    assertStable(restored, checks, "loaded " + key);
                    villager = restored;
                }
                selections.add(villager.getPersistentData().getCompound(CHOICES).toString());
            }
            if (varietyExpected) { checks.require(selections.size() > 1, "32 seeds produced no expected individual variety " + id); varied++; }
            JsonObject result = new JsonObject(); result.addProperty("profession", id.toString()); result.addProperty("seeds", SEEDS);
            result.addProperty("distinctFullSelections", selections.size()); result.addProperty("eligibleSelectionsChecked", eligibleSelections);
            result.addProperty("varietyExpected", varietyExpected); professions.add(result);
        }
        JsonObject report = new JsonObject(); report.addProperty("scope", "Unsaved in-memory native Villagers: novice preservation, levels2-5 eligible choices, reopen idempotency and exact native-NBT roundtrip. No world spawn or actual restock.");
        report.addProperty("assertionsPassed", checks.count); report.addProperty("professions", PROFESSIONS.size()); report.addProperty("seedsPerProfession", SEEDS);
        report.addProperty("variedProfessions", varied); report.add("results", professions); return report;
    }
    private static void assertStable(Villager villager, Counter checks, String label) {
        CompoundTag offers = offersTag(villager), choices = villager.getPersistentData().getCompound(CHOICES).copy();
        ProfessionTradePools.extend(villager);
        checks.require(offers.equals(offersTag(villager)), "Reopening rerolled or changed offers " + label);
        checks.require(choices.equals(villager.getPersistentData().getCompound(CHOICES)), "Reopening rerolled selections " + label);
    }
    private static CompoundTag offersTag(Villager villager) { return villager.saveWithoutId(new CompoundTag()).getCompound("Offers").copy(); }
    private static boolean hasItem(List<MerchantOffer> offers, ResourceLocation item) {
        return offers.stream().anyMatch(offer -> item.equals(ForgeRegistries.ITEMS.getKey(offer.getResult().getItem()))
                || item.equals(ForgeRegistries.ITEMS.getKey(offer.getBaseCostA().getItem())));
    }
    private static boolean wheatOnly(MerchantOffer offer) {
        return offer.getBaseCostA().is(Items.WHEAT) && offer.getBaseCostA().getCount() == 20 && offer.getResult().is(Items.EMERALD)
                || offer.getResult().is(Items.WHEAT) && offer.getResult().getCount() == 20 && offer.getBaseCostA().is(Items.EMERALD);
    }
    private static final class Counter {
        private int count;
        void require(boolean value, String message) { if (!value) throw new IllegalStateException("Native merchant contract: " + message); count++; }
    }
}
