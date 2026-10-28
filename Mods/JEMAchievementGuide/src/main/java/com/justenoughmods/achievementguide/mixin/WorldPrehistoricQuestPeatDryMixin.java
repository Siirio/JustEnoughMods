package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="net.mcreator.unearthedjourney.procedures.PeatPieblockBlockAddedProcedure",remap=false)
public abstract class WorldPrehistoricQuestPeatDryMixin {
 @Redirect(method="lambda$execute$0",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/LevelAccessor;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))private static boolean jem$nativeDry(LevelAccessor world,BlockPos pos,BlockState dried,int flags){var before=world.getBlockState(pos);var origin=world instanceof ServerLevel level?WorldPrehistoricQuestOrigins.get(level,pos,"Placed"):new net.minecraft.nbt.CompoundTag();boolean accepted=world.setBlock(pos,dried,flags);if(accepted && world instanceof ServerLevel level && origin.hasUUID("actor") && before.is(net.mcreator.unearthedjourney.init.UnearthedJourneyModBlocks.PEAT_PIE.get()) && dried.is(net.mcreator.unearthedjourney.init.UnearthedJourneyModBlocks.DRIED_PEAT_PIE.get()) && level.getBlockState(pos).equals(dried)){origin.putString("block","unearthed_journey:dried_peat_pie");WorldPrehistoricQuestOrigins.put(level,pos,"Dried",origin);}return accepted;}
}
