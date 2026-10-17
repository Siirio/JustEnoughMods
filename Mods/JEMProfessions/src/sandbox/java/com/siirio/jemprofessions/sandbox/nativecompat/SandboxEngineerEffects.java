package com.siirio.jemprofessions.sandbox.nativecompat;

import com.mojang.logging.LogUtils;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.sandbox.SandboxProfessionSupport;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleItem;
import com.simibubi.create.content.trains.track.TrackBlockItem;
import com.simibubi.create.foundation.utility.CreatePaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

/** Bounded conveniences around Create placement, schematic printer and schedule owners. */
final class SandboxEngineerEffects {
    private static final Map<ServerPlayer, Corner> CORNERS = new WeakHashMap<>();
    private static final Map<ServerPlayer, PendingWrite> WRITES = new WeakHashMap<>();
    private SandboxEngineerEffects() {}
    static void logout(ServerPlayer player) { CORNERS.remove(player); WRITES.remove(player); }
    private static boolean has(ServerPlayer player, String node) { return SandboxProfessionSupport.enabled(player, ProfessionId.ENGINEER, node); }

    static boolean activate(ServerPlayer player, String node, CompoundTag input) {
        if (!(node.equals("gear_swap") || node.equals("shaft_builder") || node.equals("factory_copy")
                || node.equals("track_run") || node.equals("schedule_templates"))) return false;
        if (!has(player, node) || !player.isAlive()) return false;
        switch (node) {
            case "gear_swap" -> swap(player);
            case "shaft_builder" -> line(player, input.getInt("Value"), false);
            case "track_run" -> line(player, input.getInt("Value"), true);
            case "factory_copy" -> schematic(player, input.getString("Mode"), input.getInt("Value"));
            case "schedule_templates" -> schedule(player, input.getString("Mode"), input.getInt("Value"));
            default -> { }
        }
        return true;
    }

