package com.justenoughmods.achievementguide.integration.photo;

import com.justenoughmods.achievementguide.integration.home.HomeInteractionProof;
import io.github.mortuusars.exposure.world.camera.CameraId;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.entity.CameraHolder;
import io.github.mortuusars.exposure.world.entity.CameraStandEntity;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

/** Cause scopes wrap the original delayed-redstone and timer release calls once. */
public final class PhotoCameraProof {
    public enum Cause { REDSTONE, TIMER }
    private record Shot(CameraHolder holder, ServerPlayer executor, ItemStack camera, Cause cause) {}
    private static final ThreadLocal<Deque<Cause>> CAUSES = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<Shot>> SHOTS = ThreadLocal.withInitial(ArrayDeque::new);

    public static void cause(Cause cause) {
        if (CAUSES.get().size() >= 16) CAUSES.get().clear();
        CAUSES.get().push(cause);
    }
    public static void releaseDone() { CAUSES.get().poll(); }
    public static void shot(CameraHolder holder, ServerPlayer executor, ItemStack stack) {
        if (SHOTS.get().size() >= 16) SHOTS.get().clear();
        SHOTS.get().push(new Shot(holder, executor, stack, CAUSES.get().peek()));
    }
    public static void shotDone() { SHOTS.get().poll(); }
    public static void frame(ServerPlayer actor, CameraHolder holder, ItemStack camera, Frame frame) {
        Shot shot = SHOTS.get().peek();
        if (shot == null || shot.cause == null || shot.holder != holder || shot.camera != camera
                || shot.executor instanceof FakePlayer || !(holder instanceof CameraStandEntity stand)
                || HomeInteractionProof.actor(actor) == null || actor.level() != stand.level()
                || !stand.getOwnerPlayerUuid().equals(actor.getUUID()) || !frame.isTakenBy(actor)
                || CameraId.ofStack(camera).equals(CameraId.NIL)) return;
        frame.identifier().getId().filter(id -> !id.isBlank()).ifPresent(id ->
                PhotoImages.output(actor, id, frame.type(), shot.executor.getUUID(), false, "v3_home_remote_frame"));
    }
    public static void clear() { CAUSES.remove(); SHOTS.remove(); }
    private PhotoCameraProof() {}
}
