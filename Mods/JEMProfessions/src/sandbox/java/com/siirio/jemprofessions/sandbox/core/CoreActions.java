package com.siirio.jemprofessions.sandbox.core;

import com.siirio.jemprofessions.sandbox.SandboxProfessionSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.Event;

final class CoreActions {
    private CoreActions() { }
    static BlockHitResult ray(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        return player.serverLevel().clip(new ClipContext(start, start.add(player.getViewVector(1).scale(range)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }
    static boolean permitted(ServerPlayer player, BlockPos pos, ItemStack stack) {
        return player.serverLevel().hasChunkAt(pos) && player.serverLevel().getWorldBorder().isWithinBounds(pos)
                && !player.serverLevel().isOutsideBuildHeight(pos) && player.mayBuild()
                && player.serverLevel().mayInteract(player, pos) && player.mayUseItemAt(pos, Direction.UP, stack);
    }
    static int slot(ServerPlayer player, Predicate<ItemStack> predicate) {
        for (int i = 0; i < player.getInventory().items.size(); i++) if (predicate.test(player.getInventory().items.get(i))) return i;
        return -1;
    }
    static boolean safeBlock(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem
                && (!stack.hasTag() || (!stack.getTag().contains("BlockEntityTag") && !stack.getTag().contains("EntityTag")));
    }
    static boolean safeArrival(ServerPlayer player, Vec3 arrival) {
        if (!player.isAlive() || player.isPassenger() || player.isFallFlying()) return false;
        var box = player.getBoundingBox().move(arrival.subtract(player.position())).inflate(0.02D);
        if (!player.serverLevel().getWorldBorder().isWithinBounds(box) || !player.serverLevel().noCollision(player, box)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (!permitted(player, pos, player.getMainHandItem()) || !player.serverLevel().getFluidState(pos).isEmpty()) return false;
        }
        BlockPos floor = BlockPos.containing(arrival).below();
        BlockState support = player.serverLevel().getBlockState(floor);
        return !support.getCollisionShape(player.serverLevel(), floor).isEmpty();
    }

    /** One native snapshot transaction: denied placement restores every block and every material. */
    static boolean place(ServerPlayer player, List<Placement> plan) {
        if (plan.isEmpty() || plan.size() > 49 || player.isCreative() || player.isSpectator()) return false;
        var level = player.serverLevel(); if (level.captureBlockSnapshots || !level.capturedBlockSnapshots.isEmpty()) return false;
        List<ItemStack> inventory = new ArrayList<>(); for (int i = 0; i < player.getInventory().getContainerSize(); i++) inventory.add(player.getInventory().getItem(i).copy());
        for (Placement part : plan) {
            if (part.slot < 0 || part.slot >= player.getInventory().items.size()) return false;
            ItemStack stack = player.getInventory().getItem(part.slot);
            if (!safeBlock(stack) || !permitted(player, part.pos, stack)) return false;
            if (!level.getBlockState(part.pos).canBeReplaced()) return false;
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(part.pos), part.face, part.pos, false);
            var event = ForgeHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, part.pos, hit);
            if (event.isCanceled() || event.getUseItem() == Event.Result.DENY || event.getUseBlock() == Event.Result.DENY) return false;
        }
        boolean succeeded = true;
        List<BlockSnapshot> snapshots;
        level.captureBlockSnapshots = true;
        try {
            for (Placement part : plan) {
                ItemStack stack = player.getInventory().getItem(part.slot);
                if (!safeBlock(stack)) { succeeded = false; break; }
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(part.pos), part.face, part.pos, false);
                BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit);
                if (!context.getClickedPos().equals(part.pos) || !((BlockItem) stack.getItem()).place(context).consumesAction()
                        || level.getBlockState(part.pos).isAir() || level.capturedBlockSnapshots.size() > 147) { succeeded = false; break; }
            }
        } catch (RuntimeException error) {
            succeeded = false;
            com.mojang.logging.LogUtils.getLogger().error("Profession placement transaction failed; restoring native snapshots", error);
        } finally {
            level.captureBlockSnapshots = false;
            snapshots = new ArrayList<>(level.capturedBlockSnapshots); level.capturedBlockSnapshots.clear();
        }
        if (succeeded && snapshots.isEmpty()) succeeded = false;
        if (succeeded) {
            try {
                succeeded = !(snapshots.size() == 1 ? ForgeEventFactory.onBlockPlace(player, snapshots.get(0), plan.get(0).face)
                        : ForgeEventFactory.onMultiBlockPlace(player, snapshots, plan.get(0).face));
            } catch (RuntimeException error) {
                succeeded = false;
                com.mojang.logging.LogUtils.getLogger().error("Profession placement permission event failed; restoring native snapshots", error);
            }
        }
        if (!succeeded) {
            level.restoringBlockSnapshots = true;
            try { for (int i = snapshots.size() - 1; i >= 0; i--) snapshots.get(i).restore(true, false); }
            finally { level.restoringBlockSnapshots = false; }
            for (int i = 0; i < inventory.size(); i++) player.getInventory().setItem(i, inventory.get(i));
            player.containerMenu.broadcastChanges(); return false;
        }
        for (BlockSnapshot snapshot : snapshots) {
            BlockState old = snapshot.getReplacedBlock(), current = level.getBlockState(snapshot.getPos());
            current.onPlace(level, snapshot.getPos(), old, false);
            level.markAndNotifyBlock(snapshot.getPos(), level.getChunkAt(snapshot.getPos()), old, current, snapshot.getFlag(), 512);
            SandboxCoreEffects.committedPlacement(player, snapshot.getPos(), current);
        }
        player.getInventory().setChanged(); player.containerMenu.broadcastChanges(); return true;
    }
    static List<Placement> plane(ServerPlayer player, BlockPos center, Direction face, int size, int slot) {
        List<Placement> plan = new ArrayList<>(); int half = size / 2;
        for (int first = -half; first <= half; first++) for (int second = -half; second <= half; second++) {
            BlockPos pos = switch (face.getAxis()) {
                case X -> center.offset(0, first, second);
                case Y -> center.offset(first, 0, second);
                case Z -> center.offset(first, second, 0);
            };
            plan.add(new Placement(pos, face, slot));
        }
        return plan;
    }
    record Placement(BlockPos pos, Direction face, int slot) { }
}
