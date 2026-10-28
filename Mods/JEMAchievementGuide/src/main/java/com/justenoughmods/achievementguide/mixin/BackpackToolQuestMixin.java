package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.upgrades.toolswapper.ToolSwapperUpgradeWrapper", remap = false)
public abstract class BackpackToolQuestMixin {
    @Unique private ItemStack jem$previous = ItemStack.EMPTY;

    @Inject(method = "tryToSwapTool", at = @At("HEAD"))
    private void jem$beforeSwap(Player player, BlockPos pos, BlockState state, double speed, ItemStack held,
                                CallbackInfoReturnable<Boolean> callback) {
        jem$previous = player.getMainHandItem().copy();
    }

    @Inject(method = "tryToSwapTool", at = @At("RETURN"))
    private void jem$afterSwap(Player player, BlockPos pos, BlockState state, double speed, ItemStack held,
                               CallbackInfoReturnable<Boolean> callback) {
        ItemStack before = jem$previous;
        jem$previous = ItemStack.EMPTY;
        ItemStack after = player.getMainHandItem();
        if (player instanceof ServerPlayer actor && callback.getReturnValue() && ((IUpgradeWrapper) this).isEnabled()
                && !after.isEmpty() && !ItemStack.isSameItemSameTags(before, after)) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_store_tools");
        }
    }
}
