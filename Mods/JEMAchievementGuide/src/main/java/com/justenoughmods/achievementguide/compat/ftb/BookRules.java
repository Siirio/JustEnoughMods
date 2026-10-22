package com.justenoughmods.achievementguide.compat.ftb;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.CustomTask;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Authored predicates, indexed by event and registry target. No world or inventory tick loop. */
final class BookRules {
    record Match(List<String> ids, String tag, String category, Map<String, Integer> enchantments,
                 String potion, String cave, Boolean dataRequired, Boolean baby, String topic, String proof) {
        boolean item(ItemStack stack) {
            if (stack.isEmpty()) return false;
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!ids.isEmpty() && !ids.contains(key.toString())) return false;
            if (tag != null && !stack.is(TagKey.create(Registries.ITEM, new ResourceLocation(tag)))) return false;
            if (category != null && !switch (category) {
                case "pickaxe" -> stack.getItem() instanceof net.minecraft.world.item.PickaxeItem;
                case "sword" -> stack.getItem() instanceof net.minecraft.world.item.SwordItem;
                case "bow" -> stack.getItem() instanceof net.minecraft.world.item.BowItem;
                case "crossbow" -> stack.getItem() instanceof net.minecraft.world.item.CrossbowItem;
                case "chestplate" -> stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor
                        && armor.getType() == net.minecraft.world.item.ArmorItem.Type.CHESTPLATE;
                case "food" -> stack.isEdible();
                case "block" -> stack.getItem() instanceof net.minecraft.world.item.BlockItem;
                default -> throw new IllegalStateException("Unknown quest item category: " + category);
            }) return false;
            if (potion != null && (!BuiltInRegistries.POTION.containsKey(new ResourceLocation(potion))
                    || !net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack)
                    .equals(BuiltInRegistries.POTION.get(new ResourceLocation(potion))))) return false;
            if (enchantments != null) for (var entry : enchantments.entrySet()) {
                var enchantment = BuiltInRegistries.ENCHANTMENT.get(new ResourceLocation(entry.getKey()));
                if (enchantment == null || EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) < entry.getValue()) return false;
            }
            if (cave != null && (stack.getTag() == null || !cave.equals(stack.getTag().getString("CaveBiome")))) return false;
            if (topic != null && !BookStackProofs.topic(stack, topic)) return false;
            if (proof != null && !BookStackProofs.valid(stack, proof)) return false;
            return dataRequired == null || !dataRequired || stack.hasTag() && !stack.getTag().isEmpty();
        }

        boolean entity(Entity entity) {
            if (entity == null) return false;
            if (!ids.isEmpty() && !ids.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString())) return false;
            if (baby != null && (!(entity instanceof LivingEntity living) || living.isBaby() != baby)) return false;
            if ("hostile".equals(category)) return entity instanceof Enemy;
            if ("living".equals(category)) return entity instanceof LivingEntity;
            return true;
        }

        boolean block(BlockState state) {
            return (ids.isEmpty() || ids.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()))
                    && (tag == null || state.is(TagKey.create(Registries.BLOCK, new ResourceLocation(tag))))
                    && (!"mature_crop".equals(category) || state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state));
        }
    }

    record Rule(String id, String quest, String kind, List<Match> targets, int count, int distinct,
                List<Match> held, List<Match> mainHand, Map<String, List<Match>> heldByTarget,
                boolean offHand, boolean emptyHand, boolean sneaking, boolean hero,
                String profession, String signal, String argument, boolean dedicated,
                String equipmentSlot, boolean directMelee, boolean noPlayers, boolean notSneaking, String race,
                List<Match> secondHand, boolean distinctById, boolean nativeFood) {}
    private static final Map<Long, Rule> RULES = load();
    private static final Map<String, Map<String, List<Rule>>> INDEX = index();

    private BookRules() {}

    private static Map<Long, Rule> load() {
        try (var stream = BookRules.class.getResourceAsStream("/data/jem_advancements/questbook/rules.json")) {
            if (stream == null) throw new IllegalStateException("Missing authored quest predicates");
            var result = new LinkedHashMap<Long, Rule>();
            Gson gson = new Gson();
            for (JsonElement entry : JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray()) {
                Rule rule = gson.fromJson(entry, Rule.class);
                if (result.put(Long.parseUnsignedLong(rule.id, 16), rule) != null) throw new IllegalStateException("Duplicate quest task: " + rule.id);
            }
            return Map.copyOf(result);
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot read authored quest predicates", failure); }
    }

    private static Map<String, Map<String, List<Rule>>> index() {
        var result = new HashMap<String, Map<String, List<Rule>>>();
        for (Rule rule : RULES.values()) {
            var targets = result.computeIfAbsent(rule.kind, key -> new HashMap<>());
            var keys = new java.util.HashSet<String>();
            for (Match target : rule.targets) {
                if (target.ids.isEmpty()) keys.add("*");
                else keys.addAll(target.ids);
            }
            for (String key : keys) targets.computeIfAbsent(key, ignored -> new ArrayList<>()).add(rule);
        }
        return result;
    }

    static boolean bind(CustomTask task) {
        Rule rule = RULES.get(task.id);
        if (rule == null) return false;
        task.setEnableButton(false);
        task.setMaxProgress(rule.distinct > 0 ? rule.distinct : rule.count);
        boolean checked = rule.kind.equals("inventory") || rule.kind.equals("equip") || rule.kind.equals("native");
        task.setCheckTimer(checked ? 20 : 0);
        if (checked) task.setCheck((data, player) -> {
            if (!FtbQuestProjection.active(task.getQuestFile(), task.getQuest().id)) return;
            if (data.getProgress() >= task.getMaxProgress()) return;
            if (rule.kind.equals("native")) {
                long progress = BookSignals.progress(player, rule.signal, rule.argument);
                if (progress > data.getProgress()) data.setProgress(Math.min(task.getMaxProgress(), progress));
                return;
            }
            List<ItemStack> items = new ArrayList<>();
            if (rule.kind.equals("equip")) {
                if ("offhand".equals(rule.equipmentSlot)) items.add(player.getOffhandItem());
                else player.getArmorSlots().forEach(items::add);
            }
            else {
                items.addAll(player.getInventory().items);
                items.addAll(player.getInventory().armor);
                items.addAll(player.getInventory().offhand);
            }
            long count = 0;
            for (int group = 0; group < rule.targets.size(); group++) {
                Match match = rule.targets.get(group);
                long matching = items.stream().filter(match::item).mapToLong(ItemStack::getCount).sum();
                if (rule.distinct > 0) {
                    if (matching > 0) {
                        if (rule.distinctById) {
                            for (ItemStack stack : items) if (match.item(stack)) count = Math.max(count,
                                    BookCollections.get(player.server).observe(dev.ftb.mods.ftbquests.quest.TeamData.get(player).getTeamId(), task.id,
                                            BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
                        } else count = Math.max(count, BookCollections.get(player.server).observe(
                                dev.ftb.mods.ftbquests.quest.TeamData.get(player).getTeamId(), task.id, Integer.toString(group)));
                    }
                } else count += matching;
            }
            if (count > data.getProgress()) data.setProgress(Math.min(task.getMaxProgress(), count));
        });
        return true;
    }

    static String unavailable(long task) {
        Rule rule = RULES.get(task);
        if (rule == null || rule.kind.equals("native") || rule.kind.equals("operation") || rule.kind.equals("advancement") || rule.kind.equals("dimension")) return "";
        int availableGroups = 0;
        boolean openGroup = false;
        var availableIds = new java.util.HashSet<String>();
        for (Match match : rule.targets) {
            boolean found = match.ids.isEmpty();
            openGroup |= found;
            if (!match.ids.isEmpty()) {
                for (String key : match.ids) {
                    ResourceLocation id = new ResourceLocation(key);
                    boolean registered = switch (rule.kind) {
                        case "place", "break", "use_block" -> BuiltInRegistries.BLOCK.containsKey(id);
                        case "interact_entity", "mount", "hit", "kill", "guard_hit" -> BuiltInRegistries.ENTITY_TYPE.containsKey(id);
                        default -> BuiltInRegistries.ITEM.containsKey(id);
                    };
                    if (registered) { found = true; availableIds.add(key); }
                }
            }
            if (found) availableGroups++;
            if (match.enchantments != null) for (String id : match.enchantments.keySet())
                if (!BuiltInRegistries.ENCHANTMENT.containsKey(new ResourceLocation(id))) return "Недоступно зачарование " + id;
            if (match.potion != null && !BuiltInRegistries.POTION.containsKey(new ResourceLocation(match.potion))) return "Недоступно зелье " + match.potion;
        }
        int needed = Math.max(1, rule.distinct);
        if (rule.distinctById ? !openGroup && availableIds.size() < needed : availableGroups < needed)
            return "В этом мире нет достаточного числа целей для указанного действия или коллекции";
        for (List<Match> equipment : List.of(rule.held == null ? List.<Match>of() : rule.held,
                rule.mainHand == null ? List.<Match>of() : rule.mainHand, rule.secondHand == null ? List.<Match>of() : rule.secondHand)) {
            if (!equipment.isEmpty() && equipment.stream().noneMatch(match -> match.ids.isEmpty()
                    || match.ids.stream().anyMatch(id -> BuiltInRegistries.ITEM.containsKey(new ResourceLocation(id)))))
                return "Указанный предмет для действия отсутствует в этом мире";
        }
        return "";
    }

    static void operation(MinecraftServer server, java.util.UUID player, String kind, ItemStack delivery) {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null || file.server != server || file.isLoading()) return;
        var team = dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api().getManager().getTeamForPlayerID(player);
        if (team.isEmpty()) return;
        TeamData data = file.getOrCreateTeamData(team.get());
        for (Rule rule : INDEX.getOrDefault("operation", Map.of()).getOrDefault(kind, List.of())) {
            var task = file.getTask(Long.parseUnsignedLong(rule.id, 16));
            if (task == null || !FtbQuestProjection.active(file, task.getQuest().id)
                    || !data.canStartTasks(task.getQuest()) || data.isCompleted(task)) continue;
            if ("block".equals(rule.argument) && !(delivery.getItem() instanceof net.minecraft.world.item.BlockItem)) continue;
            // All operation objectives are one-shot flags, so native replay/recovery cannot increment a counter.
            data.markTaskCompleted(task);
        }
    }

    static void event(ServerPlayer player, String kind, String key, ItemStack item, Entity entity,
                      BlockState block, boolean offHand) {
        event(player, kind, key, item, entity, block, offHand, false);
    }

    static void event(ServerPlayer player, String kind, String key, ItemStack item, Entity entity,
                      BlockState block, boolean offHand, boolean directMelee) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer) return;
        var targets = INDEX.get(kind);
        if (targets == null) return;
        List<Rule> candidates = new ArrayList<>(targets.getOrDefault(key, List.of()));
        for (Rule rule : targets.getOrDefault("*", List.of())) if (!candidates.contains(rule)) candidates.add(rule);
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null || file.server != player.server || file.isLoading()) return;
        TeamData data = file.getOrCreateTeamData(player);
        for (Rule rule : candidates) {
            if (rule.offHand != offHand || rule.dedicated && !player.server.isDedicatedServer()) continue;
            if (rule.directMelee && !directMelee || rule.noPlayers && entity instanceof net.minecraft.world.entity.player.Player
                    || rule.notSneaking && player.isShiftKeyDown()
                    || rule.race != null && !rule.race.equals(QuestSignals.race(player))) continue;
            var task = file.getTask(Long.parseUnsignedLong(rule.id, 16));
            if (task == null || !FtbQuestProjection.active(file, task.getQuest().id)
                    || !data.canStartTasks(task.getQuest()) || data.isCompleted(task)) continue;
            ItemStack active = offHand ? player.getOffhandItem() : player.getMainHandItem();
            if (rule.emptyHand && !active.isEmpty() || rule.sneaking && !player.isShiftKeyDown()
                    || rule.hero && !player.hasEffect(net.minecraft.world.effect.MobEffects.HERO_OF_THE_VILLAGE)) continue;
            if (rule.held != null && !rule.held.isEmpty() && rule.held.stream().noneMatch(match -> match.item(active))) continue;
            if (rule.nativeFood && (!(entity instanceof net.minecraft.world.entity.animal.Animal animal) || !animal.isFood(active))) continue;
            if (rule.mainHand != null && !rule.mainHand.isEmpty() && rule.mainHand.stream().noneMatch(match -> match.item(player.getMainHandItem()))) continue;
            if (rule.secondHand != null && !rule.secondHand.isEmpty() && rule.secondHand.stream().noneMatch(match -> match.item(player.getOffhandItem()))) continue;
            if (rule.profession != null && (!(entity instanceof net.minecraft.world.entity.npc.Villager villager)
                    || !java.util.Arrays.asList(rule.profession.split(",")).contains(BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).toString()))) continue;
            if (rule.heldByTarget != null && rule.heldByTarget.containsKey(key)
                    && rule.heldByTarget.get(key).stream().noneMatch(match -> match.item(active))) continue;
            for (int group = 0; group < rule.targets.size(); group++) {
                Match match = rule.targets.get(group);
                boolean matches = block != null ? match.block(block) : entity != null ? match.entity(entity)
                        : item != null ? match.item(item) : match.ids.contains(key);
                if (!matches) continue;
                if (rule.distinct > 0) data.setProgress(task, Math.min(rule.distinct,
                        BookCollections.get(player.server).observe(data.getTeamId(), task.id, rule.distinctById ? key : Integer.toString(group))));
                else data.addProgress(task, 1L);
                break;
            }
        }
    }

}
