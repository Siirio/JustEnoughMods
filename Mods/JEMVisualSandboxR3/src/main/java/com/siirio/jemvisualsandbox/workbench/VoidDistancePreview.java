package com.siirio.jemvisualsandbox.workbench;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Live physical trajectory and contact-plane marker; teleport safety remains server-owned. */
final class VoidDistancePreview {
    private static ThrownEnderpearl probe;
    private static ProjectileEstimation.Prediction prediction;
    private static boolean supported;
    private static long computedTick = Long.MIN_VALUE;
    private static Vec3 computedStart, computedVelocity;
    private VoidDistancePreview() {}

    static void clear() {
        probe = null; prediction = null; computedStart = computedVelocity = null;
        computedTick = Long.MIN_VALUE; supported = false;
    }

    static void render(RenderLevelStageEvent event, boolean enabled) {
        var client = Minecraft.getInstance();
        if (!enabled || client.player == null || client.level == null || client.screen != null
                || !(com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.supported()) || !client.player.isAlive()) { clear(); return; }
        if (probe == null || probe.level() != client.level || probe.getOwner() != client.player)
            probe = new ThrownEnderpearl(client.level, client.player);
        Vec3 start = ProjectileEstimation.initialPosition(client.player);
        Vec3 velocity = ProjectileEstimation.initialVelocity(client.player);
        if (prediction == null || computedTick != client.level.getGameTime()
                || !start.equals(computedStart) || !velocity.equals(computedVelocity)) {
            prediction = ProjectileEstimation.predict(client.player, probe, 160);
            computedTick = client.level.getGameTime(); computedStart = start; computedVelocity = velocity;
            supported = clearContact(prediction);
        }
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        try {
            Vec3 camera = event.getCamera().getPosition();
            poses.translate(-camera.x, -camera.y, -camera.z);
            var buffers = client.renderBuffers().bufferSource();
            RenderType type = RenderType.lines();
            VertexConsumer vertices = buffers.getBuffer(type);
            List<Vec3> points = prediction.points();
            double distance = 0, nextMarker = .35;
            for (int i = 1; i < points.size(); i++) {
                Vec3 a = points.get(i - 1), b = points.get(i);
                double length = a.distanceTo(b);
                if (length < .0001) continue;
                // Equal world-distance spacing describes the arc, not its simulation tick rate.
                while (nextMarker <= distance + length) {
                    Vec3 center = a.lerp(b, (nextMarker - distance) / length);
                    Vec3 view = center.subtract(camera).normalize();
                    Vec3 travel = b.subtract(a).normalize();
                    Vec3 forward = travel.subtract(view.scale(travel.dot(view)));
                    if (forward.lengthSqr() < .00001) forward = new Vec3(0, 1, 0).subtract(view.scale(view.y));
                    forward = forward.normalize();
                    Vec3 side = view.cross(forward).normalize();
                    double size = Math.max(.045, Math.min(.28, center.distanceTo(camera) * .007));
                    Vec3 tip = center.add(forward.scale(size));
                    Vec3 back = center.subtract(forward.scale(size * .7));
                    float fade = 1 - i / (float)Math.max(1, points.size()) * .3F;
                    line(vertices, poses, back.add(side.scale(size * .8)), tip, .73F, .48F, 1, .9F * fade);
                    line(vertices, poses, tip, back.subtract(side.scale(size * .8)), .73F, .48F, 1, .9F * fade);
                    nextMarker += .75;
                }
                distance += length;
            }
            if (prediction.impact() instanceof BlockHitResult block) contact(vertices, poses, block, supported);
            else if (!points.isEmpty()) invalid(vertices, poses, points.get(points.size() - 1));
            buffers.endBatch(type);
        } finally { poses.popPose(); }
    }

    private static boolean clearContact(ProjectileEstimation.Prediction result) {
        var client = Minecraft.getInstance();
        if (!(result.impact() instanceof BlockHitResult hit) || hit.getDirection() != Direction.UP) return false;
        Vec3 feet = hit.getLocation().add(0, .01, 0);
        var body = client.player.getBoundingBox().move(feet.subtract(client.player.position()));
        return client.level.getWorldBorder().isWithinBounds(body)
                && client.level.noCollision(client.player, body)
                && !client.level.containsAnyLiquid(body)
                && !client.level.noCollision(client.player, body.move(0, -.08, 0));
    }

