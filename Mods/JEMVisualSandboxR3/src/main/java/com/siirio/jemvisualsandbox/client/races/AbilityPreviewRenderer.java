package com.siirio.jemvisualsandbox.client.races;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.siirio.jemvisualsandbox.workbench.AbilityPreviewPresentation;
import com.siirio.jemvisualsandbox.workbench.RacePowerStages;
import com.siirio.jemvisualsandbox.workbench.VisualRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Bounded real-entity GUI stage. Entities are never added to, or ticked by, a level. */
public final class AbilityPreviewRenderer {
    private final AbilityPreviewPresentation presentation = new AbilityPreviewPresentation();
    private ClientLevel sourceLevel;
    private RemotePlayer actor;
    private Creeper target;
    private IronGolem guardian;
    private ThrownEnderpearl pearl;
    private long previousFrame;
    private double elapsed;
    private int selection = -1, lastPoseTick = -1;
    private Vec3 lastPosePosition = AbilityPreviewPresentation.PLAYER;

    public void reset() {
        elapsed = 0; previousFrame = 0; selection = -1; lastPoseTick = -1;
        lastPosePosition = AbilityPreviewPresentation.PLAYER;
    }
    public void dispose() {
        actor = null; target = null; guardian = null; pearl = null; sourceLevel = null;
        presentation.dispose(); reset();
    }

