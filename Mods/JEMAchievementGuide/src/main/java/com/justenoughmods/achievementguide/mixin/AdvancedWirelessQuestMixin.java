package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.WirelessQuestProof;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.tom.storagemod.item.AdvWirelessTerminalItem", remap = false)
public abstract class AdvancedWirelessQuestMixin {
    @Redirect(method = "activateTerminal", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/block/state/BlockState;m_60664_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private static InteractionResult jem$openedBoundNetwork(BlockState state, Level level, Player player,
                                                            InteractionHand hand, BlockHitResult hit) {
        return WirelessQuestProof.use(state, level, player, hand, hit);
    }
}
