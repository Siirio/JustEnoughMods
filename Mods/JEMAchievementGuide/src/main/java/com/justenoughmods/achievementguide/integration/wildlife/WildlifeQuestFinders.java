package com.justenoughmods.achievementguide.integration.wildlife;

import com.github.alexthe666.alexsmobs.entity.EntitySeagull;
import com.github.alexthe666.alexsmobs.world.AMWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.item.ItemStack;

public final class WildlifeQuestFinders {
    public record Offering(ItemEntity drop, ItemStack before, ServerPlayer actor) {}
    private WildlifeQuestFinders() {}
    public static void guided(EntitySeagull bird, Offering offer) {
        if (offer == null || bird.getTreasurePos() == null || bird.treasureSitTime != 300 || bird.getMainHandItem().isEmpty()
                || !ItemStack.isSameItemSameTags(offer.before(), bird.getMainHandItem())) return;
        var tag = WildlifeQuestProof.data(bird); tag.putUUID("treasure_actor", offer.actor().getUUID());
        tag.putUUID("treasure_offer", offer.drop().getUUID()); tag.putInt("treasure_offer_count", offer.before().getCount());
        tag.putLong("treasure_marker", bird.getTreasurePos().asLong());
    }
    public static void arrived(EntitySeagull bird) {
        var actor = WildlifeQuestProof.actor(bird, "treasure_actor"); if (actor == null || !bird.isSitting() || bird.getTreasurePos() == null) return;
        var proof = WildlifeQuestProof.data(bird); if (!proof.hasUUID("treasure_offer") || proof.getLong("treasure_marker") != bird.getTreasurePos().asLong()) return;
        var offer = actor.serverLevel().getEntity(proof.getUUID("treasure_offer"));
        if (offer instanceof ItemEntity item && item.getItem().getCount() >= proof.getInt("treasure_offer_count")) return;
        BlockPos marker = bird.getTreasurePos();
        for (int y = actor.level().getMinBuildHeight(); y < actor.level().getMaxBuildHeight(); y++) {
            BlockPos pos = new BlockPos(marker.getX(), y, marker.getZ());
            if (actor.level().getBlockEntity(pos) instanceof ChestBlockEntity chest
                    && chest.saveWithoutMetadata().getString("LootTable").equals("minecraft:chests/buried_treasure")) {
                CompoundTag receipt = new CompoundTag(); receipt.putLong("pos", pos.asLong()); receipt.putUUID("bird", bird.getUUID());
                receipt.putString("dimension", actor.level().dimension().location().toString()); WildlifeQuestProof.data(actor).put("guided_treasure", receipt);
                proof.remove("treasure_offer"); return;
            }
        }
    }
    public static void opened(ChestBlockEntity chest, ServerPlayer actor) {
        if (WildlifeQuestProof.real(actor) == null) return; var proof = WildlifeQuestProof.data(actor).getCompound("guided_treasure");
        if (proof.hasUUID("bird") && proof.getLong("pos") == chest.getBlockPos().asLong()
                && proof.getString("dimension").equals(actor.level().dimension().location().toString())
                && actor.containerMenu.slots.stream().anyMatch(slot -> slot.container == chest)) {
            WildlifeQuestProof.emit(actor, "v3_fauna_hidden_finders"); WildlifeQuestProof.data(actor).remove("guided_treasure");
        }
    }
    public static void located(ServerPlayer actor) {
        var data = AMWorldData.get(actor.level()); if (data == null || data.getPupfishChunk() == null) return;
        var proof = new CompoundTag(); proof.putLong("chunk", data.getPupfishChunk().toLong());
        proof.putString("dimension", actor.level().dimension().location().toString()); WildlifeQuestProof.data(actor).put("pupfish_locator", proof);
    }
    public static void captured(ServerPlayer actor, net.minecraft.world.entity.LivingEntity fish) {
        if (!WildlifeQuestProof.type(fish).equals("alexsmobs:devils_hole_pupfish")) return;
        var proof = WildlifeQuestProof.data(actor).getCompound("pupfish_locator"); var data = AMWorldData.get(actor.level());
        if (proof.contains("chunk") && data != null && data.isInPupfishChunk(fish.blockPosition()) && data.getPupfishChunk() != null
                && proof.getLong("chunk") == data.getPupfishChunk().toLong() && proof.getString("dimension").equals(actor.level().dimension().location().toString()))
            WildlifeQuestProof.emit(actor, "v3_fauna_hidden_finders");
    }
}
