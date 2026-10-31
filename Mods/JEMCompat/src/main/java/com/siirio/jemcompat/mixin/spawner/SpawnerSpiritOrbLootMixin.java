package com.siirio.jemcompat.mixin.spawner;

import com.siirio.jemcompat.feature.spawner.SpawnerSpiritOrbPolicy;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "tictim.paraglider.forge.contents.loot.SpawnerSpiritOrbLoot", remap = false)
public abstract class SpawnerSpiritOrbLootMixin {
    @Inject(method = "doApply", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemcompat$rejectPlayerPlacedSpawner(ObjectArrayList<ItemStack> generatedLoot, LootContext context,
                                                      CallbackInfoReturnable<ObjectArrayList<ItemStack>> callback) {
        if (SpawnerSpiritOrbPolicy.isPlayerPlaced(context)) {
            callback.setReturnValue(generatedLoot);
        }
    }
}
