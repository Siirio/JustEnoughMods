package com.siirio.jemserver.client;

import com.siirio.jemserver.ClaimBoundaryNetwork;
import com.siirio.jemserver.ClaimBoundaryGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.siirio.jemserver.client.ui.JemPalette;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_server", value = Dist.CLIENT)
public final class ClaimBoundaryClient {
    private static final int PREVIEW_INTERVAL = 4;
    private static final float OUTLINE_WIDTH = 7;
    private static final float LINE_WIDTH = 3;
    private static final float GROUND_OFFSET = 0.02F;
    private static ClaimBoundaryNetwork.Snapshot snapshot;
    private static Boundary preview;
    private static BlockPos previewFirst;
    private static BlockPos previewSecond;
    private record Boundary(int style, String name, ClaimBoundaryGeometry.Mesh mesh) {}
    private static java.util.List<Boundary> boundaries = java.util.List.of();

    private ClaimBoundaryClient() {}
    public static void accept(ClaimBoundaryNetwork.Snapshot value) {
        snapshot = value;
        preview = null; previewFirst = null; previewSecond = null;
        boundaries = value.bounds().stream().collect(java.util.stream.Collectors.groupingBy(ClaimBoundaryNetwork.Bounds::owner)).values().stream()
                .map(group -> new Boundary(group.stream().mapToInt(ClaimBoundaryNetwork.Bounds::style).min().orElseThrow(), group.get(0).name(), ClaimBoundaryGeometry.union(group.stream()
                        .map(bounds -> new ClaimBoundaryGeometry.Rectangle(bounds.minX(), bounds.minZ(), bounds.maxX() + 1, bounds.maxZ() + 1)).toList()))).toList();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        var minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END) return;
        if (minecraft.player == null || minecraft.level == null) { snapshot = null; boundaries = java.util.List.of(); preview = null; return; }
        if (snapshot == null || !snapshot.dimension().equals(minecraft.level.dimension().location())) return;
        var stack = minecraft.player.getMainHandItem();
        if (!stack.is(Items.STICK) || !stack.hasCustomHoverName() || !stack.getHoverName().getString().equals(snapshot.toolName())) return;
        if (minecraft.player.tickCount % PREVIEW_INTERVAL != 0) return;
        BlockPos target = minecraft.hitResult instanceof BlockHitResult hit ? hit.getBlockPos() : null;
        if (snapshot.first() != null) {
            BlockPos second = snapshot.second() != null ? snapshot.second() : target;
            if (second != null) {
                int minX = Math.min(snapshot.first().getX(), second.getX()), maxX = Math.max(snapshot.first().getX(), second.getX());
                int minZ = Math.min(snapshot.first().getZ(), second.getZ()), maxZ = Math.max(snapshot.first().getZ(), second.getZ());
                long width = (long) maxX - minX + 1, length = (long) maxZ - minZ + 1;
                boolean overlap = snapshot.bounds().stream().anyMatch(bounds -> bounds.style() == 2 && minX <= bounds.maxX() && maxX >= bounds.minX() && minZ <= bounds.maxZ() && maxZ >= bounds.minZ());
                if (!snapshot.first().equals(previewFirst) || !second.equals(previewSecond)) {
                    var rectangles = new java.util.ArrayList<ClaimBoundaryGeometry.Rectangle>();
                    snapshot.bounds().stream().filter(bounds -> bounds.style() == 0)
                            .map(bounds -> new ClaimBoundaryGeometry.Rectangle(bounds.minX(), bounds.minZ(), bounds.maxX() + 1, bounds.maxZ() + 1))
                            .forEach(rectangles::add);
                    rectangles.add(new ClaimBoundaryGeometry.Rectangle(minX, minZ, maxX + 1, maxZ + 1));
                    preview = new Boundary(0, "", ClaimBoundaryGeometry.union(rectangles));
                    previewFirst = snapshot.first(); previewSecond = second;
                }
                minecraft.player.displayClientMessage(Component.translatable("jem.claim.preview", width, length, width * length, snapshot.remaining())
                        .append(overlap ? Component.translatable("jem.claim.overlap") : Component.empty()), true);
            }
        }
        for (var bounds : boundaries) {
            if (snapshot.first() == null && target != null && edge(bounds, target)) {
                minecraft.player.displayClientMessage(Component.translatable("jem.claim.relation." + bounds.style()).append(": " + bounds.name()), true);
            }
        }
    }

    private static boolean edge(Boundary boundary, BlockPos target) {
        return boundary.mesh().edges().stream().anyMatch(edge -> target.getX() >= edge.x1() - 1 && target.getX() <= edge.x2()
                && target.getZ() >= edge.z1() - 1 && target.getZ() <= edge.z2());
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        var minecraft = Minecraft.getInstance();
        if (snapshot == null || minecraft.player == null || minecraft.level == null
                || !snapshot.dimension().equals(minecraft.level.dimension().location())) return;
        var stack = minecraft.player.getMainHandItem();
        if (!stack.is(Items.STICK) || !stack.hasCustomHoverName() || !stack.getHoverName().getString().equals(snapshot.toolName())) return;
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        float lineWidth = RenderSystem.getShaderLineWidth();
        var shader = RenderSystem.getShader();
        pose.pushPose();
        try {
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            for (int pass = 0; pass < 2; pass++) {
                boolean backing = pass == 0;
                RenderSystem.lineWidth(backing ? OUTLINE_WIDTH : LINE_WIDTH);
                var buffer = Tesselator.getInstance().getBuilder();
                buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
                boolean selecting = preview != null && snapshot.first() != null;
                for (var boundary : boundaries) {
                    if (!selecting || boundary.style() != 0) draw(buffer, pose.last().pose(), pose.last().normal(), boundary, camera, backing);
                }
                if (selecting) draw(buffer, pose.last().pose(), pose.last().normal(), preview, camera, backing);
                Tesselator.getInstance().end();
            }
        } finally {
            RenderSystem.lineWidth(lineWidth);
            RenderSystem.setShader(() -> shader);
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.depthMask(depthMask);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            pose.popPose();
        }
    }

    private static void draw(BufferBuilder buffer, org.joml.Matrix4f pose, org.joml.Matrix3f normal, Boundary boundary, net.minecraft.world.phys.Vec3 camera, boolean backing) {
        var player = Minecraft.getInstance().player;
        int radius = snapshot.radius();
        int color = backing ? JemPalette.OUTLINE : switch (boundary.style()) {
            case 0 -> ChatFormatting.GREEN.getColor();
            case 1 -> ChatFormatting.YELLOW.getColor();
            default -> ChatFormatting.RED.getColor();
        };
        int red = color >> 16 & 255, green = color >> 8 & 255, blue = color & 255;
        float height = (float) (player.getBlockY() + GROUND_OFFSET - camera.y);
        for (var edge : boundary.mesh().edges()) {
            double x1 = Math.max(edge.x1(), player.getBlockX() - radius);
            double x2 = Math.min(edge.x2(), player.getBlockX() + radius);
            double z1 = Math.max(edge.z1(), player.getBlockZ() - radius);
            double z2 = Math.min(edge.z2(), player.getBlockZ() + radius);
            if (x1 > x2 || z1 > z2 || (x1 == x2 && z1 == z2)) continue;
            float normalX = (float) Math.signum(x2 - x1), normalZ = (float) Math.signum(z2 - z1);
            buffer.vertex(pose, (float) (x1 - camera.x), height, (float) (z1 - camera.z)).color(red, green, blue, 255).normal(normal, normalX, 0, normalZ).endVertex();
            buffer.vertex(pose, (float) (x2 - camera.x), height, (float) (z2 - camera.z)).color(red, green, blue, 255).normal(normal, normalX, 0, normalZ).endVertex();
        }
    }
}
