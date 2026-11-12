package dev.taste.engine.effects;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.taste.engine.config.ConfigSnapshot;
import dev.taste.engine.render.GlStateScope;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;

/** Renders a settling native model over the visible chunk model after a matching server block update. */
public final class ClientPlacementEffects {
    private record Candidate(Block expected, BlockState previous, long expires) { }
    private record Placement(BlockPos position, BlockState state, long start) { }

    private static final int MAX_CANDIDATES = 8;
    private static final int MAX_PROXIES = 6;
    private final Map<BlockPos, Candidate> candidates = new LinkedHashMap<>();
    private final Deque<Placement> placements = new ArrayDeque<>();
    private final BufferBuilder vertices = new BufferBuilder(8192);
    private final MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(vertices);
    private ClientLevel level;

    public void watch(ClientLevel world, BlockPlaceContext context, BlockItem item) {
        switchWorld(world);
        BlockPos position = context.getClickedPos().immutable();
        Block expected = item.getBlock();
        if (!world.hasChunkAt(position) || !context.canPlace()
                || !BuiltInRegistries.BLOCK.getKey(expected).getNamespace().equals("minecraft")) return;
        BlockState previous = world.getBlockState(position);
        if (previous.is(expected)) return;
        candidates.put(position, new Candidate(expected, previous, world.getGameTime() + 40));
        while (candidates.size() > MAX_CANDIDATES) candidates.remove(candidates.keySet().iterator().next());
    }

    public void confirm(ClientLevel world, BlockPos position, BlockState state) {
        if (level != world) return;
        Candidate candidate = candidates.remove(position);
        if (candidate == null || candidate.expires() < world.getGameTime() || !world.hasChunkAt(position)
                || candidate.previous() == state
                || !state.is(candidate.expected()) || !eligible(world, position, state)) return;
        placements.removeIf(placement -> placement.position().equals(position));
        if (placements.size() >= MAX_PROXIES) placements.removeFirst();
        placements.addLast(new Placement(position.immutable(), state, System.nanoTime()));
    }

    public void tick(ClientLevel world, ConfigSnapshot settings, boolean graphicsActive) {
        switchWorld(world);
        if (!graphicsActive || !active(settings) || world == null) {
            candidates.clear();
            placements.clear();
            return;
        }
        long tick = world.getGameTime();
        candidates.values().removeIf(candidate -> candidate.expires() < tick);
    }

    public void render(Minecraft client, Camera camera, Matrix4f pose, ConfigSnapshot settings) {
        if (!active(settings) || level != client.level || placements.isEmpty()) return;
        long now = System.nanoTime();
        double duration = settings.value("interaction.placementDuration").getAsDouble();
        placements.removeIf(placement -> (now - placement.start()) * 1.0E-9 >= duration
                || !level.hasChunkAt(placement.position())
                || level.getBlockState(placement.position()) != placement.state());
        if (placements.isEmpty()) return;
        Vec3 eye = camera.getPosition();
        PoseStack poses = new PoseStack();
        poses.mulPoseMatrix(pose);
        try (GlStateScope ignored = GlStateScope.capture()) {
            for (Placement placement : placements) {
                BlockPos position = placement.position();
                Vec3 relative = Vec3.atCenterOf(position).subtract(eye);
                if (relative.lengthSqr() > 18.0 * 18.0) continue;
                float progress = (float) ((now - placement.start()) * 1.0E-9 / duration);
                float remaining = 1.0F - progress;
                float scale = 1.0F + remaining * remaining * 0.10F;
                poses.pushPose();
                try {
                    poses.translate(relative.x, relative.y + remaining * 0.035, relative.z);
                    poses.scale(scale, scale, scale);
                    poses.translate(-0.5, -0.5, -0.5);
                    int light = LevelRenderer.getLightColor(level, position.above());
                    client.getBlockRenderer().renderSingleBlock(placement.state(), poses, buffers, light,
                            OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
                } finally {
                    poses.popPose();
                }
            }
            buffers.endBatch();
        }
    }

    public static boolean active(ConfigSnapshot settings) {
        return settings.requestedEnabled() && settings.value("interaction.enabled").getAsBoolean()
                && settings.value("interaction.placementMotion").getAsBoolean()
                && !settings.value("accessibility.reducedMotion").getAsBoolean();
    }

    private void switchWorld(ClientLevel world) {
        if (level == world) return;
        level = world;
        candidates.clear();
        placements.clear();
    }

    private static boolean eligible(ClientLevel world, BlockPos position, BlockState state) {
        return state.getRenderShape() == RenderShape.MODEL && !state.hasBlockEntity()
                && state.getFluidState().isEmpty() && state.isSolidRender(world, position)
                && Block.isShapeFullBlock(state.getShape(world, position));
    }
}
