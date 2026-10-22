package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;
import com.siirio.jemworldbosstiers.encounter.PendingRewardContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class CookingShow {
    public static void prepare(MinecraftServer server, EventState row, long now) {
        var dishes = CookingRecipes.available(server);
        SmpRecords.require(!dishes.isEmpty(), "invalid_cooking_menu");
        int difficulty = server.overworld().random.nextInt(3);
        var pool = CookingRecipes.tier(dishes, difficulty);
        var requirements = new LinkedHashMap<String, Integer>();
        int count = Math.min(SmpConfig.COOKING_DISH_COUNT.get(), pool.size());
        var choices = new ArrayList<>(pool);
        for (int index = 0; index < count; index++) {
            var dish = choices.remove(server.overworld().random.nextInt(choices.size()));
            requirements.put(dish.item().toString(), SmpConfig.COOKING_DISH_QUANTITY.get());
        }
        row.cookingRequirements(requirements);
        row.cookingDifficulty( List.of("EASY", "MEDIUM", "HARD").get(difficulty));
        row.cookingReward( "jem_server:events/cooking_" + List.of("easy", "medium", "hard").get(difficulty));
        row.ends( now + java.util.concurrent.TimeUnit.MINUTES.toMillis(SmpConfig.COOKING_MINUTES.get()));
    }

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) progress(player, event.getCrafting());
    }

    public static void progress(ServerPlayer player, ItemStack stack) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { progressInScope(player, stack); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> progressInScope(player, stack));
    }

    private static void progressInScope(ServerPlayer player, ItemStack stack) {
        if (player instanceof FakePlayer || stack.isEmpty()) return;
        var row = EventScheduler.active(player.server, "COOKING_SHOW");
        if (row == null || System.currentTimeMillis() >= row.ends()) return;
        String item = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        var requirements = row.cookingRequirements();
        if (!requirements.containsKey(item)) return;
        var scores = new LinkedHashMap<>(row.cookingScores());
        var score = scores.getOrDefault(player.getUUID(), new EventCookingProgress());
        if (!advance(requirements, score, item, stack.getCount())) return;
        scores.put(player.getUUID(), score);
        row.cookingScores(scores);

        if (!requirements.entrySet().stream().allMatch(entry -> score.quantities().getOrDefault(entry.getKey(), 0) >= entry.getValue())) return;
        var loot = player.server.getLootData().getLootTable(new ResourceLocation(row.cookingReward()));
        var params = new LootParams.Builder(player.serverLevel()).withParameter(LootContextParams.ORIGIN, player.position()).create(LootContextParamSets.CHEST);
        score.rewarded(true);

        Profiles.countEvent(player.server, player.getUUID(), "COOKING_SHOW");
        PendingRewardContainer.enqueue(player.server, player.getUUID(), row.id().toString(), "jem.smp.COOKING_SHOW", loot.getRandomItems(params));
        player.sendSystemMessage(Component.translatable("jem.smp.cooking_complete").withStyle(ChatFormatting.GOLD));
    }

    static boolean advance(Map<String, Integer> requirements, EventCookingProgress score, String item, int count) {
        if (count <= 0 || score.rewarded()) return false;
        int required = requirements.getOrDefault(item, 0);
        int previous = Math.max(0, score.quantities().getOrDefault(item, 0));
        if (required <= previous) return false;
        var quantities = new LinkedHashMap<>(score.quantities());
        quantities.put(item, (int) Math.min(required, (long) previous + count));
        score.quantities(quantities);
        return true;
    }

    public static void view(ServerPlayer player, EventState row, CompoundTag detail) {
        detail.remove("cookingScores");
        var progress = new CompoundTag();
        var score = row.cookingScores().get(player.getUUID());
        if (score != null) {
            score.quantities().forEach(progress::putInt);
            if (score.hasRewarded()) progress.putBoolean("rewarded", score.rewarded());
        }
        detail.put("cookingProgress", progress);
    }

    private CookingShow() {}
}
