package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestMotion;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class WildlifeQuestPlayerSettleMixin {
    @Inject(method = "doTick", at = @At("RETURN"))
    private void jem$settled(CallbackInfo callback) { ServerPlayer actor = (ServerPlayer)(Object)this; WildlifeQuestMotion.settled(actor);
        if (actor.getPersistentData().contains(com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof.KEY)) {
            var receipt = com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof.data(actor).getCompound("guided_treasure");
            if (receipt.hasUUID("bird") && actor.serverLevel().getBlockEntity(net.minecraft.core.BlockPos.of(receipt.getLong("pos"))) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)
                com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestFinders.opened(chest, actor);
        } }
}