    private boolean prepare() {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null) { dispose(); return false; }
        if (sourceLevel == client.level && actor != null
                && actor.getSkinTextureLocation().equals(client.player.getSkinTextureLocation())) return true;
        dispose();
        sourceLevel = client.level;
        ResourceLocation skin = client.player.getSkinTextureLocation();
        String model = client.player.getModelName();
        var parts = java.util.EnumSet.noneOf(PlayerModelPart.class);
        for (var part : PlayerModelPart.values()) if (client.player.isModelPartShown(part)) parts.add(part);
        actor = new RemotePlayer(sourceLevel, client.player.getGameProfile()) {
            @Override public ResourceLocation getSkinTextureLocation() { return skin; }
            @Override public String getModelName() { return model; }
            @Override public boolean isModelPartShown(PlayerModelPart part) { return parts.contains(part); }
        };
        target = new Creeper(EntityType.CREEPER, sourceLevel);
        guardian = new IronGolem(EntityType.IRON_GOLEM, sourceLevel);
        pearl = new ThrownEnderpearl(EntityType.ENDER_PEARL, sourceLevel);
        return true;
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       int race, int slot, int tier, int bloomStage, float partialTick) {
        var client = Minecraft.getInstance();
        if (width <= 0 || height <= 0 || x >= graphics.guiWidth() || y >= graphics.guiHeight()
                || x + width <= 0 || y + height <= 0 || !prepare()) { previousFrame = 0; return; }
        int key = race * 100 + slot * 40 + tier * 4 + bloomStage;
        long now = System.nanoTime();
        if (selection != key) { reset(); selection = key; }
        if (previousFrame != 0) {
            // A hidden screen advances no timeline; resume from the last visible frame.
            double gap = (now - previousFrame) / 1_000_000_000.0;
            if (gap < .25) elapsed = (elapsed + gap) % 5;
        }
        previousFrame = now;
        float ticks = (float)(elapsed * 20), age = ticks - 6;
        Vec3 position = presentation.playerPosition(race, slot, tier, age);
        int poseTick = (int)ticks;
        if (poseTick != lastPoseTick) {
            int deltaTicks = lastPoseTick < 0 || poseTick < lastPoseTick ? 1 : poseTick - lastPoseTick;
            float stride = (float)Math.min(1, position.subtract(lastPosePosition).horizontalDistance() * 4 / deltaTicks);
            actor.walkAnimation.update(stride, .4F);
            target.walkAnimation.update(race==1&&slot==0&&age>=14&&age<74?(tier>=3?.075F:tier>=2?.135F:.36F):0,.4F);
            lastPoseTick = poseTick; lastPosePosition = position;
        }
        float attack = age >= -6 && age < 0 ? (age + 6) / 6 : 0;
        if (race == 1 && slot == 0 && age >= 14 && age < 20) attack = (age - 14) / 6;
        if (race == 4 && slot == 1 && age >= 0 && age < 38)
            attack = Math.min(1, (age - (age >= 22 ? 22 : age >= 10 ? 10 : 0)) / 6);
        actor.attackAnim = actor.oAttackAnim = attack;
        actor.swingingArm = net.minecraft.world.InteractionHand.MAIN_HAND;
        actor.tickCount = target.tickCount = (int)ticks;
        face(actor, race == 1 && slot == 0 && age >= 14 && age < 64 ? 90 : -90);
        face(target, 90);
        face(guardian, -90);
        float hit = presentation.hitAge(race, slot, bloomStage, age);
        target.hurtTime = hit >= 0 && hit < 6 ? 5 : 0;
        // Echo detection is visual only; it never marks any real entity.
        target.setGlowingTag(race == 3 && slot == 1 && age >= 7 && age < 28);
        Vec3 targetPosition = AbilityPreviewPresentation.TARGET;
        if (slot == 0 && race == 0 && age >= 12)
            targetPosition = targetPosition.add(Math.min(1.1, (age - 12) * .12) * (1 - smooth((age - 55) / 30)), 0, 0);
        if (race == 3 && slot == 0 && tier >= 1 && age >= 15)
            targetPosition = targetPosition.add(.6 * smooth((age - 15) / 6) * (1 - smooth((age - 45) / 24)), 0, 0);

        if(race==1 && slot==0 && age>=14 && age<74) {
            float pace=tier>=3?.025F:tier>=2?.045F:.12F;
            targetPosition=targetPosition.add(0,0,Math.sin((age-14)*pace)*.55);
        }
        // Detached pose feedback represents mob disorientation, never target immunity.
        if(race==3 && slot==0 && tier>=2 && age>=15 && age<78) {
            target.yHeadRot=90+(float)Math.sin(age*.55)*32;
        }
        // Fire duration and strength belong to the server; only the dummy's flag is drawn here.
        target.setSharedFlagOnFire(race==4 && hit>=0 && hit<RacePowerStages.fireSeconds(tier)*20);

        graphics.flush();
        double sx = client.getWindow().getWidth() / (double)graphics.guiWidth();
        double sy = client.getWindow().getHeight() / (double)graphics.guiHeight();
        int vx = (int)Math.round(x * sx), vy = (int)Math.round((graphics.guiHeight() - y - height) * sy);
        int vw = Math.max(1, (int)Math.round(width * sx)), vh = Math.max(1, (int)Math.round(height * sy));
        float aspect = vw / (float)vh;
        float distance = Math.max(5.5F, 13 / aspect) / (2 * (float)Math.tan(Math.toRadians(17)));
        Vec3 camera = new Vec3(.28, .25, .925).normalize().scale(distance).add(0, 1.7, 0);
        Matrix4f view = new Matrix4f().lookAt((float)camera.x, (float)camera.y, (float)camera.z, 0, 1.7F, 0, 0, 1, 0);
        Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(34), aspect, .01F, 128);
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix3f oldInverseView = new Matrix3f(RenderSystem.getInverseViewRotationMatrix());
        var oldSorting = RenderSystem.getVertexSorting();
        var oldShader = RenderSystem.getShader();
        float[] oldColor = RenderSystem.getShaderColor().clone();
        float oldFogStart = RenderSystem.getShaderFogStart(), oldFogEnd = RenderSystem.getShaderFogEnd();
        int[] oldTextures = {RenderSystem.getShaderTexture(0), RenderSystem.getShaderTexture(1), RenderSystem.getShaderTexture(2)};
        int[] viewport = new int[4], scissor = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissor);
        boolean oldScissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean oldDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean oldBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean oldCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        var dispatcher = client.getEntityRenderDispatcher();
        Quaternionf oldCamera = new Quaternionf(dispatcher.cameraOrientation());
        var modelView = RenderSystem.getModelViewStack();
        var buffers = client.renderBuffers().bufferSource();
        modelView.pushPose();
        try {
            RenderSystem.viewport(vx, vy, vw, vh);
            int clipX = Math.max(0, vx), clipY = Math.max(0, vy);
            int clipRight = Math.min(client.getWindow().getWidth(), vx + vw), clipTop = Math.min(client.getWindow().getHeight(), vy + vh);
            if (oldScissor) {
                clipX = Math.max(clipX, scissor[0]); clipY = Math.max(clipY, scissor[1]);
                clipRight = Math.min(clipRight, scissor[0] + scissor[2]); clipTop = Math.min(clipTop, scissor[1] + scissor[3]);
            }
            RenderSystem.enableScissor(clipX, clipY, Math.max(0, clipRight - clipX), Math.max(0, clipTop - clipY));
            RenderSystem.enableDepthTest(); RenderSystem.depthMask(true); RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
            modelView.setIdentity(); RenderSystem.applyModelViewMatrix();
            RenderSystem.setInverseViewRotationMatrix(new Matrix3f(view).transpose());
            RenderSystem.setShaderFogStart(64); RenderSystem.setShaderFogEnd(128);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            dispatcher.overrideCameraOrientation(new Quaternionf().setFromNormalized(new Matrix3f(view)).conjugate());
            PoseStack poses = new PoseStack();
            poses.last().pose().set(view); poses.last().normal().set(new Matrix3f(view));
            boolean domain=AbilityPreviewPresentation.sculkDomainActive(race,slot,tier,age);
            if(domain) {
                // Native blocks on a fixed detached patch: no setBlock, world queue or terrain mutation.
                for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++) {
                    if(dx*dx+dz*dz>5)continue;
                    poses.pushPose();poses.translate(position.x+dx-.5,-1,position.z+dz-.5);
                    client.getBlockRenderer().renderSingleBlock(Blocks.SCULK.defaultBlockState(),poses,buffers,15728880,OverlayTexture.NO_OVERLAY);
                    poses.popPose();
                }
                poses.pushPose();poses.translate(position.x,position.y,position.z);poses.scale(1.15F,1.15F,1.15F);poses.translate(-position.x,-position.y,-position.z);
            }
            drawEntity(actor, position, poses, partialTick);
            if(domain)poses.popPose();
            drawEntity(target, targetPosition, poses, partialTick);
            if (race == 0 && slot == 1 && age >= 0 && age < 32+RacePowerStages.guardianTicks(tier)/25F)
                drawEntity(guardian, AbilityPreviewPresentation.PLAYER.add(1.5, 0, -1), poses, partialTick);
            if (race == 1 && slot == 1 && age >= 0 && age < AbilityPreviewPresentation.PEARL_FLIGHT_TICKS)
                drawEntity(pearl, AbilityPreviewPresentation.pearlPosition(age), poses, partialTick);
            if (race == 2 && slot == 1 && age >= 0 && age < 80) {
                for (int i = -1; i <= RacePowerStages.bridgeForward(tier); i++) for (int z = -1; z <= 1; z++) {
                    if (age < Math.max(0, i) * 4) continue;
                    poses.pushPose(); poses.translate(-3.5 + i, -1, z - .5);
                    client.getBlockRenderer().renderSingleBlock(Blocks.OAK_LEAVES.defaultBlockState(), poses, buffers, 15728880, OverlayTexture.NO_OVERLAY);
                    poses.popPose();
                }
            }
            buffers.endBatch();
            presentation.render(new VisualRenderContext(poses, Vec3.ZERO, partialTick, true), race, slot, tier, bloomStage, age);
            buffers.endBatch();
        } finally {
            dispatcher.overrideCameraOrientation(oldCamera);
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
            RenderSystem.setInverseViewRotationMatrix(oldInverseView);
            RenderSystem.setShader(() -> oldShader);
            RenderSystem.setShaderFogStart(oldFogStart);
            RenderSystem.setShaderFogEnd(oldFogEnd);
            for (int i = 0; i < oldTextures.length; i++) RenderSystem.setShaderTexture(i, oldTextures[i]);
            RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            if (oldScissor) RenderSystem.enableScissor(scissor[0], scissor[1], scissor[2], scissor[3]);
            else RenderSystem.disableScissor();
            RenderSystem.depthMask(oldDepthMask);
            RenderSystem.depthFunc(oldDepthFunc);
            if (oldDepth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (oldBlend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (oldCull) RenderSystem.enableCull(); else RenderSystem.disableCull();
        }
        // These captions state the conditional beat; they do not claim a second cast or a world effect.
        String caption=caption(race,slot,tier,age);
        if(!caption.isEmpty())graphics.drawString(client.font,
                client.font.plainSubstrByWidth(caption,Math.max(1,width-8)),x+4,y+4,0xD6D6D6,false);
    }

    private static String caption(int race,int slot,int tier,float age) {
        boolean ru=Minecraft.getInstance().options.languageCode.startsWith("ru");
        if(age<0)return ru?"Подготовка":"Preparing";
        if(slot==0) return switch(race) {
            case 0 -> age<13?(ru?"Исцеление · радиус ":"Healing · radius ")+RacePowerStages.chorusRadius(tier)
                    :tier>=3?(ru?"Полное HP → поглощение":"Full HP → absorption")
                    :tier>=2?(ru?"Очищение отрицательных эффектов":"Cleansing harmful effects"):(ru?"Щиты · −25% урона · 5 с":"Wards · −25% damage · 5s");
            case 1 -> tier>=2&&age>=14?(ru?"Замедление ":"Slow ")+RacePowerStages.riftSlowTicks(tier)/20+(ru?" с · радиус ":"s · radius ")+RacePowerStages.riftSlowRadius(tier)
                    :(ru?"Дальность ":"Reach ")+RacePowerStages.riftRange(tier);
            case 2 -> tier>=3?(ru?"Блокировка · сжатие · яд":"Action lock · constriction · poison")
                    :tier>=2?(ru?"Блокировка · сжатие 3 HP/с":"Action lock · constriction 3 HP/s")
                    :tier>=1?(ru?"Блокировка действий":"Action lock"):(ru?"Базовое попадание · 6 HP":"Initial hit · 6 HP");
            case 3 -> AbilityPreviewPresentation.sculkDomainActive(race,slot,tier,age)?(ru?"Внутри скалка: размер / атака ×1,15":"Inside sculk: size / attack ×1.15")
                    :tier>=2&&age>=15?(ru?"Тьма · дезориентация":"Darkness · disorientation")
                    :tier>=1&&age>=15?(ru?"Отбрасывание":"Knockback"):(ru?"12 HP · радиус 8":"12 HP · radius 8");
            case 4 -> (ru?"5 извержений · ":"5 eruptions · ")+decimal(RacePowerStages.eruptionDamage(tier))+" HP · "+RacePowerStages.fireSeconds(tier)+(ru?" с огня":"s fire");
            default -> "";
        };
        return switch(race) {
            case 0 -> (ru?"HP < 40% · страж ":"HP < 40% · guardian ")+RacePowerStages.guardianTicks(tier)/20+(ru?" с":"s");
            case 1 -> age<24?(ru?"B · заряженный жемчуг":"B · charged pearl"):("Shift+B · "+RacePowerStages.gravityTicks(tier)/20+(ru?" с без гравитации":"s gravity denial"));
            case 2 -> (ru?"Мост ":"Bridge ")+RacePowerStages.bridgeForward(tier)+(ru?" вперёд · ":" forward · ")+RacePowerStages.bridgeTicks(tier)/20+(ru?" с":"s");
            case 3 -> age<40?(ru?"Резонанс <25 · эхо ":"Resonance <25 · echo ")+RacePowerStages.echoRadius(tier)
                    :(ru?"Резонанс ≥25 · +":"Resonance ≥25 · +")+RacePowerStages.recoveryHeal(tier)+" HP · "+RacePowerStages.absorptionTicks(tier)/20+(ru?" с":"s");
            case 4 -> (ru?"3 рассечения · ":"3 slashes · ")+decimal(RacePowerStages.slashDamage(tier))+" HP · "+RacePowerStages.fireSeconds(tier)+(ru?" с огня":"s fire");
            default -> "";
        };
    }
    private static String decimal(double value){return String.format(java.util.Locale.ROOT,"%.1f",value);}

    private static void face(LivingEntity entity, float yaw) {
        entity.yBodyRot = entity.yBodyRotO = yaw; entity.yHeadRot = entity.yHeadRotO = yaw;
        entity.setYRot(yaw); entity.yRotO = yaw; entity.setXRot(0); entity.xRotO = 0;
    }
    private static <T extends Entity> void drawEntity(T entity, Vec3 at, PoseStack poses, float partialTick) {
        var client = Minecraft.getInstance();
        poses.pushPose(); poses.translate(at.x, at.y, at.z);
        RenderSystem.runAsFancy(() -> client.getEntityRenderDispatcher().getRenderer(entity)
                .render(entity, entity.getYRot(), partialTick, poses, client.renderBuffers().bufferSource(), 15728880));
        poses.popPose();
    }
    private static float smooth(float value) {
        float t = Mth.clamp(value, 0, 1); return t * t * (3 - 2 * t);
    }
}
