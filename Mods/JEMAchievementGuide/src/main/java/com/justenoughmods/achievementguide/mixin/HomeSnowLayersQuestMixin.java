package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeInteractionProof;
import com.justenoughmods.achievementguide.integration.home.HomeItemUseProof;
import com.justenoughmods.achievementguide.integration.home.HomeSnowState;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "snownee.snow.Hooks", remap = false)
public abstract class HomeSnowLayersQuestMixin {
    @Unique private static final ThreadLocal<Deque<HomeSnowState.Snapshot>> jem$snow = ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "placeLayersOn", at = @At("HEAD"))
    private static void jem$before(Level level, BlockPos pos, int layers, boolean falling, BlockPlaceContext context,
                                  boolean sound, boolean convert, CallbackInfoReturnable<Boolean> callback) {
        if (jem$snow.get().size() >= 16) jem$snow.get().clear();
        BlockState before = level.getBlockState(pos);
        BlockState original = jem$raw(level, pos, before);
        ServerPlayer player = HomeItemUseProof.actor();
        jem$snow.get().push(new HomeSnowState.Snapshot(level, pos.immutable(), original, jem$layers(before),
                !falling && context != null && context.getPlayer() == player ? player : null));
    }

    @Inject(method = "placeLayersOn", at = @At("RETURN"))
    private static void jem$after(Level level, BlockPos pos, int layers, boolean falling, BlockPlaceContext context,
                                 boolean sound, boolean convert, CallbackInfoReturnable<Boolean> callback) {
        HomeSnowState.Snapshot proof = jem$snow.get().poll();
        if (proof == null || proof.player() == null || !callback.getReturnValue() || proof.level() != level
                || !(proof.original().getBlock() instanceof FenceBlock || proof.original().getBlock() instanceof WallBlock)) return;
        BlockState after = level.getBlockState(proof.pos());
        if (jem$layers(after) > proof.layers() && jem$raw(level, proof.pos(), after).equals(proof.original()))
            HomeItemUseProof.defer(proof.player(),
                    () -> jem$layers(level.getBlockState(proof.pos())) > proof.layers()
                            && jem$raw(level, proof.pos(), level.getBlockState(proof.pos())).equals(proof.original()),
                    () -> HomeInteractionProof.emit(proof.player(), "v3_world_snow_garden"));
    }

    @Unique private static BlockState jem$raw(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof HomeSnowState snow ? snow.getContainedState() : state;
    }
    @Unique private static int jem$layers(BlockState state) {
        String value = HomeInteractionProof.property(state, "layers");
        return value.isEmpty() ? 0 : Integer.parseInt(value);
    }
}
