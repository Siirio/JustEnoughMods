package com.siirio.jembuilding.blueprint;

import com.siirio.jembuilding.item.BlueprintRollItem;
import com.siirio.jembuilding.network.BuildingNetwork;
import com.siirio.jembuilding.registry.BuildingContent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BlueprintRuntime {
    private static final int QUIET_UPDATE = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final TagKey<Block> MOVE_DENIED = BlockTags.create(new ResourceLocation("jem_building", "blueprint_move_denied"));
    private static final TagKey<Block> MOVE_ALLOWED = BlockTags.create(new ResourceLocation("jem_building", "blueprint_move_allowed"));

    public static void selectFirst(ServerPlayer player, InteractionHand hand, BlockPos pos) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlueprintRollItem) || BlueprintRollItem.blueprintId(stack).isPresent()
                || !validReach(player, pos) || !player.serverLevel().hasChunkAt(pos)) return;
        BlueprintRollItem.setFirst(stack, pos);
        BlueprintRollItem.setFirstDimension(stack, player.level().dimension().location().toString());
        player.getInventory().setChanged();
        player.displayClientMessage(Component.translatable("message.jem_building.blueprint.first", pos.getX(), pos.getY(), pos.getZ()), true);
    }

    public static void clearSelection(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof BlueprintRollItem && BlueprintRollItem.blueprintId(stack).isEmpty()) {
            BlueprintRollItem.clearFirst(stack);
            player.getInventory().setChanged();
        }
    }

    public static void capture(ServerPlayer player, InteractionHand hand, BlockPos second, BlueprintMode mode) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlueprintRollItem) || BlueprintRollItem.blueprintId(stack).isPresent()) return;
        Optional<BlockPos> firstValue = BlueprintRollItem.first(stack);
        if (firstValue.isEmpty() || !validReach(player, second)) return;
        if (BlueprintRollItem.firstDimension(stack).filter(level -> level.equals(player.level().dimension().location().toString())).isEmpty()) {
            BlueprintRollItem.clearFirst(stack);
            player.getInventory().setChanged();
            player.displayClientMessage(Component.translatable("message.jem_building.blueprint.dimension_changed"), false);
            return;
        }
        BlockPos first = firstValue.get();
        BlockPos min = new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()));
        BlockPos max = new BlockPos(Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));
        int width = max.getX() - min.getX() + 1;
        int height = max.getY() - min.getY() + 1;
        int depth = max.getZ() - min.getZ() + 1;
        if (width > BlueprintConfig.MAX_AXIS.get() || height > BlueprintConfig.MAX_AXIS.get() || depth > BlueprintConfig.MAX_AXIS.get()) {
            player.displayClientMessage(Component.translatable("message.jem_building.blueprint.too_large_axis", BlueprintConfig.MAX_AXIS.get()), false);
            return;
        }
        ServerLevel level = player.serverLevel();
        List<CompoundTag> palette = new ArrayList<>();
        Map<BlockState, Integer> paletteIndex = new HashMap<>();
        List<BlueprintSavedData.Entry> entries = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.hasChunkAt(pos)) {
                player.displayClientMessage(Component.translatable("message.jem_building.blueprint.unloaded"), false);
                return;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(BuildingContent.CONSTRUCTION_PLACEHOLDER.get())) continue;
            if (entries.size() >= BlueprintConfig.MAX_CAPTURE_BLOCKS.get()) {
                player.displayClientMessage(Component.translatable("message.jem_building.blueprint.too_many_blocks", BlueprintConfig.MAX_CAPTURE_BLOCKS.get()), false);
                return;
            }
            if (mode == BlueprintMode.MOVE && !movable(player, level, pos, state)) return;
            int stateIndex = paletteIndex.computeIfAbsent(state, key -> {
                palette.add(NbtUtils.writeBlockState(key));
                return palette.size() - 1;
            });
            entries.add(new BlueprintSavedData.Entry(pos.getX() - min.getX(), pos.getY() - min.getY(), pos.getZ() - min.getZ(), stateIndex));
        }
        if (entries.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.jem_building.blueprint.empty"), false);
            return;
        }
        BlueprintSavedData data = BlueprintSavedData.get(player.server);
        int number = data.nextNumber(player.getUUID());
        BlueprintSavedData.Blueprint blueprint = data.create(player.getUUID(), number, mode, level.dimension(), min,
                width, height, depth, palette, entries);
        ItemStack paper = new ItemStack(BuildingContent.STRUCTURE_BLUEPRINT.get());
        BlueprintRollItem.bind(paper, blueprint.id(), number, player.getGameProfile().getName(), mode, width, height, depth);
        BlueprintRollItem.clearFirst(stack);
        if (!player.getInventory().add(paper)) player.drop(paper, false);
        player.getInventory().setChanged();
        data.changed();
        player.server.overworld().getDataStorage().save();
        player.displayClientMessage(Component.translatable("message.jem_building.blueprint.saved", number, entries.size()), false);
    }

    private static boolean movable(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) != null || state.is(MOVE_DENIED) || state.is(BlockTags.PORTALS)
                || state.is(Tags.Blocks.ORES) || state.is(Blocks.BEDROCK) || state.is(Blocks.SPAWNER)
                || state.is(Blocks.END_PORTAL_FRAME) || state.getDestroySpeed(level, pos) < 0F
                || (!state.is(MOVE_ALLOWED) && !hasRequiredTool(player, state))) {
            player.displayClientMessage(Component.translatable("message.jem_building.blueprint.move_blocked", state.getBlock().getName()), false);
            return false;
        }
        if (!permitted(player, pos, new ItemStack(state.getBlock().asItem()))) {
            player.displayClientMessage(Component.translatable("message.jem_building.blueprint.permission_source", pos.getX(), pos.getY(), pos.getZ()), false);
            return false;
        }
        BlockEvent.BreakEvent probe = new BlockEvent.BreakEvent(level, pos, state, player);
        if (MinecraftForge.EVENT_BUS.post(probe)) {
            player.displayClientMessage(Component.translatable("message.jem_building.blueprint.permission_source", pos.getX(), pos.getY(), pos.getZ()), false);
            return false;
        }
        return true;
    }

    public static void requestPreview(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlueprintRollItem.blueprintId(stack).flatMap(id -> BlueprintSavedData.get(player.server).get(id)).ifPresent(blueprint -> {
            if (!blueprint.owner().equals(player.getUUID())) return;
            BuildingNetwork.sendBlueprint(player, blueprint);
        });
    }

    public static void place(ServerPlayer player, InteractionHand hand, BlockPos destination, BlueprintRotation rotation) {
        ItemStack stack = player.getItemInHand(hand);
        Optional<UUID> id = BlueprintRollItem.blueprintId(stack);
        if (id.isEmpty()) {
            BlueprintRollItem.clearFirst(stack);
            player.getInventory().setChanged();
            return;
        }
        BlueprintSavedData data = BlueprintSavedData.get(player.server);
        Optional<BlueprintSavedData.Blueprint> value = data.get(id.get());
        if (value.isEmpty()) return;
        BlueprintSavedData.Blueprint blueprint = value.get();
        if (!blueprint.owner().equals(player.getUUID()) || !blueprint.dimension().equals(player.level().dimension())) return;
        if (blueprint.mode() == BlueprintMode.MOVE && blueprint.status() != BlueprintStatus.PENDING_MOVE) {
            player.displayClientMessage(Component.translatable("message.jem_building.blueprint.move_not_ready"), false);
            return;
        }
        if (blueprint.mode() == BlueprintMode.COPY && blueprint.status() != BlueprintStatus.SAVED
                && blueprint.status() != BlueprintStatus.COMPLETE) return;
        if (blueprint.mode() == BlueprintMode.MOVE && !player.getAbilities().instabuild
                && distance(blueprint.sourceMin(), destination) > BlueprintConfig.MAX_MOVE_DISTANCE.get()) {
            player.displayClientMessage(Component.translatable("message.jem_building.blueprint.move_too_far", BlueprintConfig.MAX_MOVE_DISTANCE.get()), false);
            return;
        }
        ServerLevel level = player.serverLevel();
        for (int index = 0; index < blueprint.entries().size(); index++) {
            BlockPos target = destination.offset(rotation.apply(blueprint.entries().get(index).relative(), blueprint.width(), blueprint.depth()));
            if (!level.hasChunkAt(target) || !within(level, target) || !permitted(player, target, stack)) {
                player.displayClientMessage(Component.translatable("message.jem_building.blueprint.permission_destination", target.getX(), target.getY(), target.getZ()), false);
                return;
            }
            if (blueprint.mode() == BlueprintMode.MOVE && !level.getBlockState(target).canBeReplaced()) {
                player.displayClientMessage(Component.translatable("message.jem_building.blueprint.move_obstructed"), false);
                return;
            }
            if (!rotationSupported(state(level, blueprint, index), rotation)) {
                player.displayClientMessage(Component.translatable("message.jem_building.blueprint.rotation_unsupported",
                        state(level, blueprint, index).getBlock().getName()), false);
                return;
            }
        }
        if (blueprint.mode() == BlueprintMode.COPY) blueprint.clearProgress();
        blueprint.destination(destination);
        blueprint.rotation(rotation);
        blueprint.cursor(0);
        blueprint.status(blueprint.mode() == BlueprintMode.MOVE ? BlueprintStatus.PLACING_MOVE
                : player.getAbilities().instabuild ? BlueprintStatus.PLACING_MOVE : BlueprintStatus.CONSTRUCTING_COPY);
        if (blueprint.status() == BlueprintStatus.CONSTRUCTING_COPY) data.reindexCells();
        data.changed();
        player.displayClientMessage(Component.translatable(blueprint.mode() == BlueprintMode.COPY && !player.getAbilities().instabuild
                ? "message.jem_building.blueprint.construction_started" : "message.jem_building.blueprint.placement_started"), false);
    }

    public static void cancel(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Optional<UUID> id = BlueprintRollItem.blueprintId(stack);
        if (id.isEmpty()) {
            clearSelection(player, hand);
            return;
        }
        BlueprintSavedData data = BlueprintSavedData.get(player.server);
        data.get(id.get()).ifPresent(blueprint -> {
            if (!blueprint.owner().equals(player.getUUID())) return;
            if (blueprint.mode() == BlueprintMode.MOVE && blueprint.status() == BlueprintStatus.PENDING_MOVE) {
                blueprint.destination(blueprint.sourceMin()); blueprint.rotation(BlueprintRotation.NONE);
                blueprint.cursor(0); blueprint.status(BlueprintStatus.PLACING_MOVE); data.changed();
                player.displayClientMessage(Component.translatable("message.jem_building.blueprint.restoring"), false);
            } else if (blueprint.mode() == BlueprintMode.COPY && blueprint.status() == BlueprintStatus.CONSTRUCTING_COPY) {
                blueprint.cursor(0); blueprint.status(BlueprintStatus.CANCELLED); data.changed();
                player.displayClientMessage(Component.translatable("message.jem_building.blueprint.construction_cancelled"), false);
            }
        });
    }

    public static boolean fillPlaceholder(ServerPlayer player, InteractionHand hand, BlockPos pos) {
        BlueprintSavedData data = BlueprintSavedData.get(player.server);
        Optional<BlueprintSavedData.BlueprintCell> cellValue = data.cellAt(player.level().dimension(), pos);
        if (cellValue.isEmpty()) return false;
        BlueprintSavedData.BlueprintCell cell = cellValue.get();
        BlueprintSavedData.Blueprint blueprint = cell.blueprint();
        if (!blueprint.owner().equals(player.getUUID()) && !player.hasPermissions(2)) return false;
        BlockState target = state(player.serverLevel(), blueprint, cell.index());
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof BlockItem) || held.getItem() != target.getBlock().asItem()) {
            player.displayClientMessage(Component.translatable("message.jem_building.blueprint.wrong_material", target.getBlock().getName()), true);
            return true;
        }
        List<Integer> group = coordinatedGroup(player.serverLevel(), blueprint, cell.index(), target);
        List<BlockPos> placed = new ArrayList<>();
        for (int groupIndex : group) {
            BlockPos groupPos = blueprint.target(groupIndex);
            BlockState groupState = state(player.serverLevel(), blueprint, groupIndex).rotate(blueprint.rotation().vanilla());
            if (!player.serverLevel().getBlockState(groupPos).is(BuildingContent.CONSTRUCTION_PLACEHOLDER.get())
                    || !placeExact(player, groupPos, groupState, false)) {
                for (BlockPos rollback : placed) player.serverLevel().setBlock(rollback,
                        BuildingContent.CONSTRUCTION_PLACEHOLDER.get().defaultBlockState(), Block.UPDATE_ALL);
                return true;
            }
            placed.add(groupPos);
        }
        if (!player.getAbilities().instabuild) held.shrink(1);
        for (int groupIndex : group) {
            blueprint.completed(groupIndex, true); blueprint.placeholder(groupIndex, false);
        }
        data.changed();
        if (complete(blueprint)) blueprint.status(BlueprintStatus.COMPLETE);
        return true;
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        BlueprintSavedData.get(event.getServer());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        BlueprintSavedData data = BlueprintSavedData.get(server);
        int budget = BlueprintConfig.WORK_PER_TICK.get();
        for (BlueprintSavedData.Blueprint blueprint : data.all()) {
            if (budget <= 0) break;
            ServerLevel level = server.getLevel(blueprint.dimension());
            if (level == null) continue;
            int used = switch (blueprint.status()) {
                case REMOVING_SOURCE -> removeSource(level, blueprint, budget);
                case PLACING_MOVE -> placeReal(level, blueprint, budget);
                case FINALIZING_PLACEMENT -> finalizePlacement(level, blueprint, budget);
                case CONSTRUCTING_COPY -> server.getTickCount() % 100 == 0 ? createPlaceholders(level, blueprint, budget) : 0;
                case CANCELLED -> removePlaceholders(level, blueprint, budget);
                default -> 0;
            };
            if (used > 0) { budget -= used; data.changed(); }
        }
    }

    private static int removeSource(ServerLevel level, BlueprintSavedData.Blueprint blueprint, int budget) {
        int used = 0;
        while (used < budget && blueprint.cursor() < blueprint.entries().size()) {
            int index = blueprint.cursor();
            BlueprintSavedData.Entry entry = blueprint.entries().get(index);
            BlockPos pos = blueprint.sourceMin().offset(entry.relative());
            BlockState expected = state(level, blueprint, index);
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(blueprint.owner());
            if (owner == null || owner.serverLevel() != level) return used;
            if (level.hasChunkAt(pos) && level.getBlockState(pos).equals(expected) && movable(owner, level, pos, expected)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET_UPDATE);
                blueprint.completed(index, true);
            } else {
                restoreSource(level, blueprint);
                blueprint.status(BlueprintStatus.ABORTED); blueprint.cursor(0);
                if (owner != null) owner.displayClientMessage(Component.translatable("message.jem_building.blueprint.source_changed"), false);
                return used;
            }
            blueprint.cursor(index + 1); used++;
        }
        if (blueprint.cursor() >= blueprint.entries().size()) {
            blueprint.status(BlueprintStatus.PENDING_MOVE); blueprint.cursor(0);
            for (int index = 0; index < blueprint.entries().size(); index++) blueprint.completed(index, false);
        }
        return used;
    }

    private static int placeReal(ServerLevel level, BlueprintSavedData.Blueprint blueprint, int budget) {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(blueprint.owner());
        if (owner == null || owner.serverLevel() != level) return 0;
        int used = 0;
        while (used < budget && blueprint.cursor() < blueprint.entries().size()) {
            int index = blueprint.cursor();
            BlockPos pos = blueprint.target(index);
            BlockState target = state(level, blueprint, index).rotate(blueprint.rotation().vanilla());
            if (!level.hasChunkAt(pos) || !replaceableForBlueprint(level.getBlockState(pos)) || !placeExact(owner, pos, target, true)) {
                rollbackDestination(level, blueprint);
                blueprint.status(blueprint.mode() == BlueprintMode.MOVE ? BlueprintStatus.PENDING_MOVE : BlueprintStatus.SAVED);
                blueprint.cursor(0);
                return used;
            }
            blueprint.completed(index, true); blueprint.cursor(index + 1); used++;
        }
        if (blueprint.cursor() >= blueprint.entries().size()) {
            blueprint.status(BlueprintStatus.FINALIZING_PLACEMENT); blueprint.cursor(0);
        }
        return used;
    }

    private static int finalizePlacement(ServerLevel level, BlueprintSavedData.Blueprint blueprint, int budget) {
        int used = 0;
        while (used < budget && blueprint.cursor() < blueprint.entries().size()) {
            int index = blueprint.cursor();
            BlockPos pos = blueprint.target(index);
            if (level.hasChunkAt(pos)) level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
            blueprint.cursor(index + 1);
            used++;
        }
        if (blueprint.cursor() >= blueprint.entries().size()) {
            blueprint.status(BlueprintStatus.COMPLETE);
            blueprint.cursor(0);
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(blueprint.owner());
            if (owner != null) owner.displayClientMessage(Component.translatable("message.jem_building.blueprint.complete"), false);
        }
        return used;
    }

    private static int createPlaceholders(ServerLevel level, BlueprintSavedData.Blueprint blueprint, int budget) {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(blueprint.owner());
        if (owner == null || owner.serverLevel() != level) return 0;
        int used = 0;
        while (used < budget && blueprint.cursor() < blueprint.entries().size()) {
            int index = blueprint.cursor();
            if (blueprint.completed(index) || blueprint.placeholder(index)) {
                blueprint.cursor(index + 1); used++; continue;
            }
            BlockPos pos = blueprint.target(index);
            if (level.hasChunkAt(pos)) {
                BlockState current = level.getBlockState(pos);
                if (replaceableForBlueprint(current) && permitted(owner, pos, new ItemStack(BuildingContent.BLUEPRINT_ROLL.get()))) {
                    BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
                    if (level.setBlock(pos, BuildingContent.CONSTRUCTION_PLACEHOLDER.get().defaultBlockState(), Block.UPDATE_ALL)
                            && !ForgeEventFactory.onBlockPlace(owner, snapshot, Direction.UP)) blueprint.placeholder(index, true);
                    else snapshot.restore(true, false);
                }
            }
            blueprint.cursor(index + 1); used++;
        }
        if (blueprint.cursor() >= blueprint.entries().size()) blueprint.cursor(0);
        return used;
    }

    private static int removePlaceholders(ServerLevel level, BlueprintSavedData.Blueprint blueprint, int budget) {
        int used = 0;
        while (used < budget && blueprint.cursor() < blueprint.entries().size()) {
            int index = blueprint.cursor();
            BlockPos pos = blueprint.target(index);
            if (blueprint.placeholder(index) && level.hasChunkAt(pos)
                    && level.getBlockState(pos).is(BuildingContent.CONSTRUCTION_PLACEHOLDER.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            blueprint.placeholder(index, false);
            blueprint.cursor(index + 1); used++;
        }
        if (blueprint.cursor() >= blueprint.entries().size()) {
            blueprint.clearProgress(); blueprint.cursor(0); blueprint.status(BlueprintStatus.SAVED);
        }
        return used;
    }

    private static boolean complete(BlueprintSavedData.Blueprint blueprint) {
        for (int index = 0; index < blueprint.entries().size(); index++) if (!blueprint.completed(index)) return false;
        return true;
    }

    private static void restoreSource(ServerLevel level, BlueprintSavedData.Blueprint blueprint) {
        for (int index = 0; index < blueprint.entries().size(); index++) {
            if (!blueprint.completed(index)) continue;
            BlockPos pos = blueprint.sourceMin().offset(blueprint.entries().get(index).relative());
            if (level.hasChunkAt(pos) && replaceableForBlueprint(level.getBlockState(pos))) level.setBlock(pos, state(level, blueprint, index), QUIET_UPDATE);
            blueprint.completed(index, false);
        }
    }

    private static void rollbackDestination(ServerLevel level, BlueprintSavedData.Blueprint blueprint) {
        for (int index = 0; index < blueprint.entries().size(); index++) {
            if (!blueprint.completed(index)) continue;
            BlockPos pos = blueprint.target(index);
            BlockState expected = state(level, blueprint, index).rotate(blueprint.rotation().vanilla());
            if (level.hasChunkAt(pos) && level.getBlockState(pos).equals(expected)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET_UPDATE);
            blueprint.completed(index, false);
        }
    }

    private static boolean rotationSupported(BlockState state, BlueprintRotation rotation) {
        if (rotation == BlueprintRotation.NONE) return true;
        BlockState rotated = state.rotate(rotation.vanilla());
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return !rotated.equals(state);
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)
                && (rotation == BlueprintRotation.CLOCKWISE_90 || rotation == BlueprintRotation.COUNTERCLOCKWISE_90)) return !rotated.equals(state);
        return true;
    }

    private static List<Integer> coordinatedGroup(ServerLevel level, BlueprintSavedData.Blueprint blueprint, int index, BlockState target) {
        boolean doubleHeight = target.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF);
        boolean bed = target.hasProperty(BlockStateProperties.BED_PART);
        if (!doubleHeight && !bed) return List.of(index);
        BlockPos origin = blueprint.target(index);
        for (int candidate = 0; candidate < blueprint.entries().size(); candidate++) {
            if (candidate == index || blueprint.completed(candidate)) continue;
            BlockState other = state(level, blueprint, candidate).rotate(blueprint.rotation().vanilla());
            if (other.getBlock() != target.getBlock()) continue;
            BlockPos otherPos = blueprint.target(candidate);
            int manhattan = Math.abs(origin.getX() - otherPos.getX()) + Math.abs(origin.getY() - otherPos.getY()) + Math.abs(origin.getZ() - otherPos.getZ());
            if (manhattan != 1) continue;
            if (doubleHeight && other.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && other.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != target.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)) return List.of(index, candidate);
            if (bed && other.hasProperty(BlockStateProperties.BED_PART)
                    && other.getValue(BlockStateProperties.BED_PART) != target.getValue(BlockStateProperties.BED_PART)) return List.of(index, candidate);
        }
        return List.of(index);
    }

    private static BlockState state(ServerLevel level, BlueprintSavedData.Blueprint blueprint, int index) {
        int paletteIndex = blueprint.entries().get(index).paletteIndex();
        return NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), blueprint.palette().get(paletteIndex));
    }

    private static boolean placeExact(ServerPlayer player, BlockPos pos, BlockState state, boolean quiet) {
        if (!permitted(player, pos, new ItemStack(state.getBlock().asItem()))) return false;
        ServerLevel level = player.serverLevel();
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, state, quiet ? QUIET_UPDATE : Block.UPDATE_ALL)) return false;
        if (ForgeEventFactory.onBlockPlace(player, snapshot, Direction.UP)) {
            snapshot.restore(true, false);
            return false;
        }
        return true;
    }

    private static boolean permitted(ServerPlayer player, BlockPos pos, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        return player.mayBuild() && level.hasChunkAt(pos) && within(level, pos)
                && level.mayInteract(player, pos) && player.mayUseItemAt(pos, Direction.UP, stack);
    }

    private static boolean replaceableForBlueprint(BlockState state) {
        return state.canBeReplaced() && !state.is(BuildingContent.CONSTRUCTION_PLACEHOLDER.get());
    }

    private static boolean hasRequiredTool(ServerPlayer player, BlockState state) {
        if (!state.requiresCorrectToolForDrops()) return true;
        for (ItemStack stack : player.getInventory().items) if (stack.isCorrectToolForDrops(state)) return true;
        for (ItemStack stack : player.getInventory().offhand) if (stack.isCorrectToolForDrops(state)) return true;
        return false;
    }

    private static boolean validReach(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64D && within(player.serverLevel(), pos);
    }

    private static boolean within(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos);
    }

    private static double distance(BlockPos first, BlockPos second) {
        return Math.sqrt(first.distSqr(second));
    }

    private BlueprintRuntime() { }
}