    private static BlockHitResult hit(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = player.serverLevel().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(6)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private static boolean permitted(ServerPlayer player, BlockPos pos) {
        return player.mayBuild() && player.serverLevel().hasChunkAt(pos) && player.serverLevel().getWorldBorder().isWithinBounds(pos)
                && !player.serverLevel().isOutsideBuildHeight(pos) && player.serverLevel().mayInteract(player, pos);
    }

    private static void say(ServerPlayer player, String ru, String en) { SandboxProfessionSupport.message(player, ru, en); }

    private static void swap(ServerPlayer player) {
        BlockHitResult aimed = hit(player);
        ItemStack replacement = player.getOffhandItem();
        if (aimed == null || !(replacement.getItem() instanceof BlockItem item)
                || !(item.getBlock() instanceof CogWheelBlock next)) {
            say(player, "Наведи на шестерню; другую шестерню возьми во вторую руку.", "Aim at a cogwheel with its replacement in your off hand."); return;
        }
        BlockPos pos = aimed.getBlockPos();
        BlockState old = player.serverLevel().getBlockState(pos);
        if (!(old.getBlock() instanceof CogWheelBlock) || old.getBlock() == next || !permitted(player, pos)
                || !old.hasProperty(BlockStateProperties.AXIS)) {
            say(player, "Нужна доступная установленная шестерня другого размера.", "Choose an accessible installed cogwheel of a different size."); return;
        }
        Direction.Axis axis = old.getValue(BlockStateProperties.AXIS);
        BlockState state = next.defaultBlockState().setValue(BlockStateProperties.AXIS, axis);
        if (!CogWheelBlock.isValidCogwheelPosition(next.isLargeCog(), player.serverLevel(), pos, axis)
                || !state.canSurvive(player.serverLevel(), pos)
                || !player.serverLevel().isUnobstructed(state, pos, net.minecraft.world.phys.shapes.CollisionContext.empty())) {
            say(player, "Create не разрешает эту шестерню в выбранном месте.", "Create does not allow that cogwheel at this position."); return;
        }
        if (ForgeHooks.onBlockBreakEvent(player.serverLevel(), player.gameMode.getGameModeForPlayer(), player, pos) < 0) return;
        BlockSnapshot snapshot = BlockSnapshot.create(player.level().dimension(), player.serverLevel(), pos);
        if (!player.serverLevel().setBlock(pos, state, Block.UPDATE_ALL)) return;
        if (ForgeEventFactory.onBlockPlace(player, snapshot, aimed.getDirection())) {
            snapshot.restore(true, false);
            say(player, "Замена отменена правилами размещения.", "Placement rules canceled the replacement."); return;
        }
        replacement.shrink(1);
        ItemStack returned = new ItemStack(old.getBlock());
        if (!player.getInventory().add(returned)) player.drop(returned, false);
        next.setPlacedBy(player.serverLevel(), pos, state, player, replacement);
        player.getInventory().setChanged();
        SandboxNativeEffects.engineerPlacement(player, pos, BuiltInRegistries.BLOCK.getKey(next));
        say(player, "Шестерня заменена; прежняя возвращена.", "Cogwheel replaced; the old part has been returned.");
    }

    private static void line(ServerPlayer player, int requested, boolean track) {
        int limit = track ? 12 : 16;
        int count = Math.max(1, Math.min(limit, requested));
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BlockItem item) || (track ? !(item instanceof TrackBlockItem) : !(item.getBlock() instanceof ShaftBlock))) {
            say(player, track ? "Возьми подходящие пути Create в основную руку." : "Возьми валы Create в основную руку.",
                    track ? "Hold Create tracks in your main hand." : "Hold Create shafts in your main hand."); return;
        }
        BlockHitResult aimed = hit(player);
        if (aimed == null) { say(player, "Наведи на начало линии.", "Aim at the start of the line."); return; }
        // TrackBlockItem's own selected curve operation can place an unbounded connection; never invoke it here.
        if (track && stack.hasTag() && !stack.getTag().isEmpty()) {
            say(player, "Сначала сбрось выбранное соединение пути обычным действием Create.", "Clear the native track connection selection first."); return;
        }
        Direction direction = track ? player.getDirection() : aimed.getDirection();
        BlockPos start = aimed.getBlockPos().relative(aimed.getDirection());
        if (track && aimed.getDirection() != Direction.UP) {
            say(player, "Для прямого пути наведи на верх опоры.", "Aim at the top of the support for straight tracks."); return;
        }
        int placed = 0;
        for (int i = 0; i < count && !stack.isEmpty(); i++) {
            if (!has(player, track ? "track_run" : "shaft_builder")) break;
            BlockPos target = start.relative(direction, i);
            if (!permitted(player, target) || !player.serverLevel().getBlockState(target).canBeReplaced()) break;
            BlockPos support = track ? target.below() : target.relative(direction.getOpposite());
            if (!permitted(player, support)) break;
            Direction face = track ? Direction.UP : direction;
            BlockHitResult step = new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)), face, support, false);
            // Native interaction calls Forge hooks, BlockItem placement, costs, axis/track graph and survival.
            player.gameMode.useItemOn(player, player.serverLevel(), stack, InteractionHand.MAIN_HAND, step);
            if (!player.serverLevel().getBlockState(target).is(item.getBlock())) break;
            placed++;
            SandboxNativeEffects.engineerPlacement(player, target, BuiltInRegistries.BLOCK.getKey(item.getBlock()));
        }
        player.getInventory().setChanged();
        say(player, "Установлено: " + placed + "/" + count + ". Остальное остановлено материалами или правилами Create.",
                "Placed " + placed + "/" + count + ". Remaining work stopped at materials or Create placement rules.");
    }

    private static void schedule(ServerPlayer player, String mode, int slot) {
        if (slot < 1 || slot > 3 || !(player.getMainHandItem().getItem() instanceof ScheduleItem)) {
            say(player, "Возьми настоящий предмет расписания и выбери ячейку 1–3.", "Hold an actual schedule item and choose slot 1–3."); return;
        }
        CompoundTag storage = SandboxNativeEffects.state(player);
        String key = "Schedule" + slot;
        if (mode.equals("save")) {
            Schedule nativeSchedule = ScheduleItem.getSchedule(player.getMainHandItem());
            if (nativeSchedule == null || nativeSchedule.entries.isEmpty()) { say(player, "В расписании нет остановок.", "The schedule has no stops."); return; }
            CompoundTag schedule = nativeSchedule.write();
            if (schedule.sizeInBytes() > 8192) { say(player, "Расписание слишком большое для шаблона.", "This schedule is too large for a template."); return; }
            storage.put(key, schedule);
            say(player, "Расписание сохранено в ячейке " + slot + ".", "Schedule saved in slot " + slot + ".");
        } else if (mode.equals("write")) {
            if (!storage.contains(key, Tag.TAG_COMPOUND)) { say(player, "Ячейка расписания пуста.", "The schedule slot is empty."); return; }
            Schedule validated = Schedule.fromTag(storage.getCompound(key).copy());
            if (validated.entries.isEmpty()) return;
            validated.savedProgress = 0;
            player.getMainHandItem().getOrCreateTag().put("Schedule", validated.write());
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            say(player, "Шаблон записан в предмет расписания.", "Template written onto the schedule item.");
        } else say(player, "Выбери «Сохранить» или «Записать».", "Choose Save or Write.");
    }

    private static void schematic(ServerPlayer player, String mode, int slot) {
        if (slot < 1 || slot > 3) { say(player, "Выбери ячейку чертежа 1–3.", "Choose schematic slot 1–3."); return; }
        if (mode.equals("first")) {
            BlockHitResult aimed = hit(player);
            if (aimed == null || !permitted(player, aimed.getBlockPos())) return;
            CORNERS.put(player, new Corner(player.level().dimension().location(), aimed.getBlockPos().immutable(), player.serverLevel().getGameTime()));
            say(player, "Первый угол выбран. Наведи на второй и сохрани чертёж.", "First corner selected. Aim at the second corner and save.");
            return;
        }
        String owner = player.getUUID().toString();
        Path directory = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(owner);
        if (mode.equals("write")) {
            ItemStack held = player.getMainHandItem();
            CompoundTag storage = SandboxNativeEffects.state(player);
            if (!storage.hasUUID("SchematicRevision" + slot)
                    || !BuiltInRegistries.ITEM.getKey(held.getItem()).equals(new ResourceLocation("create:empty_schematic"))) {
                say(player, "Нужны сохранённый чертёж и пустая схема Create в руке.", "You need a saved template and an empty Create schematic in hand."); return;
            }
            UUID version = storage.getUUID("SchematicRevision" + slot);
            Path file = directory.resolve(SandboxSchematicProjection.filename(player.getUUID(), slot, version));
            try {
                CompoundTag data = SandboxSchematicProjection.read(file);
                if (SandboxSchematicProjection.send(player, slot, version, data)) {
                    WRITES.put(player, new PendingWrite(slot, version, held));
                    say(player, "Чертёж передаётся. Держи пустую схему в руке до завершения.", "Transferring the template. Keep the empty schematic in hand until ready.");
                } else say(player, "Передача уже выполняется или клиент ещё не готов.", "A transfer is already pending or the client is not ready.");
            } catch (IOException | IllegalArgumentException failure) {
                LogUtils.getLogger().error("Could not read owned sandbox Create schematic {}", file, failure);
                say(player, "Не удалось прочитать чертёж. Сохрани его заново.", "Could not read the template. Save it again.");
            }
            return;
        }
        if (!mode.equals("save")) { say(player, "Выбери первый угол, сохранение или запись схемы.", "Choose First corner, Save or Write schematic."); return; }
        Corner first = CORNERS.get(player);
        BlockHitResult second = hit(player);
        if (first == null || second == null || !first.dimension.equals(player.level().dimension().location())
                || player.serverLevel().getGameTime() - first.tick > 1200) {
            say(player, "Сначала выбери первый угол в этом измерении.", "First select the initial corner in this dimension."); return;
        }
        BlockPos a = first.pos;
        BlockPos b = second.getBlockPos();
        BlockPos origin = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        Vec3i size = new Vec3i(Math.abs(a.getX() - b.getX()) + 1, Math.abs(a.getY() - b.getY()) + 1, Math.abs(a.getZ() - b.getZ()) + 1);
        if (size.getX() > 7 || size.getY() > 7 || size.getZ() > 7 || a.distSqr(player.blockPosition()) > 256) {
            say(player, "Чертёж ограничен 7×7×7; оба угла должны быть рядом.", "Templates are limited to 7×7×7; both corners must be nearby."); return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size).offset(-1, -1, -1))) {
            if (!permitted(player, pos)) return;
            if (player.serverLevel().getBlockEntity(pos) instanceof KineticBlockEntity kinetic && kinetic.getSpeed() != 0) {
                say(player, "Останови работающий механизм перед сохранением.", "Stop the running mechanism before saving."); return;
            }
        }
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(player.serverLevel(), origin, size, false, Blocks.STRUCTURE_VOID);
        CompoundTag data = template.save(new CompoundTag());
        data.remove("entities");
        for (Tag row : data.getList("blocks", Tag.TAG_COMPOUND)) ((CompoundTag) row).remove("nbt");
        for (Tag row : data.getList("palette", Tag.TAG_COMPOUND)) {
            CompoundTag palette = (CompoundTag) row;
            ResourceLocation id = ResourceLocation.tryParse(palette.getString("Name"));
            if (id != null && BuiltInRegistries.BLOCK.get(id) instanceof LiquidBlock) {
                palette.putString("Name", "minecraft:air"); palette.remove("Properties");
            } else if (palette.getCompound("Properties").contains("waterlogged")) palette.getCompound("Properties").putString("waterlogged", "false");
        }
        UUID version = UUID.randomUUID();
        Path file = directory.resolve(SandboxSchematicProjection.filename(player.getUUID(), slot, version));
        try {
            SandboxSchematicProjection.write(file, data);
            SandboxNativeEffects.state(player).putUUID("SchematicRevision" + slot, version);
            CORNERS.remove(player);
            say(player, "Чертёж " + slot + " сохранён без содержимого, жидкостей и данных механизмов.",
                    "Template " + slot + " saved without contents, fluids or machine data.");
        } catch (IOException | IllegalArgumentException failure) {
            LogUtils.getLogger().error("Could not save owned sandbox Create schematic {}", file, failure);
            say(player, "Не удалось записать чертёж. Причина сохранена в журнале.", "Could not write the schematic. The cause is recorded in the log.");
        }
    }

    static void projectionReady(ServerPlayer player, int slot, UUID version, boolean success) {
        PendingWrite pending = WRITES.remove(player);
        if (pending == null || pending.slot != slot || !pending.version.equals(version)) return;
        if (!success) {
            say(player, "Не удалось передать чертёж. Пустая схема сохранена.", "Could not transfer the template. Your empty schematic is unchanged.");
            return;
        }
        ItemStack held = player.getMainHandItem();
        CompoundTag storage = SandboxNativeEffects.state(player);
        String name = SandboxSchematicProjection.filename(player.getUUID(), slot, version);
        Path file = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(player.getUUID().toString()).resolve(name);
        if (!has(player, "factory_copy") || !player.isAlive() || held != pending.blank || held.isEmpty()
                || !storage.hasUUID("SchematicRevision" + slot) || !storage.getUUID("SchematicRevision" + slot).equals(version)
                || !Files.isRegularFile(file) || !BuiltInRegistries.ITEM.getKey(held.getItem()).equals(new ResourceLocation("create:empty_schematic"))) {
            say(player, "Запись отменена: навык, предмет или выбранный чертёж изменился.", "Write canceled: the skill, held item or selected template changed.");
            return;
        }
        ItemStack schematic = SchematicItem.create(player.serverLevel(), name, player.getUUID().toString());
        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(InteractionHand.MAIN_HAND, schematic);
        else if (!player.getInventory().add(schematic)) player.drop(schematic, false);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        say(player, "Чертёж готов: используй обычный принтер Create со своими материалами.", "Schematic ready: use the ordinary Create printer with your materials.");
    }

    private record Corner(ResourceLocation dimension, BlockPos pos, long tick) {}
    private record PendingWrite(int slot, UUID version, ItemStack blank) {}
}
