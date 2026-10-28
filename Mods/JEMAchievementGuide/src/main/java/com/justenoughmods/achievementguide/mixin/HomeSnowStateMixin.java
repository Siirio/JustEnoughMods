package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeSnowState;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "snownee.snow.block.entity.SnowBlockEntity", remap = false)
public abstract class HomeSnowStateMixin implements HomeSnowState {
    @Shadow public abstract BlockState getContainedState();
}
