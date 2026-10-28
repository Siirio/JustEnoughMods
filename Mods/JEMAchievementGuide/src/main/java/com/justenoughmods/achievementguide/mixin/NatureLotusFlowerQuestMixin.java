package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.nature.LotusMovementProof;
import net.hibiscus.naturespirit.blocks.LotusFlowerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LotusFlowerBlock.class, remap = false)
public abstract class NatureLotusFlowerQuestMixin {
    @Unique private LotusMovementProof.Step jem$lotusStep;
    @Unique private LotusMovementProof.Descent jem$lotusDescent;

    @Inject(method = "m_214148_(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"))
    private void jem$beginRaise(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo callback) { jem$lotusStep = null; }

    @Redirect(method = "m_214148_(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_6478_(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", remap = false))
    private void jem$actualNativeLift(Player player, MoverType type, Vec3 delta,
                                     ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        jem$lotusStep = LotusMovementProof.move(player, type, delta, LotusMovementProof.probe(level, pos));
    }

    @Inject(method = "m_214148_(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("RETURN"))
    private void jem$committedRaise(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo callback) {
        LotusMovementProof.grew(jem$lotusStep); jem$lotusStep = null;
    }

    @Inject(method = "m_213897_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V", at = @At("HEAD"))
    private void jem$beginLower(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) { jem$lotusDescent = null; }

    @Redirect(method = "m_213897_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_45924_(DDDDZ)Lnet/minecraft/world/entity/player/Player;", remap = false))
    private Player jem$nativeSelectedRider(ServerLevel world, double x, double y, double z, double distance, boolean ignored,
                                          BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Player selected = world.getNearestPlayer(x, y, z, distance, ignored);
        jem$lotusDescent = LotusMovementProof.descending(level, pos, selected);
        return selected;
    }

    @Inject(method = "m_213897_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V", at = @At("RETURN"))
    private void jem$committedLower(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        LotusMovementProof.descended(jem$lotusDescent); jem$lotusDescent = null;
    }
}
