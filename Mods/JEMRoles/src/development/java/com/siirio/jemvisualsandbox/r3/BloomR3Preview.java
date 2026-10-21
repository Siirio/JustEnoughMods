package com.siirio.jemvisualsandbox.r3;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Standalone sandbox presentation only; no packets, gameplay or world writes. */
@Mod.EventBusSubscriber(modid="jem_roles", value = Dist.CLIENT)
public final class BloomR3Preview {
    private static final KeyMapping PREVIEW = new KeyMapping("key.jem_visual_sandbox.bloom_r3",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, "key.categories.jem_visual_sandbox");
    private static BloomR3Mesh mesh;
    private static Vec3 origin;
    private static ClientLevel previewLevel;
    private static long startTick;

    private BloomR3Preview() {}

    @Mod.EventBusSubscriber(modid="jem_roles", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        private Keys() {}
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(PREVIEW); }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level != previewLevel || !(com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.supported())) clear();
        while (PREVIEW.consumeClick()) {
            var status=com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.status();
            if(status==null || !status.developerMode())continue;
            if (client.player == null || client.level == null || client.screen != null
                    || !(com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.supported())) continue;
            if (origin != null) {
                clear();
                message(client, "message.jem_visual_sandbox.r3_stopped");
                continue;
            }
            var hit = client.player.pick(8.0D, 0.0F, false);
            if (!(hit instanceof BlockHitResult block) || block.getDirection() != Direction.UP
                    || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
                message(client, "message.jem_visual_sandbox.r3_aim");
                continue;
            }
            try {
                mesh = BloomR3Mesh.load();
                // Require the real materials; missing assets are errors, never substituted.
                for (var texture : BloomR3Mesh.TEXTURES)
                    client.getResourceManager().getResourceOrThrow(texture);
            } catch (IOException error) {
                clear();
                LogUtils.getLogger().error("Cannot load sandbox Bloom R3 candidate", error);
                message(client, "message.jem_visual_sandbox.r3_error");
                continue;
            }
            origin = block.getLocation().add(0.0D, 0.025D, 0.0D);
            previewLevel = client.level;
            startTick = client.level.getGameTime();
            message(client, "message.jem_visual_sandbox.r3_started");
        }
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    private static void clear() { origin = null; previewLevel = null; mesh = null; }

    private static void message(Minecraft client, String key) {
        client.player.displayClientMessage(Component.translatable(key), true);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || origin == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level != previewLevel || !(com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.supported()) || mesh == null) return;
        double seconds = ((client.level.getGameTime() - startTick + event.getPartialTick()) % 60.0D) / 20.0D;
        if (seconds >= 2.2D) return;
        float sample = (float)(seconds / mesh.step);
        int first = Mth.clamp((int)sample, 0, mesh.poses.length - 2);
        float blend = sample - first;
        float[] before = mesh.poses[first];
        float[] after = mesh.poses[first + 1];
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(origin.x - camera.x, origin.y - camera.y, origin.z - camera.z);
        PoseStack.Pose pose = poses.last();
        int light = LevelRenderer.getLightColor(client.level, BlockPos.containing(origin).above());
        var buffers = client.renderBuffers().bufferSource();
        for (int material = 0; material < BloomR3Mesh.TEXTURES.length; material++) {
            RenderType type = RenderType.entityCutoutNoCull(BloomR3Mesh.TEXTURES[material]);
            VertexConsumer out = buffers.getBuffer(type);
            for (int face = 0; face < mesh.materials.length; face++) {
                if (mesh.materials[face] != material) continue;
                int base = face * 12;
                float ax = coordinate(before, after, base + 3, blend) - coordinate(before, after, base, blend);
                float ay = coordinate(before, after, base + 4, blend) - coordinate(before, after, base + 1, blend);
                float az = coordinate(before, after, base + 5, blend) - coordinate(before, after, base + 2, blend);
                float bx = coordinate(before, after, base + 6, blend) - coordinate(before, after, base, blend);
                float by = coordinate(before, after, base + 7, blend) - coordinate(before, after, base + 1, blend);
                float bz = coordinate(before, after, base + 8, blend) - coordinate(before, after, base + 2, blend);
                float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
                float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
                if (length < 0.000001F) continue;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int offset = base + vertex * 3;
                    out.vertex(pose.pose(), coordinate(before, after, offset, blend),
                                    coordinate(before, after, offset + 1, blend), coordinate(before, after, offset + 2, blend))
                            .color(255, 255, 255, 255).uv(mesh.uv[face][vertex * 2], mesh.uv[face][vertex * 2 + 1])
                            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                            .normal(pose.normal(), nx / length, ny / length, nz / length).endVertex();
                }
            }
            buffers.endBatch(type);
        }
        poses.popPose();
    }

    private static float coordinate(float[] before, float[] after, int index, float blend) {
        return Mth.lerp(blend, before[index], after[index]);
    }
}
