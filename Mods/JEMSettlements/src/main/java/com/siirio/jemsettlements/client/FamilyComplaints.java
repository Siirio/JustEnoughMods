package com.siirio.jemsettlements.client;

import com.siirio.jemsettlements.JemSettlements;
import com.siirio.jemsettlements.network.FamilyComplaintPacket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=JemSettlements.MOD_ID,value=Dist.CLIENT)
public final class FamilyComplaints {
    private record Notice(int entityId,String reason,long expires) { }
    private static final Map<UUID,Notice> NOTICES=new LinkedHashMap<>(); private static Object world;
    private FamilyComplaints(){ }
    public static void accept(FamilyComplaintPacket packet){var mc=Minecraft.getInstance();if(mc.level==null)return;if(world!=mc.level){NOTICES.clear();world=mc.level;}if(NOTICES.size()>=128)NOTICES.remove(NOTICES.keySet().iterator().next());NOTICES.put(packet.entityUuid(),new Notice(packet.entityId(),packet.reason(),mc.level.getGameTime()+100));}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){NOTICES.clear();world=null;}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;var mc=Minecraft.getInstance();if(mc.level==null||mc.options.hideGui)return;if(world!=mc.level){NOTICES.clear();world=mc.level;return;}NOTICES.values().removeIf(n->n.expires()<=mc.level.getGameTime());var camera=event.getCamera();var origin=camera.getPosition();var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();boolean drawn=false;
        for(var entry:NOTICES.entrySet()){var entity=mc.level.getEntity(entry.getValue().entityId());if(entity==null||!entity.getUUID().equals(entry.getKey())||origin.distanceToSqr(entity.position())>24*24)continue;var p=entity.getPosition(event.getPartialTick());pose.pushPose();pose.translate(p.x-origin.x,p.y+entity.getBbHeight()+.8-origin.y,p.z-origin.z);pose.mulPose(camera.rotation());pose.scale(-.025F,-.025F,.025F);var lines=mc.font.split(Component.translatable("jem.settlements.family.complaint."+entry.getValue().reason()),190);for(int i=0;i<lines.size();i++)mc.font.drawInBatch(lines.get(i),-mc.font.width(lines.get(i))/2F,(i-lines.size()+1)*10,0xffffffff,false,pose.last().pose(),buffers,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0x80000000,15728880);pose.popPose();drawn=true;}if(drawn)buffers.endBatch();}
}
