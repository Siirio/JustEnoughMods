package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeChimneyState;
import com.justenoughmods.achievementguide.integration.home.HomeInteractionProof;
import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "io.github.mortuusars.sootychimneys.block.ChimneyBlock", remap = false)
public abstract class HomeChimneyQuestMixin implements HomeChimneyState {
    @Shadow public abstract boolean isDirty();
    @Shadow public abstract boolean isClean();
    @Unique private final ThreadLocal<Deque<HomeInteractionProof.Call>> jem$homeCalls = ThreadLocal.withInitial(HomeInteractionProof::calls);

    @Inject(method = "m_6227_", at = @At("HEAD"))
    private void jem$before(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        HomeInteractionProof.begin(jem$homeCalls.get(), level, pos, player, hand);
    }

    @Inject(method = "m_6227_", at = @At("RETURN"))
    private void jem$after(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                           BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        HomeInteractionProof.Call call = HomeInteractionProof.finish(jem$homeCalls.get(), level, pos, player, callback.getReturnValue());
        if (call == null) return;
        BlockState after = level.getBlockState(pos);
        if (call.material.getItem() instanceof AxeItem && call.before.getBlock() instanceof HomeChimneyState dirty
                && dirty.isDirty() && after.getBlock() instanceof HomeChimneyState clean && clean.isClean()) {
            HomeInteractionProof.emit(player, "v3_home_chimney");
        }
        if (call.material.isEmpty() && HomeInteractionProof.changed(call, after, "blocked"))
            HomeInteractionProof.emit(player, "v3_home_chimney_smoke");
    }
}
