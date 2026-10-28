package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestFinders;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ChestBlockEntity.class)
public abstract class WildlifeQuestTreasureOpenMixin {
    @Inject(method = "startOpen", at = @At("RETURN"))
    private void jem$opened(Player player, CallbackInfo callback) { if (player instanceof ServerPlayer actor) WildlifeQuestFinders.opened((ChestBlockEntity)(Object)this, actor); }
}
