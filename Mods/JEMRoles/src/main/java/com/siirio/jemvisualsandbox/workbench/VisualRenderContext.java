package com.siirio.jemvisualsandbox.workbench;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Presentation inputs, independent of entity lookup, scene queues and gameplay. */
public record VisualRenderContext(PoseStack poses, Vec3 camera, float partialTick, boolean miniature) {
    public PoseStack getPoseStack() { return poses; }
    public CameraPosition getCamera() { return new CameraPosition(camera); }
    public float getPartialTick() { return partialTick; }
    public record CameraPosition(Vec3 position) { public Vec3 getPosition() { return position; } }
    static VisualRenderContext world(RenderLevelStageEvent event) {
        return new VisualRenderContext(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick(), false);
    }
    double surface(Vec3 point) {
        if (miniature) return .045;
        var level = Minecraft.getInstance().level;
        if (level == null) return Double.NaN;
        BlockPos base = BlockPos.containing(point);
        for (int dy = 3; dy >= -4; dy--) {
            var at = base.offset(0, dy, 0);
            var shape = level.getBlockState(at).getCollisionShape(level, at);
            if (!shape.isEmpty()) return at.getY() + shape.max(net.minecraft.core.Direction.Axis.Y) + .045;
        }
        return Double.NaN;
    }
}
