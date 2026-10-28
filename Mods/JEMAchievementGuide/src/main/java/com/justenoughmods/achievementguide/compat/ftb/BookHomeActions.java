package com.justenoughmods.achievementguide.compat.ftb;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.siirio.mobhome.feature.shelter.CompanionData;
import com.siirio.mobhome.feature.shelter.CompanionHomeEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Only quest stage evidence is retained, native pet identity and admission remain MobHome-owned. */
public final class BookHomeActions {
    private static final String MOVES = "JemQuestHomeMovesV4";
    private BookHomeActions() {}
    static void register() { MinecraftForge.EVENT_BUS.register(BookHomeActions.class); }

    private static boolean eligible(ServerPlayer player) {
        return !(player instanceof FakePlayer) && !player.isSpectator() && !player.getAbilities().instabuild;
    }

    @SubscribeEvent
    public static void committed(CompanionHomeEvent event) {
        ServerPlayer player = event.player();
        CompoundTag moves = player.getPersistentData().getCompound(MOVES);
        String key = event.pet().toString();
        if (!eligible(player)) {
            moves.remove(key);
            player.getPersistentData().put(MOVES, moves);
            return;
        }
        if (!event.bound()) {
            CompoundTag origin = new CompoundTag();
            origin.putString("Dimension", event.dimension());
            origin.putLong("Position", event.home().asLong());
            moves.put(key, origin);
        } else {
            QuestActionTrigger.INSTANCE.emit(player, "shelter_bound");
            if (moves.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                CompoundTag origin = moves.getCompound(key);
                if (!origin.getString("Dimension").equals(event.dimension())
                        || origin.getLong("Position") != event.home().asLong()) {
                    QuestActionTrigger.INSTANCE.emit(player, "v3_pet_moving_day");
                }
                moves.remove(key);
            }
        }
        player.getPersistentData().put(MOVES, moves);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !eligible(player)) return;
        var pets = CompanionData.get(player.serverLevel()).owned(player.getUUID());
        if (pets.stream().anyMatch(pet -> pet.home != null && pet.supported)) {
            QuestActionTrigger.INSTANCE.emit(player, "shelter_bound");
        }
        // Stage keys can only reference the owner's current native journal, never a global pet index.
        var known = pets.stream().map(pet -> pet.id.toString()).collect(java.util.stream.Collectors.toSet());
        CompoundTag moves = player.getPersistentData().getCompound(MOVES);
        for (String key : java.util.Set.copyOf(moves.getAllKeys())) if (!known.contains(key)) moves.remove(key);
        player.getPersistentData().put(MOVES, moves);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        var proof = event.getOriginal().getPersistentData().get(MOVES);
        if (proof != null) event.getEntity().getPersistentData().put(MOVES, proof.copy());
    }
}
