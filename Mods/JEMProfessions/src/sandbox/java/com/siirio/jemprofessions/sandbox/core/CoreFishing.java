package com.siirio.jemprofessions.sandbox.core;

import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.ProfessionSavedData;
import com.siirio.jemprofessions.progress.SourceKind;
import com.siirio.jemprofessions.sandbox.SandboxProfessionSupport;
import com.siirio.jemprofessions.mixin.sandbox.core.LootItemAccessor;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CoreFishing {
    private static final Set<Item> FISH = Set.of(Items.COD, Items.SALMON, Items.PUFFERFISH, Items.TROPICAL_FISH);
    private CoreFishing() { }
    static boolean activate(ServerPlayer player, String node, CompoundTag input) {
        var data = SandboxCoreData.get(player.getServer()); var settings = data.settings(player.getUUID());
        if (input.getString("Mode").equals("select")) {
            ResourceLocation id = ResourceLocation.tryParse(input.getString("Option"));
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || !FISH.contains(BuiltInRegistries.ITEM.get(id))) {
                SandboxCoreEffects.tell(player, "Выбери треску, лосося, иглобрюха или тропическую рыбу.", "Choose cod, salmon, pufferfish or tropical fish."); return true;
            }
            settings.fish = id.toString(); data.setDirty();
        }
        if (input.getString("Mode").equals("bait")) {
            if (!SandboxCoreEffects.has(player, ProfessionId.FISHER, "rare_bait") && !SandboxCoreEffects.has(player, ProfessionId.FISHER, "trophy_catch")) {
                SandboxCoreEffects.tell(player, "Для наживки нужен навык «Редкая наживка» или «Трофейный улов».", "Rare Bait or Trophy Catch is required to use bait."); return true;
            }
            settings.bait = !settings.bait; data.setDirty();
        }
        SandboxCoreEffects.tell(player, "Выбранная рыба: " + settings.fish + ". Наживка: " + (settings.bait ? "одна рыба при забросе" : "выключена"),
                "Selected fish: " + settings.fish + ". Bait: " + (settings.bait ? "one fish per cast" : "off")); return true;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void cast(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof FishingHook hook) || !(hook.getPlayerOwner() instanceof ServerPlayer player) || !SandboxCoreEffects.eligible(player)) return;
        SandboxCoreEffects.defer(() -> {
            if (event.isCanceled() || !hook.isAlive() || !hook.isAddedToWorld() || hook.getPersistentData().getBoolean("JemProfessionCast")) return;
            hook.getPersistentData().putBoolean("JemProfessionCast", true);
            var settings = SandboxCoreData.get(player.getServer()).settings(player.getUUID()); hook.getPersistentData().putString("JemSelectedFish", settings.fish);
            if (!settings.bait || (!SandboxCoreEffects.has(player, ProfessionId.FISHER, "rare_bait") && !SandboxCoreEffects.has(player, ProfessionId.FISHER, "trophy_catch"))) return;
            int slot = CoreActions.slot(player, stack -> !stack.hasTag() && FISH.contains(stack.getItem()));
            if (slot < 0) { SandboxCoreEffects.tell(player, "Наживки нет: этот заброс без усиления.", "No bait available: this cast has no bait bonus."); return; }
            player.getInventory().getItem(slot).shrink(1); player.containerMenu.broadcastChanges(); hook.getPersistentData().putBoolean("JemFishBait", true);
        });
    }
    /** Wrap only an exclusively ordinary native fish pool. Loot generation and functions remain native. */
    public static boolean expand(LootPoolEntryContainer container, LootContext context, Consumer<LootPoolEntry> output, LootPoolEntryContainer[] pool) {
        if (!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof FishingHook hook)
                || !(hook.getPlayerOwner() instanceof ServerPlayer player) || !SandboxCoreEffects.eligible(player)
                || !(container instanceof LootItem) || pool.length == 0 || pool.length > 32) return container.expand(context, output);
        for (LootPoolEntryContainer candidate : pool) if (!(candidate instanceof LootItem item) || !new ItemStack(((LootItemAccessor) item).jem$item()).is(ItemTags.FISHES)) return container.expand(context, output);
        boolean bait = hook.getPersistentData().getBoolean("JemFishBait");
        int bonus = bait && SandboxCoreEffects.has(player, ProfessionId.FISHER, "trophy_catch") ? 50
                : bait && SandboxCoreEffects.has(player, ProfessionId.FISHER, "rare_bait") ? 35
                : SandboxCoreEffects.has(player, ProfessionId.FISHER, "target_fishing") || SandboxCoreEffects.has(player, ProfessionId.FISHER, "rare_bait") || SandboxCoreEffects.has(player, ProfessionId.FISHER, "trophy_catch") ? 20 : 0;
        if (bonus == 0) return container.expand(context, output);
        String selected = hook.getPersistentData().getString("JemSelectedFish");
        if (selected.isEmpty()) selected = SandboxCoreData.get(player.getServer()).settings(player.getUUID()).fish;
        boolean chosen = BuiltInRegistries.ITEM.getKey(((LootItemAccessor) container).jem$item()).toString().equals(selected);
        int multiplier = chosen ? 100 + bonus : 100;
        return container.expand(context, entry -> output.accept(new LootPoolEntry() {
            @Override public int getWeight(float luck) {
                return (int) Math.min(1_000_000L, Math.max(0L, (long) entry.getWeight(luck)) * multiplier);
            }
            @Override public void createItemStack(Consumer<ItemStack> consumer, LootContext nativeContext) { entry.createItemStack(consumer, nativeContext); }
        }));
    }
    /** Called on the original native list after ItemFishedEvent cancellation has been checked. */
    public static List<ItemStack> completed(FishingHook hook, List<ItemStack> loot) {
        if (!(hook.getPlayerOwner() instanceof ServerPlayer player) || !SandboxCoreEffects.eligible(player) || hook.getPersistentData().getBoolean("JemFishCompleted")) return loot;
        hook.getPersistentData().putBoolean("JemFishCompleted", true);
        ItemStack ordinary = loot.stream().filter(stack -> !stack.isEmpty() && stack.is(ItemTags.FISHES)).findFirst().orElse(ItemStack.EMPTY);
        if (ordinary.isEmpty()) return loot;
        var record = SandboxProfessionSupport.record(player); if (record == null) return loot;
        ResourceLocation species = BuiltInRegistries.ITEM.getKey(ordinary.getItem());
        boolean first = record.claimDiscovery("fisher:species:" + species); ProfessionSavedData.get(player.getServer()).changed();
        SandboxCoreEffects.credit(player, ProfessionId.FISHER, SourceKind.FISH_CAUGHT, species, first ? 12 : 3,
                first ? null : "fisher:repeat", first ? 0 : 300, "hook:" + hook.getUUID());
        float chance = SandboxCoreEffects.has(player, ProfessionId.FISHER, "net_master") ? .20F : SandboxCoreEffects.has(player, ProfessionId.FISHER, "double_catch") ? .10F : 0;
        if (FISH.contains(ordinary.getItem()) && !ordinary.hasTag() && player.getRandom().nextFloat() < chance) loot.add(new ItemStack(ordinary.getItem(), 1));
        return loot;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void steady(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SandboxCoreEffects.eligible(player) && SandboxCoreEffects.has(player, ProfessionId.FISHER, "steady_line")
                && !event.getDrops().isEmpty() && event.getRodDamage() == 1 && player.getRandom().nextFloat() < .20F) event.damageRodBy(0);
    }
}
