package dev.taste.engine.light;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.taste.engine.mixin.client.EntityDispatcherShadowAccessor;
import dev.taste.engine.render.FrameSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Replays native opaque and cutout actor meshes into the owning light depth target. */
public final class ActorShadowPass {
    private static final double RANGE = 96;
    private static final int MAX_ENTITIES = 96;
    private static final int MAX_BLOCK_ENTITIES = 96;
    private static boolean replaying;
    private static final VertexConsumer DISCARD = new VertexConsumer() {
        @Override public VertexConsumer vertex(double x, double y, double z) { return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer uv(float u, float v) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void endVertex() { }
        @Override public void defaultColor(int r, int g, int b, int a) { }
        @Override public void unsetDefaultColor() { }
    };
    private final MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(new BufferBuilder(65_536));
    private final MultiBufferSource casterBuffers = this::selectBuffer;
    private List<Entity> actors = List.of();
    private List<BlockEntity> blocks = List.of();
    private float partialTick;
    private boolean playerSkin;

    public void prepare(FrameSnapshot frame, float partialTick, boolean useEntities, boolean useBlocks) {
        this.partialTick = partialTick;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(useEntities || useBlocks)) {
            clear();
            return;
        }
        FrameSnapshot.Vec3d camera = frame.cameraWorld();
        Vec3 eye = new Vec3(camera.x(), camera.y(), camera.z());
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        actors = useEntities ? level.getEntitiesOfClass(Entity.class,
                new AABB(eye.x - RANGE, eye.y - RANGE, eye.z - RANGE, eye.x + RANGE, eye.y + RANGE, eye.z + RANGE),
                entity -> entity.isAlive() && !entity.isInvisible() && nativeRenderer(dispatcher.getRenderer(entity)))
                .stream().sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(eye))).limit(MAX_ENTITIES).toList()
                : List.of();
        blocks = useBlocks ? nearbyBlocks(level, eye) : List.of();
    }

    private static List<BlockEntity> nearbyBlocks(ClientLevel level, Vec3 eye) {
        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        int cx = SectionPos.blockToSectionCoord(eye.x);
        int cz = SectionPos.blockToSectionCoord(eye.z);
        List<BlockEntity> result = new ArrayList<>();
        for (int z = cz - 6; z <= cz + 6; z++) for (int x = cx - 6; x <= cx + 6; x++) {
            var chunk = level.getChunkSource().getChunk(x, z, false);
            if (chunk == null) continue;
            for (BlockEntity block : chunk.getBlockEntities().values()) {
                if (block.isRemoved() || distance(block, eye) > RANGE * RANGE) continue;
                var renderer = dispatcher.getRenderer(block);
                if (renderer != null && renderer.getClass().getPackageName().equals("net.minecraft.client.renderer.blockentity"))
                    result.add(block);
            }
        }
        return result.stream().sorted(Comparator.comparingDouble(block -> distance(block, eye)))
                .limit(MAX_BLOCK_ENTITIES).toList();
    }

    private static double distance(BlockEntity block, Vec3 eye) {
        return Vec3.atCenterOf(block.getBlockPos()).distanceToSqr(eye);
    }

    private static boolean nativeRenderer(Object renderer) {
        return renderer != null && (renderer.getClass() == PlayerRenderer.class
                || renderer.getClass().getPackageName().equals("net.minecraft.client.renderer.entity"));
    }

    public int draw(Matrix4f lightProjection, FrameSnapshot.Vec3d origin) {
        if (actors.isEmpty() && blocks.isEmpty()) return 0;
        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderDispatcher entities = minecraft.getEntityRenderDispatcher();
        BlockEntityRenderDispatcher blockRenderer = minecraft.getBlockEntityRenderDispatcher();
        Matrix4f priorProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting priorSorting = RenderSystem.getVertexSorting();
        PoseStack modelView = RenderSystem.getModelViewStack();
        PoseStack pose = new PoseStack();
        FrustumIntersection frustum = new FrustumIntersection(lightProjection);
        boolean oldShadows = ((EntityDispatcherShadowAccessor) entities).taste$renderShadowEnabled();
        boolean oldHitboxes = entities.shouldRenderHitBoxes();
        int count = 0;
        boolean previousReplay = replaying;
        replaying = true;
        modelView.pushPose();
        try {
            modelView.setIdentity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(lightProjection, VertexSorting.ORTHOGRAPHIC_Z);
            entities.setRenderShadow(false);
            entities.setRenderHitBoxes(false);
            for (Entity entity : actors) {
                AABB bounds = entity.getBoundingBox().inflate(0.5);
                if (!visible(frustum, bounds, origin)) continue;
                Vec3 position = entity.getPosition(partialTick);
                playerSkin = entities.getRenderer(entity).getClass() == PlayerRenderer.class;
                try {
                    entities.render(entity, position.x - origin.x(), position.y - origin.y(), position.z - origin.z(),
                            entity.getViewYRot(partialTick), partialTick, pose, casterBuffers, entities.getPackedLightCoords(entity, partialTick));
                } finally {
                    playerSkin = false;
                }
                count++;
            }
            for (BlockEntity block : blocks) {
                BlockPos position = block.getBlockPos();
                AABB bounds = block.getRenderBoundingBox();
                if (!visible(frustum, bounds, origin)) continue;
                pose.pushPose();
                try {
                    pose.translate(position.getX() - origin.x(), position.getY() - origin.y(), position.getZ() - origin.z());
                    blockRenderer.render(block, partialTick, pose, casterBuffers);
                    count++;
                } finally {
                    pose.popPose();
                }
            }
            buffers.endBatch();
        } finally {
            replaying = previousReplay;
            entities.setRenderShadow(oldShadows);
            entities.setRenderHitBoxes(oldHitboxes);
            RenderSystem.setProjectionMatrix(priorProjection, priorSorting);
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
        }
        return count;
    }

    private VertexConsumer selectBuffer(RenderType type) {
        String name = type.toString();
        if (!(name.startsWith("RenderType[entity_solid:") || name.startsWith("RenderType[entity_cutout:")
                || name.startsWith("RenderType[entity_cutout_no_cull:")
                || name.startsWith("RenderType[entity_cutout_no_cull_z_offset:")
                || playerSkin && name.startsWith("RenderType[entity_translucent:")
                || name.startsWith("RenderType[armor_cutout_no_cull:"))) return DISCARD;
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);
        return buffers.getBuffer(type);
    }

    private static boolean visible(FrustumIntersection frustum, AABB bounds, FrameSnapshot.Vec3d origin) {
        return frustum.testAab((float) (bounds.minX - origin.x()), (float) (bounds.minY - origin.y()),
                (float) (bounds.minZ - origin.z()), (float) (bounds.maxX - origin.x()),
                (float) (bounds.maxY - origin.y()), (float) (bounds.maxZ - origin.z()));
    }

    public void clear() {
        actors = List.of();
        blocks = List.of();
    }

    public boolean hasActors() { return !actors.isEmpty() || !blocks.isEmpty(); }

    public static boolean replaying() { return replaying; }
}