    private static void contact(VertexConsumer vertices, PoseStack poses, BlockHitResult block, boolean valid) {
        Vec3 normal = Vec3.atLowerCornerOf(block.getDirection().getNormal());
        Vec3 tangent = Math.abs(normal.y) > .5 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 bitangent = normal.cross(tangent).normalize();
        Vec3 center = block.getLocation().add(normal.scale(.018));
        float red = valid ? .81F : 1F, green = valid ? .62F : .34F, blue = valid ? 1F : .45F;
        double radius = .46;
        for (int i = 0; i < 40; i++) {
            double a = i * Math.PI * 2 / 40, b = (i + .82) * Math.PI * 2 / 40;
            Vec3 from = center.add(tangent.scale(Math.cos(a) * radius)).add(bitangent.scale(Math.sin(a) * radius));
            Vec3 to = center.add(tangent.scale(Math.cos(b) * radius)).add(bitangent.scale(Math.sin(b) * radius));
            line(vertices, poses, from, to, red, green, blue, .95F);
        }
        if (!valid) {
            Vec3 x = tangent.scale(.11), y = bitangent.scale(.11);
            line(vertices, poses, center.subtract(x).subtract(y), center.add(x).add(y), red, green, blue, 1);
            line(vertices, poses, center.subtract(x).add(y), center.add(x).subtract(y), red, green, blue, 1);
        }
    }

    private static void invalid(VertexConsumer vertices, PoseStack poses, Vec3 point) {
        // An entity / unknown horizon has no invented landing plane or nearby floor.
        Vec3 a = new Vec3(.12, .12, 0), b = new Vec3(.12, -.12, 0);
        line(vertices, poses, point.subtract(a), point.add(a), 1, .34F, .45F, .9F);
        line(vertices, poses, point.subtract(b), point.add(b), 1, .34F, .45F, .9F);
    }

    private static void line(VertexConsumer out, PoseStack poses, Vec3 a, Vec3 b, float r, float g, float blue, float alpha) {
        Vec3 direction = b.subtract(a).normalize();
        if (direction.lengthSqr() < .0001) return;
        var pose = poses.last();
        out.vertex(pose.pose(), (float) a.x, (float) a.y, (float) a.z).color(r, g, blue, alpha)
                .normal(pose.normal(), (float) direction.x, (float) direction.y, (float) direction.z).endVertex();
        out.vertex(pose.pose(), (float) b.x, (float) b.y, (float) b.z).color(r, g, blue, alpha)
                .normal(pose.normal(), (float) direction.x, (float) direction.y, (float) direction.z).endVertex();
    }

    static void hud(RenderGuiEvent.Post event) {
        var client = Minecraft.getInstance();
        if (prediction == null || prediction.points().isEmpty() || client.player == null || client.screen != null) return;
        Vec3 end = prediction.points().get(prediction.points().size() - 1);
        boolean russian = client.options.languageCode.startsWith("ru");
        String state = supported ? (russian ? "РєРѕРЅС‚Р°РєС‚ СЃРІРѕР±РѕРґРµРЅ" : "clear contact")
                : prediction.complete() ? (russian ? "РЅРµС‚ СЃРІРѕР±РѕРґРЅРѕР№ РїР»РѕС‰Р°РґРєРё" : "no clear landing")
                : (russian ? "РєРѕРЅРµС† РїСѓС‚Рё РЅРµРёР·РІРµСЃС‚РµРЅ" : "path end unknown");
        String label = String.format(java.util.Locale.ROOT, "%.1f m В· %s", client.player.position().distanceTo(end), state);
        event.getGuiGraphics().drawCenteredString(client.font, label, client.getWindow().getGuiScaledWidth() / 2,
                client.getWindow().getGuiScaledHeight() / 2 + 25, supported ? 0xD4B4FF : 0xFF8D9A);
    }
}
