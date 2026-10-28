package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Exact installed RCH4.6.1 post-success path with explicit actual position and old crop. */
@Pseudo
@Mixin(targets = "io.github.jamalam360.rightclickharvest.RightClickHarvest", remap = false)
public abstract class RightClickGreenhouseHarvestMixin {
    @Unique private static final Set<String> jem$crops = Set.of("minecraft:wheat", "farm_and_charm:lettuce_crop");
    @Inject(method = "completeHarvest", at = @At("RETURN"))
    private static void jem$harvest(Level level, BlockState before, BlockPos pos, Player actor, InteractionHand hand,
                                   ItemStack tool, boolean damageHoe, boolean consumeSeed, Runnable replant,
                                   CallbackInfoReturnable<InteractionResult> callback) {
        if (!(actor instanceof ServerPlayer player) || callback.getReturnValue() != InteractionResult.SUCCESS
                || !jem$crops.contains(String.valueOf(ForgeRegistries.BLOCKS.getKey(before.getBlock())))
                || !(before.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(before)) return;
        BlockState after = level.getBlockState(pos);
        if (after.getBlock() == before.getBlock() && crop.getAge(after) == 0 && !after.equals(before)
                && GreenhouseQuestAccess.jem$covered(level, pos))
            QuestActionTrigger.INSTANCE.emit(player, "food/greenhouse_harvest");
    }
}
