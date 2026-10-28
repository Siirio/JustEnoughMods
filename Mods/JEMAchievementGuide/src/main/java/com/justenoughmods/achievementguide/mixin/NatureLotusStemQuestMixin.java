package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.nature.LotusMovementProof;
import java.util.Optional;
import net.hibiscus.naturespirit.blocks.LotusStemBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = LotusStemBlock.class, remap = false)
public abstract class NatureLotusStemQuestMixin {
    @Shadow private Optional<BlockPos> getFlowerHeadPos(BlockGetter world, BlockPos pos, Block block) { throw new AssertionError(); }
    @Unique private LotusMovementProof.Probe jem$lotusHead;
    @Unique private LotusMovementProof.Step jem$lotusStep;

    @Inject(method = "grow", at = @At("HEAD"))
    private void jem$beginStemRaise(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo callback) {
        jem$lotusHead = null; jem$lotusStep = null;
    }

    @Redirect(method = "grow", at = @At(value = "INVOKE", target = "Lnet/hibiscus/naturespirit/blocks/LotusStemBlock;getFlowerHeadPos(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)Ljava/util/Optional;"))
    private Optional<BlockPos> jem$nativeConnectedHead(LotusStemBlock stem, BlockGetter world, BlockPos pos, Block block) {
        Optional<BlockPos> head = getFlowerHeadPos(world, pos, block);
        if (world instanceof ServerLevel level && head.isPresent()) jem$lotusHead = LotusMovementProof.probe(level, head.get());
        return head;
    }

    @Redirect(method = "grow", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_6478_(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", remap = false))
    private void jem$actualStemLift(Player player, MoverType type, Vec3 delta) {
        jem$lotusStep = LotusMovementProof.move(player, type, delta, jem$lotusHead);
    }

    @Inject(method = "grow", at = @At("RETURN"))
    private void jem$committedStemRaise(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo callback) {
        LotusMovementProof.grew(jem$lotusStep); jem$lotusHead = null; jem$lotusStep = null;
    }
}
