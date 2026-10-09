package com.siirio.jemclaims.compat.storage;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import com.tom.storagemod.util.IProxy;
import java.lang.ref.WeakReference;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

public final class StorageAccess {
    private static final ThreadLocal<Access> ACTOR = new ThreadLocal<>();

    private record Access(ServerPlayer player, Map<BlockEntity, Boolean> permissions) {}

    private StorageAccess() {}

    public static <T> T as(Player player, Supplier<T> action) {
        Access previous = ACTOR.get();
        if (player instanceof ServerPlayer serverPlayer && (previous == null || previous.player() != player))
            ACTOR.set(new Access(serverPlayer, new IdentityHashMap<>()));
        try {
            return action.get();
        } finally {
            if (previous == null) ACTOR.remove();
            else ACTOR.set(previous);
        }
    }

    public static <T> LazyOptional<T> capability(BlockEntity source, BlockEntity target, Capability<T> capability, Direction side) {
        LazyOptional<T> original = target.getCapability(capability, side);
        if (capability != ForgeCapabilities.ITEM_HANDLER || !(target.getLevel() instanceof ServerLevel)) return original;
        var cache = ((StorageInventoryCache) source).jemclaims$inventoryCache();
        cache.entrySet().removeIf(entry -> !entry.getKey().isPresent());
        LazyOptional<?> cached = cache.get(original);
        if (cached != null) return cached.cast();
        LazyOptional<IItemHandler> guarded = LazyOptional.of(() -> new GuardedInventory(source, target,
                (IItemHandler) original.orElseThrow(() -> new IllegalStateException("Invalidated storage inventory"))));
        WeakReference<LazyOptional<IItemHandler>> reference = new WeakReference<>(guarded);
        original.addListener(ignored -> {
            LazyOptional<IItemHandler> value = reference.get();
            if (value != null) value.invalidate();
        });
        if (!original.isPresent()) guarded.invalidate();
        else cache.put(original, guarded);
        return guarded.cast();
    }

    private record GuardedInventory(BlockEntity source, BlockEntity target, IItemHandler delegate) implements IItemHandler, IProxy {
        @Override
        public IItemHandler get() { return delegate; }

        private boolean allowed(BlockPos position) {
            ServerLevel level = (ServerLevel) target.getLevel();
            Access actor = ACTOR.get();
            if (actor != null) return FlanBridge.can(actor.player(), level, position, ClaimPermission.OPENCONTAINER);
            return source.getLevel() == level && FlanBridge.canAutomate(level, source.getBlockPos(), position, ClaimPermission.OPENCONTAINER);
        }

        private boolean allowed() {
            if (target.isRemoved()) return false;
            Access access = ACTOR.get();
            return access == null ? checkPermission() : access.permissions().computeIfAbsent(target, ignored -> checkPermission());
        }

        private boolean checkPermission() {
            if (!allowed(target.getBlockPos())) return false;
            var state = target.getBlockState();
            return !(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                    || allowed(target.getBlockPos().relative(ChestBlock.getConnectedDirection(state)));
        }

        @Override
        public int getSlots() { return delegate.getSlots(); }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ACTOR.get() == null || allowed() ? delegate.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return allowed() ? delegate.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return allowed() ? delegate.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) { return delegate.getSlotLimit(slot); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) { return allowed() && delegate.isItemValid(slot, stack); }
    }
}
