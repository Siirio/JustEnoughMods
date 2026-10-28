package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ServerPlayerGameMode.class)
public abstract class WorldPrehistoricQuestBreakMixin {
 @Shadow protected ServerPlayer player;
 @Inject(method="destroyBlock",at=@At("HEAD"))private void jem$before(BlockPos pos,CallbackInfoReturnable<Boolean> ci){WorldPrehistoricQuestDrops.beginBreak(player,pos);}
 @Inject(method="destroyBlock",at=@At("RETURN"))private void jem$after(BlockPos pos,CallbackInfoReturnable<Boolean> ci){WorldPrehistoricQuestDrops.endBreak();}
}
