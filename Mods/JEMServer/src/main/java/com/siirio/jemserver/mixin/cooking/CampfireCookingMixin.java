package com.siirio.jemserver.mixin.cooking;

import com.siirio.jemserver.smp.events.CookingStations;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireCookingMixin {
    @Redirect(method = "cookTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"))
    private static void jem$cooked(Level level, double x, double y, double z, ItemStack output,
                                   Level tickLevel, BlockPos pos, BlockState state, CampfireBlockEntity station) {
        CookingStations.produced(station, output.copy());
        Containers.dropItemStack(level, x, y, z, output);
    }
}
