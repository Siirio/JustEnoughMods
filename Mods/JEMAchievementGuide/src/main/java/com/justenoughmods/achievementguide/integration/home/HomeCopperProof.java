package com.justenoughmods.achievementguide.integration.home;

import com.github.smallinger.copperagebackport.block.CopperChestBlock;
import com.github.smallinger.copperagebackport.entity.CopperGolemEntity;
import com.github.smallinger.copperagebackport.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Native golem creator and physical chest-to-hand lineage, outside ordinary stack NBT. */
public final class HomeCopperProof {
    private static final String BUILDER = "jem_home_copper_builder";
    private static final String CARGO = "jem_home_copper_cargo";
    private record Transfer(PathfinderMob mob, Container container, TransportItemTarget target, boolean pickup,
                            List<ItemStack> stock, ItemStack held, boolean eligible) {}
    private static final ThreadLocal<Deque<Transfer>> TRANSFERS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<Entity>> LOADING = ThreadLocal.withInitial(ArrayDeque::new);

    public static void created(Entity entity, BlockPos pumpkin) {
        ServerPlayer actor = HomeItemUseProof.actor();
        if (!(entity instanceof CopperGolemEntity) || actor == null || actor.level() != entity.level()
                || !HomeItemUseProof.used(Items.CARVED_PUMPKIN)) return;
        var level = actor.serverLevel();
        BlockPos chest = pumpkin.below();
        HomeItemUseProof.defer(actor, () -> level.getEntity(entity.getUUID()) == entity && !entity.isRemoved()
                        && level.getBlockState(pumpkin).isAir() && level.getBlockState(chest).getBlock() instanceof CopperChestBlock,
                () -> entity.getPersistentData().putUUID(BUILDER, actor.getUUID()));
    }

    public static void begin(PathfinderMob mob, Container container, TransportItemTarget target, boolean pickup) {
        if (TRANSFERS.get().size() >= 16) TRANSFERS.get().clear();
        boolean valid = mob instanceof CopperGolemEntity && target != null && target.container() == container
                && validTarget(mob, target) && container.getContainerSize() <= 256;
        List<ItemStack> stock = new ArrayList<>();
        if (valid) for (int slot = 0; slot < container.getContainerSize(); slot++) stock.add(container.getItem(slot).copy());
        TRANSFERS.get().push(new Transfer(mob, container, target, pickup, stock, mob.getMainHandItem().copy(), valid));
    }

    public static void finish(PathfinderMob mob, Container container, boolean pickup) {
        Transfer before = TRANSFERS.get().poll();
        if (before == null || before.mob != mob || before.container != container || before.pickup != pickup
                || !before.eligible || !validTarget(mob, before.target)) return;
        ItemStack now = mob.getMainHandItem();
        if (pickup) {
            mob.getPersistentData().remove(CARGO);
            if (!before.held.isEmpty() || now.isEmpty() || !before.target.state().is(com.github.smallinger.copperagebackport.ModTags.Blocks.COPPER_CHESTS)
                    || count(before.stock, now) - count(container, now) != now.getCount()) return;
            CompoundTag cargo = new CompoundTag();
            cargo.put("stack", now.save(new CompoundTag()));
            cargo.putString("dimension", mob.level().dimension().location().toString());
            cargo.putLong("source", before.target.pos().asLong());
            mob.getPersistentData().put(CARGO, cargo);
            return;
        }
        ItemStack expected = expected(mob);
        if (expected.isEmpty() || !same(expected, before.held)
                || !now.isEmpty() && !ItemStack.isSameItemSameTags(before.held, now)) {
            mob.getPersistentData().remove(CARGO);
            return;
        }
        int delivered = before.held.getCount() - (now.isEmpty() ? 0 : now.getCount());
        if (delivered <= 0 || count(container, before.held) - count(before.stock, before.held) != delivered) return;
        CompoundTag data = mob.getPersistentData();
        if (data.hasUUID(BUILDER) && mob.level() instanceof net.minecraft.server.level.ServerLevel level) {
            ServerPlayer actor = level.getServer().getPlayerList().getPlayer(data.getUUID(BUILDER));
            if (actor != null && actor.level() == level) HomeInteractionProof.emit(actor, "v3_home_copper_sort");
        }
        if (now.isEmpty()) data.remove(CARGO);
        else data.getCompound(CARGO).put("stack", now.save(new CompoundTag()));
    }

    public static void assigned(Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof CopperGolemEntity) || slot != EquipmentSlot.MAINHAND) return;
        Transfer transfer = TRANSFERS.get().peek();
        if (transfer != null && transfer.mob == entity || LOADING.get().peek() == entity) return;
        entity.getPersistentData().remove(CARGO);
    }
    public static void loading(Entity entity) {
        if (LOADING.get().size() >= 16) LOADING.get().clear();
        LOADING.get().push(entity);
    }
    public static void loaded(PathfinderMob mob) {
        Entity loading = LOADING.get().poll();
        if (loading == mob && !same(expected(mob), mob.getMainHandItem())) mob.getPersistentData().remove(CARGO);
    }
    private static ItemStack expected(PathfinderMob mob) {
        CompoundTag cargo = mob.getPersistentData().getCompound(CARGO);
        return cargo.getString("dimension").equals(mob.level().dimension().location().toString())
                ? ItemStack.of(cargo.getCompound("stack")) : ItemStack.EMPTY;
    }
    private static boolean same(ItemStack first, ItemStack second) {
        return !first.isEmpty() && !second.isEmpty() && first.getCount() == second.getCount() && ItemStack.isSameItemSameTags(first, second);
    }
    private static boolean validTarget(PathfinderMob mob, TransportItemTarget target) {
        return !mob.isRemoved() && target.blockEntity().getLevel() == mob.level() && !target.blockEntity().isRemoved()
                && mob.level().hasChunkAt(target.pos()) && mob.level().getBlockEntity(target.pos()) == target.blockEntity()
                && mob.level().getBlockState(target.pos()).getBlock() == target.state().getBlock();
    }
    private static int count(List<ItemStack> items, ItemStack variant) {
        int total = 0;
        for (ItemStack item : items) if (ItemStack.isSameItemSameTags(item, variant)) total += item.getCount();
        return total;
    }
    private static int count(Container container, ItemStack variant) {
        if (container.getContainerSize() > 256) return -1;
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack item = container.getItem(slot);
            if (ItemStack.isSameItemSameTags(item, variant)) total += item.getCount();
        }
        return total;
    }
    private HomeCopperProof() {}
}
