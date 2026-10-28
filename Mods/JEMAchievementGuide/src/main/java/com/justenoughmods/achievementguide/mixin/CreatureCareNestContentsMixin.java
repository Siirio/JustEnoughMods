package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareLineage;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.primal.block_entity.NestBlockEntity", remap = false)
public abstract class CreatureCareNestContentsMixin {
    @Inject(method = "setItem", at = @At("HEAD"))
    private void jem$changed(ItemStack stack, CallbackInfo callback) {
        BlockEntity self = (BlockEntity)(Object)this;
        if (self.getLevel() instanceof ServerLevel level && level.getServer().isSameThread()) {
            CreatureCareLineage.get(level).eggs(level, self.getBlockPos(), List.of());
        }
    }
}
