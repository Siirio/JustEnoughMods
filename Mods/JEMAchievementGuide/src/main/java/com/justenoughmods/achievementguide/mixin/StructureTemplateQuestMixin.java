package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.StructureLootOrigin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateQuestMixin {
    @Redirect(method = "placeInWorld", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/block/entity/BlockEntity;load(Lnet/minecraft/nbt/CompoundTag;)V"))
    private void jem$nativeTemplateOrigin(BlockEntity entity, CompoundTag tag) {
        entity.load(tag);
        if (entity instanceof RandomizableContainerBlockEntity container && tag.contains("LootTable")) {
            StructureLootOrigin.generated(container);
        }
    }
}
