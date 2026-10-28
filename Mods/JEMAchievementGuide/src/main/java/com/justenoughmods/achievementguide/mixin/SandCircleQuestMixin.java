package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Preserves the native April Fools/config admission and requires its actual block transformation. */
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.event.ServerEvents", remap = false)
public abstract class SandCircleQuestMixin {
    @Redirect(method = "onUseItemOnBlock", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 2)
    private boolean jem$circleActuallyDrawn(Level level, BlockPos position, BlockState result,
                                           PlayerInteractEvent.RightClickBlock event) {
        BlockState before = level.getBlockState(position);
        boolean changed = level.setBlockAndUpdate(position, result);
        String block = BuiltInRegistries.BLOCK.getKey(result.getBlock()).toString();
        boolean supported = before.is(Blocks.SAND) && block.equals("alexsmobs:sand_circle")
                || before.is(Blocks.RED_SAND) && block.equals("alexsmobs:red_sand_circle");
        if (changed && supported && event.getEntity() instanceof ServerPlayer actor && actor.level() == level
                && event.getPos().equals(position) && event.getItemStack().is(Items.STICK)
                && level.getBlockState(position).equals(result)) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_fauna_serious_circle");
        }
        return changed;
    }
}
