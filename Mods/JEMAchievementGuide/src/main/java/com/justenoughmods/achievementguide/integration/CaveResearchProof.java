package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Completed native research withdrawals only, bounded to sixteen biome identities. */
public final class CaveResearchProof {
    public interface CraftDebitWitness {
        void jem$caveMapDebit(net.minecraft.world.inventory.CraftingContainer matrix, int amount, ItemStack removed);
    }
    private static final String KEY = "JemQuestCaveResearchV4";
    private CaveResearchProof() {}

    public static String biome(ItemStack stack, String item) {
        if (stack.isEmpty() || !item.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) return "";
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("CaveBiome", Tag.TAG_STRING)) return "";
        String name = tag.getString("CaveBiome");
        return name.length() <= 256 && ResourceLocation.tryParse(name) != null ? name : "";
    }

    public static void withdrewCodex(ServerPlayer actor, ItemStack result) {
        if (!real(actor)) return;
        String biome = biome(result, "alexscaves:cave_codex");
        if (biome.isEmpty()) return;
        CompoundTag persisted = actor.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag stages = persisted.getCompound(KEY);
        if (stages.contains(biome) || stages.size() < 16) stages.putBoolean(biome, true);
        persisted.put(KEY, stages);
        actor.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static void craftedMap(ServerPlayer actor, String biome) {
        if (!real(actor) || biome.isEmpty()) return;
        CompoundTag persisted = actor.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag stages = persisted.getCompound(KEY);
        if (stages.getBoolean(biome)) {
            stages.remove(biome);
            persisted.put(KEY, stages);
            actor.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
            QuestActionTrigger.INSTANCE.emit(actor, "v3_cave_research");
        }
    }

    private static boolean real(ServerPlayer actor) {
        return !actor.getAbilities().instabuild && !actor.isSpectator()
                && !(actor instanceof net.minecraftforge.common.util.FakePlayer);
    }
}
