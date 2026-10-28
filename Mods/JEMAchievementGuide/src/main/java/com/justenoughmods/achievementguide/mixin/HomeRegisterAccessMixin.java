package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeRegisterAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.mcreator.dustydecorations.world.inventory.CashRegisterMenu", remap = false)
public interface HomeRegisterAccessMixin extends HomeRegisterAccess {
    @Override @Accessor("boundBlockEntity") BlockEntity jem$homeRegister();
    @Override @Accessor("bound") boolean jem$homeBound();
}
