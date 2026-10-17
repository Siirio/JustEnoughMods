package com.siirio.jemprofessions.sandbox.core;

import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.SourceKind;
import com.siirio.jemprofessions.sandbox.SandboxProfessionSupport;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

final class CoreFarming {
    private static final Map<UUID, Harvest> HARVESTS = new HashMap<>();
    private static final Map<UUID, Herd> HERDS = new HashMap<>();
    private static boolean EXTRA_GROWTH;
    private static long growthTick = Long.MIN_VALUE;
    private static int growthAttempts;
    private CoreFarming() { }
    static boolean activate(ServerPlayer player, String node, CompoundTag input) {
        switch (node) {
            case "crop_sense" -> sense(player);
            case "sweep", "harvest_master" -> harvest(player, node);
            case "herd_call" -> call(player);
            case "feed_herd", "quick_shear" -> handleAnimals(player, node);
            case "greenhouse_master", "greenhouse_keeper", "greenhouse_abundance" -> greenhouse(player, input);
            default -> { return false; }
        }
        return true;
    }
    static void sense(ServerPlayer player) {
        if (!player.getMainHandItem().is(ItemTags.HOES)) { SandboxCoreEffects.tell(player, "Возьми мотыгу в основную руку.", "Hold a hoe in your main hand."); return; }
        if (!SandboxProfessionSupport.cooldown(player, "farmer:sense", 40)) return;
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(player.blockPosition().offset(-12, -4, -12), player.blockPosition().offset(12, 4, 12))) {
            if (pos.distToCenterSqr(player.position()) > 144 || !player.serverLevel().hasChunkAt(pos)) continue;
            BlockState state = player.serverLevel().getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                count++; if (count <= 128) player.serverLevel().sendParticles(player, ParticleTypes.HAPPY_VILLAGER, false, pos.getX() + .5, pos.getY() + .8, pos.getZ() + .5, 1, .05, .05, .05, 0);
            }
        }
        SandboxCoreEffects.tell(player, "Спелых растений поблизости: " + count, "Mature crops nearby: " + count);
    }
    private static boolean harvestEnabled(ServerPlayer player) { return SandboxCoreEffects.has(player, ProfessionId.FARMER, "sweep") || SandboxCoreEffects.has(player, ProfessionId.FARMER, "harvest_master"); }
    private static void harvest(ServerPlayer player, String node) {
        if (!player.getMainHandItem().is(ItemTags.HOES)) { SandboxCoreEffects.tell(player, "Возьми мотыгу в основную руку.", "Hold a hoe in your main hand."); return; }
        if (HARVESTS.containsKey(player.getUUID())) { SandboxCoreEffects.tell(player, "Сбор уже идёт.", "Harvest is already running."); return; }
        var hit = CoreActions.ray(player, 6); if (hit.getType() != HitResult.Type.BLOCK) return;
        BlockState first = player.serverLevel().getBlockState(hit.getBlockPos());
        if (!(first.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(first)) { SandboxCoreEffects.tell(player, "Выбери спелое растение.", "Aim at a mature crop."); return; }
        int limit = SandboxCoreEffects.has(player, ProfessionId.FARMER, "harvest_master") ? 1024 : 64;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(), eligible = new ArrayDeque<>(); Set<BlockPos> seen = new HashSet<>();
        queue.add(hit.getBlockPos()); seen.add(hit.getBlockPos());
        while (!queue.isEmpty() && eligible.size() < limit && seen.size() < 8192) {
            BlockPos pos = queue.removeFirst(); if (!player.serverLevel().hasChunkAt(pos)) continue;
            BlockState state = player.serverLevel().getBlockState(pos);
            if (!(state.getBlock() instanceof CropBlock candidate) || !candidate.isMaxAge(state) || !CoreActions.permitted(player, pos, player.getMainHandItem())) continue;
            eligible.add(pos.immutable());
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos next = pos.offset(x, 0, z); if (next.distSqr(hit.getBlockPos()) <= 1024 && seen.add(next)) queue.add(next);
            }
        }
        HARVESTS.put(player.getUUID(), new Harvest(SandboxCoreEffects.dimension(player), eligible, node, limit > 64 ? "harvest_master" : node));
        SandboxCoreEffects.tell(player, "Начат сбор: до " + eligible.size() + " растений.", "Harvest started: up to " + eligible.size() + " crops.");
    }
    static void tick(ServerPlayer player) {
        Harvest harvest = HARVESTS.get(player.getUUID());
        if (harvest != null) {
            if (!SandboxCoreEffects.eligible(player) || !SandboxCoreEffects.has(player, ProfessionId.FARMER, harvest.node)
                    || !SandboxCoreEffects.has(player, ProfessionId.FARMER, harvest.capacityNode)
                    || !harvest.dimension.equals(SandboxCoreEffects.dimension(player)) || !player.getMainHandItem().is(ItemTags.HOES)) HARVESTS.remove(player.getUUID());
            else {
                for (int i = 0; i < 64 && !harvest.positions.isEmpty(); i++) {
                    BlockPos pos = harvest.positions.removeFirst(); if (!player.serverLevel().hasChunkAt(pos) || pos.distToCenterSqr(player.position()) > 1600 || !CoreActions.permitted(player, pos, player.getMainHandItem())) continue;
                    BlockState state = player.serverLevel().getBlockState(pos);
                    if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) continue;
                    ItemStack seed = crop.getCloneItemStack(player.serverLevel(), pos, state);
                    if (!player.gameMode.destroyBlock(pos)) continue;
                    harvest.count++;
                    int slot = CoreActions.slot(player, stack -> !stack.isEmpty() && stack.is(seed.getItem()) && CoreActions.safeBlock(stack));
                    if (slot >= 0) CoreActions.place(player, List.of(new CoreActions.Placement(pos, Direction.UP, slot)));
                }
                if (harvest.positions.isEmpty()) { HARVESTS.remove(player.getUUID()); SandboxCoreEffects.tell(player, "Собрано растений: " + harvest.count, "Harvested crops: " + harvest.count); }
            }
        }
        Herd herd = HERDS.get(player.getUUID());
        if (herd != null && (!player.isAlive() || player.tickCount >= herd.until || !SandboxCoreEffects.has(player, ProfessionId.FARMER, "herd_call") || !herd.dimension.equals(SandboxCoreEffects.dimension(player)))) stopHerd(player.getUUID());
        else if (herd != null && player.tickCount % 10 == 0) for (UUID id : herd.animals) {
            var entity = player.serverLevel().getEntity(id);
            if (entity instanceof Animal animal && suitable(player, animal) && animal.distanceToSqr(player) <= 400
                    && animal.isFood(player.getMainHandItem()) && animal.distanceToSqr(player) > 4) {
                animal.getNavigation().moveTo(player, 1.0); herd.paths.put(id, animal.getNavigation().getPath());
            } else if (entity instanceof Animal animal) stopAnimal(herd, animal);
        }
    }
    private static boolean suitable(ServerPlayer player, Animal animal) {
        return animal.isAlive() && !animal.isLeashed() && animal.getTarget() == null
                && (!(animal instanceof TamableAnimal tame) || !tame.isTame() || tame.isOwnedBy(player));
    }
    private static List<Animal> animals(ServerPlayer player) {
        List<Animal> animals = player.serverLevel().getEntitiesOfClass(Animal.class, player.getBoundingBox().inflate(12), animal -> suitable(player, animal) && animal.distanceToSqr(player) <= 144 && player.hasLineOfSight(animal));
        animals.sort(Comparator.comparingDouble(player::distanceToSqr)); return animals;
    }
    private static void call(ServerPlayer player) {
        List<UUID> animals = animals(player).stream().filter(animal -> animal.isFood(player.getMainHandItem())).limit(8).map(Animal::getUUID).toList();
        if (animals.isEmpty()) { SandboxCoreEffects.tell(player, "Держи подходящий корм рядом с животными.", "Hold suitable food near animals."); return; }
        stopHerd(player.getUUID()); HERDS.put(player.getUUID(), new Herd(player.serverLevel(), SandboxCoreEffects.dimension(player), animals, player.tickCount + 200));
        SandboxCoreEffects.tell(player, "Созвано животных: " + animals.size(), "Called animals: " + animals.size());
    }
    private static void handleAnimals(ServerPlayer player, String node) {
        int handled = 0;
        for (Animal animal : animals(player)) {
            if (handled >= 8 || !SandboxCoreEffects.has(player, ProfessionId.FARMER, node)) break;
            boolean shear = node.equals("quick_shear");
            if (shear ? !(animal instanceof Sheep sheep && sheep.readyForShearing()) : animal.isBaby() || !animal.canFallInLove()) continue;
            if (!CoreActions.permitted(player, animal.blockPosition(), player.getMainHandItem())) continue;
            int slot = CoreActions.slot(player, stack -> !stack.isEmpty() && (shear ? stack.is(Items.SHEARS) : animal.isFood(stack)));
            if (slot < 0) continue;
            int selected = player.getInventory().selected; ItemStack original = player.getMainHandItem();
            if (slot != selected) { player.setItemInHand(InteractionHand.MAIN_HAND, player.getInventory().getItem(slot)); player.getInventory().setItem(slot, original); }
            boolean before = shear ? ((Sheep) animal).isSheared() : animal.isInLove();
            try { player.interactOn(animal, InteractionHand.MAIN_HAND); }
            finally {
                if (slot != selected) { ItemStack remainder = player.getMainHandItem(); player.setItemInHand(InteractionHand.MAIN_HAND, player.getInventory().getItem(slot)); player.getInventory().setItem(slot, remainder); }
            }
            boolean after = shear ? ((Sheep) animal).isSheared() : animal.isInLove(); if (!before && after) handled++;
        }
        player.containerMenu.broadcastChanges();
        SandboxCoreEffects.tell(player, (node.equals("quick_shear") ? "Подстрижено овец: " : "Покормлено взрослых животных: ") + handled,
                (node.equals("quick_shear") ? "Sheared sheep: " : "Fed adult animals: ") + handled);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void bred(BabyEntitySpawnEvent event) {
        if (!(event.getCausedByPlayer() instanceof ServerPlayer player) || !SandboxCoreEffects.eligible(player)) return;
        SandboxCoreEffects.defer(() -> {
            var child = event.getChild();
            if (!event.isCanceled() && child != null && child.isAlive() && child.isAddedToWorld()) SandboxCoreEffects.credit(player, ProfessionId.FARMER, SourceKind.CROP_HARVEST,
                    BuiltInRegistries.ENTITY_TYPE.getKey(child.getType()), 6, "farmer:animals", 200, "breed:" + child.getUUID());
        });
    }
    public static void sheared(ServerPlayer player, Sheep sheep) {
        if (SandboxCoreEffects.eligible(player)) SandboxCoreEffects.credit(player, ProfessionId.FARMER, SourceKind.CROP_HARVEST,
                BuiltInRegistries.ENTITY_TYPE.getKey(sheep.getType()), 2, "farmer:animals", 200, "shear:" + sheep.getUUID() + ":" + sheep.level().getGameTime());
    }
    private static void greenhouse(ServerPlayer player, CompoundTag input) {
        var hit = CoreActions.ray(player, 6); if (hit.getType() != HitResult.Type.BLOCK || !player.serverLevel().getBlockState(hit.getBlockPos()).is(Blocks.COMPOSTER)) {
            SandboxCoreEffects.tell(player, "Выбери компостер в центре стеклянной теплицы 9×9.", "Aim at the composter at the center of a 9×9 glass greenhouse."); return;
        }
        BlockPos pos = hit.getBlockPos(); if (!CoreActions.permitted(player, pos, player.getMainHandItem())) return;
        var data = SandboxCoreData.get(player.getServer()); String key = SandboxCoreEffects.position(player, pos); var house = data.greenhouses.get(key);
        if (house != null && !house.owner.equals(player.getUUID())) { SandboxCoreEffects.tell(player, "Этот контроллер принадлежит другому игроку.", "This controller belongs to another player."); return; }
        if (input.getString("Mode").equals("remove")) { data.greenhouses.remove(key); data.setDirty(); SandboxCoreEffects.tell(player, "Контроллер отключён.", "Controller disabled."); return; }
        if (house == null) {
            if (data.greenhouses.size() >= 128 || data.greenhouses.values().stream().anyMatch(other -> other.owner.equals(player.getUUID()))) {
                SandboxCoreEffects.tell(player, "Доступна одна теплица. Сначала отключи прежний контроллер.", "One greenhouse is available. Disable the previous controller first."); return;
            }
            house = new SandboxCoreData.Greenhouse(player.getUUID(), SandboxCoreEffects.dimension(player), pos); data.greenhouses.put(key, house); data.setDirty();
        }
        house.enclosed = enclosure(player.serverLevel(), house); house.checked = player.serverLevel().getGameTime();
        if (input.getString("Mode").equals("fuel")) {
            int requested = Math.max(1, Math.min(64, input.getInt("Value"))); int used = 0;
            for (int i = 0; i < player.getInventory().items.size() && used < requested && house.fuel <= 4032; i++) {
                ItemStack stack = player.getInventory().getItem(i); if (!stack.is(Items.BONE_MEAL)) continue;
                int count = Math.min(stack.getCount(), Math.min(requested - used, (4096 - house.fuel) / 64)); stack.shrink(count); used += count; house.fuel += count * 64;
            }
            data.setDirty(); player.containerMenu.broadcastChanges();
        }
        SandboxCoreEffects.tell(player, "Теплица: " + (house.enclosed ? "стеклянное ограждение готово" : "нужны стеклянные стены и крыша") + ". Запас попыток: " + house.fuel,
                "Greenhouse: " + (house.enclosed ? "enclosure ready" : "glass walls and roof required") + ". Remaining attempts: " + house.fuel);
        player.serverLevel().sendParticles(player, house.enclosed ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.SMOKE, false, pos.getX() + .5, pos.getY() + 1.1, pos.getZ() + .5, 12, .3, .3, .3, .01);
    }
    private static boolean glass(BlockState state) {
        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key.getNamespace().equals("minecraft") && (key.getPath().equals("glass") || key.getPath().equals("tinted_glass") || key.getPath().endsWith("_stained_glass") || key.getPath().equals("glass_pane") || key.getPath().endsWith("_stained_glass_pane"));
    }
    private static boolean enclosure(ServerLevel level, SandboxCoreData.Greenhouse house) {
        if (!level.hasChunkAt(house.pos) || !level.getBlockState(house.pos).is(Blocks.COMPOSTER)) return false;
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = 1; y <= 4; y++) {
            if (y != 4 && Math.abs(x) != 4 && Math.abs(z) != 4) continue;
            BlockPos part = house.pos.offset(x, y, z); if (!level.hasChunkAt(part) || !glass(level.getBlockState(part))) return false;
        }
        return true;
    }
    public static void growth(ServerLevel level, BlockPos pos, RandomSource random) {
        if (EXTRA_GROWTH || !level.getServer().isSingleplayer()) return;
        if (growthTick != level.getGameTime()) { growthTick = level.getGameTime(); growthAttempts = 0; }
        if (growthAttempts >= 64) return;
        var data = SandboxCoreData.get(level.getServer());
        for (var house : data.greenhouses.values()) {
            if (house.fuel <= 0 || !house.dimension.equals(level.dimension().location().toString()) || pos.getY() != house.pos.getY() + 1
                    || Math.abs(pos.getX() - house.pos.getX()) > 3 || Math.abs(pos.getZ() - house.pos.getZ()) > 3) continue;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(house.owner);
            // Cross-dimension owners pause the bonus without spending fuel: permissions below
            // are evaluated by CoreActions in owner.serverLevel(), which must be this crop's level.
            if (owner == null || owner.serverLevel() != level || !SandboxCoreEffects.eligible(owner)) continue;
            float rate = SandboxCoreEffects.has(owner, ProfessionId.FARMER, "greenhouse_abundance") ? .20F : SandboxCoreEffects.has(owner, ProfessionId.FARMER, "greenhouse_keeper") ? .15F : SandboxCoreEffects.has(owner, ProfessionId.FARMER, "greenhouse_master") ? .10F : 0;
            if (rate == 0 || random.nextFloat() >= rate || !CoreActions.permitted(owner, pos, owner.getMainHandItem())) continue;
            house.enclosed = enclosure(level, house); house.checked = level.getGameTime();
            if (!house.enclosed) continue;
            BlockState state = level.getBlockState(pos); if (!(state.getBlock() instanceof CropBlock crop) || crop.isMaxAge(state)) continue;
            house.fuel--; growthAttempts++; data.setDirty(); EXTRA_GROWTH = true;
            try { state.randomTick(level, pos, random); } finally { EXTRA_GROWTH = false; }
            break; // overlapping controllers never stack.
        }
    }
    private static void stopAnimal(Herd herd, Animal animal) {
        var path = herd.paths.remove(animal.getUUID());
        if (path != null && animal.getNavigation().getPath() == path) animal.getNavigation().stop();
    }
    private static void stopHerd(UUID id) {
        Herd herd = HERDS.remove(id); if (herd == null) return;
        for (UUID animalId : herd.animals) if (herd.level.getEntity(animalId) instanceof Animal animal) stopAnimal(herd, animal);
    }
    static void logout(UUID id) { HARVESTS.remove(id); stopHerd(id); }
    static void clear() { HARVESTS.clear(); for (UUID id : List.copyOf(HERDS.keySet())) stopHerd(id); EXTRA_GROWTH = false; growthTick = Long.MIN_VALUE; growthAttempts = 0; }
    private static final class Harvest {
        final String dimension, node, capacityNode; final ArrayDeque<BlockPos> positions; int count;
        Harvest(String dimension, ArrayDeque<BlockPos> positions, String node, String capacityNode) { this.dimension = dimension; this.positions = positions; this.node = node; this.capacityNode = capacityNode; }
    }
    private static final class Herd {
        final ServerLevel level; final String dimension; final List<UUID> animals; final int until;
        final Map<UUID, net.minecraft.world.level.pathfinder.Path> paths = new HashMap<>();
        Herd(ServerLevel level, String dimension, List<UUID> animals, int until) { this.level = level; this.dimension = dimension; this.animals = animals; this.until = until; }
    }
}
