package com.siirio.jembuilding.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.siirio.jembuilding.JEMBuilding;
import com.siirio.jembuilding.blueprint.BlueprintRotation;
import com.siirio.jembuilding.blueprint.BlueprintSavedData;
import com.siirio.jembuilding.item.BlueprintRollItem;
import com.siirio.jembuilding.item.StructureBlueprintItem;
import com.siirio.jembuilding.network.BuildingNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = JEMBuilding.MOD_ID, value = Dist.CLIENT)
public final class BlueprintClientState {
    private static final String CATEGORY = "key.categories.jem_building";
    private static final KeyMapping ROTATE = key("key.jem_building.blueprint.rotate", InputConstants.KEY_R);
    private static final KeyMapping X_NEG = key("key.jem_building.blueprint.x_neg", InputConstants.KEY_LEFT);
    private static final KeyMapping X_POS = key("key.jem_building.blueprint.x_pos", InputConstants.KEY_RIGHT);
    private static final KeyMapping Z_NEG = key("key.jem_building.blueprint.z_neg", InputConstants.KEY_UP);
    private static final KeyMapping Z_POS = key("key.jem_building.blueprint.z_pos", InputConstants.KEY_DOWN);
    private static final KeyMapping Y_POS = key("key.jem_building.blueprint.y_pos", InputConstants.KEY_PAGEUP);
    private static final KeyMapping Y_NEG = key("key.jem_building.blueprint.y_neg", InputConstants.KEY_PAGEDOWN);
    private static UUID id;
    private static int width;
    private static int height;
    private static int depth;
    private static List<BlockState> palette = List.of();
    private static List<BlueprintSavedData.Entry> entries = List.of();
    private static BlockPos anchor;
    private static BlueprintRotation rotation = BlueprintRotation.NONE;
    private static boolean requested;

    private static KeyMapping key(String name, int key) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    public static void receive(BuildingNetwork.BlueprintPreviewResponse packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        boolean changed = !packet.id().equals(id);
        id = packet.id(); width = packet.width(); height = packet.height(); depth = packet.depth();
        List<BlockState> states = new ArrayList<>(packet.palette().size());
        for (CompoundTag state : packet.palette()) states.add(NbtUtils.readBlockState(client.level.holderLookup(Registries.BLOCK), state));
        palette = List.copyOf(states); entries = List.copyOf(packet.entries()); requested = false;
        if (changed || anchor == null) {
            anchor = initialAnchor(client);
            rotation = BlueprintRotation.NONE;
        }
    }

    public static void confirm(InteractionHand hand) {
        if (anchor == null || id == null) return;
        BuildingNetwork.placeBlueprint(hand, anchor, rotation);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) { clear(); return; }
        var stack = client.player.getMainHandItem();
        var heldId = BlueprintRollItem.blueprintId(stack);
        if (heldId.isEmpty()) { clear(); return; }
        if (!heldId.get().equals(id) && !requested) {
            requested = true;
            BuildingNetwork.requestBlueprint(InteractionHand.MAIN_HAND);
        }
        if (client.screen != null || anchor == null) return;
        if (ROTATE.consumeClick()) { rotation = rotation.next(); announce(client); }
        move(X_NEG, -1, 0, 0, client); move(X_POS, 1, 0, 0, client);
        move(Z_NEG, 0, 0, -1, client); move(Z_POS, 0, 0, 1, client);
        move(Y_NEG, 0, -1, 0, client); move(Y_POS, 0, 1, 0, client);
    }

    private static void move(KeyMapping key, int x, int y, int z, Minecraft client) {
        if (!key.consumeClick()) return;
        anchor = anchor.offset(x, y, z);
        announce(client);
    }

    private static void announce(Minecraft client) {
        client.player.displayClientMessage(Component.translatable("message.jem_building.blueprint.transform",
                anchor.getX(), anchor.getY(), anchor.getZ(), rotation.ordinal() * 90), true);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || id == null || anchor == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || !(client.player.getMainHandItem().getItem() instanceof StructureBlueprintItem)) return;
        PoseStack poses = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        int limit = entries.size();
        float pulse = 0.42F + 0.12F * (float) Math.sin((client.level.getGameTime() + event.getPartialTick()) * 0.18D);
        for (int index = 0; index < limit; index++) {
            BlueprintSavedData.Entry entry = entries.get(index);
            BlockPos pos = anchor.offset(rotation.apply(entry.relative(), width, depth));
            if (Vec3.atCenterOf(pos).distanceToSqr(camera) > 256D * 256D) continue;
            if (!event.getFrustum().isVisible(new AABB(pos))) continue;
            BlockState state = palette.get(entry.paletteIndex()).rotate(rotation.vanilla());
            BlockState existing = client.level.getBlockState(pos);
            boolean collision = !existing.canBeReplaced()
                    || existing.is(com.siirio.jembuilding.registry.BuildingContent.CONSTRUCTION_PLACEHOLDER.get());
            poses.pushPose();
            poses.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            float red = collision ? 1.0F : 0.62F;
            float green = collision ? 0.22F : 0.78F;
            float blue = collision ? 0.22F : 1.0F;
            float alpha = collision ? pulse : 0.48F;
            MultiBufferSource source = type -> new TintVertexConsumer(buffers.getBuffer(RenderType.translucent()), red, green, blue, alpha);
            client.getBlockRenderer().renderSingleBlock(state, poses, source, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, net.minecraftforge.client.model.data.ModelData.EMPTY, RenderType.translucent());
            poses.popPose();
        }
        buffers.endBatch(RenderType.translucent());
    }

    private static BlockPos initialAnchor(Minecraft client) {
        HitResult hit = client.hitResult;
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) return block.getBlockPos().relative(block.getDirection());
        Vec3 point = client.player.getEyePosition().add(client.player.getLookAngle().scale(6D));
        return BlockPos.containing(point);
    }

    private static void clear() {
        id = null; palette = List.of(); entries = List.of(); anchor = null; requested = false;
    }

    @Mod.EventBusSubscriber(modid = JEMBuilding.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(ROTATE); event.register(X_NEG); event.register(X_POS); event.register(Z_NEG); event.register(Z_POS);
            event.register(Y_POS); event.register(Y_NEG);
        }
    }

    private static final class TintVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float red;
        private final float green;
        private final float blue;
        private final float alpha;

        private TintVertexConsumer(VertexConsumer delegate, float red, float green, float blue, float alpha) {
            this.delegate = delegate; this.red = red; this.green = green; this.blue = blue; this.alpha = alpha;
        }
        @Override public VertexConsumer vertex(double x, double y, double z) { delegate.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) {
            delegate.color((int) (r * red), (int) (g * green), (int) (b * blue), (int) (a * alpha)); return this;
        }
        @Override public VertexConsumer uv(float u, float v) { delegate.uv(u, v); return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { delegate.overlayCoords(u, v); return this; }
        @Override public VertexConsumer uv2(int u, int v) { delegate.uv2(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
        @Override public void endVertex() { delegate.endVertex(); }
        @Override public void defaultColor(int r, int g, int b, int a) { delegate.defaultColor(r, g, b, (int) (a * alpha)); }
        @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
        @Override public void putBulkData(PoseStack.Pose pose, net.minecraft.client.renderer.block.model.BakedQuad quad,
                                          float[] brightness, float red, float green, float blue, int[] light,
                                          int overlay, boolean readExistingColor) {
            delegate.putBulkData(pose, quad, brightness, red * this.red, green * this.green, blue * this.blue,
                    this.alpha, light, overlay, readExistingColor);
        }
    }

    private BlueprintClientState() { }
}
