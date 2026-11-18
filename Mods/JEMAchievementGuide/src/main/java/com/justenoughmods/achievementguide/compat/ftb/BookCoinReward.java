package com.justenoughmods.achievementguide.compat.ftb;

import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.ledger.SystemAccount;
import com.siirio.jemeconomy.ledger.TransferResult;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Optional finite-money quest payment. FTB retains the claim when Economy is disabled. */
public final class BookCoinReward extends Reward {
    private static final RewardType TYPE = RewardTypes.register(
            new ResourceLocation("jem_advancements", "quest_coin"), BookCoinReward::new,
            () -> Icon.getIcon("jem_economy:item/copper_coin"))
            .setDisplayName(Component.translatable("jem.questbook.coin_reward.type"));
    private long amount;

    public BookCoinReward(long id, Quest quest) {
        super(id, quest);
    }

    static void register() { }

    @Override
    public RewardType getType() {
        return TYPE;
    }

    @Override
    public void writeData(CompoundTag tag) {
        super.writeData(tag);
        tag.putLong("amount", amount);
    }

    @Override
    public void readData(CompoundTag tag) {
        super.readData(tag);
        amount = tag.getLong("amount");
    }

    @Override
    public void writeNetData(FriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeVarLong(amount);
    }

    @Override
    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        amount = buffer.readVarLong();
    }

    @Override
    public Component getAltTitle() {
        return Component.translatable("jem.questbook.coin_reward", amount);
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        if (!EconomyApi.isEnabled(player.getServer())) return;
        TeamData origin = dev.ftb.mods.ftbquests.quest.ServerQuestFile.INSTANCE.getOrCreateTeamData(player);
        if (amount <= 0L) {
            reopen(player, origin, "invalid_amount");
            return;
        }
        String key = "ftbQuest:" + Long.toUnsignedString(id, 16) + ":" + player.getUUID();
        TransferResult result = EconomyApi.fundedSystemReward(player, SystemAccount.OPERATIONS, amount, key);
        if (result.status() == TransferResult.Status.COMMITTED || result.status() == TransferResult.Status.REPLAYED) {
            if (notify) player.displayClientMessage(Component.translatable("jem.questbook.coin_reward.paid", amount), false);
            return;
        }
        reopen(player, origin, result.status().name().toLowerCase(java.util.Locale.ROOT));
    }

    private void reopen(ServerPlayer player, TeamData origin, String reason) {
        origin.resetReward(player.getUUID(), this);
        if (origin.getFile() instanceof dev.ftb.mods.ftbquests.quest.ServerQuestFile file) file.saveNow();
        player.displayClientMessage(Component.translatable("jem.questbook.coin_reward.failed", reason), false);
    }
}
