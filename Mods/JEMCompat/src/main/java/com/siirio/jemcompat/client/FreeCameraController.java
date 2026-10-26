package com.siirio.jemcompat.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Client view only. The local player stays the server-owned body. */
@Mod.EventBusSubscriber(modid = "jemcompat", value = Dist.CLIENT)
public final class FreeCameraController {
    private static final KeyMapping TOGGLE = new KeyMapping("key.jemcompat.free_camera",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F4, KeyMapping.CATEGORY_MISC);
    // The camera's swept volume includes a near-plane allowance on every side.
    private static final double VIEW_CLEARANCE = 0.24;
    private static final double NORMAL_SPEED = 0.45;
    private static final double SPRINT_SPEED = 0.90;

    private static FreeCameraEntity camera;
    private static ClientLevel ownerLevel;
    private static LocalPlayer ownerPlayer;
    private static CameraType previousType;
    private static float previousYaw;
    private static float previousPitch;

    private FreeCameraController() { }

    @Mod.EventBusSubscriber(modid = "jemcompat", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() { }

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE);
        }
    }

    public static boolean isActive() {
        return camera != null;
    }

    /** MouseHandler and Epic Fight turn LocalPlayer even when Minecraft.cameraEntity differs. */
    public static boolean turnCameraForPlayer(Entity entity, double horizontal, double vertical) {
        if (camera == null || entity != ownerPlayer || Minecraft.getInstance().getCameraEntity() != camera) return false;
        camera.turn(horizontal, vertical);
        return true;
    }

    /** Called after the native position packet has been applied on the client thread. */
    public static void onServerTeleport() {
        if (camera != null) close(false);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (camera != null && (minecraft.player != ownerPlayer || minecraft.level != ownerLevel
                || !ownerPlayer.isAlive() || ownerPlayer.isRemoved() || ownerPlayer.isPassenger()
                || minecraft.getCameraEntity() != camera)) {
            close(false);
        }

        boolean toggle = false;
        while (TOGGLE.consumeClick()) toggle = true;
        if (minecraft.screen != null || !minecraft.isWindowActive()) return;
        if (toggle) {
            if (camera == null) open(minecraft);
            else close(true);
            return;
        }
        if (camera != null) move(minecraft);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void suppressBodyMovement(MovementInputUpdateEvent event) {
        if (camera == null || event.getEntity() != ownerPlayer) return;
        Input input = event.getInput();
        input.leftImpulse = 0;
        input.forwardImpulse = 0;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void suppressInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (camera == null || Minecraft.getInstance().screen != null
                || !(event.isAttack() || event.isUseItem() || event.isPickBlock())) return;
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (camera != null) close(false);
    }

    private static void open(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || minecraft.gameMode == null || !player.isAlive()
                || player.isPassenger() || player.isUsingItem() || minecraft.getCameraEntity() != player) return;
        // The native camera position preserves the visible starting point in either prior POV.
        Camera renderedCamera = minecraft.gameRenderer.getMainCamera();
        boolean currentView = renderedCamera.isInitialized() && renderedCamera.getEntity() == player;
        Vec3 eye = currentView ? renderedCamera.getPosition() : player.getEyePosition();
        float eyeHeight = player.getEyeHeight();
        FreeCameraEntity next = new FreeCameraEntity(level, eyeHeight);
        next.setPos(eye.x, eye.y - eyeHeight, eye.z);
        next.setYRot(currentView ? renderedCamera.getYRot() : player.getYRot());
        next.setXRot(currentView ? renderedCamera.getXRot() : player.getXRot());
        next.setOldPosAndRot();
        ownerPlayer = player;
        ownerLevel = level;
        previousType = minecraft.options.getCameraType();
        previousYaw = player.getYRot();
        previousPitch = player.getXRot();
        camera = next;
        minecraft.gameMode.stopDestroyBlock();
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        minecraft.setCameraEntity(next);
    }

    private static void close(boolean restoreLook) {
        Minecraft minecraft = Minecraft.getInstance();
        FreeCameraEntity oldCamera = camera;
        LocalPlayer oldPlayer = ownerPlayer;
        ClientLevel oldLevel = ownerLevel;
        CameraType oldType = previousType;
        camera = null;
        ownerPlayer = null;
        ownerLevel = null;
        previousType = null;
        if (minecraft.getCameraEntity() == oldCamera) minecraft.setCameraEntity(minecraft.player);
        if (oldType != null) minecraft.options.setCameraType(oldType);
        if (restoreLook && minecraft.player == oldPlayer && minecraft.level == oldLevel && oldPlayer != null) {
            oldPlayer.setYRot(previousYaw);
            oldPlayer.setXRot(previousPitch);
            oldPlayer.yRotO = previousYaw;
            oldPlayer.xRotO = previousPitch;
        }
    }

    private static void move(Minecraft minecraft) {
        // Keep the temporary POV stable if vanilla's perspective binding was pressed.
        if (minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        }
        double forwardInput = (minecraft.options.keyUp.isDown() ? 1 : 0)
                - (minecraft.options.keyDown.isDown() ? 1 : 0);
        double rightInput = (minecraft.options.keyRight.isDown() ? 1 : 0)
                - (minecraft.options.keyLeft.isDown() ? 1 : 0);
        double upInput = (minecraft.options.keyJump.isDown() ? 1 : 0)
                - (minecraft.options.keyShift.isDown() ? 1 : 0);
        double yaw = Math.toRadians(camera.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 input = forward.scale(forwardInput).add(right.scale(rightInput)).add(0, upInput, 0);
        camera.setOldPosAndRot();
        if (input.lengthSqr() < 1.0E-8) return;
        double speed = minecraft.options.keySprint.isDown() ? SPRINT_SPEED : NORMAL_SPEED;
        Vec3 desired = input.normalize().scale(speed);
        Vec3 eye = camera.getEyePosition();
        AABB volume = new AABB(eye.x - VIEW_CLEARANCE, eye.y - VIEW_CLEARANCE, eye.z - VIEW_CLEARANCE,
                eye.x + VIEW_CLEARANCE, eye.y + VIEW_CLEARANCE, eye.z + VIEW_CLEARANCE);
        Vec3 loadedPath = clipAtUnloadedChunk(ownerLevel, volume, desired);
        if (loadedPath.lengthSqr() < 1.0E-8) return;
        Vec3 allowed = Entity.collideBoundingBox(camera, loadedPath, volume, ownerLevel, List.of());
        camera.setPos(camera.position().add(allowed));
    }

    /** The swept box must remain inside client chunks that already have data. */
    private static Vec3 clipAtUnloadedChunk(ClientLevel level, AABB volume, Vec3 desired) {
        if (!allChunksLoaded(level, volume)) return Vec3.ZERO;
        if (allChunksLoaded(level, volume.expandTowards(desired))) return desired;
        double low = 0;
        double high = 1;
        for (int i = 0; i < 16; i++) {
            double middle = (low + high) * 0.5;
            if (allChunksLoaded(level, volume.expandTowards(desired.scale(middle)))) low = middle;
            else high = middle;
        }
        return desired.scale(Math.max(0, low - 1.0E-4));
    }

    private static boolean allChunksLoaded(ClientLevel level, AABB volume) {
        int minChunkX = (int) Math.floor(volume.minX) >> 4;
        int maxChunkX = (int) Math.floor(volume.maxX - 1.0E-7) >> 4;
        int minChunkZ = (int) Math.floor(volume.minZ) >> 4;
        int maxChunkZ = (int) Math.floor(volume.maxZ - 1.0E-7) >> 4;
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                if (!level.hasChunk(x, z)) return false;
            }
        }
        return true;
    }

    private static final class FreeCameraEntity extends Marker {
        private final float cameraEyeHeight;

        private FreeCameraEntity(ClientLevel level, float eyeHeight) {
            super(EntityType.MARKER, level);
            cameraEyeHeight = eyeHeight;
            refreshDimensions();
        }

        @Override
        protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
            return cameraEyeHeight;
        }
    }
}
