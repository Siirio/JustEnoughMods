package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.tom.storagemod.gui.StorageTerminalMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Wraps the one native remote use, then verifies the actual newly opened, connected menu. */
public final class WirelessQuestProof {
    private WirelessQuestProof() {}

    public static InteractionResult use(BlockState state, Level level, Player player,
                                        InteractionHand hand, BlockHitResult hit) {
        var previous = player.containerMenu;
        InteractionResult result = state.use(level, player, hand, hit);
        if (player instanceof ServerPlayer actor && result.consumesAction()
                && player.containerMenu != previous && player.containerMenu instanceof StorageTerminalMenu menu) {
            var terminal = menu.getTerminal();
            if (terminal != null && terminal.getLevel() == level && terminal.getBlockPos().equals(hit.getBlockPos())
                    && terminal.canInteractWith(player)
                    && ((StorageNetworkAccess) terminal).jem$questStorageNetwork() != null) {
                QuestActionTrigger.INSTANCE.emit(actor, "wireless_storage");
            }
        }
        return result;
    }
}
