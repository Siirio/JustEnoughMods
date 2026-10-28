package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeInteractionProof;
import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.ordana.immersive_weathering.events.ModEvents", remap = false)
public abstract class HomeWeatheringQuestMixin {
    @Unique private static final ThreadLocal<Deque<HomeInteractionProof.Call>> jem$repairs = ThreadLocal.withInitial(HomeInteractionProof::calls);

    @Inject(method = {"brickRepair", "barkRepairing"}, at = @At("HEAD"))
    private static void jem$before(Item item, ItemStack stack, BlockPos pos, BlockState state, Player player,
                                  Level level, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        HomeInteractionProof.begin(jem$repairs.get(), level, pos, player, hand);
    }

    // The first bark setter is the installed stripped-log restoration branch. The second
    // setter makes wood from a log and deliberately supplies no restoration evidence.
    @Redirect(method = {"brickRepair", "barkRepairing"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 0, remap = false))
    private static boolean jem$nativeCommit(Level level, BlockPos pos, BlockState result) {
        boolean accepted = level.setBlockAndUpdate(pos, result);
        HomeInteractionProof.Call call = jem$repairs.get().peek();
        if (call != null && call.level == level && call.pos.equals(pos)) {
            call.committed = result;
            call.accepted = accepted;
        }
        return accepted;
    }

    @Inject(method = "brickRepair", at = @At("RETURN"))
    private static void jem$brick(Item item, ItemStack stack, BlockPos pos, BlockState state, Player player,
                                 Level level, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        HomeInteractionProof.Call call = HomeInteractionProof.finish(jem$repairs.get(), level, pos, player, callback.getReturnValue());
        if (jem$committed(call) && HomeInteractionProof.item(call.material).equals("immersive_weathering:mortar"))
            HomeInteractionProof.emit(player, "v3_home_restore");
    }

    @Inject(method = "barkRepairing", at = @At("RETURN"))
    private static void jem$bark(Item item, ItemStack stack, BlockPos pos, BlockState state, Player player,
                                Level level, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        HomeInteractionProof.Call call = HomeInteractionProof.finish(jem$repairs.get(), level, pos, player, callback.getReturnValue());
        if (jem$committed(call)) HomeInteractionProof.emit(player, "v3_home_restore_bark");
    }

    @Unique private static boolean jem$committed(HomeInteractionProof.Call call) {
        return call != null && call.accepted && call.committed != null && call.committed.getBlock() != call.before.getBlock()
                && call.level.getBlockState(call.pos).equals(call.committed) && call.debitedOne();
    }
}
