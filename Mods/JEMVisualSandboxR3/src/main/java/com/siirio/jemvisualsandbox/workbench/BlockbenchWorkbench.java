package com.siirio.jemvisualsandbox.workbench;

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

/** Solo sandbox main-skill visuals, driven by integrated-server ability results. */
@Mod.EventBusSubscriber(modid = "jem_visual_sandbox", value = Dist.CLIENT)
public final class BlockbenchWorkbench {
    private record Asset(String id, String name, int materials) {}
    private static final Asset[] ASSETS = {
        new Asset("shield", "Ancestors / Life Chorus + guardian", 1),
        new Asset("void", "Void / native End tear", 1),
        new Asset("entity_vines", "Bloom / adaptive root bind", 2),
        new Asset("sculk", "Sculk / circular sonic wave", 1),
        new Asset("eruption0", "Nether / earth eruptions (B: fiery slashes)", 2),
        new Asset("eruption1", "Eruption / fractured basalt", 2),
        new Asset("eruption2", "Eruption / molten crown", 2)
    };
    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.jem_visual_sandbox." + name, InputConstants.Type.KEYSYM, code, "key.categories.jem_visual_sandbox");
    }
    private static final KeyMapping RACES = key("race_menu", GLFW.GLFW_KEY_F6);
    private static final KeyMapping CAST = key("race_cast", GLFW.GLFW_KEY_V);
    private static final KeyMapping SECONDARY = key("fire_slashes", GLFW.GLFW_KEY_B);
    private static final KeyMapping NEXT = key("art_next", GLFW.GLFW_KEY_RIGHT_BRACKET);
    private static final KeyMapping PREVIOUS = key("art_previous", GLFW.GLFW_KEY_LEFT_BRACKET);
    private static final KeyMapping ART = key("art_play", GLFW.GLFW_KEY_F10);
    private static int selectedRace, selectedArt, selectedUpgrade;
    private static RaceExperience.Status raceStatus;
    private static boolean secondaryDown;
    private record Instance(WorkbenchMesh mesh, Vec3 origin, ClientLevel level, long startTick, float yaw, int targetId, int ticks) {}
    private static final java.util.List<Instance> INSTANCES = new java.util.ArrayList<>();
    private static final java.util.Map<String, WorkbenchMesh> CACHE = new java.util.HashMap<>();
    private static ClientLevel clientWorld;

    private static WorkbenchMesh mesh;
    private static Vec3 origin;
    private static ClientLevel previewLevel;
    private static long startTick;
    private static float yaw;
    private BlockbenchWorkbench() {}
    @Mod.EventBusSubscriber(modid = "jem_visual_sandbox", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) {
            for (KeyMapping key : new KeyMapping[]{RACES, CAST, SECONDARY, NEXT, PREVIOUS, ART}) event.register(key);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        if (clientWorld != client.level) {
            clear(); raceStatus=null;clientWorld = client.level;
            if (client.level != null && client.hasSingleplayerServer()) SandboxNetwork.sync();
        }
        if (!client.hasSingleplayerServer()) clear();
        INSTANCES.removeIf(instance -> instance.level() != client.level || client.level == null || client.level.getGameTime() - instance.startTick() >= instance.ticks());

        boolean connected=client.getConnection()!=null&&SandboxNetwork.supported(client.getConnection().getConnection());
        com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.tick();
        if(connected&&client.player!=null&&client.player.tickCount%40==0&&raceStatus==null)SandboxNetwork.sync();
        boolean allowed = client.player != null && client.level != null && client.screen == null && connected;
        while (RACES.consumeClick()) {
            if (allowed)com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.openProgression();
            else if(client.player!=null&&!connected)label(client,"This server does not provide the JEM Races gameplay protocol");
        }
        boolean developer=allowed&&raceStatus!=null&&raceStatus.developerMode();
        while (NEXT.consumeClick()) if (developer) { selectedArt = (selectedArt + 1) % ASSETS.length; label(client, ASSETS[selectedArt].name()); }
        while (PREVIOUS.consumeClick()) if (developer) { selectedArt = (selectedArt + ASSETS.length - 1) % ASSETS.length; label(client, ASSETS[selectedArt].name()); }
        boolean nativeDedicated=!client.hasSingleplayerServer()&&net.minecraftforge.fml.ModList.get().isLoaded("jem_roles");
        while (CAST.consumeClick()) if (allowed&&!nativeDedicated&&!com.siirio.jemvisualsandbox.client.races.OptionalRacePresentation.nativeKey(CAST)) SandboxNetwork.cast();
        boolean pressed=false;
        while (SECONDARY.consumeClick()) if (allowed&&!nativeDedicated) { SandboxNetwork.secondary();pressed=true; }
        boolean down=allowed && SECONDARY.isDown();
        if((secondaryDown || pressed) && !down){if(!nativeDedicated)SandboxNetwork.secondaryRelease();VoidDistancePreview.clear();}
        secondaryDown=down;
        while (ART.consumeClick()) if (developer) play(client, selectedArt);
    }
    private static void play(Minecraft client, int index) {
        var hit = client.player.pick(12.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK || block.getDirection() != Direction.UP) {
            label(client, "Aim at the top of a ground block (within 12 blocks)"); return;
        }
        Asset asset = ASSETS[index];
        if(index==1 || index==3) { ElementalRenderer.receive(new SandboxNetwork.Visual(index==1?5:7,index,block.getLocation().add(0,.04,0),client.player.getYRot()));return; }
        try { mesh = WorkbenchMesh.load(asset.id(), asset.materials()); }
        catch (IOException error) { clear(); LogUtils.getLogger().error("Cannot load Blockbench sandbox asset " + asset.id(), error); label(client, "Asset failed to load; see log"); return; }
        origin = block.getLocation().add(0, .025D, 0);
        yaw = -client.player.getYRot();
        previewLevel = client.level;
        startTick = client.level.getGameTime();
        INSTANCES.clear();
        add(mesh, origin, yaw);
        label(client, asset.name() + " / editor study");
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear();raceStatus=null; }
    public static void acceptRace(RaceExperience.Status status){raceStatus=status;selectedRace=status.race();selectedUpgrade=status.bloomUpgrade();var p=Minecraft.getInstance().player;if(p!=null)p.getPersistentData().putInt("JemSandboxRace",status.selected()?status.race():-1);}
    private static void clear() { origin = null; previewLevel = null; mesh = null; INSTANCES.clear(); CACHE.clear(); FlameSlashRenderer.clear(); ElementalRenderer.clear(); SurfaceVfx.clear(); UtilityVfx.clear(); LifeChorusRenderer.clear(); VoidDistancePreview.clear(); }
    private static void add(WorkbenchMesh model, Vec3 position, float rotation) {
        Minecraft client = Minecraft.getInstance();
        if (INSTANCES.size() >= 64) INSTANCES.remove(0);
        INSTANCES.add(new Instance(model, position.add(0,.025,0), client.level, client.level.getGameTime(), rotation,-1,model.poses.length-1));
    }
    public static void receive(SandboxNetwork.Visual packet) {
        Minecraft client = Minecraft.getInstance();
        if (!client.hasSingleplayerServer() || client.level == null || packet.race() < 0 || packet.race() >= 5) return;
        if (packet.action() == 0) {
            selectedRace = packet.race(); selectedUpgrade=packet.subject();
            if(client.player!=null)client.player.getPersistentData().putInt("JemSandboxRace",selectedRace);
            return;
        }
        if(packet.action()==36){SonicWaveRenderer.impact(packet);return;}
        if(packet.action()==27 || packet.action()==35 || packet.action()>=30 && packet.action()<=32){LifeChorusRenderer.receive(packet);return;}
        if(packet.action()==20 || packet.action()==21)return;
        if(packet.action()>=20 && packet.action()<=29) { UtilityVfx.receive(packet);return; }
        if (packet.action() >= 2 && packet.action() <= 4 && packet.race() == 4) { FlameSlashRenderer.receive(packet); return; }
        if(packet.action()==5 || packet.action()==7 || packet.action()==9) { ElementalRenderer.receive(packet);return; }
        if(packet.action()==8 || packet.action()==10 || packet.action()==34) {
            String id=packet.action()!=8?"entity_vines":"eruption"+Math.floorMod(packet.subject(),3);
            try {
                WorkbenchMesh model=CACHE.get(id);if(model==null){model=WorkbenchMesh.load(id,2);CACHE.put(id,model);}
                if(INSTANCES.size()>=64)INSTANCES.remove(0);
                INSTANCES.add(new Instance(model,packet.origin(),client.level,client.level.getGameTime(),packet.yaw(),packet.action()!=8?packet.subject():-1,packet.action()==10?90:model.poses.length-1));
            } catch(IOException error){LogUtils.getLogger().error("Cannot load elemental model "+id,error);label(client,"Elemental model failed to load; see log");}
            return;
        }
        if(packet.action()!=1 || packet.race()!=0)return;
        // Main-skill route: server-owned origins, not crosshair gallery placement.
        Asset asset = ASSETS[0];
        try {
            WorkbenchMesh model = CACHE.get(asset.id());
            if (model == null) { model = WorkbenchMesh.load(asset.id(),asset.materials()); CACHE.put(asset.id(),model); }
            Vec3 position = packet.origin();
            // Guardian emission already supplies its threat-facing origin and 180-degree correction.
            add(model,position,packet.yaw());
        } catch (IOException error) { LogUtils.getLogger().error("Cannot load main-skill Blockbench asset",error); label(client,"Main skill model failed to load; see log"); }
    }

    private static boolean clientReadyForAim(){var c=Minecraft.getInstance();return c.player!=null && !c.player.isShiftKeyDown() && c.screen==null;}
    @SubscribeEvent public static void hud(net.minecraftforge.client.event.RenderGuiEvent.Post event){VoidDistancePreview.hud(event);com.siirio.jemvisualsandbox.client.races.RaceAbilityHud.render(event);}
    private static void label(Minecraft client, String text) { client.player.displayClientMessage(Component.literal(text), true); }
    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        FlameSlashRenderer.render(event);
        ElementalRenderer.render(event);
        SurfaceVfx.render(event);UtilityVfx.render(event);LifeChorusRenderer.render(event);
        VoidDistancePreview.render(event,selectedRace==1 && secondaryDown && clientReadyForAim());
        for (Instance instance : java.util.List.copyOf(INSTANCES)) renderInstance(event,instance);
    }
    private static void renderInstance(RenderLevelStageEvent event, Instance instance) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != instance.level() || !client.hasSingleplayerServer()) return;
        WorkbenchMesh mesh = instance.mesh();
        Vec3 origin = instance.origin();
        long startTick = instance.startTick();
        float yaw = instance.yaw();
        double seconds = (client.level.getGameTime() - startTick + event.getPartialTick()) / 20.0D;
        net.minecraft.world.entity.Entity subject=null;
        if(instance.targetId()>=0) {
            subject=client.level.getEntity(instance.targetId());
            if(subject==null || !subject.isAlive())return;
            origin=subject.getPosition(event.getPartialTick());
            double tick=seconds*20;
            if(instance.ticks()==90)seconds=(tick<16?tick:tick<78?16:16+(tick-78)*8/12)/20;
        }
        float sx=1,sy=1,sz=1;
        if(subject!=null){var box=subject.getBoundingBox();sx=(float)(box.getXsize()+.16);sy=(float)(box.getYsize()/2);sz=(float)(box.getZsize()+.16);}
        drawMesh(VisualRenderContext.world(event),mesh,origin,yaw,seconds,sx,sy,sz,LevelRenderer.getLightColor(client.level,BlockPos.containing(origin).above()));
    }
    static void drawMesh(VisualRenderContext context,WorkbenchMesh mesh,Vec3 origin,float yaw,double seconds,float scaleX,float scaleY,float scaleZ,int packedLight) {
        Minecraft client=Minecraft.getInstance();
        if (seconds >= (mesh.poses.length - 1) * mesh.step) return;
        float sample = (float)(seconds / mesh.step);
        int first = Mth.clamp((int)sample, 0, mesh.poses.length - 2);
        float blend = sample - first;
        float[] before = mesh.poses[first];
        float[] after = mesh.poses[first + 1];
        Vec3 camera = context.getCamera().getPosition();
        PoseStack poses = context.getPoseStack();
        poses.pushPose();
        poses.translate(origin.x - camera.x, origin.y - camera.y, origin.z - camera.z);
        poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
        poses.scale(scaleX,scaleY,scaleZ);
        PoseStack.Pose pose = poses.last();
        int light = packedLight;
        var buffers = client.renderBuffers().bufferSource();
        for (int material = 0; material < mesh.textures.length; material++) {
            RenderType type = RenderType.entityCutoutNoCull(mesh.textures[material]);
            VertexConsumer out = buffers.getBuffer(type);
            String texturePath = mesh.textures[material].getPath();
            int materialLight = texturePath.startsWith("workbench/eruption") ? 15728880 : light;
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
                            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(materialLight)
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
