package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.beach.BeachGoalRun;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.satisfy.beachparty.core.block.entity.BeachGoalBlockEntity;
import net.satisfy.beachparty.core.entity.BeachBallEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.satisfy.beachparty.core.block.entity.BeachGoalBlockEntity", remap = false)
public abstract class BeachGoalQuestMixin implements BeachGoalRun.Access {
    @Unique private final BeachGoalRun jem$nativeGoal = new BeachGoalRun();

    @Override
    public BeachGoalRun jem$nativeGoalRun() { return jem$nativeGoal; }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private static void jem$begin(Level level, BlockPos pos, BlockState state, BeachGoalBlockEntity goal, CallbackInfo callback) {
        ((BeachGoalRun.Access)goal).jem$nativeGoalRun().clear();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/satisfy/beachparty/core/entity/BeachBallEntity;m_20191_()Lnet/minecraft/world/phys/AABB;"), remap = false)
    private static AABB jem$actualBall(BeachBallEntity ball, Level level, BlockPos pos, BlockState state, BeachGoalBlockEntity goal) {
        ((BeachGoalRun.Access)goal).jem$nativeGoalRun().iterated(ball);
        return ball.getBoundingBox();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/shapes/Shapes;m_83157_(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/BooleanOp;)Z"), remap = false)
    private static boolean jem$actualIntersection(VoxelShape goalShape, VoxelShape ballShape, BooleanOp operation,
                                                  Level level, BlockPos pos, BlockState state, BeachGoalBlockEntity goal) {
        boolean result = Shapes.joinIsNotEmpty(goalShape, ballShape, operation);
        ((BeachGoalRun.Access)goal).jem$nativeGoalRun().shapeResult(result);
        return result;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/satisfy/beachparty/core/block/entity/BeachGoalBlockEntity;fireRockets(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V"), remap = false)
    private static void jem$nativeEntry(Level level, BlockPos pos, BlockState state, BeachGoalBlockEntity goal, CallbackInfo callback) {
        ((BeachGoalRun.Access)goal).jem$nativeGoalRun().arm();
    }

    @Redirect(method = "fireRockets", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"), remap = false)
    private boolean jem$acceptedNativeFirework(ServerLevel level, Entity firework) {
        boolean accepted = level.addFreshEntity(firework);
        jem$nativeGoal.inserted(accepted);
        return accepted;
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private static void jem$finished(Level level, BlockPos pos, BlockState state, BeachGoalBlockEntity goal, CallbackInfo callback) {
        BeachGoalRun run = ((BeachGoalRun.Access)goal).jem$nativeGoalRun();
        if (level instanceof ServerLevel server) run.completed(server, goal.hasBeachBall());
        else run.clear();
    }
}
