package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.StructureLootProof;
import com.justenoughmods.achievementguide.integration.StructureLootOrigin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class StructureLootQuestMixin {
    @Inject(method = "setLootTable(Lnet/minecraft/resources/ResourceLocation;J)V", at = @At("TAIL"))
    private void jem$nativeGeneratedOrigin(ResourceLocation table, long seed, CallbackInfo callback) {
        if (table != null) StructureLootOrigin.generated((RandomizableContainerBlockEntity) (Object) this);
    }

    @Redirect(method = "unpackLootTable", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/storage/loot/LootTable;fill(Lnet/minecraft/world/Container;Lnet/minecraft/world/level/storage/loot/LootParams;J)V"))
    private void jem$generatedLoot(LootTable table, Container container, LootParams params, long seed, Player player) {
        table.fill(container, params, seed);
        if (container == (Object) this) StructureLootProof.generated((RandomizableContainerBlockEntity) (Object) this, player);
    }
}
