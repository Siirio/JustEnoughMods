package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.TideCatchProof;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.li64.tide.registries.entities.misc.LootCrateEntity", remap = false)
public abstract class RemainingTideCrateLootMixin {
    @Redirect(method = "addCrateLoot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootTable;m_287188_(Lnet/minecraft/world/Container;Lnet/minecraft/world/level/storage/loot/LootParams;J)V"))
    private void jem$actualLoot(LootTable table, Container target, LootParams params, long seed) {
        int before = jem$count(target);
        table.fill(target, params, seed);
        if (target instanceof BlockEntity crate) TideCatchProof.landed((Entity)(Object)this, crate, jem$count(target) > before);
    }
    private static int jem$count(Container target) {
        if (target.getContainerSize() > 54) return Integer.MAX_VALUE;
        int count = 0;
        for (int i = 0; i < target.getContainerSize(); i++) count += target.getItem(i).getCount();
        return count;
    }
}
