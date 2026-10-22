package com.justenoughmods.achievementguide.compat.ftb;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import dev.ftb.mods.ftbquests.quest.reward.XPReward;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** XP points keep the native presentation and a player-persistent cross-team receipt. */
public final class BookXpReward extends XPReward {
    static final String RECEIPTS = "jem_quest_xp_receipts";
    private static final RewardType TYPE = RewardTypes.register(new ResourceLocation("jem_advancements", "quest_xp"),
            BookXpReward::new, () -> Icon.getIcon("minecraft:item/experience_bottle"));
    public BookXpReward(long id, Quest quest) { super(id, quest); }
    static void register() { }
    @Override public RewardType getType() { return TYPE; }
    @Override public void claim(ServerPlayer player, boolean notify) {
        CompoundTag receipts = player.getPersistentData().getCompound(RECEIPTS);
        String key = Long.toHexString(id);
        if (receipts.getBoolean(key)) return;
        super.claim(player, notify);
        receipts.putBoolean(key, true);
        player.getPersistentData().put(RECEIPTS, receipts);
    }
}
