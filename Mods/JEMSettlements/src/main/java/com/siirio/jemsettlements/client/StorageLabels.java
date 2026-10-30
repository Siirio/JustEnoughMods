package com.siirio.jemsettlements.client;

import com.siirio.jemsettlements.JemSettlements;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemSettlements.MOD_ID, value = Dist.CLIENT)
public final class StorageLabels {
    private static final int MAX_VIEWS = 256;
    private static final int MAX_LABELS = 64;
    private record Label(BlockPos position, double x, double y, double z, Component text) { }
    private record View(ResourceLocation dimension, List<Label> labels, long expires) { }
    private static final Map<UUID, View> VIEWS = new LinkedHashMap<>();
    private static final Set<RenderType> DRAWN_TYPES = new LinkedHashSet<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private static MultiBufferSource.BufferSource activeBuffers;
    private static final MultiBufferSource LABEL_BUFFERS = type -> {
        DRAWN_TYPES.add(type);
        return activeBuffers.getBuffer(type);
    };

    private StorageLabels() { }

    public static void accept(CompoundTag tag) {
        var client = Minecraft.getInstance();
        if (client.level == null || !tag.hasUUID("Town")) return;
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (dimension == null) return;
        if (world.get() != client.level) {
            clear();
            world = new WeakReference<>(client.level);
        }
        var rows = tag.getList("Labels", Tag.TAG_COMPOUND);
        List<Label> labels = new ArrayList<>(Math.min(MAX_LABELS, rows.size()));
        for (int index = 0; index < Math.min(MAX_LABELS, rows.size()); index++) {
            CompoundTag row = rows.getCompound(index);
            BlockPos position = BlockPos.of(row.getLong("Position"));
            BlockPos other = row.contains("Second") ? BlockPos.of(row.getLong("Second")) : position;
            labels.add(new Label(position, (position.getX() + other.getX()) * .5 + .5,
                    position.getY() + 1.3, (position.getZ() + other.getZ()) * .5 + .5,
                    Component.translatable("jem.settlements.storage_name", row.getInt("Number"))));
        }
        UUID town = tag.getUUID("Town");
        if (VIEWS.size() >= MAX_VIEWS && !VIEWS.containsKey(town)) VIEWS.remove(VIEWS.keySet().iterator().next());
        VIEWS.put(town, new View(dimension, List.copyOf(labels), client.level.getGameTime() + 60));
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() == world.get()) clear();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || VIEWS.isEmpty()) return;
        var client = Minecraft.getInstance();
        if (client.level == null || world.get() != client.level) {
            clear();
            return;
        }
        if (client.options.hideGui || ShaderState.shadowPass()) return;
        VIEWS.values().removeIf(view -> view.expires() < client.level.getGameTime());
        if (VIEWS.isEmpty()) return;
        var camera = event.getCamera();
        var origin = camera.getPosition();
        var poses = event.getPoseStack();
        var dimension = client.level.dimension().location();
        try {
            for (View view : VIEWS.values()) {
                if (!view.dimension().equals(dimension)) continue;
                for (Label label : view.labels()) {
                    if (origin.distanceToSqr(label.x(), label.y(), label.z()) > 32 * 32
                            || !client.level.hasChunkAt(label.position()) || client.level.getBlockEntity(label.position()) == null) continue;
                    if (activeBuffers == null) activeBuffers = client.renderBuffers().bufferSource();
                    poses.pushPose();
                    try {
                        poses.translate(label.x() - origin.x, label.y() - origin.y, label.z() - origin.z);
                        poses.mulPose(camera.rotation());
                        poses.scale(-.025F, -.025F, .025F);
                        client.font.drawInBatch(label.text(), -client.font.width(label.text()) / 2F, 0, 0xffffffff, false,
                                poses.last().pose(), LABEL_BUFFERS, Font.DisplayMode.NORMAL, 0x70000000, 15728880);
                    } finally {
                        poses.popPose();
                    }
                }
            }
        } finally {
            try {
                if (activeBuffers != null) for (RenderType type : DRAWN_TYPES) activeBuffers.endBatch(type);
            } finally {
                DRAWN_TYPES.clear();
                activeBuffers = null;
            }
        }
    }

    private static void clear() {
        VIEWS.clear();
        world.clear();
        DRAWN_TYPES.clear();
        activeBuffers = null;
    }

    private static final class ShaderState {
        private static final Object API = api();
        private static final Method SHADOW = shadowMethod();

        private static Object api() {
            if (!ModList.get().isLoaded("oculus")) return null;
            try {
                return Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        private static Method shadowMethod() {
            if (API == null) return null;
            try {
                return API.getClass().getMethod("isRenderingShadowPass");
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        private static boolean shadowPass() {
            if (SHADOW == null) return false;
            try {
                return Boolean.TRUE.equals(SHADOW.invoke(API));
            } catch (ReflectiveOperationException ignored) {
                return false;
            }
        }
    }
}
