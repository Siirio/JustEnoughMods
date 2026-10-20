package com.siirio.jemsettlements.client;

import com.siirio.jemsettlements.network.SettlementNetwork;
import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemcurrency.CurrencyItems;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Standalone native presentation. Each operation names a town and waits for its server ACK. */
public class SettlementScreen extends Screen {
    private static final String[] SECTIONS={"settlement","residents","happiness","chests"};
    private static final int ROW_HEIGHT=43;
    private static final String[] CHAPTERS={"founding","territory","feeding","housing","food_satisfaction","safety","jobs","community","decoration","chest_delivery","payroll","rebellion","treasury","tax"};
    private static final int INK=0xff342b26, MUTED=0xff6c6257, PALE=0xfff4e7cb, GOLD=0xffd7b86a;
    private CompoundTag envelope, town=new CompoundTag(), detail, chest;
    private String section="settlement", mode="loading", search="", filter="all", sort="name", notice="";
    private String guideReturn="residents", guideMode="town";
    private int chapter, scroll, left, top, panelW, panelH, cx, cy, cw, bottom, listTop, listBottom;
    private boolean compact, information,scaleLimited;
    private UUID expectedHousing;
    private String expectedHousingSection;
    private UUID desiredEntity,inspectedCityResident;
    private EditBox searchBox, nameBox, amountBox;
    private String editedName="", amount="1";
    private CoinDenomination denomination=CoinDenomination.COPPER;
    private final SettlementMap map=new SettlementMap();
    private final HousingInspector housing=new HousingInspector();
    private boolean inspecting,rotatingHouse,panningHouse;
    private int housePreviewY,housePreviewH,cityContentTop,cityScrollLimit;
    private int sidebarWidth, dashboardTop, dashboardLimit, residentScroll, detailScroll, detailTop, detailBottom;
    private boolean residentSplit, searchDirty;
    private int renderMouseX, renderMouseY;
    private final List<Row> rows=new ArrayList<>();
    private final List<Runnable> drawings=new ArrayList<>();
    private Runnable confirm;
    private Component confirmText;
    private String confirmMode;
    private GuiGraphics graphics;
    private record Row(Component name,Component subtitle,ItemStack icon,CompoundTag data,Runnable action) { }
    public SettlementScreen() { this(SettlementClientState.view()); }
    public SettlementScreen(CompoundTag view) { super(tr("title")); envelope=view.copy(); initial(); }
    public static void open() { SettlementClientState.open(); }
    public static void openGuide() { SettlementClientState.openGuide(); }
    private static Component tr(String key,Object... args) { return Component.translatable("jem.settlements.gui."+key,args); }
    private static Component metricText(String key,Object... args) { return Component.translatable("jem.settlements.metrics."+key,args); }
    private static Component amenityText(String key) { return Component.translatable("jem.settlements.amenity."+key); }
    private void initial() {
        if (!SettlementNetwork.supported()) { mode="unsupported"; return; }
        if (envelope.getBoolean("Guide")) { mode="guide"; return; }
        if (envelope.getBoolean("Founding")) { mode="create"; return; }
        var directory=envelope.getList("Directory",Tag.TAG_COMPOUND);
        if (envelope.contains("Selected",Tag.TAG_COMPOUND)) { town=envelope.getCompound("Selected").copy(); mode="town"; }
        else if (envelope.contains("Directory")) mode=directory.isEmpty()?"create":"selector";
    }
    public void focusEntity(UUID id){desiredEntity=id;focusDesired();}
    private void focusDesired(){if(desiredEntity==null || !mode.equals("town"))return; for(String key:new String[]{"Residents","Defense"})for(CompoundTag row:tags(town,key))if((!key.equals("Defense")||row.getBoolean("Living")||row.getBoolean("NeedsEnrolled"))&&row.hasUUID("Id")&&desiredEntity.equals(row.getUUID("Id"))){detail=row;section="residents";desiredEntity=null;scroll=0;return;}}
    public void accept(CompoundTag view,String completed) {
        if(view.contains("Housing")&&completed.equals("housing")&&mode.equals("town")&&section.equals(expectedHousingSection)&&expectedHousing!=null
                &&(section.equals("settlement")||detail!=null&&detail.hasUUID("Id")&&expectedHousing.equals(detail.getUUID("Id")))
                &&view.getCompound("Housing").hasUUID("Resident")&&expectedHousing.equals(view.getCompound("Housing").getUUID("Resident"))){
            housing.surfaceOnly=false;housing.onlyAmenities=false;
            if(section.equals("settlement")){housing.example=false;inspectedCityResident=expectedHousing;}
            housing.accept(view.getCompound("Housing"));inspecting=true;
        }
        if(completed.equals("housing")){expectedHousing=null;expectedHousingSection=null;}
        UUID previous=town.hasUUID("Id")?town.getUUID("Id"):null;
        envelope=view.copy();
        town=view.contains("Selected",Tag.TAG_COMPOUND)?view.getCompound("Selected").copy():new CompoundTag();
        map.accept(town);
        if (view.contains("Reason")) notice=view.getString("Reason");
        boolean accepted=view.getString("Result").equals("ACCEPTED") || view.getString("Result").equals("REPLAYED");
        if (completed.equals("create") && accepted || completed.equals("select") && !town.isEmpty()) { mode="town"; detail=null; chest=null; section="settlement"; map.clearQuote(); }
        else if (completed.equals("delete") && accepted) { mode="selector"; detail=null; map.clearQuote(); }
        else if (mode.equals("loading")) {
            int total=view.contains("DirectoryTotal")?view.getInt("DirectoryTotal"):view.getList("Directory",Tag.TAG_COMPOUND).size();
            if (view.getBoolean("Guide")) mode="guide";
            else if (view.getBoolean("Founding") || total==0) mode="create";
            else if (total==1 && !town.isEmpty()) mode="town";
            else mode="selector";
        }
        if (view.getBoolean("Founding")) mode="create";
        if (previous!=null && town.hasUUID("Id") && !previous.equals(town.getUUID("Id"))) { inspecting=false; inspectedCityResident=null; editedName=""; if(nameBox!=null)nameBox.setValue(""); scroll=0; map.center(BlockPos.of(town.getLong("Center")));  }
        if (detail!=null && detail.hasUUID("Id")) {
            UUID id=detail.getUUID("Id"); CompoundTag refreshed=null;
            for (String key:new String[]{"Residents","WorkforceRows","Defense"}) for (CompoundTag row:tags(town,key)) if (row.hasUUID("Id") && id.equals(row.getUUID("Id"))) refreshed=row;
            detail=refreshed;
        }
        if (completed.equals("storage") && accepted && town.contains("SelectedStorage",Tag.TAG_COMPOUND)) chest=town.getCompound("SelectedStorage").copy();
        if(chest!=null && town.contains("SelectedStorage",Tag.TAG_COMPOUND) && chest.getLong("Position")==town.getCompound("SelectedStorage").getLong("Position"))chest=town.getCompound("SelectedStorage").copy();
        if(previous!=null && town.hasUUID("Id") && !previous.equals(town.getUUID("Id")))chest=null;
        if (completed.equals("expand") || completed.equals("mapexpand")) map.clearQuote();
        focusDesired();
        if (minecraft!=null) rebuild();
    }
    public void guide() { expectedHousing=null;guideReturn=section; guideMode=mode; mode="guide"; scroll=0; if (minecraft!=null) rebuild(); }
    @Override protected void init() { rebuild(); }
    private void rebuild() {
        String focus=getFocused()==searchBox&&searchBox!=null?"search":getFocused()==nameBox&&nameBox!=null?"name":getFocused()==amountBox&&amountBox!=null?"amount":"";
        int cursor=focus.equals("search")?searchBox.getCursorPosition():focus.equals("name")?nameBox.getCursorPosition():focus.equals("amount")?amountBox.getCursorPosition():0;
        rebuildContent();
        if(confirm==null){EditBox restored=focus.equals("search")?searchBox:focus.equals("name")?nameBox:focus.equals("amount")?amountBox:null;if(restored!=null){setInitialFocus(restored);restored.setCursorPosition(cursor);}}
    }
    private void rebuildContent() {
        if (searchBox!=null) search=searchBox.getValue();
        if (nameBox!=null) editedName=nameBox.getValue();
        if (amountBox!=null) amount=amountBox.getValue();
        searchBox=null; nameBox=null; amountBox=null; clearWidgets(); drawings.clear(); rows.clear(); map.release();
        boolean citySection=mode.equals("town")&&section.equals("settlement");
        boolean directory=mode.equals("selector"), storage=mode.equals("town")&&section.equals("chests")&&chest!=null;
        int wantedW=directory?460:storage?364:860;
        int wantedH=directory?148+Math.min(6,Math.max(1,tags(envelope,"Directory").size()))*ROW_HEIGHT:
                storage?Math.max(260,168+18*((Math.min(54,Math.max(1,chest.getInt("SlotsTotal")))+8)/9)):548;
        panelW=Math.min(wantedW,width-24); panelH=Math.min(wantedH,height-36); left=(width-panelW)/2; top=(height-panelH)/2;
        compact=directory || (storage?width<390||panelH<250:panelW<500||panelH<270);
        sidebarWidth=compact?0:storage?96:Math.max(96,Math.min(132,panelW/5));
        cx=left+(compact?10:sidebarWidth+14); cy=top+(compact?80:60); cw=panelW-(compact?20:sidebarWidth+26); bottom=top+panelH-22;
        residentSplit=mode.equals("town")&&section.equals("residents")&&cw>=350&&!inspecting;
        dashboardLimit=0; dashboardTop=cy;
        scaleLimited=panelW<228 || panelH<180 || (inspecting||citySection)&&panelH<228;
        if (scaleLimited) {rotatingHouse=false;panningHouse=false;button(tr("close"),left+8,bottom-20,panelW-16,this::onClose,false);return;}
        if (mode.equals("town") && envelope.getInt("DirectoryTotal")>1 && !SettlementClientState.pending())
            button(tr("switch"),left+panelW-118,top+13,56,()->{mode="selector";scroll=0;detail=null;rebuild();},false);
        if (confirm!=null) { confirmationWidgets(); return; }
        if (mode.equals("loading") || mode.equals("unsupported")) return;
        if (mode.equals("selector")) { selector(); return; }
        if (mode.equals("create")) { creation(); return; }
        if (mode.equals("guide")) { guideWidgets(); return; }
        if (town.isEmpty()) { mode="selector"; selector(); return; }
        navigation();
        if (inspecting) { housingWidgets(); return; }
        if (detail!=null&&!residentSplit) { residentDetail(); return; }
        switch(section) {
            case "settlement" -> cityWidgets();
            case "residents" -> {if(residentSplit)residentsDashboard();else residentRows();}
            case "happiness" -> happinessRows();
            case "chests" -> {if(chest==null)chestRows();else chestContents();}
            
            default -> { }
        }
    }
    private Button button(Component label,int x,int y,int w,Runnable action,boolean mutation) {
        return button(label,x,y,w,action,mutation,ItemStack.EMPTY);
    }
    private Button button(Component label,int x,int y,int w,Runnable action,boolean mutation,ItemStack icon) {
        boolean danger=label.equals(tr("delete"));
        String actionIcon=w<72?null:danger?"warning":label.equals(tr("confirm"))?"check":label.equals(tr("back"))||label.equals(tr("cancel"))?"back":null;
        Button widget=new Button(Button.builder(label,b->action.run()).bounds(x,y,Math.max(20,w),20)) {
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial) {
                if(danger)g.setColor(1F,.46F,.43F,1F);
                SettlementDashboardArt.control(g,getX(),getY(),getWidth(),getHeight(),false,isHoveredOrFocused(),active);
                g.setColor(1F,1F,1F,1F);
                int reserve=actionIcon==null&&icon.isEmpty()?0:18;
                if(actionIcon!=null)SettlementDashboardArt.icon(g,actionIcon,getX()+3,getY()+2,16);
                else if(!icon.isEmpty())icon(g,icon,getX()+3,getY()+2,16);
                String text=font.plainSubstrByWidth(getMessage().getString(),Math.max(1,getWidth()-reserve-8));
                g.drawString(font,text,getX()+reserve+(getWidth()-reserve-font.width(text))/2,getY()+6,active?0xfffff1cb:0xffaaa38c,false);
            }
        };
        widget.setTooltip(Tooltip.create(label));
        widget.active=!mutation || !SettlementClientState.pending();
        if (!widget.active) widget.setTooltip(Tooltip.create(tr("pending")));
        addRenderableWidget(widget); return widget;
    }
    private void navigation() {
        if(compact) {
            int index=Arrays.asList(SECTIONS).indexOf(section);
            button(tr("section."+section),left+10,top+53,panelW-20,()->selectSection(SECTIONS[(index+1)%SECTIONS.length]),false);
            return;
        }
        for(int i=0;i<SECTIONS.length;i++) {
            String key=SECTIONS[i];
            int tx=left+8,ty=cy+i*43,tw=sidebarWidth-4;
            Button tab=new Button(Button.builder(tr("section."+key),b->selectSection(key)).bounds(tx,ty,tw,38)) {
                @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial) {
                    boolean selected=section.equals(key);SettlementDashboardArt.control(g,getX(),getY(),getWidth(),getHeight(),selected,isHoveredOrFocused(),true);
                    SettlementDashboardArt.icon(g,key,getX()+6,getY()+5,28);
                    g.drawString(font,clip(getMessage(),getWidth()-39),getX()+34,getY()+15,0xfff6eddd,false);
                }
            };
            tab.setTooltip(Tooltip.create(tr("section."+key)));addRenderableWidget(tab);
        }
    }
    private void selectSection(String key) {
        expectedHousing=null;section=key;inspecting=false;inspectedCityResident=null;rotatingHouse=false;panningHouse=false;
        detail=null;chest=null;scroll=0;search="";filter="all";map.clearQuote();
        rebuild();
    }
    private boolean cityActive() { return !scaleLimited&&mode.equals("town")&&section.equals("settlement")&&detail==null&&!inspecting&&confirm==null; }
    private static Component cityText(String key,Object...args) { return Component.translatable("jem.settlements.city."+key,args); }
    private void cityWidgets() {
        var metrics=town.getCompound("Metrics");
        summaryCards(false);
        cityContentTop=cy+66;listTop=cityContentTop;listBottom=bottom;
        int viewHeight=Math.max(1,bottom-cityContentTop),contentHeight=361;
        cityScrollLimit=Math.max(0,contentHeight-viewHeight);scroll=Math.min(scroll,cityScrollLimit);
        int firstDrawing=drawings.size(),firstWidget=children().size();int y=cityContentTop-scroll;
        final int treasuryY=y;
        drawings.add(()->{
            SettlementDashboardArt.panel(graphics,cx,treasuryY,cw,91,true);
            SettlementDashboardArt.treasury(graphics,cx+9,treasuryY+24);
            graphics.drawString(font,tr("section.treasury"),cx+58,treasuryY+9,PALE,false);
            graphics.drawString(font,tr("dashboard.balance"),cx+58,treasuryY+25,0xffbbb9b2,false);
            graphics.drawString(font,clip(SettlementPriceRenderer.format(town.getLong("Treasury"),coinCurrency()),Math.max(45,cw/2-62)),cx+58,treasuryY+42,GOLD,false);
            int rx=cx+cw/2+6;
            graphics.drawString(font,tr("dashboard.yesterday"),rx,treasuryY+10,PALE,false);
            graphics.drawString(font,clip(tr("resident_income",SettlementPriceRenderer.format(town.getLong("LastResidentTax"),coinCurrency())),cw/2-16),rx,treasuryY+28,0xffc8cfb3,false);
            graphics.drawString(font,clip(tr("comfort_income",SettlementPriceRenderer.format(town.getLong("LastBeautyTax"),coinCurrency())),cw/2-16),rx,treasuryY+43,0xffc8cfb3,false);
            graphics.drawString(font,clip(tr("daily_income",SettlementPriceRenderer.format(town.getLong("LastDailyTax"),coinCurrency())),cw/2-16),rx,treasuryY+62,0xffa9d77b,false);
        });
        final int nextTax=switch(town.getInt("Tax")){case 0->5;case 5->10;case 10->15;default->0;};
        var tax=button(tr("tax",town.getInt("Tax")),cx+8,y+65,Math.max(90,cw/2-18),()->{CompoundTag payload=new CompoundTag();payload.putInt("Percent",nextTax);request("tax",payload);},true);
        tax.active=town.getBoolean("SetTaxAvailable")&&!SettlementClientState.pending();y+=98;
        int quarter=Math.max(28,cw/4),moneyY=y;
        Button unit=button(currencyItem().getHoverName(),cx,moneyY,quarter,()->{denomination=CoinDenomination.values()[(denomination.ordinal()+1)%CoinDenomination.values().length];rebuild();},false);
        unit.active=coinCurrency();unit.setTooltip(Tooltip.create(tr("coin_cycle")));
        amountBox=edit(cx+quarter+3,moneyY,quarter-3,amount,"amount",16);amountBox.setFilter(value->value.matches("[0-9]*"));amountBox.setResponder(value->amount=value);
        button(tr("deposit"),cx+quarter*2+3,moneyY,quarter-3,()->transfer("deposit"),true);
        button(tr("withdraw"),cx+quarter*3+3,moneyY,cw-quarter*3-3,()->transfer("withdraw"),true);y+=30;
        final int sectionY=y;
        drawings.add(()->SettlementDashboardArt.panel(graphics,cx,sectionY,cw,126,false));
        drawLine(tr("dashboard.comfort"),cx+8,y+8,INK);
        var categories=metrics.getCompound("ComfortCategories");int index=0;
        for(String category:new String[]{"paths","animals","lighting","greenery"}) {
            String field=switch(category){case "paths"->"Path";case "animals"->"Animals";case "lighting"->"Lighting";default->"Greenery";};
            int value=(int)Math.round(categories.getDouble(field)*4),position=index++,cardW=(cw-24)/2;
            int cardX=cx+8+(position%2)*(cardW+8),rowY=y+27+(position/2)*46;
            ItemStack item=new ItemStack(switch(category){case "paths"->Items.COBBLESTONE;case "animals"->Items.WHEAT;case "lighting"->Items.LANTERN;default->Items.OAK_SAPLING;});
            drawings.add(()->{
                icon(graphics,item,cardX,rowY,19);
                graphics.drawString(font,clip(tr("dashboard."+category),cardW-28),cardX+26,rowY,INK,false);
                graphics.drawString(font,Math.round(categories.getDouble(field))+"/25",cardX+26,rowY+12,MUTED,false);
                SettlementDashboardArt.track(graphics,cardX,rowY+27,cardW,6,value,category.equals("lighting")?0xff43bfe0:category.equals("paths")?0xffe0b24b:0xff7ab849);
            });
        }
        y+=137;
        drawLine(tr("name"),cx,y,INK);y+=15;
        nameBox=edit(cx,y,Math.max(50,cw-88),editedName.isEmpty()?town.getString("Name"):editedName,"name",32);
        button(tr("save_name"),cx+cw-83,y,83,()->{CompoundTag payload=new CompoundTag();payload.putString("Name",nameBox.getValue().strip());request("rename",payload);},true);y+=31;
        button(tr("delete"),cx,y,cw,()->ask(tr("delete_confirm",town.getString("Name")),()->request("delete",new CompoundTag())),true,new ItemStack(Items.LAVA_BUCKET));
        clipSection(firstDrawing,firstWidget,cityContentTop,bottom);
        scrollRail(cx+cw-2,cityContentTop,viewHeight,contentHeight,scroll);
    }
    private EditBox edit(int x,int y,int w,String value,String hint,int max) {
        EditBox box=new EditBox(font,x,y,w,20,tr(hint)) {
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial) {
                SettlementDashboardArt.control(g,getX(),getY(),getWidth(),getHeight(),isFocused(),isHoveredOrFocused(),isActive());
                // Unbordered native text retains the original4px/6px inset and full outer hit region.
                g.enableScissor(getX()+3,getY()+3,getX()+getWidth()-3,getY()+getHeight()-3);
                g.pose().pushPose();
                g.pose().translate(4,(getHeight()-8)/2F,0);
                super.renderWidget(g,mx,my,partial);
                g.pose().popPose();
                if(hint.equals("search")&&getValue().isEmpty()&&getWidth()>80)
                    SettlementDashboardArt.icon(g,"search",getX()+getWidth()-19,getY()+3,14);
                g.disableScissor();
            }
            @Override public int getInnerWidth() { return getWidth()-8; }
            @Override public void onClick(double mx,double my) { super.onClick(mx-4,my); }
            @Override public int getScreenX(int cursor) { return super.getScreenX(cursor)+4; }
        }; box.setBordered(false);box.setMaxLength(max);box.setHint(tr(hint));box.setValue(value);addRenderableWidget(box);return box;
    }
    private void search(int y) {
        searchBox=edit(cx,y,cw,search,"search",64);
        searchBox.setResponder(value->{search=value;scroll=0;residentScroll=0;searchDirty=true;});
    }
    private void request(String action,CompoundTag payload) {
        UUID id=town.hasUUID("Id")?town.getUUID("Id"):null;
        if (SettlementClientState.action(action,id,payload)) {if(action.equals("housing")&&payload.hasUUID("Entity")){expectedHousing=payload.getUUID("Entity");expectedHousingSection=section;}rebuild();}
    }
    private void readDirectory(int offset) {CompoundTag payload=new CompoundTag();payload.putInt("Offset",Math.max(0,offset));request("open",payload);}
    private void selector() {
        cx=left+8;cw=panelW-16;cy=top+60;search(cy);listTop=cy+26;listBottom=bottom-30;
        List<CompoundTag> list=tags(envelope,"Directory");list.sort(Comparator.comparing(r->r.getString(sort.equals("name")?"Name":"Dimension"),String.CASE_INSENSITIVE_ORDER));
        for (CompoundTag row:list) if (matches(row.getString("Name")+" "+row.getString("Dimension"))) rows.add(new Row(Component.literal(row.getString("Name")),tr("town_row",dimension(row.getString("Dimension")),row.getInt("Residents"),row.getInt("Happiness")),new ItemStack(Items.BELL),row,()->{
            if (SettlementClientState.action("select",row.getUUID("Id"),new CompoundTag())) { mode="loading";rebuild(); }
        }));
        listWidgets();int offset=envelope.getInt("DirectoryOffset"),total=envelope.getInt("DirectoryTotal");
        if(total>64){
            button(Component.literal("<"),cx,bottom-24,22,()->readDirectory(offset-64),true).active=offset>0 && !SettlementClientState.pending();
            button(Component.literal(">"),cx+26,bottom-24,22,()->readDirectory(offset+64),true).active=offset+64<total && !SettlementClientState.pending();
        }
        button(tr("create"),cx+cw-100,bottom-24,100,()->{mode="create";editedName="";rebuild();},false);drawLine(tr("directory_count",total),cx+(total>64?54:0),bottom-18,MUTED);
    }
    private void creation() {
        cx=left+8;cw=panelW-16;cy=top+40;nameBox=edit(cx,cy,cw,editedName,"name",32);
        drawWrapped(tr("found_instruction"),cx,cy+28,cw-92,MUTED);
        drawWrapped(tr("location",dimension(envelope.getString("FoundingDimension")),envelope.contains("Bell")?position(envelope.getLong("Bell")):tr("bell_required")),cx,cy+64,cw-92,MUTED);
        drawLine(tr("cost"),cx,cy+100,MUTED);
        drawings.add(()->coins(envelope.getLong("FoundingCost"),cx+48,cy+96,cw-140));
        drawings.add(()->{
            int px=cx+cw-84,py=cy+28;SettlementDashboardArt.panel(graphics,px,py,84,78,false);
            graphics.fill(px+12,py+7,px+72,py+67,0x806d9562);graphics.renderOutline(px+12,py+7,60,60,0xff314e2e);
            graphics.renderItem(new ItemStack(Items.BELL),px+34,py+27);
            int side=envelope.getInt("InitialRadius")*2+1;graphics.drawCenteredString(font,side+" x "+side,px+42,py+68,0xff292824);
        });
        Button create=button(tr("create"),cx,bottom-44,Math.max(80,cw/2-2),()->{
            CompoundTag payload=new CompoundTag();payload.putString("Name",nameBox.getValue().strip());payload.putLong("Bell",envelope.getLong("Bell"));request("create",payload);
        },true);
        create.active=!SettlementClientState.pending() && envelope.contains("Bell") && editedName.strip().length()>=3;
        if (!envelope.contains("Bell")) create.setTooltip(Tooltip.create(tr("bell_required")));
        nameBox.setResponder(value->{editedName=value;create.active=!SettlementClientState.pending()&&envelope.contains("Bell")&&value.strip().length()>=3;});
        button(tr("back"),cx+cw/2+2,bottom-44,cw/2-2,()->{mode="selector";rebuild();},false);
    }
    private void mapWidgets() {
        long total=map.cost(town);
        int priceHeight=map.quoted()?12+SettlementPriceRenderer.fullHeight(font,total,cw,coinCurrency()):12;
        drawLine(tr("section.map"),cx,cy,INK);int my=cy+16,mh=bottom-my-52-priceHeight;map.bounds(cx,my,cw,Math.max(24,mh));
        
        button(Component.literal("-"),cx+cw-44,my+2,20,()->map.zoom(-1),false);button(Component.literal("+"),cx+cw-22,my+2,20,()->map.zoom(1),false);
        button(tr("center"),cx,bottom-46,Math.max(48,cw/3),()->map.center(BlockPos.of(town.getLong("Center"))),false);
        button(tr("player"),cx+cw/3+2,bottom-46,Math.max(48,cw/3-2),()->{if(minecraft.player!=null&&minecraft.level!=null&&minecraft.level.dimension().location().toString().equals(town.getString("Dimension")))map.center(minecraft.player.blockPosition());},false);
        Button expand=button(tr("expand"),cx+cw*2/3+2,bottom-46,cw/3-2,()->{
            if (!map.preview()) return;long added=map.added(town);CompoundTag payload=map.payload(town);
            Component fullPrice=coinCurrency()?com.siirio.jemeconomy.physical.CoinFormatter.formatAll(map.cost(town)):SettlementPriceRenderer.format(map.cost(town),false);
            ask(tr("expansion_confirmation",added).copy().append("\n").append(fullPrice),()->{if(SettlementClientState.action("mapexpand",town.getUUID("Id"),payload))map.committed();});
        },true);expand.active=map.preview()&&map.invalidReason(town).isEmpty()&&!SettlementClientState.pending();expand.setTooltip(Tooltip.create(tr(!map.invalidReason(town).isEmpty()?"map_invalid."+map.invalidReason(town):map.quoted()?"expansion_configured":"select_neighbor")));
        if(map.quoted()) {
            drawLine(tr("expansion_area",map.added(town)),cx,bottom-50-priceHeight,MUTED);
            drawings.add(()->SettlementPriceRenderer.drawFull(graphics,font,map.cost(town),cx,bottom-38-priceHeight,cw,INK,coinCurrency()));
        } else drawLine(tr("map_help"),cx,bottom-60,MUTED);
    }
    private static boolean recruit(CompoundTag row){return row.getString("Type").equals("RECRUIT") || row.contains("Loyalty");}
    private static boolean guard(CompoundTag row){return recruit(row)||row.getString("Type").equals("GOLEM")||row.getString("NativeType").contains("golem");}
    private static Component residentName(CompoundTag row) {
        if(!row.getString("NameComponent").isEmpty()) {
            Component name=Component.Serializer.fromJson(row.getString("NameComponent"));
            if(name!=null)return name;
        }
        if(!row.getString("NameKey").isEmpty())return Component.translatable(row.getString("NameKey"));
        return guard(row)&&!recruit(row)?Component.translatable("entity.minecraft.iron_golem"):recruit(row)?Component.translatable("entity.recruits.recruit"):profession(row);
    }
    private List<CompoundTag> roster() {
        List<CompoundTag> result=new ArrayList<>(tags(town,"Residents"));
        for(CompoundTag row:tags(town,"Defense"))if(row.getBoolean("Living")||row.getBoolean("NeedsEnrolled"))result.add(row);
        return result;
    }
    private void residentRows() {
        search(cy);button(filterLabel(),cx,cy+24,cw,()->{filter=nextResidentFilter();scroll=0;residentScroll=0;rebuild();},false);
        listTop=cy+48;listBottom=bottom-24;
        List<CompoundTag> residents=roster();residents.sort(switch(sort){case "profession"->Comparator.comparing(r->recruit(r)?tr("filter.guards").getString():profession(r).getString(),String.CASE_INSENSITIVE_ORDER);case "hunger"->Comparator.comparingInt(r->r.contains("Satiety")?r.getInt("Satiety"):Integer.MAX_VALUE);case "happiness"->Comparator.comparingInt(r->r.contains("Mood")?r.getInt("Mood"):Integer.MAX_VALUE);default->Comparator.comparing(r->residentName(r).getString(),String.CASE_INSENSITIVE_ORDER);});
        for (CompoundTag row:residents) {
            Component role=row.getBoolean("Child")?metricText("child"):guard(row)?tr("filter.guards"):profession(row);
            if (!matches(residentName(row).getString()+" "+role.getString())) continue;
            if(filter.equals("guards")&&!guard(row)||filter.equals("villagers")&&guard(row))continue;
            if(filter.startsWith("profession:")&&!row.getString("Profession").equals(filter.substring(11)) || filter.startsWith("role:")&&!row.getString("Role").equals(filter.substring(5)))continue;
            if (filter.equals("hungry")&&!row.getBoolean("Craving") || filter.equals("unhappy")&&(!row.contains("Mood")||!town.contains("UnhappyThreshold") || row.getInt("Mood")>=town.getInt("UnhappyThreshold")) || filter.equals("working")&&!row.getBoolean("Work")) continue;
            rows.add(new Row(residentName(row),role.copy().append(" | ").append(row.contains("Mood")&&row.contains("Satiety")?tr("resident_values",row.getInt("Mood"),row.getInt("Satiety")):tr("golem_role")),new ItemStack(guard(row)?Items.SHIELD:Items.VILLAGER_SPAWN_EGG),row,()->{residentScroll=scroll;detailScroll=0;detail=row;scroll=0;rebuild();}));
        }
        if(filter.equals("guards")){listBottom=bottom-48;button(tr("pay_due"),cx,bottom-44,cw,()->ask(tr("payroll_confirm"),()->request("payroll",new CompoundTag())),true).active=town.getLong("UnpaidPayroll")>0&&!SettlementClientState.pending();}
        listWidgets();refreshFooter();
    }
    private void openStorage(CompoundTag storage,int offset){
        CompoundTag payload=new CompoundTag();payload.putLong("Position",storage.getLong("Position"));payload.putInt("SlotOffset",Math.max(0,offset));request("storage",payload);
    }
    private void chestRows(){
        search(cy);listTop=cy+26;listBottom=bottom-24;
        for(CompoundTag storage:tags(town,"Storage")){
            Component name=Component.translatable("jem.settlements.storage_name",storage.getInt("Number"));
            if(!matches(name.getString()+" "+position(storage.getLong("Position"))))continue;
            rows.add(new Row(name,tr(storage.getBoolean("Loaded")?"storage_loaded":"storage_unknown").copy().append(" | ").append(position(storage.getLong("Position"))),new ItemStack(Items.CHEST),storage,()->openStorage(storage,0)));
        }
        listWidgets();refreshFooter();
    }
    private int chestColumns(){return Math.min(9,Math.max(1,(cw-8)/18));}
    private int chestPageSize(){return Math.min(54,chestColumns()*Math.max(1,(bottom-52-(cy+40))/18));}
    private void chestContents(){
        button(tr("back"),cx,cy,52,()->{chest=null;scroll=0;rebuild();},false);
        drawLine(Component.translatable("jem.settlements.storage_name",chest.getInt("Number")),cx+58,cy+5,INK);
        drawLine(chest.getInt("MealOrders")>0?tr("chest_orders",chest.getInt("MealOrders")):tr("coordinates",position(chest.getLong("Position"))),cx,cy+25,MUTED);
        if(bottom-52-(cy+40)<18){drawWrapped(tr("scale"),cx,cy+28,cw,MUTED);return;}
        int columns=chestColumns(),capacity=chestPageSize(),offset=chest.getInt("SlotOffset"),total=chest.getInt("SlotsTotal");
        if(!chest.getBoolean("InventoryKnown"))drawWrapped(tr("chest_unavailable"),cx,cy+46,cw,MUTED);
        else drawings.add(()->{
            int gx=cx+(cw-columns*18)/2,gy=cy+40;
            for(int i=0;i<Math.min(capacity,Math.max(0,total-offset));i++){
                int sx=gx+i%columns*18,sy=gy+i/columns*18;
                graphics.fill(sx,sy,sx+18,sy+18,0xff72614c);graphics.fill(sx+1,sy+1,sx+17,sy+17,0xff211b16);
                for(CompoundTag slot:tags(chest,"Slots"))if(slot.getInt("Slot")==offset+i){ItemStack stack=ItemStack.of(slot.getCompound("Stack"));graphics.renderItem(stack,sx+1,sy+1);graphics.renderItemDecorations(font,stack,sx+1,sy+1);break;}
            }
        });
        boolean paged=total>capacity;
        if(paged){
            button(Component.literal("<"),cx,bottom-44,24,()->openStorage(chest,offset-capacity),false).active=offset>0&&!SettlementClientState.pending();
            button(Component.literal(">"),cx+28,bottom-44,24,()->openStorage(chest,offset+capacity),false).active=offset+capacity<total&&!SettlementClientState.pending();
        }
        drawLine(tr("chest_slots",total==0?0:offset+1,Math.min(total,offset+capacity),total),cx+(paged?58:0),bottom-38,MUTED);
        Button feed=button(tr("chest_feed_all"),cx,bottom-20,cw,()->{CompoundTag payload=new CompoundTag();payload.putLong("Position",chest.getLong("Position"));request("feedall",payload);},true,new ItemStack(Items.BREAD));
        feed.active=chest.getBoolean("InventoryKnown")&&!SettlementClientState.pending();feed.setTooltip(Tooltip.create(tr("chest_feed_all_hint")));
    }
    private void happinessRows() {
        summaryCards(true);dashboardTop=cy+66;listTop=dashboardTop;listBottom=bottom;
        int contentHeight=405,viewport=Math.max(1,bottom-dashboardTop);dashboardLimit=Math.max(0,contentHeight-viewport);scroll=Math.min(scroll,dashboardLimit);
        int firstDrawing=drawings.size(),firstWidget=children().size();int y=dashboardTop-scroll;
        final int factorsY=y;
        drawings.add(()->{SettlementDashboardArt.panel(graphics,cx,factorsY,cw,207,true);graphics.drawString(font,tr("dashboard.factors"),cx+10,factorsY+9,PALE,false);});
        var metrics=town.getCompound("Metrics");int index=0;
        for(String category:new String[]{"housing","food","safety","jobs","community","decoration"}) {
            String field=category.substring(0,1).toUpperCase(Locale.ROOT)+category.substring(1);
            int weight=switch(category){case "decoration","community"->15;case "safety","jobs"->10;default->20;};
            double score=metrics.contains(field+"Exact")?metrics.getDouble(field+"Exact"):category.equals("decoration")?metrics.getDouble("ComfortExact"):town.getInt(field);
            ItemStack icon=new ItemStack(switch(category){case "housing"->Items.RED_BED;case "food"->Items.BREAD;case "safety"->Items.SHIELD;case "jobs"->Items.IRON_PICKAXE;case "community"->Items.BELL;default->Items.FLOWER_POT;});
            int rowY=y+29+index++*25;String value=String.format(Locale.ROOT,"%.1f / %d",score*weight/100,weight);
            drawings.add(()->{
                icon(graphics,icon,cx+8,rowY,18);
                graphics.drawString(font,clip(tr("category."+category),Math.max(45,cw/3-25)),cx+31,rowY+5,PALE,false);
                int bx=cx+cw/3+12,bw=Math.max(36,cw/3-10);
                SettlementDashboardArt.track(graphics,bx,rowY+6,bw,9,(int)Math.round(score),scoreColor((int)score));
                graphics.drawString(font,value,bx+bw+10,rowY+5,PALE,false);
            });
            factorHelp(category,cx+5,rowY,cw-10,23);
        }
        final int taxPenalty=com.siirio.jemsettlements.happiness.HappinessService.taxPenalty(town.getInt("Tax"));
        drawings.add(()->graphics.drawString(font,clip(tr("dashboard.budget",taxPenalty),cw-20),cx+10,factorsY+188,0xffc9c3ad,false));
        y+=216;final int complaintsY=y;
        drawings.add(()->{SettlementDashboardArt.panel(graphics,cx,complaintsY,cw,92,false);graphics.drawString(font,tr("dashboard.housing_complaints"),cx+9,complaintsY+9,INK,false);});
        List<CompoundTag> residents=tags(town,"Residents");
        String[] reasons={"no_bed","no_roof","crowding","light"};ItemStack[] icons={new ItemStack(Items.RED_BED),new ItemStack(Items.OAK_STAIRS),new ItemStack(Items.OAK_DOOR),new ItemStack(Items.LANTERN)};
        int cell=(cw-16)/4;
        for(int i=0;i<4;i++) {
            String reason=reasons[i];int x=cx+8+i*cell,rowY=y+29;ItemStack item=icons[i];
            long count=residents.stream().filter(r->switch(reason){case "no_bed"->r.contains("HasBed")&&!r.getBoolean("HasBed");case "no_roof"->r.getBoolean("HasBed")&&!r.getBoolean("Roofed");case "crowding"->r.getInt("CrowdingPenalty")>0;default->r.getInt("LightPenalty")>0;}).count();
            drawings.add(()->{
                icon(graphics,item,x+4,rowY+1,20);
                graphics.drawWordWrap(font,tr("dashboard."+reason),x+29,rowY+1,Math.max(24,cell-35),INK);
                graphics.drawString(font,tr("dashboard.affected",count),x+4,rowY+39,count>0?0xffa33f36:MUTED,false);
            });
        }
        y+=102;final int distributionY=y;
        long unhappy=residents.stream().filter(r->r.getInt("Mood")<town.getInt("UnhappyThreshold")).count();
        long happy=residents.stream().filter(r->r.getInt("Mood")>=80).count(),neutral=residents.size()-unhappy-happy;
        drawings.add(()->{
            SettlementDashboardArt.panel(graphics,cx,distributionY,cw,76,true);
            graphics.drawString(font,tr("dashboard.distribution"),cx+9,distributionY+9,PALE,false);
            int bx=cx+10,bw=cw-20,total=Math.max(1,residents.size());
            graphics.fill(bx,distributionY+26,bx+bw,distributionY+37,0xff171b23);
            int a=(int)(bw*unhappy/total),b=(int)(bw*neutral/total);
            graphics.fill(bx+1,distributionY+27,bx+a,distributionY+36,0xffd65f51);graphics.fill(bx+a,distributionY+27,bx+a+b,distributionY+36,0xffdfb44c);graphics.fill(bx+a+b,distributionY+27,bx+bw-1,distributionY+36,residents.isEmpty()?0xff353943:0xff78ba52);
            long[] counts={unhappy,neutral,happy};int[] moods={10,50,95};String[] labels={"unhappy","neutral","happy"};
            for(int i=0;i<3;i++){int x=cx+10+i*(cw-20)/3;SettlementDashboardArt.mood(graphics,x,distributionY+45,18,moods[i]);graphics.drawString(font,clip(tr("dashboard."+labels[i]),Math.max(24,cw/3-32)),x+23,distributionY+44,PALE,false);graphics.drawString(font,counts[i]+" ("+(residents.isEmpty()?0:Math.round(counts[i]*100.0/residents.size()))+"%)",x+23,distributionY+57,0xffc3c0b5,false);}
        });
        clipSection(firstDrawing,firstWidget,dashboardTop,bottom);scrollRail(cx+cw-2,dashboardTop,viewport,contentHeight,scroll);
    }
    private void factorHelp(String category,int x,int y,int w,int h) {
        Component explanation=switch(category){case "safety"->metricText("guards",town.getCompound("Metrics").getInt("EffectiveGuards"),town.getCompound("Metrics").getInt("RequiredGuards"));case "community"->metricText("population",town.getCompound("Metrics").getInt("RegisteredResidents"),town.getCompound("Metrics").getInt("RequiredResidents"));case "jobs"->metricText("jobs",town.getCompound("Metrics").getInt("EmployedAdults"),town.getCompound("Metrics").getInt("WorkingAdults"));case "decoration"->tr("comfort_summary");default->metricText(category);};
        Button help=new Button(Button.builder(tr("category."+category),b->{chapter=Arrays.asList(CHAPTERS).indexOf(category.equals("food")?"food_satisfaction":category);guide();}).bounds(x,y,w,h)) {
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial){if(isHoveredOrFocused())g.renderFakeItem(new ItemStack(Items.BOOK),getX()+getWidth()-19,getY()+(getHeight()-16)/2);}
        };help.setTooltip(Tooltip.create(explanation));addRenderableWidget(help);
    }
    private void conditionRows(CompoundTag source) {
        for(String key:new String[]{"CrowdingPenalty","LightPenalty","FearPenalty","InjuryPenalty","HarmfulFoodPenalty"})if(source.getInt(key)>0){
            String reason=switch(key){case "CrowdingPenalty"->"crowding";case "LightPenalty"->"light";case "InjuryPenalty"->"injury";case "HarmfulFoodPenalty"->"harmful_food";default->"fear";};
            rows.add(new Row(Component.translatable("jem.settlements.conditions."+reason,source.getInt(key)),Component.translatable("jem.settlements.conditions."+reason+"_hint"),new ItemStack(key.equals("CrowdingPenalty")?Items.RED_BED:key.equals("LightPenalty")?Items.TORCH:Items.ZOMBIE_HEAD),new CompoundTag(),null));
        }

    }
    private void residentsDashboard() {
        int pageX=cx,pageW=cw,pageY=cy,rosterW=Math.max(144,Math.min(222,(int)(cw*.42))),rightX=pageX+rosterW+8,rightW=pageW-rosterW-8;
        drawings.add(()->{SettlementDashboardArt.panel(graphics,pageX,pageY,rosterW,bottom-pageY,false);SettlementDashboardArt.panel(graphics,rightX,pageY,rightW,bottom-pageY,false);});
        cx=pageX+5;cw=rosterW-10;cy=pageY+19;scroll=residentScroll;
        residentRows();residentScroll=scroll;
        drawLine(tr("dashboard.roster",tags(town,"Residents").size()),pageX+9,pageY+7,INK);
        if(detail==null||rows.stream().noneMatch(row->row.data().hasUUID("Id")&&detail.hasUUID("Id")&&row.data().getUUID("Id").equals(detail.getUUID("Id"))))detail=rows.isEmpty()?null:rows.get(0).data();
        cx=pageX;cw=pageW;cy=pageY;
        if(detail==null){drawWrapped(tr("dashboard.select_resident"),rightX+12,pageY+24,rightW-24,MUTED);return;}
        residentPane(rightX+7,pageY+7,rightW-14,bottom-7);
    }
    private void residentPane(int x,int y,int w,int paneBottom) {
        CompoundTag selected=detail;boolean needs=selected.contains("Mood"),isRecruit=recruit(selected);
        final int portraitW=Math.max(40,Math.min(58,w/3));
        drawings.add(()->{
            SettlementDashboardArt.panel(graphics,x,y,portraitW,95,true);portrait(graphics,selected,x+3,y+3,portraitW-6,89);
            graphics.drawString(font,clip(residentName(selected),w-portraitW-10),x+portraitW+7,y+4,INK,false);
            graphics.drawString(font,clip(guard(selected)?tr("filter.guards"):profession(selected),w-portraitW-10),x+portraitW+7,y+19,MUTED,false);
            if(needs){
                SettlementDashboardArt.mood(graphics,x+portraitW+7,y+37,16,selected.getInt("Mood"));
                graphics.drawString(font,selected.getInt("Mood")+"/100",x+portraitW+27,y+41,INK,false);
                SettlementDashboardArt.track(graphics,x+portraitW+7,y+57,w-portraitW-14,7,selected.getInt("Mood"),scoreColor(selected.getInt("Mood")));
                icon(graphics,new ItemStack(Items.BREAD),x+portraitW+7,y+70,15);
                graphics.drawString(font,selected.getInt("Satiety")+"/100",x+portraitW+27,y+74,INK,false);
                SettlementDashboardArt.track(graphics,x+portraitW+7,y+87,w-portraitW-14,7,selected.getInt("Satiety"),0xff7db954);
            }else graphics.drawWordWrap(font,tr("golem_role"),x+portraitW+7,y+39,w-portraitW-14,MUTED);
        });
        if(selected.contains("Health"))drawLine(tr("health",Math.round(selected.getFloat("Health")),Math.round(selected.getFloat("MaxHealth"))),x,y+102,MUTED);
        int actionY=paneBottom-20;detailTop=y+119;detailBottom=actionY-7;
        List<Row> facts=new ArrayList<>();
        if(needs&&!isRecruit){
            if(selected.contains("HasBed")&&!selected.getBoolean("HasBed"))facts.add(new Row(tr("no_bed"),tr("no_bed_hint"),new ItemStack(Items.RED_BED),new CompoundTag(),null));
            else if(selected.contains("Roofed")&&!selected.getBoolean("Roofed"))facts.add(new Row(tr("no_roof"),tr("no_roof_hint"),new ItemStack(Items.OAK_STAIRS),new CompoundTag(),null));
        }
        for(String key:new String[]{"CrowdingPenalty","LightPenalty","FearPenalty","InjuryPenalty","HarmfulFoodPenalty"})if(selected.getInt(key)>0){String reason=switch(key){case "CrowdingPenalty"->"crowding";case "LightPenalty"->"light";case "InjuryPenalty"->"injury";case "HarmfulFoodPenalty"->"harmful_food";default->"fear";};facts.add(new Row(Component.translatable("jem.settlements.conditions."+reason,selected.getInt(key)),Component.translatable("jem.settlements.conditions."+reason+"_hint"),new ItemStack(key.equals("LightPenalty")?Items.LANTERN:Items.RED_BED),new CompoundTag(),null));}
        if(facts.isEmpty())facts.add(new Row(tr("dashboard.no_complaints"),tr("dashboard.no_complaints_hint"),new ItemStack(Items.EMERALD),new CompoundTag(),null));
        if(needs){facts.add(new Row(tr("dashboard.requested_food"),selected.contains("RequestedDish")?item(selected.getString("RequestedDish")).getHoverName():tr("no_request"),new ItemStack(Items.BREAD),new CompoundTag(),null));if(!isRecruit)facts.add(new Row(tr("dashboard.activities"),tr("personal_contribution",selected.getInt("ActivityBonus")),new ItemStack(Items.SUNFLOWER),new CompoundTag(),null));}
        if(!guard(selected)){
            facts.add(new Row(tr("home"),selected.contains("HomePos")?tr("coordinates",position(selected.getLong("HomePos"))):tr("no_bed"),new ItemStack(Items.RED_BED),new CompoundTag(),null));
            facts.add(new Row(tr("category.jobs"),selected.contains("WorkPos")?tr("coordinates",position(selected.getLong("WorkPos"))):metricText("employment.missing"),new ItemStack(Items.CRAFTING_TABLE),new CompoundTag(),null));
        }else if(isRecruit)facts.add(new Row(tr("wage"),SettlementPriceRenderer.format(selected.getLong("Wage"),coinCurrency()),new ItemStack(Items.GOLD_INGOT),new CompoundTag(),null));
        for(CompoundTag food:servings(selected))facts.add(new Row(item(food.getString("Item")).getHoverName(),tr("served_today",food.getInt("Count")),item(food.getString("Item")),new CompoundTag(),null));
        int visible=Math.max(1,(detailBottom-detailTop-16)/ROW_HEIGHT);detailScroll=Math.max(0,Math.min(detailScroll,Math.max(0,facts.size()-visible)));int start=detailScroll;
        drawings.add(()->{
            graphics.enableScissor(x,detailTop,x+w,detailBottom);graphics.drawString(font,tr("dashboard.needs"),x+2,detailTop,INK,false);
            for(int i=start;i<Math.min(facts.size(),start+visible);i++)drawRow(graphics,facts.get(i),x,detailTop+15+(i-start)*ROW_HEIGHT,w,false);
            graphics.disableScissor();
        });
        int third=Math.max(25,(w-6)/3);
        if(needs&&!isRecruit){
            button(tr("feed"),x,actionY,third,()->selectSection("chests"),false);
            button(tr("home"),x+third+3,actionY,third,()->{CompoundTag payload=new CompoundTag();payload.putUUID("Entity",selected.getUUID("Id"));request("housing",payload);},false).active=!SettlementClientState.pending();
            button(tr("remove"),x+third*2+6,actionY,w-third*2-6,()->{CompoundTag payload=new CompoundTag();payload.putUUID("Entity",selected.getUUID("Id"));ask(tr("remove_confirm",residentName(selected)),()->request("remove",payload));},true);
        }else if(isRecruit){
            button(tr("feed"),x,actionY,w/2-2,()->selectSection("chests"),false);
            button(tr("manage_native"),x+w/2+2,actionY,w/2-2,()->{CompoundTag payload=new CompoundTag();payload.putUUID("Entity",selected.getUUID("Id"));request("manage",payload);},true).active=selected.getBoolean("Loaded")&&!SettlementClientState.pending();
        }else button(tr("locate"),x,actionY,w,()->info(tr("coordinates",position(selected.getLong("Position")))),false).active=selected.contains("Position");
    }
    private void residentDetail() {
        int py=cy,portraitWidth=compact?48:64;boolean resident=detail.contains("Mood"),isRecruit=recruit(detail);
        var entity=minecraft.level!=null && detail.contains("EntityId")?minecraft.level.getEntity(detail.getInt("EntityId")):null;
        boolean actualEntity=entity instanceof LivingEntity&&detail.hasUUID("Id")&&entity.getUUID().equals(detail.getUUID("Id"));
        if (actualEntity && entity instanceof LivingEntity living) drawings.add(()->InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,cx+portraitWidth/2,py+83,36,0,0,living));
        else {drawings.add(()->graphics.renderItem(new ItemStack(resident?Items.BELL:section.equals("guards")?Items.SHIELD:Items.IRON_PICKAXE),cx+portraitWidth/2-8,py+16));drawWrapped(tr("unloaded"),cx,py+38,portraitWidth,MUTED);}
        int tx=cx+portraitWidth+6,tw=cw-portraitWidth-6;
        if(detail.contains("Health"))drawLine(tr("health",Math.round(detail.getFloat("Health")),Math.round(detail.getFloat("MaxHealth"))),cx,py+90,MUTED);
        if(resident) {
            drawWrapped(isRecruit?tr("filter.guards"):profession(detail),tx,py,tw,INK);
            drawings.add(()->bar(graphics,tx,py+24,tw,detail.getInt("Mood"),tr("happiness")));drawings.add(()->bar(graphics,tx,py+48,tw,detail.getInt("Satiety"),tr("satiety")));
            drawWrapped(detail.contains("RequestedDish")?tr("requested",item(detail.getString("RequestedDish")).getHoverName()):tr("no_request"),tx,py+72,tw,MUTED);
        } else if(section.equals("workers")) {
            drawWrapped(Component.translatable("jem.settlements.worker."+detail.getString("Type").toLowerCase(Locale.ROOT)),tx,py,tw,INK);
            drawWrapped(tr("native_work_detail"),tx,py+28,tw,MUTED);
            if(detail.contains("Position"))drawWrapped(tr("coordinates",position(detail.getLong("Position"))),tx,py+72,tw,MUTED);
        } else if(guard(detail)&&!isRecruit) {
            drawWrapped(tr("filter.guards"),tx,py,tw,INK);
            drawWrapped(tr("golem_role"),tx,py+24,tw,MUTED);
        } else {
            drawWrapped(Component.translatable("jem.settlements.gui.loyalty."+detail.getString("Loyalty").toLowerCase(Locale.ROOT)),tx,py,tw,INK);
            drawWrapped(tr("missed",detail.getInt("MissedPayrolls")),tx,py+20,tw,MUTED);
            drawWrapped(tr("due_in",String.format(Locale.ROOT,"%.1f",Math.max(0,detail.getLong("NextPayroll")-envelope.getLong("GameTime"))/24000.0)),tx,py+42,tw,MUTED);
            drawLine(tr("wage"),tx,py+66,MUTED);drawings.add(()->coins(detail.getLong("Wage"),tx,py+78,tw));
        }
        listTop=py+104;listBottom=bottom-48;
        if(resident&&!isRecruit) {
            rows.add(new Row(metricText("environment"),tr("personal_contribution",detail.getInt("ActivityBonus")),new ItemStack(Items.FLOWER_POT),new CompoundTag(),null));
            if(detail.contains("HasBed")&&!detail.getBoolean("HasBed"))rows.add(new Row(tr("no_bed"),tr("no_bed_hint"),new ItemStack(Items.RED_BED),new CompoundTag(),null));
            else if(detail.contains("Roofed")&&!detail.getBoolean("Roofed"))rows.add(new Row(tr("no_roof"),tr("no_roof_hint"),new ItemStack(Items.OAK_STAIRS),new CompoundTag(),null));
            conditionRows(detail);
            if(!detail.getString("EmploymentReason").isEmpty()) rows.add(new Row(tr("category.jobs"),metricText("employment."+detail.getString("EmploymentReason")),new ItemStack(Items.IRON_PICKAXE),new CompoundTag(),null));
        }
        for (CompoundTag row:servings(detail)) rows.add(new Row(item(row.getString("Item")).getHoverName(),tr("served_today",row.getInt("Count")),item(row.getString("Item")),row,null));listWidgets();
        if(resident && isRecruit) {
            button(tr("feed"),cx,bottom-44,cw/2-2,()->{detail=null;chest=null;section="chests";scroll=0;rebuild();},false);
            Button manage=button(tr("manage_native"),cx+cw/2+2,bottom-44,cw/2-2,()->{CompoundTag payload=new CompoundTag();payload.putUUID("Entity",detail.getUUID("Id"));request("manage",payload);},true);
            manage.active=actualEntity&&(!guard(detail)||isRecruit)&&!SettlementClientState.pending();manage.setTooltip(Tooltip.create(tr("manage_reach")));
        } else if(resident) {
            button(tr("feed"),cx,bottom-44,cw/3-2,()->{detail=null;chest=null;section="chests";scroll=0;rebuild();},false);
            Button home=button(tr("home"),cx+cw/3,bottom-44,cw/3-2,()->{CompoundTag payload=new CompoundTag();payload.putUUID("Entity",detail.getUUID("Id"));request("housing",payload);},false);home.active=!isRecruit&&!SettlementClientState.pending();
            button(tr("remove"),cx+cw*2/3,bottom-44,cw/3,()->{CompoundTag payload=new CompoundTag();payload.putUUID("Entity",detail.getUUID("Id"));ask(tr("remove_confirm",residentName(detail)),()->request("remove",payload));},true);
        } else {
            Button manage=button(tr("manage_native"),cx,bottom-44,cw*2/3-2,()->{CompoundTag payload=new CompoundTag();payload.putUUID("Entity",detail.getUUID("Id"));request("manage",payload);},true);
            manage.active=actualEntity&&(!guard(detail)||isRecruit)&&!SettlementClientState.pending();manage.setTooltip(Tooltip.create(tr("manage_reach")));
            Button locate=button(tr("locate"),cx+cw*2/3,bottom-44,cw/3,()->{info(tr("coordinates",position(detail.getLong("Position"))));},false);
            locate.active=detail.contains("Position")&&detail.getString("Dimension").equals(town.getString("Dimension"));
        }
    }
    private void housingWidgets() {
        button(tr("back"),cx,cy,52,()->{expectedHousing=null;inspecting=false;rebuild();},false);
        button(HousingInspector.tr(housing.example?"example":"mine"),cx+56,cy,Math.max(60,cw-56),()->{housing.example=!housing.example;rebuild();},false);
        int half=(cw-4)/2;
        addRenderableWidget(new AbstractSliderButton(cx,bottom-46,cw,20,HousingInspector.tr("see_inside",housing.depth()),housing.depth()/10D){
            @Override protected void updateMessage(){setMessage(HousingInspector.tr("see_inside",(int)Math.round(value*10)));}
            @Override protected void applyValue(){housing.depth((int)Math.round(value*10));}
        }).setTooltip(Tooltip.create(HousingInspector.tr("depth_hint")));
        button(HousingInspector.tr("reset_view"),cx,bottom-22,half,housing::resetView,false);
        button(tr("refresh"),cx+half+4,bottom-22,cw-half-4,()->{UUID resident=detail!=null&&detail.hasUUID("Id")?detail.getUUID("Id"):section.equals("settlement")?inspectedCityResident:null;if(resident!=null){CompoundTag payload=new CompoundTag();payload.putUUID("Entity",resident);request("housing",payload);}},true);
        drawLine(housing.score(),cx,cy+24,MUTED);
        housePreviewY=cy+36;housePreviewH=Math.max(24,bottom-78-housePreviewY);
        drawLine(HousingInspector.tr("controls"),cx,bottom-74,MUTED);
        drawings.add(()->{
            int legendY=bottom-62;
            graphics.fill(cx,legendY+1,cx+5,legendY+6,0xff4dff66);
            graphics.drawString(font,clip(HousingInspector.tr("legend_bed"),cw/3-10),cx+8,legendY,INK,false);
            graphics.fill(cx+cw/3,legendY+1,cx+cw/3+5,legendY+6,0xff40a6ff);
            graphics.drawString(font,clip(HousingInspector.tr("legend_decor"),cw/3-10),cx+cw/3+8,legendY,INK,false);
            graphics.fill(cx+2*cw/3,legendY+1,cx+2*cw/3+5,legendY+6,0xffccb866);
            graphics.drawString(font,clip(HousingInspector.tr("boundary"),cw/3-10),cx+2*cw/3+8,legendY,INK,false);
        });
    }
    private static List<CompoundTag> servings(CompoundTag resident) {return tags(resident,"FoodCounts");}
    private void foodRows() {
        search(cy);button(filterLabel(),cx,cy+24,cw,()->{filter=next(filter,new String[]{"all","stock","requested","safe"});scroll=0;rebuild();},false);listTop=cy+48;listBottom=bottom-48;
        for (CompoundTag row:tags(town,"Storage")) rows.add(new Row(Component.translatable("jem.settlements.storage_name",row.getInt("Number")),tr(row.getBoolean("Loaded")?"storage_loaded":"storage_unknown"),new ItemStack(Items.CHEST),row,()->{
            info(Component.translatable("jem.settlements.storage_shared"));
        }));
        List<CompoundTag> foods=tags(town,"FoodCatalogue");foods.sort(Comparator.comparing(r->item(r.getString("Item")).getHoverName().getString(),String.CASE_INSENSITIVE_ORDER));
        for(CompoundTag row:foods) {
            ItemStack food=item(row.getString("Item"));if(!matches(food.getHoverName().getString()))continue;
            if(filter.equals("stock")&&(!row.getBoolean("StockKnown")||row.getLong("Count")<=0)||filter.equals("requested")&&!(row.getBoolean("Requested")||row.getInt("Demand")>0)||filter.equals("safe")&&(!row.getBoolean("Classified")||row.getInt("Mood")<0))continue;
            rows.add(new Row(food.getHoverName(),tr("food_values",row.getBoolean("Classified")?signed(row.getInt("Mood")):tr("unknown").getString(),row.getBoolean("Classified")?signed(row.getInt("Satiety")):tr("unknown").getString(),row.getBoolean("StockKnown")?Long.toString(row.getLong("Count")):tr("unknown").getString()),food,row,()->info(row.getBoolean("Classified")?tr("food_detail",food.getHoverName(),signed(row.getInt("Mood")),signed(row.getInt("Satiety"))):tr("food_unknown"))));
        }
        listWidgets();
        int offset=town.getInt("FoodOffset"),total=town.getInt("FoodTotal");
        button(Component.literal("<"),cx,bottom-44,24,()->foodPage(offset-64),true).active=offset>0&&!SettlementClientState.pending();
        button(Component.literal(">"),cx+28,bottom-44,24,()->foodPage(offset+64),true).active=offset+64<total&&!SettlementClientState.pending();
        drawLine(tr("food_total",total),cx+58,bottom-38,MUTED);refreshFooter();
    }
    private void foodPage(int offset) {CompoundTag payload=new CompoundTag();payload.putInt("FoodOffset",Math.max(0,offset));request("refresh",payload);}
    private void workforceRows() {
        boolean recruit=section.equals("guards"),available=envelope.getBoolean(recruit?"RecruitsAvailable":"WorkersAvailable");
        if(!available) {drawWrapped(tr(recruit?"recruits_absent":"workers_absent"),cx,cy+20,cw,MUTED);return;}
        search(cy);listTop=cy+24;listBottom=bottom-48;
        for(CompoundTag row:tags(town,recruit?"Defense":"WorkforceRows")) {
            if(!recruit&&row.getString("Type").equals("RECRUIT"))continue;if(!matches(row.getString("Name")))continue;
            Component role=recruit?tr("recruit_state",Component.translatable("jem.settlements.gui.loyalty."+row.getString("Loyalty").toLowerCase(Locale.ROOT)),row.getInt("MissedPayrolls")):Component.translatable("jem.settlements.worker."+row.getString("Type").toLowerCase(Locale.ROOT));
            rows.add(new Row(Component.literal(row.getString("Name")),role,new ItemStack(recruit?Items.SHIELD:Items.IRON_PICKAXE),row,()->{residentScroll=scroll;detailScroll=0;detail=row;scroll=0;rebuild();}));
        }
        listWidgets();
        if(recruit) button(tr("pay_due"),cx,bottom-44,cw,()->ask(tr("payroll_confirm"),()->request("payroll",new CompoundTag())),true).active=town.getLong("UnpaidPayroll")>0&&!SettlementClientState.pending();
        else drawWrapped(tr("native_work"),cx,bottom-42,cw,MUTED);
    }
    private void transfer(String action) {
        try {
            long total=Math.multiplyExact(Long.parseLong(amountBox.getValue()),coinCurrency()?denomination.value():1L);if(total<=0){notice="jem.settlements.reason.invalid";return;}
            CompoundTag payload=new CompoundTag();payload.putLong("Amount",total);ask(tr(action+"_confirm",amountBox.getValue(),currencyItem().getHoverName()),()->request(action,payload));
        } catch(NumberFormatException|ArithmeticException invalid) {notice="jem.settlements.reason.invalid";}
    }
    private void guideWidgets() {
        cx=left+8;cw=panelW-16;cy=top+36;
        button(tr("back"),cx,cy,60,()->{mode=guideMode;section=guideReturn;scroll=0;rebuild();},false);
        button(Component.literal("<"),cx+64,cy,22,()->{chapter=Math.floorMod(chapter-1,CHAPTERS.length);scroll=0;rebuild();},false);
        button(Component.literal(">"),cx+90,cy,22,()->{chapter=(chapter+1)%CHAPTERS.length;scroll=0;rebuild();},false);drawLine(tr("chapter",chapter+1,CHAPTERS.length),cx+120,cy+6,MUTED);
        listTop=cy+28;listBottom=bottom-24;
        List<net.minecraft.util.FormattedCharSequence> lines=font.split(Component.translatable("jem.settlements.guide."+CHAPTERS[chapter]+(!coinCurrency() && (CHAPTERS[chapter].equals("territory") || CHAPTERS[chapter].equals("treasury"))?"_emerald":"")),cw-8);
        int visible=Math.max(1,(listBottom-listTop)/12),limit=Math.max(0,lines.size()-visible);scroll=Math.min(scroll,limit);int start=scroll;
        drawings.add(()->{SettlementDashboardArt.panel(graphics,cx-2,listTop-3,cw+4,listBottom-listTop+3,false);for(int i=start;i<Math.min(lines.size(),start+visible);i++)graphics.drawString(font,lines.get(i),cx+4,listTop+(i-start)*12,0xff352c20,false);});
        button(tr("previous"),cx,bottom-20,cw/2-2,()->{scroll=Math.max(0,scroll-visible);rebuild();},false).active=scroll>0;
        button(tr("next"),cx+cw/2+2,bottom-20,cw/2-2,()->{scroll=Math.min(limit,scroll+visible);rebuild();},false).active=scroll<limit;
    }
    private void info(Component message){information=true;confirmMode=mode;confirmText=message;confirm=()->{};map.release();rebuild();}
    private void ask(Component message,Runnable action) {information=false;confirmMode=mode;confirmText=message;confirm=action;map.release();rebuild();}
    private void confirmationWidgets() {
        int dx=left+Math.max(8,(panelW-224)/2),dw=Math.min(224,panelW-16);
        int lines=font.split(confirmText,dw-16).size(),dy=top+Math.max(34,(panelH-(lines*12+44))/2);drawWrapped(confirmText,dx+8,dy+8,dw-16,INK);
        Button cancel=button(tr(information?"close":"cancel"),dx,Math.min(bottom-24,dy+lines*12+20),dw/2-2,()->{confirm=null;mode=confirmMode;map.clearQuote();rebuild();},false);
        if(!information)button(tr("confirm"),dx+dw/2+2,Math.min(bottom-24,dy+lines*12+20),dw/2-2,()->{Runnable action=confirm;confirm=null;mode=confirmMode;action.run();rebuild();},true);setInitialFocus(cancel);
    }
    private void refreshFooter() {if(SettlementClientState.pending())button(tr("reconcile"),cx,bottom-20,cw,SettlementClientState::reconcileNow,false);else button(tr("refresh"),cx,bottom-20,cw,()->request("refresh",new CompoundTag()),true);}
    private void listWidgets() {
        int count=Math.max(0,(listBottom-listTop)/ROW_HEIGHT);scroll=Math.min(Math.max(0,scroll),Math.max(0,rows.size()-Math.max(1,count)));
        if(rows.isEmpty()){if(listBottom-listTop>=20)drawWrapped(tr("empty"),cx,listTop+8,cw,MUTED);return;}
        for(int i=scroll;i<Math.min(rows.size(),scroll+count);i++) {
            Row row=rows.get(i);int y=listTop+(i-scroll)*ROW_HEIGHT;
            if(row.action()!=null){
                Button open=new Button(Button.builder(row.name(),b->row.action().run()).bounds(cx,y,cw-4,ROW_HEIGHT-3)){
                    @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial){drawRow(g,row,getX(),getY(),getWidth(),isHoveredOrFocused());}
                };
                open.setTooltip(Tooltip.create(row.name().copy().append("\n").append(row.subtitle())));addRenderableWidget(open);
            }else drawings.add(()->drawRow(graphics,row,cx,y,cw-4,false));
        }
        if(count>0&&rows.size()>count){int railX=cx+cw-2,track=Math.max(1,listBottom-listTop),handle=Math.max(8,track*count/rows.size()),pos=(track-handle)*scroll/Math.max(1,rows.size()-count);drawings.add(()->graphics.fill(railX,listTop+pos,railX+2,listTop+pos+handle,0xffa9b77a));}
    }
    private void drawRow(GuiGraphics g,Row row,int x,int y,int w,boolean focused){
        boolean selected=residentSplit&&detail!=null&&detail.hasUUID("Id")&&row.data().hasUUID("Id")&&detail.getUUID("Id").equals(row.data().getUUID("Id"));
        if(selected)SettlementDashboardArt.control(g,x,y,w,ROW_HEIGHT-3,true,focused,true);
        else SettlementDashboardArt.card(g,x,y,w,ROW_HEIGHT-3,focused?0xff4aa2d3:0xffbfa67c);
        boolean npc=row.data().hasUUID("Id")&&(row.data().contains("Mood")||guard(row.data()));
        if(npc){SettlementDashboardArt.panel(g,x+3,y+3,28,33,true);portrait(g,row.data(),x+5,y+5,24,29);}else if(row.name().equals(tr("dashboard.no_complaints")))SettlementDashboardArt.icon(g,"check",x+7,y+9,20);else icon(g,row.icon(),x+7,y+9,20);
        int nameWidth=Math.max(20,w-42-(npc?19:0)),nameColor=selected?0xfff9efda:INK;
        g.drawString(font,clip(row.name(),nameWidth),x+35,y+6,nameColor,false);
        if(npc){
            g.drawString(font,clip(guard(row.data())?tr("filter.guards"):profession(row.data()),Math.max(20,w-49)),x+35,y+18,selected?0xffd8e4eb:MUTED,false);
            if(row.data().contains("Mood")){SettlementDashboardArt.mood(g,x+w-23,y+5,16,row.data().getInt("Mood"));SettlementDashboardArt.track(g,x+35,y+30,w-43,6,row.data().getInt("Mood"),scoreColor(row.data().getInt("Mood")));}
            else if(row.data().contains("Health"))SettlementDashboardArt.track(g,x+35,y+30,w-43,6,(int)(100*row.data().getFloat("Health")/Math.max(1,row.data().getFloat("MaxHealth"))),0xffa3bb78);
        }else {
            var lines=font.split(row.subtitle(),Math.max(20,w-42));
            for(int i=0;i<Math.min(2,lines.size());i++)g.drawString(font,lines.get(i),x+35,y+18+i*10,MUTED,false);
        }
    }
    private static void smallItem(GuiGraphics g,ItemStack stack,int x,int y){g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(.5F,.5F,1);g.renderFakeItem(stack,0,0);g.pose().popPose();}
    private void bar(GuiGraphics g,int x,int y,int w,int value,Component label) {
        value=Math.max(0,Math.min(100,value));
        if(!label.getString().isEmpty())g.drawString(font,label.copy().append(" "+value+"/100"),x,y,INK,false);
        SettlementDashboardArt.track(g,x,y+(label.getString().isEmpty()?0:12),w,8,value,scoreColor(value));
    }
    private static int scoreColor(int value){return value<35?0xffd45f52:value<80?0xffe4b74e:0xff7ebd52;}
    private static void icon(GuiGraphics g,ItemStack item,int x,int y,int size) {
        String authored=semanticIcon(item);
        if(authored!=null){SettlementDashboardArt.icon(g,authored,x,y,size);return;}
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size/16F,size/16F,1);g.renderFakeItem(item,0,0);g.pose().popPose();
    }
    /** Generic UI subjects use authored assets; real inventory stacks keep native item rendering. */
    private static String semanticIcon(ItemStack item) {
        if(item.is(Items.OAK_DOOR)||item.is(Items.RED_BED)||item.is(Items.OAK_STAIRS))return "home";
        if(item.is(Items.VILLAGER_SPAWN_EGG))return "residents";
        if(item.is(Items.CHEST))return "chests";
        if(item.is(Items.BREAD))return "bread";
        if(item.is(Items.LANTERN)||item.is(Items.TORCH))return "lantern";
        if(item.is(Items.FLOWER_POT)||item.is(Items.OAK_SAPLING))return "development";
        return null;
    }
    private void portrait(GuiGraphics g,CompoundTag row,int x,int y,int w,int h) {
        var entity=minecraft.level!=null&&row.contains("EntityId")?minecraft.level.getEntity(row.getInt("EntityId")):null;
        g.enableScissor(x,y,x+w,y+h);
        if(entity instanceof LivingEntity living&&row.hasUUID("Id")&&entity.getUUID().equals(row.getUUID("Id")))
            InventoryScreen.renderEntityInInventoryFollowsMouse(g,x+w/2,y+h+Math.max(5,h/5),Math.max(13,(int)(h*.64)),0,0,living);
        else {icon(g,new ItemStack(guard(row)?Items.SHIELD:Items.VILLAGER_SPAWN_EGG),x+(w-20)/2,y+Math.max(3,(h-20)/2),20);}
        g.disableScissor();
    }
    private void summaryCards(boolean happinessPage) {
        var residents=tags(town,"Residents");var metrics=town.getCompound("Metrics");int gap=5,cell=(cw-gap*3)/4;
        String[] keys=happinessPage?new String[]{"happiness","satiety","unhappy","beauty"}:new String[]{"residents","happiness","beauty","treasury"};
        for(int i=0;i<4;i++) {
            String key=keys[i];int x=cx+i*(cell+gap),w=i==3?cx+cw-x:cell;
            int value=switch(key){case "happiness"->town.getInt("Happiness");case "satiety"->(int)Math.round(residents.stream().mapToInt(r->r.getInt("Satiety")).average().orElse(0));case "unhappy"->(int)(100*residents.stream().filter(r->r.getInt("Mood")<town.getInt("UnhappyThreshold")).count()/Math.max(1,residents.size()));case "beauty"->town.getInt("Decoration");case "residents"->Math.min(100,residents.size()*100/Math.max(1,metrics.getInt("RequiredResidents")));default->0;};
            Component display=switch(key){case "residents"->Component.literal(residents.size()+" / "+metrics.getInt("RequiredResidents"));case "unhappy"->Component.literal(residents.stream().filter(r->r.getInt("Mood")<town.getInt("UnhappyThreshold")).count()+" / "+residents.size());case "treasury"->SettlementPriceRenderer.format(town.getLong("Treasury"),coinCurrency());default->Component.literal(value+"/100");};
            drawings.add(()->{
                SettlementDashboardArt.card(graphics,x,cy,w,58,key.equals("beauty")?0xffc58bac:key.equals("treasury")?GOLD:key.equals("residents")?0xff92b66c:0xffd6b168);
                graphics.drawString(font,clip(tr("dashboard."+key),w-12),x+6,cy+7,INK,false);
                if(key.equals("happiness"))SettlementDashboardArt.mood(graphics,x+6,cy+21,24,value);else SettlementDashboardArt.icon(graphics,switch(key){case "treasury"->"coins";case "beauty"->"development";case "satiety"->"bread";case "unhappy"->"mood_sad";default->"residents";},x+6,cy+21,24);
                graphics.drawString(font,clip(display,w-35),x+30,cy+27,INK,false);
                if(!key.equals("treasury"))SettlementDashboardArt.track(graphics,x+7,cy+47,w-14,6,value,key.equals("beauty")?0xff45b8d5:key.equals("unhappy")?0xffd36459:scoreColor(value));
            });
        }
    }
    private void clipSection(int firstDrawing,int firstWidget,int from,int to) {
        for(int i=firstWidget;i<children().size();i++)if(children().get(i) instanceof net.minecraft.client.gui.components.AbstractWidget widget)
            widget.visible=widget.getY()>=from&&widget.getY()+widget.getHeight()<=to;
        List<Runnable> content=new ArrayList<>(drawings.subList(firstDrawing,drawings.size()));drawings.subList(firstDrawing,drawings.size()).clear();
        int x=cx,w=cw;
        drawings.add(()->{graphics.enableScissor(x,from,x+w,to);for(Runnable draw:content)draw.run();graphics.disableScissor();});
    }
    private void scrollRail(int x,int y,int h,int total,int offset) {
        if(total<=h)return;int handle=Math.max(12,h*h/total),pos=(h-handle)*offset/Math.max(1,total-h);
        drawings.add(()->{graphics.fill(x,y,x+2,y+h,0x33292e35);graphics.fill(x,y+pos,x+2,y+pos+handle,0xffa18a66);});
    }
    private boolean coinCurrency() { return envelope.getBoolean("CoinCurrency"); }
    private ItemStack currencyItem() { return coinCurrency()?item("jem_economy:"+denomination.id()):new ItemStack(Items.EMERALD); }
    private void coins(long copper,int x,int y,int maxWidth) {
        if (!coinCurrency()) { SettlementPriceRenderer.draw(graphics,font,copper,x,y,maxWidth,INK,false); return; }
        int dx=x,dy=y;
        for(CoinDenomination coin:CoinDenomination.descending()) {
            long count=copper/coin.value();copper%=coin.value();if(count==0&&coin!=CoinDenomination.COPPER)continue;
            int w=18+font.width(Long.toString(count));if(dx>x&&dx+w>x+maxWidth){dx=x;dy+=18;}
            graphics.renderItem(new ItemStack(CurrencyItems.item(coin)),dx,dy);graphics.drawString(font,Long.toString(count),dx+16,dy+4,INK,false);dx+=w;
        }
    }
    private Component clip(Component text,int width){return Component.literal(font.plainSubstrByWidth(text.getString(),width));}
    private void drawLine(Component text,int x,int y,int color) {drawings.add(()->graphics.drawString(font,clip(text,Math.max(20,left+panelW-8-x)),x,y,color,false));}
    private void drawWrapped(Component text,int x,int y,int w,int color) {drawings.add(()->graphics.drawWordWrap(font,text,x,y,w,color));}
    private Component filterLabel(){if(filter.startsWith("profession:")){CompoundTag row=new CompoundTag();row.putString("Profession",filter.substring(11));return profession(row);}if(filter.startsWith("role:"))return Component.translatable("jem.settlements.worker."+filter.substring(5).toLowerCase(Locale.ROOT));return tr("filter."+filter);}
    private String nextResidentFilter(){List<String> options=new ArrayList<>(List.of("all","villagers","guards","hungry","unhappy","working"));for(CompoundTag row:tags(town,"Residents")){if(!row.getString("Profession").isEmpty()&&!options.contains("profession:"+row.getString("Profession")))options.add("profession:"+row.getString("Profession"));if(!row.getString("Role").isEmpty()&&!options.contains("role:"+row.getString("Role")))options.add("role:"+row.getString("Role"));}return options.get((options.indexOf(filter)+1)%options.size());}
    private boolean matches(String value){return value.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));}
    private static String next(String value,String[] values){return values[(Arrays.asList(values).indexOf(value)+1)%values.length];}
    private static String signed(int value){return value>0?"+"+value:Integer.toString(value);}
    private static String position(long packed){BlockPos pos=BlockPos.of(packed);return pos.getX()+", "+pos.getY()+", "+pos.getZ();}
    private static Component dimension(String id){ResourceLocation key=ResourceLocation.tryParse(id);if(key==null)return tr("unknown");if(key.getNamespace().equals("minecraft")&&List.of("overworld","the_nether","the_end").contains(key.getPath()))return tr("dimension."+key.getPath());String translation="dimension."+key.getNamespace()+"."+key.getPath();return net.minecraft.client.resources.language.I18n.exists(translation)?Component.translatable(translation):Component.literal(key.getPath().replace('_',' '));}
    private static Component profession(CompoundTag row){ResourceLocation id=ResourceLocation.tryParse(row.getString("Profession"));return id==null?tr("unknown_role"):Component.translatable("entity."+id.getNamespace()+".villager."+id.getPath());}
    private static ItemStack item(String value){ResourceLocation id=ResourceLocation.tryParse(value);return id!=null&&BuiltInRegistries.ITEM.containsKey(id)?new ItemStack(BuiltInRegistries.ITEM.get(id)):ItemStack.EMPTY;}
    private static List<CompoundTag> tags(CompoundTag tag,String key){List<CompoundTag> result=new ArrayList<>();for(Tag row:tag.getList(key,Tag.TAG_COMPOUND))result.add(((CompoundTag)row).copy());return result;}
    @Override public boolean isPauseScreen(){return false;}
    private int refreshTicks;
    @Override public void tick(){
        if(minecraft.level==null||minecraft.player==null){onClose();return;}
        if(!minecraft.isWindowActive())map.clearQuote();
        if(map.dragging() && org.lwjgl.glfw.GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(),0)!=org.lwjgl.glfw.GLFW.GLFW_PRESS) {map.clearQuote();rebuild();}
        SettlementClientState.reconcile();
        if(searchDirty){searchDirty=false;rebuild();}
        if(++refreshTicks>=100){refreshTicks=0;if(SettlementNetwork.supported()){
            UUID id=town.hasUUID("Id")?town.getUUID("Id"):null;
            SettlementNetwork.action("heartbeat",id,new CompoundTag(),UUID.randomUUID());
        }}
    }
    @Override public void onClose(){map.clearQuote();SettlementClientState.closed();minecraft.setScreen(null);}
    @Override public void removed(){map.clearQuote();SettlementClientState.closed();}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==256&&inspecting&&confirm==null){expectedHousing=null;inspecting=false;rotatingHouse=false;panningHouse=false;rebuild();return true;}
        if(key==256){expectedHousing=null;map.clearQuote();if(confirm!=null){confirm=null;mode=confirmMode;rebuild();return true;}if(mode.equals("guide")){mode=guideMode;section=guideReturn;rebuild();return true;}if(chest!=null){chest=null;rebuild();return true;}if(detail!=null&&!residentSplit){detail=null;rebuild();return true;}}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(super.mouseClicked(x,y,button))return true;
        if(scaleLimited)return false;
        if(inspecting&&mode.equals("town")&&confirm==null&&housing.inside(x,y)&&(button==0||button==1)){rotatingHouse=button==0;panningHouse=button==1;return true;}
        if(!inspecting&&confirm==null&&mode.equals("town")&&section.equals("map")&&detail==null&&map.click(x,y,button,town)){return true;}
        return false;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(inspecting&&confirm==null){if(rotatingHouse&&button==0){housing.rotate(dx,dy);return true;}if(panningHouse&&button==1){housing.pan(dx,dy);return true;}}return !inspecting&&confirm==null&&section.equals("map")&&map.drag(x,y,dx,dy,town)||super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){rotatingHouse=false;panningHouse=false;boolean selected=map.dragging();map.release();if(selected)rebuild();return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double delta){
        if(scaleLimited)return false;
        if(confirm!=null)return false;
        if(inspecting&&housing.inside(x,y)){housing.zoom(delta);return true;}
        if(!inspecting&&mode.equals("town")&&section.equals("map")&&detail==null&&map.contains(x,y)){map.zoom(delta,x,y);return true;}
        if(cityActive()&&y>=cityContentTop&&x>=cx&&x<cx+cw){scroll=Math.max(0,Math.min(cityScrollLimit,scroll+(delta>0?-24:24)));rebuild();return true;}
        if(mode.equals("town")&&section.equals("happiness")&&y>=dashboardTop&&y<bottom&&x>=cx&&x<cx+cw){scroll=Math.max(0,Math.min(dashboardLimit,scroll+(delta>0?-24:24)));rebuild();return true;}
        if(residentSplit&&y>=detailTop&&y<detailBottom&&x>=cx+Math.max(144,Math.min(222,(int)(cw*.42)))+8){detailScroll=Math.max(0,detailScroll+(delta>0?-1:1));rebuild();return true;}
        if(chest==null&&x>=cx&&x<(residentSplit?cx+Math.max(144,Math.min(222,(int)(cw*.42))):cx+cw)&&y>=listTop&&y<listBottom){scroll=Math.max(0,scroll+(delta>0?-1:1));if(residentSplit)residentScroll=scroll;rebuild();return true;}
        return super.mouseScrolled(x,y,delta);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick){
        graphics=g;renderMouseX=mouseX;renderMouseY=mouseY;renderBackground(g);
        SettlementDashboardArt.shell(g,left,top+44,panelW,panelH-44,compact?8:sidebarWidth+6);
        SettlementDashboardArt.header(g,left+(compact?0:sidebarWidth-3),top,panelW-(compact?0:sidebarWidth-3),44);
        Component heading=mode.equals("town")?Component.literal(town.getString("Name")):title;
        int hx=left+(compact?57:sidebarWidth+58),hw=Math.max(25,panelW-(compact?120:sidebarWidth+120)-(mode.equals("town")&&envelope.getInt("DirectoryTotal")>1?62:0));
        g.pose().pushPose();g.pose().translate(hx,top+17,0);g.pose().scale(1.4F,1.4F,1);g.drawString(font,clip(heading,(int)(hw/1.4)),0,0,INK,false);g.pose().popPose();
        if(scaleLimited){g.drawWordWrap(font,tr("scale"),left+12,top+50,Math.max(20,panelW-24),INK);super.render(g,mouseX,mouseY,partialTick);graphics=null;return;}
        if(mode.equals("loading")||mode.equals("unsupported"))g.drawWordWrap(font,tr(mode),left+12,top+50,Math.max(20,panelW-24),INK);
        if(!inspecting&&mode.equals("town")&&section.equals("map")&&detail==null&&confirm==null)map.render(g,town,town,minecraft);
        for(Runnable draw:drawings)draw.run();super.render(g,mouseX,mouseY,partialTick);
        if(inspecting&&mode.equals("town")&&confirm==null){
            housing.render(g,cx,housePreviewY,cw,housePreviewH,mouseX,mouseY);
            if(mouseX>=cx&&mouseX<cx+cw&&mouseY>=bottom-76&&mouseY<bottom-50)g.renderComponentTooltip(font,List.of(HousingInspector.tr("controls"),HousingInspector.tr("legend_bed"),HousingInspector.tr("legend_decor"),HousingInspector.tr("area"),HousingInspector.tr("legend_missing")),mouseX,mouseY);
        }
        if(!notice.isBlank()&&!SettlementClientState.pending()&&envelope.getInt("UncertainActions")==0)g.drawString(font,clip(Component.translatable(notice),panelW-16),left+8,top+panelH-12,0xffe6bd75,false);
        if(envelope.getInt("UncertainActions")>0&&!SettlementClientState.pending())g.drawString(font,clip(tr("unresolved"),panelW-16),left+8,top+panelH-12,0xffedcc84,false);
        if(SettlementClientState.pending())g.drawString(font,clip(tr(SettlementClientState.outcomeUnknown()?"outcome_unknown":"pending"),panelW-16),left+8,top+panelH-12,0xffedcc84,false);
        if(mode.equals("town")&&section.equals("chests")&&chest!=null&&confirm==null&&bottom-52-(cy+40)>=18){
            int columns=chestColumns(),gx=cx+(cw-columns*18)/2,gy=cy+40;
            int col=(mouseX-gx)/18,row=(mouseY-gy)/18,index=row*columns+col;
            if(mouseX>=gx&&col<columns&&mouseY>=gy&&index<chestPageSize()&&mouseY<bottom-52){
                int slotIndex=chest.getInt("SlotOffset")+index;
                for(CompoundTag slot:tags(chest,"Slots"))if(slot.getInt("Slot")==slotIndex){ItemStack stack=ItemStack.of(slot.getCompound("Stack"));if(!stack.isEmpty()){if(slot.getBoolean("NbtOmitted"))g.renderComponentTooltip(font,List.of(stack.getHoverName(),tr("chest_nbt_omitted")),mouseX,mouseY);else g.renderTooltip(font,stack,mouseX,mouseY);}break;}
            }
        }
        graphics=null;
    }
}
