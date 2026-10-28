package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastProof;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Block.class)
public abstract class MagicLastResourceMiningQuestMixin {
    @Inject(method = "playerDestroy", at = @At("RETURN"))
    private void jem$mined(Level level, Player user, BlockPos pos, BlockState original, BlockEntity entity, ItemStack tool, CallbackInfo callback) { if (MagicLastProof.real(user) && level == user.level() && BuiltInRegistries.BLOCK.getKey(original.getBlock()).toString().equals("irons_spellbooks:arcane_debris") && tool.isCorrectToolForDrops(original) && level.getBlockState(pos).getBlock() != original.getBlock()) MagicLastProof.emit((ServerPlayer)user, "v3_magic_arcane_resources_mined"); }
}
