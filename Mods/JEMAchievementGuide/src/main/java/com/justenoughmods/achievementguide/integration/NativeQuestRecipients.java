package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.JemAdvancementGuide;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Pending personal notifications from committed native producers, never world-wide completion history. */
@Mod.EventBusSubscriber(modid = JemAdvancementGuide.MOD_ID)
public final class NativeQuestRecipients extends SavedData {
    private static final String KEY = "jem_guide_native_recipients_v4";
    private record Entry(String action, ResourceLocation advancement) {}
    private final Map<UUID, Set<Entry>> pending = new HashMap<>();

    private static NativeQuestRecipients get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(NativeQuestRecipients::load, NativeQuestRecipients::new, KEY);
    }
    private static NativeQuestRecipients load(CompoundTag tag) {
        var data = new NativeQuestRecipients();
        for (var raw : tag.getList("Rows", 10)) {
            CompoundTag row = (CompoundTag) raw;
            var entry = new Entry(row.getString("action"), new ResourceLocation(row.getString("advancement")));
            data.pending.computeIfAbsent(row.getUUID("actor"), unused -> new HashSet<>()).add(entry);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var rows = new ListTag();
        pending.forEach((actor, entries) -> entries.forEach(entry -> {
            var row = new CompoundTag();
            row.putUUID("actor", actor); row.putString("action", entry.action);
            row.putString("advancement", entry.advancement.toString()); rows.add(row);
        }));
        tag.put("Rows", rows);
        return tag;
    }

    public static void accepted(MinecraftServer server, UUID actor, String action, ResourceLocation advancement) {
        if (!server.isSameThread() || action.isBlank() || !advancement.getNamespace().equals(JemAdvancementGuide.MOD_ID)) {
            throw new IllegalArgumentException("Native quest receipts require the logical server and an exact Guide action");
        }
        var data = get(server);
        if (data.pending.computeIfAbsent(actor, unused -> new HashSet<>()).add(new Entry(action, advancement))) data.setDirty();
        var player = server.getPlayerList().getPlayer(actor);
        if (player != null) data.deliver(player, false);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer actor) get(actor.server).deliver(actor, true);
    }
    private void deliver(ServerPlayer actor, boolean freshlyLoaded) {
        var entries = pending.get(actor.getUUID());
        if (entries == null || actor.isSpectator() || actor.getAbilities().instabuild
                || actor instanceof net.minecraftforge.common.util.FakePlayer) return;
        for (Entry entry : Set.copyOf(entries)) {
            var advancement = actor.server.getAdvancements().getAdvancement(entry.advancement);
            if (advancement == null) continue;
            if (freshlyLoaded && actor.getAdvancements().getOrStartProgress(advancement).isDone()) {
                entries.remove(entry); setDirty();
            } else QuestActionTrigger.INSTANCE.emit(actor, entry.action);
        }
        if (entries.isEmpty()) pending.remove(actor.getUUID());
    }
}
