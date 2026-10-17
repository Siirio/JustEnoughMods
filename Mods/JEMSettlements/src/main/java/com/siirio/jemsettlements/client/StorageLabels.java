package com.siirio.jemsettlements.client;
import com.siirio.jemsettlements.JemSettlements;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=JemSettlements.MOD_ID,value=Dist.CLIENT)
public final class StorageLabels {
    private record View(CompoundTag data,long expires) {}
    private static final java.util.Map<java.util.UUID,View> VIEWS=new java.util.HashMap<>();
    public static void accept(CompoundTag tag){var mc=Minecraft.getInstance();if(mc.level!=null&&tag.hasUUID("Town")){if(VIEWS.size()>=256)VIEWS.clear();VIEWS.put(tag.getUUID("Town"),new View(tag,mc.level.getGameTime()+60));}}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){VIEWS.clear();}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.options.hideGui)return;
        VIEWS.values().removeIf(view->view.expires()<mc.level.getGameTime());
        var camera=event.getCamera();var origin=camera.getPosition();var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();
        for(var view:VIEWS.values()) {
            if(!view.data().getString("Dimension").equals(mc.level.dimension().location().toString()))continue;
            ListTag rows=view.data().getList("Labels",Tag.TAG_COMPOUND);
            for(int i=0;i<Math.min(64,rows.size());i++) {
                CompoundTag row=rows.getCompound(i);BlockPos pos=BlockPos.of(row.getLong("Position"));if(!mc.level.hasChunkAt(pos)||mc.level.getBlockEntity(pos)==null)continue;
                BlockPos other=row.contains("Second")?BlockPos.of(row.getLong("Second")):pos;
                double x=(pos.getX()+other.getX())*.5+.5,z=(pos.getZ()+other.getZ())*.5+.5,y=pos.getY()+1.3;
                if(origin.distanceToSqr(x,y,z)>32*32)continue;
                Component text=Component.translatable("jem.settlements.storage_name",row.getInt("Number"));pose.pushPose();pose.translate(x-origin.x,y-origin.y,z-origin.z);pose.mulPose(camera.rotation());pose.scale(-.025F,-.025F,.025F);
                mc.font.drawInBatch(text,-mc.font.width(text)/2F,0,0xffffffff,false,pose.last().pose(),buffers,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0x70000000,15728880);pose.popPose();
            }
        }
        buffers.endBatch();
    }
}
