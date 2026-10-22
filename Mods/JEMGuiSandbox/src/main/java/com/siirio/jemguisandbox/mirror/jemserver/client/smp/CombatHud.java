package com.siirio.jemguisandbox.mirror.jemserver.client.smp;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
public final class CombatHud {
    public static void head(GuiGraphics g, UUID id, int x, int y, int size) {
        var connection=Minecraft.getInstance().getConnection();
        var info=connection==null?null:connection.getPlayerInfo(id);
        var skin=info==null?DefaultPlayerSkin.getDefaultSkin(id):info.getSkinLocation();
        g.blit(skin,x,y,size,size,8,8,8,8,64,64); g.blit(skin,x,y,size,size,40,8,8,8,64,64);
    }
    public static Component title(CompoundTag data) { return Component.literal(data.getString("title")); }
}
