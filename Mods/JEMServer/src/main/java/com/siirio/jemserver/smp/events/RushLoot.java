package com.siirio.jemserver.smp.events;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.siirio.jemserver.smp.SmpData;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.*;
import net.minecraftforge.common.util.FakePlayer;

public final class RushLoot extends LootModifier {
    public static final Codec<RushLoot> CODEC =
            RecordCodecBuilder.create(i -> codecStart(i).apply(i, RushLoot::new));
    private static final TagKey<Block> ELIGIBLE =
            TagKey.create(Registries.BLOCK, new ResourceLocation("jem_server", "rush_resources"));
    private static final TagKey<Block> BLOCK_DROPS =
            TagKey.create(Registries.BLOCK, new ResourceLocation("jem_server", "rush_block_drops"));
    private static final TagKey<Block> ORES =
            TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores"));

    public RushLoot(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> loot, LootContext context) {
        var actor = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        var origin = context.getParamOrNull(LootContextParams.ORIGIN);
        var state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (!(actor instanceof net.minecraft.server.level.ServerPlayer player)
                || player instanceof FakePlayer
                || player.isCreative()
                || origin == null
                || state == null
                || state.hasBlockEntity()
                || !state.is(ELIGIBLE)) return loot;
        var event = EventScheduler.active(player.server, "RESOURCE_RUSH");
        var pos = BlockPos.containing(origin);
        if (event == null
                || event.getBoolean("rushTrackingFull")
                || ResourcePlacements.excluded(player.serverLevel(),pos)
                || !EventRegions.contains(event, player.serverLevel(), pos)) return loot;
        var excluded = event.getCompound("placements");
        String key = Long.toString(pos.asLong());
        if (excluded.contains(key)) return loot;
        if (excluded.size() >= com.siirio.jemserver.smp.SmpConfig.RUSH_TRACKED_BLOCKS.get()) {
            event.putBoolean("rushTrackingFull", true);
            SmpData.get(player.server).changed(event);
            return loot;
        }
        excluded.putBoolean(key, true);
        event.put("placements", excluded);
        var participants = event.getCompound("rushParticipants");
        if (!participants.getBoolean(player.getStringUUID())) {
            participants.putBoolean(player.getStringUUID(), true);
            event.put("rushParticipants", participants);
            com.siirio.jemserver.smp.Profiles.countEvent(player.server, player.getUUID(), "RESOURCE_RUSH");
        }
        SmpData.get(player.server).changed(event);
        var tool=context.getParamOrNull(LootContextParams.TOOL);
        if(state.is(ORES) && tool!=null && net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,tool)>0) return loot;
        int size = loot.size();
        for (int i = 0; i < size; i++) {
            var stack = loot.get(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof BlockItem block) {
                var drop = block.getBlock().defaultBlockState();
                if (drop.hasBlockEntity() || drop.is(ORES) || !drop.is(BLOCK_DROPS)) continue;
            }
            loot.add(stack.copy());
        }
        return loot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
