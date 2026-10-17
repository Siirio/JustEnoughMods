package com.siirio.jemprofessions.client;

import com.siirio.jemprofessions.network.*;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.tree.*;
import com.siirio.jemprofessions.sandbox.*;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** All links, frames, native icons and hit regions use the same world-to-screen transform. */
public final class ProfessionRadialScreen extends Screen {
    private static ProfessionId lastProfession=ProfessionId.ARCHITECT;
    private static String lastNode="long_arms_1";
    private static boolean free;
    private ProfessionStatusPacket status;
    private ProfessionId selected=lastProfession;
    private ProfessionNode focused=ProfessionCatalog.node(lastProfession,lastNode);
    private final Map<ProfessionId,Point> roots=new LinkedHashMap<>();
    private final Map<Ref,Point> nodes=new LinkedHashMap<>();
    private double zoom,panX,panY,pressX,pressY;
    private boolean dragging,moved,pending;
    private int viewportTop=59,viewportBottom,detailTop,detailScroll;
    private Button learn,activate,select,toggle;
    public ProfessionRadialScreen(ProfessionStatusPacket status){super(Component.translatable("jem.professions.screen.title"));this.status=status;}
    public void accept(ProfessionStatusPacket packet){status=packet;pending=false;updateButtons();}
    public void refreshControls(){updateButtons();}
    @Override protected void init(){
        detailTop=Math.max(viewportTop+42,height-112);viewportBottom=detailTop-4;
        buildGraph();if(zoom==0)fit();
        addRenderableWidget(Button.builder(text("Вписать","Fit"),b->fit()).bounds(width-152,9,62,18).build());
        addRenderableWidget(Button.builder(text("Закрыть","Close"),b->onClose()).bounds(width-84,9,74,18).build());
        toggle=addRenderableWidget(Button.builder(Component.empty(),b->{free=!free;updateButtons();}).bounds(12,32,Math.min(194,width/2-16),18).build());
        learn=addRenderableWidget(Button.builder(Component.empty(),b->learn()).bounds(width-132,height-26,120,19).build());
        activate=addRenderableWidget(Button.builder(text("Действие / настройки","Action / settings"),b->{if(focused!=null)minecraft.setScreen(new SandboxActionScreen(this,selected,focused));}).bounds(width-132,detailTop+5,120,19).build());
        select=addRenderableWidget(Button.builder(text("Назначить на клавишу","Bind action key"),b->request("select")).bounds(width-132,detailTop+28,120,19).build());
        updateButtons();if(focused!=null)request("view");
    }
    private void buildGraph(){
        roots.clear();nodes.clear();
        for(ProfessionId id:ProfessionId.values()){
            double angle=-Math.PI/2+id.ordinal()*Math.PI/5;roots.put(id,point(angle,116,0));
            var branches=ProfessionCatalog.branches(id);
            for(var node:ProfessionCatalog.nodes(id))nodes.put(new Ref(id,node),point(angle,280+(node.rank()-1)*100,(branches.indexOf(node.branch())-(branches.size()-1)/2.0)*60));
        }
    }
    private static Point point(double a,double r,double t){return new Point(Math.cos(a)*r-Math.sin(a)*t,Math.sin(a)*r+Math.cos(a)*t);}
    private void fit(){zoom=Math.min(.95,Math.max(.04,Math.min((width-34)/1080.0,(viewportBottom-viewportTop-10)/1080.0)));panX=panY=0;}
    private double cx(){return width/2.0+panX;}private double cy(){return(viewportTop+viewportBottom)/2.0+panY;}
    private double wx(double x){return(x-cx())/zoom;}private double wy(double y){return(y-cy())/zoom;}
    private static Component text(String ru,String en){return SandboxSkillText.text(ru,en);}
    private void updateButtons(){
        if(toggle==null)return;
        if(!SandboxClientState.owner())free=false;
        toggle.setMessage(text("Свободное изучение: "+(free?"ВКЛ":"ВЫКЛ"),"Free unlocks: "+(free?"ON":"OFF")));
        toggle.active=SandboxClientState.owner();
        boolean unlocked=focused!=null&&unlocked(selected,focused);
        learn.setMessage(pending?text("Ожидание…","Waiting…"):free?text(unlocked?"Убрать навык":"Изучить бесплатно",unlocked?"Remove skill":"Unlock free"):text(unlocked?"Изучено":"Изучить · 1 очко",unlocked?"Unlocked":"Learn · 1 point"));
        learn.active=focused!=null&&!pending&&status.authenticated()&&SandboxProfessionPolicy.available(focused.id())&&(free&&SandboxClientState.owner()||ready(selected,focused));
        activate.active=!pending&&(unlocked||selected==ProfessionId.MERCHANT||focused!=null&&focused.id().equals("cargo_contract"));select.active=unlocked&&!pending&&!SandboxSkillText.passive(focused.id());
    }
    private void learn(){if(focused==null||pending)return;if(free)request(unlocked(selected,focused)?"unclaim":"claim");else{pending=true;ProfessionsNetwork.unlock(selected,focused.id());}updateButtons();}
    private void request(String action){if(focused==null)return;if(action.equals("claim")||action.equals("unclaim"))pending=true;SandboxProfessionRuntime.request(selected,focused.id(),action,new CompoundTag());}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);g.fill(6,5,width-6,height-5,0xee171310);g.drawString(font,title,13,12,0xffffe0ad,false);
        String values=Component.translatable("jem.professions.name."+selected.name().toLowerCase(java.util.Locale.ROOT)).getString()+"  L"+status.levels()[selected.ordinal()]+"  XP "+status.xp()[selected.ordinal()]+"  ◆ "+status.points()[selected.ordinal()];
        g.drawString(font,font.plainSubstrByWidth(values,Math.max(30,width-230)),220,36,0xffe7c78f,false);g.fill(10,54,width-10,56,0xffa17642);
        g.enableScissor(10,viewportTop,width-10,viewportBottom);g.pose().pushPose();g.pose().translate(cx(),cy(),0);g.pose().scale((float)zoom,(float)zoom,1);
        for(var e:roots.entrySet())line(g,new Point(0,0),e.getValue(),e.getKey()==selected?0xffd6ad59:0xff51483a);
        for(var e:nodes.entrySet()){
            Ref ref=e.getKey();var parent=ProfessionCatalog.node(ref.profession,ref.node.parentId());Point from=parent==null?roots.get(ref.profession):nodes.get(new Ref(ref.profession,parent));
            if(from!=null)line(g,from,e.getValue(),unlocked(ref.profession,ref.node)?0xffb5ba7c:0xff5b5142);
        }
        for(var e:roots.entrySet()){Point p=e.getValue();frame(g,p,36,e.getKey()==selected?0xffffcf64:0xff9c7853,0xff30251c);g.renderItem(new ItemStack(rootIcon(e.getKey())),(int)p.x-8,(int)p.y-8);}
        for(var e:nodes.entrySet()){
            Ref r=e.getKey();Point p=e.getValue();boolean active=unlocked(r.profession,r.node),focus=r.profession==selected&&r.node==focused;
            frame(g,p,28,focus?0xffffd76f:active?0xffb6c784:ready(r.profession,r.node)?0xffb49562:0xff514b44,active?0xff343e28:0xff201c18);
            g.renderItem(new ItemStack(item(r.node.icon())),(int)p.x-8,(int)p.y-8);
            if(active)g.fill((int)p.x+9,(int)p.y+9,(int)p.x+12,(int)p.y+12,SandboxClientState.testClaim(r.node.id())?0xff78c6db:0xffa8cf68);
        }
        frame(g,new Point(0,0),54,0xffd8b36a,0xff30251e);
        ItemStack head=new ItemStack(Items.PLAYER_HEAD);
        if(minecraft.player!=null)head.getOrCreateTag().put("SkullOwner",net.minecraft.nbt.NbtUtils.writeGameProfile(new CompoundTag(),minecraft.player.getGameProfile()));
        g.renderItem(head,-8,-8);
        g.pose().popPose();g.disableScissor();drawDetails(g);super.render(g,mx,my,partial);
        if(my>=viewportTop&&my<viewportBottom){Ref hovered=hitNode(mx,my);
            if(hovered!=null)g.renderComponentTooltip(font,java.util.List.of(SandboxSkillText.name(hovered.node.id()),text("Уровень "+hovered.node.level(),"Level "+hovered.node.level())),mx,my);
            else for(var e:roots.entrySet())if(contains(e.getValue(),wx(mx),wy(my),36))g.renderTooltip(font,Component.translatable("jem.professions.name."+e.getKey().name().toLowerCase(java.util.Locale.ROOT)),mx,my);
        }
    }
    private void drawDetails(GuiGraphics g){
        g.fill(10,detailTop,width-10,height-31,0xff211b16);if(focused==null)return;
        int w=Math.max(48,width-172),y=detailTop+7-detailScroll;g.enableScissor(14,detailTop+3,width-146,height-33);
        g.drawString(font,SandboxSkillText.name(focused.id()),16,y,0xffffe1aa,false);y+=13;
        y=wrapped(g,SandboxSkillText.field(focused.id(),"effect"),16,y,w,0xffefeee7);
        y=wrapped(g,text("Как использовать: ","Use: ").copy().append(SandboxSkillText.field(focused.id(),"use")),16,y+3,w,0xffd5c7ae);
        y=wrapped(g,text("Опыт: ","XP: ").copy().append(SandboxSkillText.field(focused.id(),"xp")),16,y+3,w,0xffbcc995);
        if(SandboxClientState.testClaim(focused.id()))y=wrapped(g,text("Изучено для теста: опыт не менялся.","Test unlock: earned XP is unchanged."),16,y+3,w,0xff80c8d8);
        Component requirement=text("Уровень "+focused.level()+" · 1 очко","Level "+focused.level()+" · 1 point");
        if(!focused.parentId().isBlank())requirement=requirement.copy().append(" · ").append(SandboxSkillText.name(focused.parentId()));
        wrapped(g,requirement,16,y+3,w,0xffb6a991);g.disableScissor();
        g.drawString(font,font.plainSubstrByWidth(text("Колесо: масштаб · Перетащи пустое место","Wheel: zoom · Drag empty space: pan").getString(),Math.max(30,width-158)),13,height-21,0xffa9a094,false);
    }
    private int wrapped(GuiGraphics g,Component text,int x,int y,int w,int color){for(var line:font.split(text,w)){g.drawString(font,line,x,y,color,false);y+=11;}return y;}
    private static void frame(GuiGraphics g,Point p,int s,int border,int fill){int x=(int)p.x-s/2,y=(int)p.y-s/2;g.fill(x,y,x+s,y+s,border);g.fill(x+2,y+2,x+s-2,y+s-2,fill);}
    private static void line(GuiGraphics g,Point a,Point b,int color){int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(a.x-b.x),Math.abs(a.y-b.y))));for(int i=0;i<=steps;i++){int x=(int)Math.round(a.x+(b.x-a.x)*i/steps),y=(int)Math.round(a.y+(b.y-a.y)*i/steps);g.fill(x,y,x+2,y+2,color);}}
    private Ref hitNode(double x,double y){double a=wx(x),b=wy(y);for(var e:nodes.entrySet())if(contains(e.getValue(),a,b,28))return e.getKey();return null;}
    private static boolean contains(Point p,double x,double y,int s){return Math.abs(x-p.x)<=s/2.0&&Math.abs(y-p.y)<=s/2.0;}
    @Override public boolean mouseClicked(double x,double y,int button){if(super.mouseClicked(x,y,button))return true;if(button!=0||y<viewportTop||y>=viewportBottom)return false;pressX=x;pressY=y;moved=false;dragging=hitNode(x,y)==null;return true;}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(button==0&&y>=viewportTop&&y<viewportBottom){if(Math.hypot(x-pressX,y-pressY)>3)moved=true;if(dragging){panX+=dx;panY+=dy;return true;}}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){
        dragging=false;if(button==0&&!moved&&y>=viewportTop&&y<viewportBottom){Ref ref=hitNode(x,y);
            if(ref!=null){choose(ref.profession,ref.node);return true;}
            for(var e:roots.entrySet())if(contains(e.getValue(),wx(x),wy(y),36)){choose(e.getKey(),ProfessionCatalog.nodes(e.getKey()).get(0));return true;}
        }return super.mouseReleased(x,y,button);
    }
    private void choose(ProfessionId id,ProfessionNode node){selected=id;focused=node;detailScroll=0;lastProfession=id;lastNode=node.id();updateButtons();request("view");}
    @Override public boolean mouseScrolled(double x,double y,double delta){
        if(y>=detailTop){detailScroll=Math.max(0,Math.min(140,detailScroll-(int)(delta*12)));return true;}
        if(y>=viewportTop&&y<viewportBottom){double a=wx(x),b=wy(y);zoom=Math.max(.04,Math.min(2.2,zoom*Math.pow(1.15,delta)));panX=x-width/2.0-a*zoom;panY=y-(viewportTop+viewportBottom)/2.0-b*zoom;return true;}
        return super.mouseScrolled(x,y,delta);
    }
    @Override public boolean isPauseScreen(){return false;}
    private boolean unlocked(ProfessionId id,ProfessionNode node){return status.unlocked().contains(id.name()+":"+node.id());}
    private boolean ready(ProfessionId id,ProfessionNode node){return status.authenticated()&&!unlocked(id,node)&&status.levels()[id.ordinal()]>=node.level()&&status.points()[id.ordinal()]>0&&(node.parentId().isBlank()||status.unlocked().contains(id.name()+":"+node.parentId()));}
    private static Item item(String id){return BuiltInRegistries.ITEM.getOptional(new ResourceLocation(id)).orElse(Items.BOOK);}
    private static Item rootIcon(ProfessionId id){return switch(id){case ARCHITECT->Items.BRICKS;case FARMER->Items.WHEAT;case FISHER->Items.FISHING_ROD;case MINER->Items.IRON_PICKAXE;case EXPLORER->Items.COMPASS;case ENGINEER->item("create:wrench");case WARRIOR->Items.IRON_SWORD;case MERCHANT->Items.EMERALD;case MAYOR->Items.BELL;case MAGE->item("irons_spellbooks:iron_spell_book");};}
    private record Point(double x,double y){}private record Ref(ProfessionId profession,ProfessionNode node){}
}
