package com.siirio.jemrewards.client;

import java.util.UUID;
import com.siirio.jemrewards.RewardsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Native-texture take-only side slots; no client inventory mutation or container insertion path. */
@Mod.EventBusSubscriber(modid="jem_rewards", value=Dist.CLIENT)
public final class RewardsPanel {
    private static final ResourceLocation INVENTORY = new ResourceLocation("minecraft", "textures/gui/container/inventory.png");
    private static final UUID ZERO=new UUID(0,0);
    private static RewardsNetwork.Snapshot snapshot;
    private static Button previous,next,reveal;
    private static InventoryScreen active;
    private static long refreshed=-1;
    private static int x,y;
    public static void accept(RewardsNetwork.Snapshot value) {
        if(Minecraft.getInstance().screen==active) snapshot=value;
    }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if(!(event.getScreen() instanceof InventoryScreen screen))return;
        active=screen;snapshot=null;refreshed=-1;
        previous=Button.builder(Component.literal("<"),b->request(2,Math.max(0,snapshot.page()-1),ZERO,0)).bounds(0,0,20,20).build();
        next=Button.builder(Component.literal(">"),b->request(2,snapshot.page()+1,ZERO,0)).bounds(0,0,20,20).build();
        reveal=Button.builder(Component.translatable("jem_rewards.title"),b->{
            if(screen.getRecipeBookComponent().isVisible())screen.getRecipeBookComponent().toggleVisibility();
            screen.init(Minecraft.getInstance(),screen.width,screen.height);
        }).bounds(0,0,58,20).build();
        previous.visible=false;next.visible=false;reveal.visible=false;
        event.addListener(previous);event.addListener(next);event.addListener(reveal);
        request(0,0,ZERO,0);
    }
    @SubscribeEvent public static void close(ScreenEvent.Closing event) {
        if(event.getScreen()!=active)return;
        if(snapshot!=null)request(3,snapshot.page(),ZERO,0);
        active=null;snapshot=null;
    }
    private static void request(int action,int page,UUID bundle,int item) {
        RewardsNetwork.send(new RewardsNetwork.Request(action,snapshot==null?ZERO:snapshot.session(),snapshot==null?0:snapshot.revision(),page,bundle,item));
    }
    public static net.minecraft.client.renderer.Rect2i bounds(InventoryScreen screen) {
        if(screen!=active || snapshot==null || snapshot.rows().isEmpty())return new net.minecraft.client.renderer.Rect2i(0,0,0,0);
        boolean book=screen.getRecipeBookComponent().isVisible();
        int left=screen.getGuiLeft()-46-(book?147:0);
        if(left<0) return new net.minecraft.client.renderer.Rect2i(Math.max(0,screen.getGuiLeft()-58),screen.getGuiTop()+166,58,20);
        return new net.minecraft.client.renderer.Rect2i(left,screen.getGuiTop(),44,96);
    }
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        if(!(event.getScreen() instanceof InventoryScreen screen) || screen!=active)return;
        var mc=Minecraft.getInstance();if(mc.player==null || mc.level==null)return;
        long now=mc.level.getGameTime();
        if(now-refreshed>=20 || refreshed<0) {refreshed=now;request(0,snapshot==null?0:snapshot.page(),ZERO,0);}
        boolean visible=snapshot!=null && !snapshot.rows().isEmpty();
        previous.visible=false;next.visible=false;reveal.visible=false;
        if(!visible)return;
        var bounds=bounds(screen);x=bounds.getX();y=bounds.getY();
        if(bounds.getWidth()==58) {reveal.setX(x);reveal.setY(y);reveal.visible=true;return;}
        var g=event.getGuiGraphics();
        for(int i=0;i<snapshot.rows().size();i++) {
            int sx=x+4+(i%2)*18,sy=y+(i/2)*18;
            // Sample the exact native slot at 1:1; resource packs remain authoritative.
            g.blit(INVENTORY,sx,sy,7,83,18,18);
            ItemStack stack=ItemStack.of(snapshot.rows().getCompound(i).getCompound("Stack"));
            g.renderItem(stack,sx+1,sy+1);g.renderItemDecorations(mc.font,stack,sx+1,sy+1);
            if(event.getMouseX()>=sx && event.getMouseX()<sx+18 && event.getMouseY()>=sy && event.getMouseY()<sy+18)g.renderTooltip(mc.font,stack,event.getMouseX(),event.getMouseY());
        }
        previous.setX(x);previous.setY(y+74);previous.visible=snapshot.pages()>1;previous.active=snapshot.page()>0;
        next.setX(x+24);next.setY(y+74);next.visible=snapshot.pages()>1;next.active=snapshot.page()+1<snapshot.pages();
    }
    @SubscribeEvent public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if(event.getScreen()!=active || snapshot==null || snapshot.rows().isEmpty())return;
        var bounds=bounds(active);if(bounds.getWidth()!=44)return;
        for(int i=0;i<snapshot.rows().size();i++) {
            int sx=x+4+(i%2)*18,sy=y+(i/2)*18;
            if(event.getMouseX()<sx || event.getMouseX()>=sx+18 || event.getMouseY()<sy || event.getMouseY()>=sy+18)continue;
            event.setCanceled(true);
            // Every non-take interaction is consumed without sending any mutation.
            if(event.getButton()==0 && Minecraft.getInstance().player.inventoryMenu.getCarried().isEmpty()) {
                CompoundTag row=snapshot.rows().getCompound(i);request(1,snapshot.page(),row.getUUID("Bundle"),row.getInt("Item"));
            }
            return;
        }
    }
}
