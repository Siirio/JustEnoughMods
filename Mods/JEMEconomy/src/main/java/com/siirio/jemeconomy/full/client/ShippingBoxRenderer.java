package com.siirio.jemeconomy.full.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.siirio.jemeconomy.shipping.ShippingBoxBlockEntity;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;

/** Decorative native models only: no storage, drops, synchronized items or gameplay authority. */
public final class ShippingBoxRenderer implements BlockEntityRenderer<ShippingBoxBlockEntity> {
    private record Display(ItemStack stack, float x, float y, float z, float rx, float ry, float rz, float scale, boolean log) {}
    private final net.minecraft.client.renderer.entity.ItemRenderer renderer;
    private final List<Display> displays;

    public ShippingBoxRenderer(BlockEntityRendererProvider.Context context) {
        this(context.getItemRenderer());
    }
    public ShippingBoxRenderer(net.minecraft.client.renderer.entity.ItemRenderer itemRenderer) {
        renderer = itemRenderer;
        List<Display> values = new ArrayList<>();
        try (var stream = ShippingBoxRenderer.class.getResourceAsStream("/assets/jem_economy/models/block/shipping_goods.json")) {
            if (stream == null) throw new IllegalStateException("Missing approved shipping display anchors");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var element : root.getAsJsonArray("items")) {
                var row = element.getAsJsonObject(); var position = row.getAsJsonArray("position"); var rotation = row.getAsJsonArray("rotation");
                var id = new ResourceLocation(row.get("item").getAsString()); var item = ForgeRegistries.ITEMS.getValue(id);
                if (item == null) throw new IllegalStateException("Missing native display item " + id);
                values.add(new Display(new ItemStack(item), position.get(0).getAsFloat()/16F, position.get(1).getAsFloat()/16F,
                        position.get(2).getAsFloat()/16F, rotation.get(0).getAsFloat(), rotation.get(1).getAsFloat(),
                        rotation.get(2).getAsFloat(), row.get("scale").getAsFloat(), id.getPath().equals("oak_log")));
            }
            displays = List.copyOf(values);
        } catch (java.io.IOException failure) { throw new IllegalStateException("Invalid shipping display anchors", failure); }
    }

    @Override public void render(ShippingBoxBlockEntity box, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(180F-box.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()));
        pose.translate(-.5D, 0D, -.5D);
        for (Display display : displays) {
            pose.pushPose(); pose.translate(display.x, display.y, display.z);
            pose.mulPose(Axis.ZP.rotationDegrees(display.rz)); pose.mulPose(Axis.YP.rotationDegrees(display.ry)); pose.mulPose(Axis.XP.rotationDegrees(display.rx));
            // Compensate the vanilla FIXED baseline used by the authored preview. Native pack overrides still apply.
            float scale = display.scale * (display.log ? 2F : 1F);
            pose.scale(scale, scale, scale);
            if (!display.log) pose.mulPose(Axis.YP.rotationDegrees(180F));
            renderer.renderStatic(display.stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, box.getLevel(), 0);
            pose.popPose();
        }
        pose.popPose();
    }
}
