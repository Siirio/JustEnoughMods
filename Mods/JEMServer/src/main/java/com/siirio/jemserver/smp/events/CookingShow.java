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
    public static void prepare(MinecraftServer server, CompoundTag row, long now) {
        var dishes = CookingRecipes.available(server);
        SmpRecords.require(!dishes.isEmpty(), "invalid_cooking_menu");
        int difficulty = server.overworld().random.nextInt(3);
        var pool = CookingRecipes.tier(dishes, difficulty);
        var requirements = new CompoundTag();
        int count = Math.min(SmpConfig.COOKING_DISH_COUNT.get(), pool.size());
        var choices = new ArrayList<>(pool);
        for (int index = 0; index < count; index++) {
            var dish = choices.remove(server.overworld().random.nextInt(choices.size()));
            requirements.putInt(dish.item().toString(), SmpConfig.COOKING_DISH_QUANTITY.get());
        }
        row.put("cookingRequirements", requirements);
        row.putString("cookingDifficulty", List.of("EASY", "MEDIUM", "HARD").get(difficulty));
        row.putString("cookingReward", "jem_server:events/cooking_" + List.of("easy", "medium", "hard").get(difficulty));
        row.putLong("ends", now + java.util.concurrent.TimeUnit.MINUTES.toMillis(SmpConfig.COOKING_MINUTES.get()));
    }

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) progress(player, event.getCrafting());
    }

    public static void progress(ServerPlayer player, ItemStack stack) {
        if (player instanceof FakePlayer || stack.isEmpty()) return;
        var row = EventScheduler.active(player.server, "COOKING_SHOW");
        if (row == null || System.currentTimeMillis() >= row.getLong("ends")) return;
        String item = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        var requirements = row.getCompound("cookingRequirements");
        if (!requirements.contains(item)) return;
        var scores = row.getCompound("cookingScores");
        var score = scores.getCompound(player.getStringUUID());
        if (!advance(requirements, score, item, stack.getCount())) return;
        scores.put(player.getStringUUID(), score);
        row.put("cookingScores", scores);
        SmpData.get(player.server).changed(row);
        if (!requirements.getAllKeys().stream().allMatch(key -> score.getInt(key) >= requirements.getInt(key))) return;
        var loot = player.server.getLootData().getLootTable(new ResourceLocation(row.getString("cookingReward")));
        var params = new LootParams.Builder(player.serverLevel()).withParameter(LootContextParams.ORIGIN, player.position()).create(LootContextParamSets.CHEST);
        score.putBoolean("rewarded", true);
        SmpData.get(player.server).changed(row);
        Profiles.countEvent(player.server, player.getUUID(), "COOKING_SHOW");
        PendingRewardContainer.enqueue(player.server, player.getUUID(), row.getUUID("id").toString(), "jem.smp.COOKING_SHOW", loot.getRandomItems(params));
        player.sendSystemMessage(Component.translatable("jem.smp.cooking_complete").withStyle(ChatFormatting.GOLD));
    }

    static boolean advance(CompoundTag requirements, CompoundTag score, String item, int count) {
        if (count <= 0 || score.getBoolean("rewarded")) return false;
        int required = requirements.getInt(item);
        int previous = Math.max(0, score.getInt(item));
        if (required <= previous) return false;
        score.putInt(item, (int) Math.min(required, (long) previous + count));
        return true;
    }

    public static void view(ServerPlayer player, CompoundTag row, CompoundTag detail) {
        detail.remove("cookingScores");
        detail.put("cookingProgress", row.getCompound("cookingScores").getCompound(player.getStringUUID()).copy());
    }

    private CookingShow() {}
}
