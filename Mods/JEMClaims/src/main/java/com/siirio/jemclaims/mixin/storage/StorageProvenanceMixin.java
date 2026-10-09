package com.siirio.jemclaims.mixin.storage;

import com.siirio.jemclaims.compat.storage.StorageAccess;
import com.siirio.jemclaims.compat.storage.StorageInventoryCache;
import com.tom.storagemod.tile.AbstractInventoryHopperBlockEntity;
import com.tom.storagemod.tile.InventoryConnectorBlockEntity;
import com.tom.storagemod.tile.InventoryProxyBlockEntity;
import com.tom.storagemod.tile.StorageTerminalBlockEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {InventoryConnectorBlockEntity.class, InventoryProxyBlockEntity.class, StorageTerminalBlockEntity.class, AbstractInventoryHopperBlockEntity.class}, remap = false)
public abstract class StorageProvenanceMixin implements StorageInventoryCache {
    @Unique private final Map<LazyOptional<?>, LazyOptional<?>> jemclaims$inventories = new HashMap<>();

    @Override
    public Map<LazyOptional<?>, LazyOptional<?>> jemclaims$inventoryCache() { return jemclaims$inventories; }

    @Redirect(method = "updateServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;"))
    private <T> LazyOptional<T> jemclaims$inventory(BlockEntity target, Capability<T> capability, Direction side) {
        return StorageAccess.capability((BlockEntity) (Object) this, target, capability, side);
    }
}
