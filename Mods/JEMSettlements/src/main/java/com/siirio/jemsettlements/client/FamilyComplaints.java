package com.siirio.jemsettlements.client;

import com.siirio.jemsettlements.JemSettlements;
import com.siirio.jemsettlements.network.FamilyComplaintPacket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemSettlements.MOD_ID, value = Dist.CLIENT)
public final class FamilyComplaints {
    private record Notice(int entityId, List<FormattedCharSequence> lines, long expires) { }
    private record Visible(UUID id, Notice notice, Entity entity, double distance) { }
    private static final Map<UUID, Notice> NOTICES = new LinkedHashMap<>();
    private static Object world;

    private FamilyComplaints() { }

    public static void accept(FamilyComplaintPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (world != minecraft.level) {
            NOTICES.clear();
            world = minecraft.level;
        }
        if (packet.reason().isBlank()) {
            NOTICES.remove(packet.entityUuid());
            return;
        }
        if (NOTICES.size() >= 128) NOTICES.remove(NOTICES.keySet().iterator().next());
        List<FormattedCharSequence> lines = List.copyOf(minecraft.font.split(
                Component.translatable("jem.settlements.family.complaint." + packet.reason()), 190));
        NOTICES.put(packet.entityUuid(), new Notice(packet.entityId(), lines, minecraft.level.getGameTime() + 2_400L));
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        NOTICES.clear();
        world = null;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options.hideGui) return;
        if (world != minecraft.level) {
            NOTICES.clear();
            world = minecraft.level;
            return;
        }
        long now = minecraft.level.getGameTime();
        var camera = event.getCamera();
        var origin = camera.getPosition();
        List<Visible> visible = new ArrayList<>();
        var iterator = NOTICES.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Entity entity = minecraft.level.getEntity(entry.getValue().entityId());
            if (entry.getValue().expires() <= now || entity == null || !entity.getUUID().equals(entry.getKey())) {
                iterator.remove();
                continue;
            }
            double distance = origin.distanceToSqr(entity.position());
            if (distance <= 24 * 24) visible.add(new Visible(entry.getKey(), entry.getValue(), entity, distance));
        }
        visible.sort(java.util.Comparator.comparingDouble(Visible::distance));
        var pose = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();
        int count = Math.min(4, visible.size());
        for (int index = 0; index < count; index++) {
            Visible row = visible.get(index);
            var position = row.entity().getPosition(event.getPartialTick());
            pose.pushPose();
            pose.translate(position.x - origin.x, position.y + row.entity().getBbHeight() + .8 - origin.y, position.z - origin.z);
            pose.mulPose(camera.rotation());
            pose.scale(-.025F, -.025F, .025F);
            for (int line = 0; line < row.notice().lines().size(); line++) {
                FormattedCharSequence text = row.notice().lines().get(line);
                minecraft.font.drawInBatch(text, -minecraft.font.width(text) / 2F,
                        (line - row.notice().lines().size() + 1) * 10, 0xffffffff, false,
                        pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0x80000000, 15728880);
            }
            pose.popPose();
        }
        if (count > 0) buffers.endBatch();
    }
}
