package com.siirio.jemserver.client.smp;

import com.mojang.blaze3d.systems.RenderSystem;
import com.siirio.jemserver.smp.CombatNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="jem_server",value=Dist.CLIENT)
public final class CombatHud {
    private static final int HEAD=20;
    private static final int GAP=5;
    private static final int MARGIN=7;
    private static CompoundTag snapshot=new CompoundTag();

    public static void accept(CombatNetwork.Update update) {
        if(update.result()) {
            var client=Minecraft.getInstance();
            if(client.screen instanceof ResultScreen result && result.resultId().equals(update.data().getUUID("id"))) result.update(update.data());
            else if(!update.data().getBoolean("refresh")) client.setScreen(new ResultScreen(update.data()));
            if(!update.data().getBoolean("refresh")) snapshot=new CompoundTag();
            return;
        }
        snapshot=update.data()==null?new CompoundTag():update.data().copy();
    }

    public static boolean active() {
        var player=Minecraft.getInstance().player;
        if(player==null || !snapshot.hasUUID("id")) return false;
        for(var tag:snapshot.getList("rows",Tag.TAG_COMPOUND)) {
            var row=(CompoundTag)tag;
            if(row.getUUID("id").equals(player.getUUID())) return row.getString("state").equals("ALIVE");
        }
        return false;
    }

    public static void head(GuiGraphics graphics,java.util.UUID id,int x,int y,int size) {
        var connection=Minecraft.getInstance().getConnection();
        var info=connection==null?null:connection.getPlayerInfo(id);
        var skin=info==null?DefaultPlayerSkin.getDefaultSkin(id):info.getSkinLocation();
        graphics.blit(skin,x,y,size,size,8,8,8,8,64,64);
        graphics.blit(skin,x,y,size,size,40,8,8,8,64,64);
    }

    public static Component title(CompoundTag data) {
        return data.getBoolean("blood")?Component.translatable("jem.smp.BLOOD_MOON"):data.getString("titleKey").isEmpty()?Component.literal(data.getString("title")):Component.translatable(data.getString("titleKey"));
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        var client=Minecraft.getInstance();
        if(!snapshot.hasUUID("id") || client.player==null || client.level==null || client.screen!=null || client.options.hideGui) return;
        if(!client.level.dimension().location().toString().equals(snapshot.getString("dimension"))) {clear();return;}
        var rows=snapshot.getList("rows",Tag.TAG_COMPOUND);
        int x=client.getWindow().getGuiScaledWidth()-MARGIN-HEAD;
        int y=MARGIN;
        for(var tag:rows) {
            var row=(CompoundTag)tag;
            renderPlayer(event.getGuiGraphics(),row,x,y);
            y+=HEAD+GAP;
        }
        if(active() && client.hitResult instanceof EntityHitResult hit) for(var tag:rows) {
            var row=(CompoundTag)tag;
            if(row.getString("state").equals("DOWNED") && row.getUUID("id").equals(hit.getEntity().getUUID()))
                event.getGuiGraphics().drawCenteredString(client.font,Component.translatable("jem.smp.combat.revive",client.options.keyUse.getTranslatedKeyMessage(),row.getString("name")),client.getWindow().getGuiScaledWidth()/2,client.getWindow().getGuiScaledHeight()/2+24,0xFFF4E1C1);
        }
    }

    private static void renderPlayer(GuiGraphics graphics,CompoundTag row,int x,int y) {
        String state=row.getString("state");
        boolean eliminated=state.equals("ELIMINATED");
        boolean disconnected=state.equals("DISCONNECTED");
        if(eliminated) RenderSystem.setShaderColor(.35F,.35F,.35F,1F);
        else if(disconnected) RenderSystem.setShaderColor(.65F,.65F,.65F,.45F);
        head(graphics,row.getUUID("id"),x,y,HEAD);
        RenderSystem.setShaderColor(1F,1F,1F,1F);
        int color=state.equals("DOWNED")?0xFFFF3F3F:eliminated?0xFF777777:disconnected?0xFFB6A987:0xFFE8D8BA;
        graphics.fill(x-1,y-1,x+HEAD+1,y,color);
        graphics.fill(x-1,y+HEAD,x+HEAD+1,y+HEAD+1,color);
        graphics.fill(x-1,y,x,y+HEAD,color);
        graphics.fill(x+HEAD,y,x+HEAD+1,y+HEAD,color);
        if(state.equals("ALIVE")) {
            int width=Math.max(0,Math.min(HEAD,Math.round(HEAD*row.getFloat("hp")/Math.max(1F,row.getFloat("maxHp")))));
            graphics.fill(x,y+HEAD-2,x+width,y+HEAD,0xFFD83B3B);
        }
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {clear();}
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {if(event.getLevel().isClientSide()) clear();}
    private static void clear() {snapshot=new CompoundTag();}
    private CombatHud() {}
}
