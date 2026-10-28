package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateCartProofs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecartItem.class)
public abstract class CreateCartPlacementQuestMixin {
    @Redirect(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$nativeCartSupply(Level world, Entity cart, UseOnContext context) {
        boolean accepted = world.addFreshEntity(cart);
        if (accepted && ModList.get().isLoaded("create")) CreateCartProofs.placed(world, cart, context);
        return accepted;
    }
}
