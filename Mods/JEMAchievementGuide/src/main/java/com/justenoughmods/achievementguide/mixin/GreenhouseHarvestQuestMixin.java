package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.satisfy.farm_and_charm.core.block.crops.TomatoCropBlock;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Credit only a successful native mature-crop break under the actual native greenhouse predicate. */
@Mixin(ServerPlayerGameMode.class)
public abstract class GreenhouseHarvestQuestMixin {
    @Shadow @Final protected ServerPlayer player;
    @Unique private BlockState jem$crop;
    @Unique private ServerLevel jem$world;
    @Unique private boolean jem$covered;
    @Unique private static final Set<String> jem$crops = Set.of("minecraft:wheat", "farm_and_charm:lettuce_crop", "farm_and_charm:tomato_crop");
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void jem$beforeBreak(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        jem$world = player.serverLevel();
        jem$crop = jem$world.getBlockState(pos);
        String id = String.valueOf(ForgeRegistries.BLOCKS.getKey(jem$crop.getBlock()));
        boolean mature = jem$crop.getBlock() instanceof CropBlock crop && crop.isMaxAge(jem$crop)
                || jem$crop.getBlock() instanceof TomatoCropBlock && jem$crop.getValue(TomatoCropBlock.AGE) == 4;
        jem$covered = (jem$crops.contains(id) || jem$crop.getBlock() instanceof TomatoCropBlock) && mature && GreenhouseQuestAccess.jem$covered(jem$world, pos);
    }
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void jem$committedBreak(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ() && jem$covered && jem$world != null && !jem$world.getBlockState(pos).equals(jem$crop))
            QuestActionTrigger.INSTANCE.emit(player, "food/greenhouse_harvest");
        jem$world = null;
        jem$crop = null;
        jem$covered = false;
    }
}
