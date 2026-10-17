package com.siirio.jemvisualsandbox.workbench;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Pixel cels rendered through the existing sandbox world-presentation hook. */
final class FlameSlashRenderer {
    private static final String[] FILES = {"rising_hook", "return_sweep", "descending_cleave"};
    private static final int[] FRAMES = {10,12,16};
    private static final float[] WIDTHS = {2.4F,3.6F,5.0F};
    private static final List<Slash> ACTIVE = new ArrayList<>();
    private static final class Slash {
        final ClientLevel world;
        final long tick;
        final Vec3 origin, casterStart;
        final float yaw;
        final int hit;
        Vec3 anchor;
        Slash(Minecraft client, SandboxNetwork.Visual packet) {
            world = client.level; tick = world.getGameTime(); origin = packet.origin(); anchor = origin;
            casterStart = client.player.position(); yaw = packet.yaw();
            hit = packet.action() - 2;
        }
    }
    private FlameSlashRenderer() {}
    static void clear() { ACTIVE.clear(); }
    static void receive(SandboxNetwork.Visual packet) {
        Minecraft client = Minecraft.getInstance();
        if (packet.action() < 2 || packet.action() > 4 || client.player == null || client.level == null) return;
        if (ACTIVE.size() >= 12) ACTIVE.remove(0);
        ACTIVE.add(new Slash(client, packet));
    }
    static void render(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        ACTIVE.removeIf(slash -> slash.world != client.level || client.level == null || client.level.getGameTime() - slash.tick >= FRAMES[slash.hit]);
        if (client.player == null || !client.hasSingleplayerServer()) { clear(); return; }
        for (Slash slash : ACTIVE) {
            float age = (float) (slash.world.getGameTime() - slash.tick) + event.getPartialTick();
            if (age < (slash.hit == 2 ? 5 : 4)) slash.anchor = slash.origin.add(client.player.position().subtract(slash.casterStart));
            draw(VisualRenderContext.world(event), slash.anchor, slash.yaw, slash.hit, age);

        }
    }
    static void draw(VisualRenderContext context, Vec3 origin, float yaw, int hit, float age) {
        if (age < 0 || age >= FRAMES[hit]) return;
        Minecraft client = Minecraft.getInstance();
        int frame = Math.min(FRAMES[hit] - 1, (int) age);
        ResourceLocation texture = new ResourceLocation("jem_visual_sandbox", "fire_slash/" + FILES[hit] + ".png");
            Vec3 camera = context.getCamera().getPosition();
            PoseStack poses = context.getPoseStack();
            poses.pushPose();
            poses.translate(origin.x - camera.x, origin.y - camera.y + WIDTHS[hit] / 2 + 0.05, origin.z - camera.z);
            poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
            // Two close crossed planes keep the slash visible obliquely. Their
            // entire silhouette/detail/animation comes from distinct RGBA cels.
            for (int plane = 0; plane < 2; plane++) {
                poses.pushPose();
                if (plane == 1) poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(24));
                var type = RenderType.entityCutoutNoCull(texture);
                var buffers = client.renderBuffers().bufferSource();
                var out = buffers.getBuffer(type);
                var pose = poses.last();
                float top = frame / (float)FRAMES[hit], bottom = (frame + 1) / (float)FRAMES[hit];
                float half = WIDTHS[hit] / 2;
                float[][] vertices = {{-half,-half,0,bottom},{half,-half,1,bottom},{half,half,1,top},{-half,half,0,top}};
                for (float[] vertex : vertices) out.vertex(pose.pose(),vertex[0],vertex[1],0)
                        .color(255,255,255,255).uv(vertex[2],vertex[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(15728880).normal(pose.normal(),0,0,1).endVertex();
                buffers.endBatch(type);
                poses.popPose();
            }
            poses.popPose();
    }
}
