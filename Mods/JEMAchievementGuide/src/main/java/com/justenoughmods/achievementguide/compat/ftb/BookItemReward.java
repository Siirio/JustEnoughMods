package com.justenoughmods.achievementguide.compat.ftb;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Native FTB item/choice presentation with the existing JEM take-only delivery owner. */
public final class BookItemReward extends ItemReward {
    private static final RewardType TYPE = RewardTypes.register(new ResourceLocation("jem_advancements", "quest_item"),
            BookItemReward::new, () -> Icon.getIcon("minecraft:item/chest"));
    private String claimKey = "";
    private long claimRewardId;

    public BookItemReward(long id, Quest quest) { super(id, quest); }
    static void register() { }
    @Override public RewardType getType() { return TYPE; }
    @Override public void readData(CompoundTag tag) {
        super.readData(tag); claimKey = tag.getString("claim_key");
        claimRewardId = Long.parseUnsignedLong(tag.getString("claim_reward_id"), 16);
    }
    @Override public void writeData(CompoundTag tag) {
        super.writeData(tag); tag.putString("claim_key", claimKey);
        tag.putString("claim_reward_id", String.format("%016X", claimRewardId));
    }
    @Override public void writeNetData(FriendlyByteBuf buffer) { super.writeNetData(buffer); buffer.writeUtf(claimKey, 128); buffer.writeLong(claimRewardId); }
    @Override public void readNetData(FriendlyByteBuf buffer) { super.readNetData(buffer); claimKey = buffer.readUtf(128); claimRewardId = buffer.readLong(); }

    @Override public boolean automatedClaimPre(net.minecraft.world.level.block.entity.BlockEntity block,
            List<ItemStack> output, net.minecraft.util.RandomSource random, UUID owner, ServerPlayer player) {
        // Native hopper issuance has no source-aware personal transaction or original-team context.
        if (player != null && BookMigration.managed(player.server)) return false;
        if (getQuestFile() instanceof dev.ftb.mods.ftbquests.quest.ServerQuestFile file && BookMigration.managed(file.server)) return false;
        return super.automatedClaimPre(block, output, random, owner, player);
    }

    @Override public void claim(ServerPlayer player, boolean notify) {
        var file = dev.ftb.mods.ftbquests.quest.ServerQuestFile.INSTANCE;
        claimFrom(player, file.getOrCreateTeamData(player), file.getReward(claimRewardId));
    }

    String source() { return "ftb_quest:" + claimKey; }
    long parentId() { return claimRewardId; }

    /** The caller supplies the native source team, never a shared mutable reward context. */
    void claimFrom(ServerPlayer player, TeamData origin, Reward parent) {
        try {
            enqueue(player, BookMigration.delivery(player, this));
        } catch (RuntimeException failure) {
            // FTB records the parent claim before calling this item/choice option.
            // Reopen that exact native entitlement when durable delivery fails.
            if (parent != null && !issued(player, source())) origin.resetReward(player.getUUID(), parent);
            throw failure;
        }
    }

    static boolean issued(ServerPlayer player, String source) {
        return Boolean.TRUE.equals(BookSignals.call("com.siirio.jemrewards.PendingRewardContainer", "hasIssued",
                new Class<?>[]{MinecraftServer.class, UUID.class, String.class}, player.server, player.getUUID(), source));
    }

    private void enqueue(ServerPlayer player, List<ItemStack> delivery) {
        if (claimKey.isEmpty()) throw new IllegalStateException("Quest reward has no durable entitlement key");
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack supplied : delivery) {
            ItemStack prepared = BookStackProofs.prepareReward(supplied);
            if (prepared.isEmpty() || prepared.getMaxStackSize() < 1) throw new IllegalStateException("Unavailable quest reward: " + claimKey);
            int remaining = supplied.getCount();
            while (remaining > 0) {
                int count = Math.min(remaining, prepared.getMaxStackSize());
                items.add(prepared.copyWithCount(count));
                remaining -= count;
            }
        }
        if (items.isEmpty()) throw new IllegalStateException("Empty quest entitlement: " + claimKey);
        BookSignals.call("com.siirio.jemrewards.PendingRewardContainer", "enqueue",
                new Class<?>[]{MinecraftServer.class, UUID.class, String.class, String.class, List.class},
                player.server, player.getUUID(), "ftb_quest:" + claimKey,
                "jem.questbook.reward_delivery", items);
    }
}
