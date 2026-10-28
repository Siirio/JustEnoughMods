package com.justenoughmods.achievementguide.integration.spawncare;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;
public final class SpawnCareQuestProof {
    public static final String KEY = "JemSpawnCareQuestV4";
    public record Interaction(ServerPlayer actor, LivingEntity entity, InteractionHand hand, ItemStack input, List<ItemStack> inventory, float health, boolean owned) {}
    private SpawnCareQuestProof() {}
    public static ServerPlayer real(Player player) { return player instanceof ServerPlayer actor && com.justenoughmods.achievementguide.integration.travel.TravelProof.eligible(actor) && actor.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SURVIVAL ? actor : null; }
    public static String item(ItemStack stack) { return String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())); }
    public static String type(Entity entity) { return String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())); }
    public static CompoundTag data(Entity entity) { var root = entity.getPersistentData(); if (!root.contains(KEY, 10)) root.put(KEY, new CompoundTag()); return root.getCompound(KEY); }
    public static void mark(Entity entity, String stage, UUID actor) { if (entity.level() instanceof ServerLevel && actor != null) data(entity).putUUID(stage, actor); }
    public static ServerPlayer actor(Entity entity, String stage) { if (!(entity.level() instanceof ServerLevel level) || !data(entity).hasUUID(stage)) return null; return real(level.getServer().getPlayerList().getPlayer(data(entity).getUUID(stage))); }
    public static List<ItemStack> held(ServerPlayer actor) { List<ItemStack> list = new ArrayList<>(); for (int i = 0; i < actor.getInventory().getContainerSize(); i++) list.add(actor.getInventory().getItem(i)); list.add(actor.containerMenu.getCarried()); return list; }
    public static int count(List<ItemStack> list, ItemStack variant) { return list.stream().filter(s -> ItemStack.isSameItemSameTags(s, variant)).mapToInt(ItemStack::getCount).sum(); }
    public static int countItem(List<ItemStack> list, net.minecraft.world.item.Item item) { return list.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum(); }
    public static Interaction begin(LivingEntity entity, Player player, InteractionHand hand) { var actor = real(player); return actor == null ? null : new Interaction(actor, entity, hand, player.getItemInHand(hand).copy(), held(actor).stream().map(ItemStack::copy).toList(), entity.getHealth(), entity instanceof TamableAnimal tame && tame.isOwnedBy(actor)); }
    public static boolean debited(ItemStack before, ItemStack after, int count) { return count > 0 && before.getCount() - after.getCount() == count && (after.isEmpty() || ItemStack.isSameItemSameTags(before, after)); }
    public static void emit(ServerPlayer actor, String action) { QuestActionTrigger.INSTANCE.emit(actor, action); }
    public static void fed(Interaction run) { if (run != null) { emit(run.actor, "v3_pet_supper"); emit(run.actor, "v3_pet_guardian_vest_feed"); } }
}
