package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.TomsFilterEndpointAccess;
import com.tom.storagemod.tile.FilteredInventoryCableConnectorBlockEntity;
import com.tom.storagemod.util.FilteredInventoryHandler;
import com.tom.storagemod.util.ItemPredicate;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.tom.storagemod.tile.FilteredInventoryCableConnectorBlockEntity", remap = false)
public abstract class TomsFilterEndpointQuestMixin {
    @Redirect(method = "lambda$applyFilter$1", at = @At(value = "NEW", target = "com/tom/storagemod/util/FilteredInventoryHandler"))
    private FilteredInventoryHandler jem$nativeFilteredHandler(IItemHandler parent, ItemPredicate filter, boolean keepLast) {
        FilteredInventoryHandler handler = new FilteredInventoryHandler(parent, filter, keepLast);
        ((TomsFilterEndpointAccess)handler).jem$bindFilterEndpoint((FilteredInventoryCableConnectorBlockEntity)(Object)this);
        return handler;
    }
}
