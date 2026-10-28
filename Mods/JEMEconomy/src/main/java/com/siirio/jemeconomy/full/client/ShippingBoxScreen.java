package com.siirio.jemeconomy.full.client;

import com.siirio.jemeconomy.shipping.ShippingBoxMenu;
import com.siirio.jemeconomy.shipping.ShippingBoxLayout;
import com.siirio.jemeconomy.client.CoinPriceRenderer;
import com.siirio.jemeconomy.client.CurrencyContainerScreen;
import com.siirio.jemeconomy.client.MarketClientState;
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
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ShippingBoxScreen extends AbstractContainerScreen<ShippingBoxMenu> implements CurrencyContainerScreen {
    private static final ResourceLocation SALE=art("shipping-ui"), EXCHANGE=art("shipping-exchange"), CONTROLS=art("controls");
    private final List<Button> saleControls=new ArrayList<>(), exchangeControls=new ArrayList<>();
    private CoinDenomination source=CoinDenomination.SILVER, target=CoinDenomination.GOLD, cash=CoinDenomination.COPPER;
    private EditBox count, cashCount, recipient;
    private int dropdown, dropdownIndex;
    private Button exchange, withdraw, deposit, sendButton, sourceChooser, targetChooser, cashChooser;
    private UUID pending;
    private ContainerCurrencyPacket.Action pendingAction;
    private long wallet;
    private final int[] heldCounts=new int[CoinDenomination.values().length];
    private final int[] exchangeCounts=new int[CoinDenomination.values().length];
    private boolean sourceCountEdited, assigningSourceCount;
    private int refreshTicks;
    private String result="";
    public ShippingBoxScreen(ShippingBoxMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title); imageWidth=ShippingBoxLayout.WIDTH;imageHeight=ShippingBoxLayout.HEIGHT;
        inventoryLabelX=13;inventoryLabelY=182;
    }
    private static ResourceLocation art(String name){return new ResourceLocation("jem_economy","textures/gui/shipping/"+name+".png");}
    private static Component tr(String key){return Component.translatable("jem.economy.shipping."+key);}
    @Override protected void init(){
        String oldCount=count==null?"0":count.getValue(), oldCashCount=cashCount==null?"1":cashCount.getValue();
        String oldRecipient=recipient==null?"":recipient.getValue();
        super.init();saleControls.clear();exchangeControls.clear();
        button(160,3,58,14,tr("sale_tab"),()->changeTab(0));
        button(222,3,78,14,tr("exchange_tab"),()->changeTab(1));
        button(304,3,50,14,tr("tavern_tab"),()->MarketClientState.open(this));
        saleControls.add(button(190,137,164,22,tr("collect"),()->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1)));
        sourceChooser=button(35,48,24,24,denomination(source),()->toggleDropdown(1));exchangeControls.add(sourceChooser);
        targetChooser=button(124,48,24,24,denomination(target),()->toggleDropdown(2));exchangeControls.add(targetChooser);
        count=field(16,91,76,oldCount,tr("source_count"));
        count.setResponder(value->{if(!assigningSourceCount)sourceCountEdited=true;updateControls();});
        cashChooser=button(194,89,24,24,denomination(cash),()->toggleDropdown(3));exchangeControls.add(cashChooser);
        cashChooser.setTooltip(Tooltip.create(denomination(cash)));
        cashCount=field(226,92,122,oldCashCount,tr("coin_count"));
        exchange=button(16,122,154,18,Component.translatable("jem.economy.exchange.confirm"),()->send(ContainerCurrencyPacket.Action.EXCHANGE));exchangeControls.add(exchange);
        withdraw=button(274,126,74,18,tr("withdraw_short"),()->send(ContainerCurrencyPacket.Action.WITHDRAW));exchangeControls.add(withdraw);
        deposit=button(194,126,74,18,tr("deposit"),()->send(ContainerCurrencyPacket.Action.DEPOSIT));exchangeControls.add(deposit);
        recipient = new EditBox(font,leftPos+194,topPos+149,92,16,tr("recipient")); recipient.setMaxLength(16); addRenderableWidget(recipient);
        recipient.setHint(tr("recipient"));recipient.setValue(oldRecipient);
        sendButton=button(291,148,57,18,tr("send"),()->send(ContainerCurrencyPacket.Action.SEND));exchangeControls.add(sendButton);
        if(menu.tab()==1)snapshot();updateControls();
    }
    private EditBox field(int x,int y,int width,String initial,Component label){
        EditBox field=new EditBox(font,leftPos+x,topPos+y,width,16,label);field.setMaxLength(10);
        field.setFilter(value->value.matches("[0-9]{0,10}"));field.setValue(initial);addRenderableWidget(field);return field;
    }
    private Button button(int x,int y,int width,int height,Component label,Runnable action){
        return addRenderableWidget(new Button(leftPos+x,topPos+y,width,height,label,ignored->action.run(),supplier->supplier.get()){
            private long pressedUntil;
            @Override public void onPress(){pressedUntil=net.minecraft.Util.getMillis()+120;super.onPress();}
            @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partialTick){
                int v=net.minecraft.Util.getMillis()<pressedUntil?75:!active?100:isHovered()?25:isFocused()?50:0;
                g.blit(CONTROLS,getX(),getY(),3,getHeight(),23,v,3,22,128,128);
                g.blit(CONTROLS,getX()+3,getY(),getWidth()-6,getHeight(),26,v,97,22,128,128);
                g.blit(CONTROLS,getX()+getWidth()-3,getY(),3,getHeight(),123,v,3,22,128,128);
                if(this!=sourceChooser&&this!=targetChooser&&this!=cashChooser) {
                    float scale=Math.min(1F,(getWidth()-8F)/Math.max(1,font.width(getMessage())));
                    g.pose().pushPose();g.pose().translate(getX()+getWidth()/2F,getY()+(getHeight()-8*scale)/2F,0);g.pose().scale(scale,scale,1);
                    g.drawString(font,getMessage(),-font.width(getMessage())/2,0,active?0xFFF3E6CD:0xFFBAAE98,false);g.pose().popPose();
                }
            }
        });
    }
    private void changeTab(int value){
        if(menu.tab()==value)return;dropdown=0;menu.clientTab(value);minecraft.gameMode.handleInventoryButtonClick(menu.containerId,2+value);
        if(value==1)snapshot();updateControls();
    }
    private void snapshot(){request(ContainerCurrencyPacket.Action.SNAPSHOT,UUID.randomUUID());}
    private void send(ContainerCurrencyPacket.Action action){if(pending!=null)return;pending=UUID.randomUUID();pendingAction=action;request(action,pending);updateControls();}
    private void request(ContainerCurrencyPacket.Action action,UUID id){EconomyNetwork.containerCurrency(new ContainerCurrencyPacket(menu.containerId,action,source,selectedCount(count),target,cash,selectedCount(cashCount),id,recipient==null?"":recipient.getValue()));}
    @Override public void acceptCurrency(ContainerCurrencyStatusPacket status){
        if(status.containerId()!=menu.containerId)return;wallet=status.wallet();
        for(CoinDenomination denomination:CoinDenomination.values()){
            heldCounts[denomination.ordinal()]=status.held(denomination);
            exchangeCounts[denomination.ordinal()]=status.exchangeable(denomination);
        }
        if(status.requestId().equals(pending)){
            boolean exchanged=pendingAction==ContainerCurrencyPacket.Action.EXCHANGE
                    && (status.result().endsWith(".committed")||status.result().endsWith(".replayed"));
            pending=null;pendingAction=null;result=status.result();
            if(exchanged)sourceCountEdited=false;
        }
        if(!sourceCountEdited)fillSourceCount();
        updateControls();
    }
    private static long number(EditBox field){try{return field==null||field.getValue().isBlank()?0L:Long.parseLong(field.getValue());}catch(NumberFormatException invalid){return 0L;}}
    private static int selectedCount(EditBox field){long value=number(field);return value>0L&&value<=4_096L?(int)value:0;}
    private void toggleDropdown(int value){
        if(dropdown==value){dropdown=0;}else{dropdown=value;dropdownIndex=(value==1?source:value==2?target:cash).ordinal();setFocused(null);}
        updateControls();
    }
    private void chooseDenomination(){
        Button chooser=dropdown==1?sourceChooser:dropdown==2?targetChooser:cashChooser;
        if(dropdown==1){source=CoinDenomination.values()[dropdownIndex];sourceCountEdited=false;fillSourceCount();}
        else if(dropdown==2)target=CoinDenomination.values()[dropdownIndex];
        else cash=CoinDenomination.values()[dropdownIndex];
        dropdown=0;sourceChooser.setMessage(denomination(source));targetChooser.setMessage(denomination(target));cashChooser.setMessage(denomination(cash));
        cashChooser.setTooltip(Tooltip.create(denomination(cash)));
        updateControls();setFocused(chooser);
    }
    private static Component denomination(CoinDenomination value){return new ItemStack(CurrencyItems.item(value)).getHoverName();}
    private int held(CoinDenomination denomination){return heldCounts[denomination.ordinal()];}
    private int exchangeable(CoinDenomination denomination){return exchangeCounts[denomination.ordinal()];}
    private void fillSourceCount(){
        if(count==null)return;
        assigningSourceCount=true;
        count.setValue(Integer.toString(Math.min(4_096,exchangeable(source))));
        assigningSourceCount=false;
    }
    private CoinDenomination.Conversion conversion(){return source.conversionTo(target,(int)Math.min(4_096L,number(count)));}
    private void updateControls(){
        boolean second=menu.tab()==1;
        if (!saleControls.isEmpty()) saleControls.get(0).setMessage(tr(menu.hasPayment()?"collect_payment":"sell"));
        saleControls.forEach(b->{b.visible=!second;b.active=!second;});exchangeControls.forEach(b->{b.visible=second;b.active=second&&pending==null;});
        if(count!=null){count.visible=second;count.active=second&&pending==null&&dropdown==0;}
        if(cashCount!=null){cashCount.visible=second;cashCount.active=second&&pending==null&&dropdown==0;}
        if(recipient!=null){recipient.visible=second;recipient.active=second&&pending==null&&dropdown==0;}
        if(exchange!=null){
            CoinDenomination.Conversion conversion=conversion();
            exchange.active=second&&pending==null&&conversion.possible()&&number(count)<=4_096L
                    &&number(count)<=exchangeable(source);
            exchange.setTooltip(Tooltip.create(Component.translatable("jem.economy.shipping.exchange_consumes",
                    conversion.sourceCount(),Math.max(0L,number(count)-conversion.sourceCount()))));
        }
        long cashUnits=number(cashCount), cashValue=cashUnits*cash.value();
        if(withdraw!=null)withdraw.active=second&&pending==null&&cashUnits>0L&&cashUnits<=4_096L&&cashValue<=wallet;
        if(deposit!=null)deposit.active=second&&pending==null&&cashUnits>0L&&cashUnits<=4_096L&&cashUnits<=held(cash);
        if(sendButton!=null)sendButton.active=second&&pending==null&&cashUnits>0L&&cashUnits<=4_096L
                &&cashValue<=wallet&&recipient!=null&&!recipient.getValue().isBlank();
    }
    @Override protected void containerTick(){
        super.containerTick();
        if(menu.tab()==1&&pending==null&&++refreshTicks>=20){refreshTicks=0;snapshot();}
        if(menu.tab()!=1)refreshTicks=0;
        updateControls();
    }
    @Override protected void renderBg(GuiGraphics g,float partialTick,int mouseX,int mouseY){g.blit(menu.tab()==1?EXCHANGE:SALE,leftPos,topPos,0,0,imageWidth,imageHeight,imageWidth,imageHeight);}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick){
        renderBackground(g);super.render(g,mouseX,mouseY,partialTick);renderTooltip(g,mouseX,mouseY);
        if (menu.tab()==1) {
            g.pose().pushPose(); g.pose().translate(0,0,400);
            g.renderItem(new ItemStack(CurrencyItems.item(source)),leftPos+39,topPos+52);
            g.renderItem(new ItemStack(CurrencyItems.item(target)),leftPos+128,topPos+52);
            g.renderItem(new ItemStack(CurrencyItems.item(cash)),leftPos+198,topPos+93);
            if (dropdown!=0) {
                // Item rendering uses its own Z lift. Raise the menu above the chooser coins as well.
                g.pose().pushPose(); g.pose().translate(0,0,250);
                int x=leftPos+(dropdown==1?16:dropdown==2?98:194), y=topPos+(dropdown==3?88:74);
                for (var denomination : CoinDenomination.values()) {
                    int row=y+denomination.ordinal()*18;
                    g.blit(CONTROLS,x,row,72,18,23,denomination.ordinal()==dropdownIndex||mouseX>=x&&mouseX<x+72&&mouseY>=row&&mouseY<row+18?25:0,103,22,128,128);
                    g.renderItem(new ItemStack(CurrencyItems.item(denomination)),x+3,row+1);
                    g.drawString(font,Long.toString(denomination.value()),x+23,row+5,0xFFF3E6CD,false);
                }
                g.pose().popPose();
            }
            g.pose().popPose();
        }
        if(menu.tab()==0 && mouseX>=leftPos+192 && mouseX<leftPos+354 && mouseY>=topPos+160 && mouseY<topPos+173)
            g.renderTooltip(font,font.split(tr("status."+statusKey(menu.statusCode())),220),mouseX,mouseY);
        if(menu.tab()==1 && !result.isBlank() && mouseX>=leftPos+194 && mouseX<leftPos+348
                && mouseY>=topPos+70 && mouseY<topPos+82)
            g.renderTooltip(font,font.split(Component.translatable(result),220),mouseX,mouseY);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        g.drawString(font,title,35,6,0xFFF3E6CD,false);g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xFFF3E6CD,false);
        if(menu.tab()==0){
            g.drawString(font,tr("goods"),13,20,0xFFF3E6CD,false);g.drawString(font,tr("payout"),13,142,0xFFF3E6CD,false);
            price(g,"pending_label",menu.previewPayout(),32);
            g.drawString(font,font.plainSubstrByWidth(tr("status."+statusKey(menu.statusCode())).getString(),160),192,162,0xFF8EAFAC,false);
        }else{
            g.drawString(font,tr("exchange_tab"),13,29,0xFFF3E6CD,false);g.drawString(font,tr("wallet"),192,30,0xFFF3E6CD,false);
            CoinPriceRenderer.draw(g,font,wallet,194,51,154,0xFFF3E6CD);
            g.drawString(font,"→",90,56,0xFFF3E6CD,false);
            g.drawString(font,tr("source_count"),16,78,0xFFF3E6CD,false);
            g.drawString(font,tr("coin_count"),226,82,0xFFF3E6CD,false);
            g.drawCenteredString(font,Integer.toString(conversion().targetCount()),136,94,0xFFF3E6CD);
            if(!result.isBlank())g.drawString(font,font.plainSubstrByWidth(Component.translatable(result).getString(),154),194,72,0xFFF3E6CD,false);
        }
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if (dropdown!=0) {
            int dx=leftPos+(dropdown==1?16:dropdown==2?98:194), dy=topPos+(dropdown==3?88:74);
            if (button==0 && x>=dx && x<dx+72 && y>=dy && y<dy+90) {
                dropdownIndex=(int)(y-dy)/18;chooseDenomination();return true;
            }
            dropdown=0;updateControls();return true;
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if (menu.paymentLocked() && (key==256 || minecraft.options.keyInventory.matches(key,scan))) {
            minecraft.player.displayClientMessage(tr("payment_required"),true); return true;
        }
        if (dropdown!=0) {
            if(key==256||key==258){Button chooser=dropdown==1?sourceChooser:dropdown==2?targetChooser:cashChooser;dropdown=0;updateControls();setFocused(chooser);if(key==256)return true;}
            else if(key==264||key==265){dropdownIndex=Math.floorMod(dropdownIndex+(key==264?1:-1),CoinDenomination.values().length);return true;}
            else if(key==268||key==269){dropdownIndex=key==268?0:CoinDenomination.values().length-1;return true;}
            else if(key==257||key==335||key==32){chooseDenomination();return true;}
            else return true;
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean shouldCloseOnEsc() { return !menu.paymentLocked(); }
    @Override public void onClose() {
        if (menu.paymentLocked()) { minecraft.player.displayClientMessage(tr("payment_required"),true); return; }
        super.onClose();
    }
    private void price(GuiGraphics g,String key,long value,int y){g.drawString(font,tr(key),192,y,0xFFF3E6CD,false);if(value==0)g.drawString(font,"0",192,y+17,0xFFE5C46F,false);else CoinPriceRenderer.draw(g,font,value,192,y+13,160,0xFFE5C46F);}
    private static String statusKey(int v){return switch(v){case 1->"preview";case 2->"paid";case 3->"empty";case 4->"blocked_item";case 5->"account_daily_cap";case 6->"global_daily_cap";case 7->"item_account_cap";case 8->"item_global_cap";case 9->"budget_too_small";case 10->"payout_full";case 11->"market_budget_exhausted";case 12->"quarantined";case 13->"sale_busy";default->"waiting";};}
}
