package com.justenoughmods.achievementguide.integration.home;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class HomeRegisterProof {
    private record Snapshot(AbstractContainerMenu menu, Player player, BlockEntity register,
                            Map<CompoundTag, Integer> stored, Map<CompoundTag, Integer> carried) {}
    private static final ThreadLocal<Deque<Snapshot>> CALLS = ThreadLocal.withInitial(ArrayDeque::new);

    public static void begin(AbstractContainerMenu menu, Player player) {
        if (CALLS.get().size() >= 16) CALLS.get().clear();
        BlockEntity block = menu instanceof HomeRegisterAccess nativeMenu && nativeMenu.jem$homeBound()
                ? nativeMenu.jem$homeRegister() : null;
        boolean valid = block instanceof Container && HomeInteractionProof.actor(player) != null
                && player.containerMenu == menu && block.getLevel() == player.level()
                && HomeInteractionProof.block(block.getBlockState()).equals("dustydecorations:vintage_cash_register");
        CALLS.get().push(new Snapshot(menu, player, valid ? block : null,
                valid ? count((Container)block) : Map.of(), valid ? carried(player, menu) : Map.of()));
    }

    public static void finish(AbstractContainerMenu menu, Player player) {
        Snapshot before = CALLS.get().poll();
        if (before == null || before.menu != menu || before.player != player || before.register == null
                || HomeInteractionProof.actor(player) == null || player.containerMenu != menu
                || before.register.isRemoved() || before.register.getLevel() != player.level()
                || player.level().getBlockEntity(before.register.getBlockPos()) != before.register
                || ((HomeRegisterAccess)menu).jem$homeRegister() != before.register) return;
        Map<CompoundTag, Integer> after = count((Container)before.register);
        Map<CompoundTag, Integer> pool = carried(player, menu);
        for (var entry : after.entrySet()) {
            int delta = entry.getValue() - before.stored.getOrDefault(entry.getKey(), 0);
            if (delta > 0 && before.carried.getOrDefault(entry.getKey(), 0) - pool.getOrDefault(entry.getKey(), 0) == delta) {
                HomeInteractionProof.emit(player, "v3_home_quirky_corner");
                return;
            }
        }
    }

    private static Map<CompoundTag, Integer> count(Container container) {
        Map<CompoundTag, Integer> result = new HashMap<>();
        if (container.getContainerSize() > 64) return result;
        for (int slot = 0; slot < container.getContainerSize(); slot++) add(result, container.getItem(slot));
        return result;
    }
    private static Map<CompoundTag, Integer> carried(Player player, AbstractContainerMenu menu) {
        Map<CompoundTag, Integer> result = count(player.getInventory());
        add(result, menu.getCarried());
        return result;
    }
    private static void add(Map<CompoundTag, Integer> result, ItemStack stack) {
        if (stack.isEmpty()) return;
        CompoundTag key = stack.save(new CompoundTag());
        key.remove("Count");
        result.merge(key, stack.getCount(), Integer::sum);
    }
    private HomeRegisterProof() {}
}
