package com.siirio.jemeconomy.full.client;

import com.siirio.jemeconomy.shipping.ShippingBoxMenu;
import com.siirio.jemeconomy.shipping.ShippingBoxLayout;
import com.siirio.jemeconomy.client.CoinPriceRenderer;
import com.siirio.jemeconomy.client.CurrencyContainerScreen;
import com.siirio.jemeconomy.network.ContainerCurrencyPacket;
import com.siirio.jemeconomy.network.ContainerCurrencyStatusPacket;
import com.siirio.jemeconomy.network.EconomyNetwork;
import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemcurrency.CurrencyItems;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ShippingBoxScreen extends AbstractContainerScreen<ShippingBoxMenu> implements CurrencyContainerScreen {
    private static final ResourceLocation SALE=art("shipping-ui"), EXCHANGE=art("shipping-exchange"), CONTROLS=art("controls");
    private final List<Button> saleControls=new ArrayList<>(), exchangeControls=new ArrayList<>();
    private CoinDenomination source=CoinDenomination.SILVER, target=CoinDenomination.GOLD;
    private EditBox count, amount;
    private Button exchange, withdraw;
    private UUID pending;
    private long wallet;
    private String result="";
    public ShippingBoxScreen(ShippingBoxMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title); imageWidth=ShippingBoxLayout.WIDTH;imageHeight=ShippingBoxLayout.HEIGHT;
        inventoryLabelX=13;inventoryLabelY=182;
    }
    private static ResourceLocation art(String name){return new ResourceLocation("jem_economy","textures/gui/shipping/"+name+".png");}
    private static Component tr(String key){return Component.translatable("jem.economy.shipping."+key);}
    @Override protected void init(){
        String oldCount=count==null?"10":count.getValue(), oldAmount=amount==null?"1":amount.getValue();
        super.init();saleControls.clear();exchangeControls.clear();
        button(179,3,78,14,tr("sale_tab"),()->changeTab(0));
        button(263,3,91,14,tr("exchange_tab"),()->changeTab(1));
        saleControls.add(button(190,137,164,22,tr("collect"),()->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1)));
        exchangeControls.add(button(16,44,70,18,denomination(source),()->{source=next(source);rebuildWidgets();}));
        exchangeControls.add(button(98,44,72,18,denomination(target),()->{target=next(target);rebuildWidgets();}));
        count=field(49,90,80,oldCount);amount=field(194,100,154,oldAmount);
        exchangeControls.add(button(16,89,22,18,Component.literal("−"),()->count.setValue(Long.toString(Math.max(1L,number(count)-1)))));
        exchangeControls.add(button(138,89,32,18,Component.literal("+"),()->count.setValue(Long.toString(Math.min(4096L,number(count)+1)))));
        exchange=button(16,122,154,18,Component.translatable("jem.economy.exchange.confirm"),()->send(ContainerCurrencyPacket.Action.EXCHANGE));exchangeControls.add(exchange);
        withdraw=button(194,126,154,18,tr("withdraw"),()->send(ContainerCurrencyPacket.Action.WITHDRAW));exchangeControls.add(withdraw);
        if(menu.tab()==1)snapshot();updateControls();
    }
    private EditBox field(int x,int y,int width,String initial){
        EditBox field=new EditBox(font,leftPos+x,topPos+y,width,16,Component.empty());field.setMaxLength(10);
        field.setFilter(value->value.matches("[0-9]{0,10}"));field.setValue(initial);addRenderableWidget(field);return field;
    }
    private Button button(int x,int y,int width,int height,Component label,Runnable action){
        return addRenderableWidget(new Button(leftPos+x,topPos+y,width,height,label,ignored->action.run(),supplier->supplier.get()){
            @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partialTick){
                int v=!active?96:isHoveredOrFocused()?24:0;
                g.blit(CONTROLS,getX(),getY(),3,getHeight(),24,v,3,22,128,128);
                g.blit(CONTROLS,getX()+3,getY(),getWidth()-6,getHeight(),27,v,97,22,128,128);
                g.blit(CONTROLS,getX()+getWidth()-3,getY(),3,getHeight(),124,v,3,22,128,128);
                g.drawCenteredString(font,getMessage(),getX()+getWidth()/2,getY()+(getHeight()-8)/2,active?0xFFF3E6CD:0xFFBAAE98);
            }
        });
    }
    private void changeTab(int value){
        if(menu.tab()==value)return;menu.clientTab(value);minecraft.gameMode.handleInventoryButtonClick(menu.containerId,2+value);
        if(value==1)snapshot();updateControls();
    }
    private void snapshot(){request(ContainerCurrencyPacket.Action.SNAPSHOT,UUID.randomUUID());}
    private void send(ContainerCurrencyPacket.Action action){if(pending!=null)return;pending=UUID.randomUUID();request(action,pending);updateControls();}
    private void request(ContainerCurrencyPacket.Action action,UUID id){EconomyNetwork.containerCurrency(new ContainerCurrencyPacket(menu.containerId,action,number(amount),source,(int)Math.min(4096L,number(count)),target,id));}
    @Override public void acceptCurrency(ContainerCurrencyStatusPacket status){
        if(status.containerId()!=menu.containerId)return;wallet=status.wallet();
        if(status.requestId().equals(pending)){pending=null;result=status.result();}updateControls();
    }
    private static long number(EditBox field){try{return field==null||field.getValue().isBlank()?0L:Long.parseLong(field.getValue());}catch(NumberFormatException invalid){return 0L;}}
    private static CoinDenomination next(CoinDenomination value){return CoinDenomination.values()[(value.ordinal()+1)%5];}
    private static Component denomination(CoinDenomination value){return new ItemStack(CurrencyItems.item(value)).getHoverName();}
    private boolean exact(){long n=number(count);return n>0L&&n<=4096L&&source!=target&&n*source.value()%target.value()==0L;}
    private void updateControls(){
        boolean second=menu.tab()==1;
        saleControls.forEach(b->{b.visible=!second;b.active=!second;});exchangeControls.forEach(b->{b.visible=second;b.active=second&&pending==null;});
        if(count!=null){count.visible=second;count.active=second&&pending==null;}if(amount!=null){amount.visible=second;amount.active=second&&pending==null;}
        if(exchange!=null)exchange.active=second&&pending==null&&exact();
        if(withdraw!=null)withdraw.active=second&&pending==null&&number(amount)>0L&&number(amount)<=wallet;
    }
    @Override protected void containerTick(){super.containerTick();updateControls();}
    @Override protected void renderBg(GuiGraphics g,float partialTick,int mouseX,int mouseY){g.blit(menu.tab()==1?EXCHANGE:SALE,leftPos,topPos,0,0,imageWidth,imageHeight,imageWidth,imageHeight);}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick){
        renderBackground(g);super.render(g,mouseX,mouseY,partialTick);renderTooltip(g,mouseX,mouseY);
        if(menu.tab()==0 && mouseX>=leftPos+192 && mouseX<leftPos+354 && mouseY>=topPos+160 && mouseY<topPos+173)
            g.renderTooltip(font,font.split(tr("status."+statusKey(menu.statusCode())),220),mouseX,mouseY);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        g.drawString(font,title,35,6,0xFFF3E6CD,false);g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xFFF3E6CD,false);
        if(menu.tab()==0){
            g.drawString(font,tr("goods"),13,20,0xFFF3E6CD,false);g.drawString(font,tr("payout"),13,142,0xFFF3E6CD,false);
            price(g,"pending_label",menu.previewPayout(),32);price(g,"account_limit",menu.accountRemaining(),68);price(g,"global_limit",menu.globalRemaining(),104);
            g.drawString(font,font.plainSubstrByWidth(tr("status."+statusKey(menu.statusCode())).getString(),160),192,162,0xFF8EAFAC,false);
        }else{
            g.drawString(font,tr("exchange_tab"),13,29,0xFFF3E6CD,false);g.drawString(font,tr("wallet"),192,30,0xFFF3E6CD,false);
            CoinPriceRenderer.draw(g,font,wallet,194,51,154,0xFFF3E6CD);g.drawString(font,tr("withdraw_amount"),192,85,0xFFF3E6CD,false);
            long receive=exact()?number(count)*source.value()/target.value():0L;
            g.renderItem(new ItemStack(CurrencyItems.item(source)),22,66);g.drawString(font,Long.toString(number(count)),42,71,0xFFF3E6CD,false);
            g.drawString(font,"→",85,71,0xFFF3E6CD,false);g.renderItem(new ItemStack(CurrencyItems.item(target)),116,66);g.drawString(font,Long.toString(receive),136,71,0xFFF3E6CD,false);
            Component message=result.isBlank()?tr("inventory_destination"):Component.translatable(result);
            g.drawString(font,font.plainSubstrByWidth(message.getString(),330),16,156,0xFFF3E6CD,false);
        }
    }
    private void price(GuiGraphics g,String key,long value,int y){g.drawString(font,tr(key),192,y,0xFFF3E6CD,false);CoinPriceRenderer.draw(g,font,value,192,y+13,160,0xFFE5C46F);}
    private static String statusKey(int v){return switch(v){case 1->"preview";case 2->"paid";case 3->"empty";case 4->"blocked_item";case 5->"account_daily_cap";case 6->"global_daily_cap";case 7->"item_account_cap";case 8->"item_global_cap";case 9->"budget_too_small";case 10->"payout_full";case 11->"market_budget_exhausted";case 12->"quarantined";case 13->"sale_busy";default->"waiting";};}
}
