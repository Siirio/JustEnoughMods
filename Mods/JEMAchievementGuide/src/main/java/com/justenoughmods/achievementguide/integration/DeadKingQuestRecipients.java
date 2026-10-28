package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.JemAdvancementGuide;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Personal pending delivery of a canonical hosted success, including offline participants. */
@Mod.EventBusSubscriber(modid = JemAdvancementGuide.MOD_ID)
public final class DeadKingQuestRecipients extends SavedData {
    private static final String KEY = "jem_guide_dead_king_recipients_v4";
    private final Set<UUID> pending = new HashSet<>();

    private static DeadKingQuestRecipients get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DeadKingQuestRecipients::load,
                DeadKingQuestRecipients::new, KEY);
    }

    private static DeadKingQuestRecipients load(CompoundTag tag) {
        var result = new DeadKingQuestRecipients();
        for (Tag value : tag.getList("Pending", Tag.TAG_INT_ARRAY)) result.pending.add(NbtUtils.loadUUID(value));
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        var list = new ListTag();
        pending.forEach(id -> list.add(NbtUtils.createUUID(id)));
        tag.put("Pending", list);
        return tag;
    }

    public static void accepted(MinecraftServer server, Set<UUID> participants) {
        var receipt = get(server);
        if (receipt.pending.addAll(participants)) receipt.setDirty();
        for (UUID id : participants) {
            var player = server.getPlayerList().getPlayer(id);
            if (player != null) receipt.deliver(player, false);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) get(player.server).deliver(player, true);
    }

    private void deliver(ServerPlayer player, boolean newlyLoadedPlayer) {
        if (!pending.contains(player.getUUID()) || player.getAbilities().instabuild || player.isSpectator()
                || player instanceof net.minecraftforge.common.util.FakePlayer) return;
        // Retain until the real native listener exists. V4 is deliberately not activated yet.
        var advancement = player.server.getAdvancements().getAdvancement(
                new net.minecraft.resources.ResourceLocation("jem_advancements", "quest_v2/v2_mag_dead_king"));
        if (advancement == null) return;
        if (newlyLoadedPlayer && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            // A completed personal advancement loaded by vanilla acknowledges the pending notification.
            pending.remove(player.getUUID());
            setDirty();
            return;
        }
        QuestActionTrigger.INSTANCE.emit(player, "legitimate_dead_king_victory");
    }
}
