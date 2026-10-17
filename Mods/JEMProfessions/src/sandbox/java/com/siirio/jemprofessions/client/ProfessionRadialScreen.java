package com.siirio.jemprofessions.client;

import com.siirio.jemprofessions.network.ProfessionStatusPacket;
import com.siirio.jemprofessions.network.ProfessionsNetwork;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.tree.ProfessionCatalog;
import com.siirio.jemprofessions.tree.ProfessionNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Sandbox presentation of the same catalog and server snapshot; Puffish owns all progression. */
public final class ProfessionRadialScreen extends Screen {
    private static ProfessionId lastSelected = ProfessionId.ARCHITECT;
    private final ProfessionStatusPacket status;
    private ProfessionId selected;
    private ProfessionNode focused;
    private int left, top, panelWidth, panelHeight, railWidth, graphLeft, graphTop, graphWidth, graphHeight;
    private int detailScroll;
    private boolean pending;
    private final Map<ProfessionId, Box> professions = new LinkedHashMap<>();
    private final Map<ProfessionNode, Box> nodes = new LinkedHashMap<>();

    public ProfessionRadialScreen(ProfessionStatusPacket status) {
        super(Component.translatable("jem.professions.screen.title"));
        this.status = status;
        selected = lastSelected;
    }

    @Override protected void init() {
        panelWidth = Math.min(648, width-16); panelHeight = Math.min(372, height-16);
        left = (width-panelWidth)/2; top = (height-panelHeight)/2;
        railWidth = panelWidth >= 500 ? 118 : 46;
        graphLeft = left+railWidth+12; graphTop = top+58;
        graphWidth = panelWidth-railWidth-22; graphHeight = Math.max(60,panelHeight-132);
        professions.clear();
        int rowHeight = Math.max(16,Math.min(27,(panelHeight-46)/10));
        for (ProfessionId id : ProfessionId.values())
            professions.put(id,new Box(left+7,top+37+id.ordinal()*rowHeight,railWidth-2,rowHeight-2));
        buildTree();
    }

    private void buildTree() {
        nodes.clear();
        List<String> branches = ProfessionCatalog.branches(selected);
        int row = Math.max(18,graphHeight/Math.max(1,branches.size()));
        int nodeSize = Math.min(32,Math.max(18,row-16));
        for (ProfessionNode node : ProfessionCatalog.nodes(selected)) {
            int x = graphLeft+30+(graphWidth-62)*(node.rank()-1)/2;
            int y = graphTop+row*branches.indexOf(node.branch())+row/2-4;
            nodes.put(node,new Box(x-nodeSize/2,y-nodeSize/2,nodeSize,nodeSize));
        }
        if (focused == null || !nodes.containsKey(focused)) focused=ProfessionCatalog.nodes(selected).get(0);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        ProfessionMaterials.board(g,left,top,panelWidth,panelHeight,false);
        ProfessionMaterials.board(g,left+6,top+5,panelWidth-12,27,true);
        g.renderItem(new ItemStack(Items.BOOK),left+14,top+10);
        g.drawString(font,title,left+35,top+14,0xffffecd0,false);
        String close="×";
        g.drawString(font,close,left+panelWidth-19,top+14,0xffe4d9bb,false);

        for (var entry:professions.entrySet()) {
            ProfessionId id=entry.getKey(); Box b=entry.getValue();
            ProfessionMaterials.board(g,b.x,b.y,b.w,b.h,id==selected);
            g.renderItem(new ItemStack(icon(id)),b.x+4,b.y+(b.h-16)/2);
            if (railWidth>60) {
                g.drawString(font,font.plainSubstrByWidth(Component.translatable(name(id)).getString(),b.w-28),b.x+25,b.y+4,
                        id==selected?0xffffedc6:0xffcfbea0,false);
                if (b.h>=23) g.drawString(font,"L"+status.levels()[id.ordinal()],b.x+25,b.y+14,0xff9f987c,false);
            }
        }
        g.drawString(font,font.plainSubstrByWidth(Component.translatable(name(selected)).getString(),Math.max(20,graphWidth-126)),graphLeft,top+39,0xffffe9b8,false);
        String values="L"+status.levels()[selected.ordinal()]+"  XP "+status.xp()[selected.ordinal()]+"  ◆ "+status.points()[selected.ordinal()];
        g.drawString(font,values,graphLeft+graphWidth-font.width(values),top+39,0xffe0c280,false);
        ProfessionMaterials.recess(g,graphLeft-3,graphTop-3,graphWidth+6,graphHeight+6);
        g.enableScissor(graphLeft-1,graphTop-1,graphLeft+graphWidth+1,graphTop+graphHeight+1);
        for (var entry:nodes.entrySet()) {
            ProfessionNode node=entry.getKey(); Box b=entry.getValue();
            ProfessionNode parent=ProfessionCatalog.node(selected,node.parentId());
            Box p=nodes.get(parent);
            if (p!=null) {
                int color=unlocked(node)?0xffa4ae65:0xff66523b;
                g.fill(p.cx(),b.cy()-1,b.cx(),b.cy()+2,0xff110e09);
                g.fill(p.cx(),b.cy(),b.cx(),b.cy()+1,color);
            }
        }
        ProfessionNode hovered=null;
        for (var entry:nodes.entrySet()) {
            ProfessionNode node=entry.getKey(); Box b=entry.getValue();
            boolean active=unlocked(node), ready=ready(node);
            ProfessionMaterials.board(g,b.x,b.y,b.w,b.h,active||node==focused);
            g.fill(b.x+4,b.y+4,b.x+b.w-4,b.y+b.h-4,active?0xff39402a:ready?0xff3b2e1e:0xff211b17);
            g.renderItem(new ItemStack(item(node.icon())),b.cx()-8,b.cy()-8);
            String label=!SandboxProfessionPresentation.available(node)?"—":active?"✓":"L"+node.level();
            if (b.y+b.h+10<graphTop+graphHeight) g.drawCenteredString(font,label,b.cx(),b.y+b.h+2,active?0xffbbd08c:ready?0xffe8c98c:0xff9b907c);
            if (b.contains(mouseX,mouseY)) hovered=node;
        }
        g.disableScissor();
        if (hovered!=null && hovered!=focused) { focused=hovered; detailScroll=0; }
        drawDetails(g);
        super.render(g,mouseX,mouseY,partialTick);
        for (var entry:professions.entrySet()) if (entry.getValue().contains(mouseX,mouseY))
            g.renderTooltip(font,Component.translatable(name(entry.getKey())),mouseX,mouseY);
    }

    private void drawDetails(GuiGraphics g) {
        int y=graphTop+graphHeight+10;
        ProfessionMaterials.recess(g,graphLeft-3,y,graphWidth+6,top+panelHeight-y-6);
        if (focused==null) return;
        g.renderItem(new ItemStack(item(focused.icon())),graphLeft+4,y+5);
        g.enableScissor(graphLeft+24,y+3,graphLeft+graphWidth-4,top+panelHeight-9);
        List<Component> lines=new ArrayList<>();
        lines.add(Component.translatable("jem.professions.node."+focused.id()));
        lines.add(Component.translatable("jem.professions.branch."+focused.branch()).append(" · L"+focused.level()+" · 1 ◆"));
        lines.add(!SandboxProfessionPresentation.available(focused)
                ?text("В этой песочнице нет действующей реализации эффекта.","This sandbox has no active implementation of this effect.")
                :unlocked(focused)?Component.translatable("jem.professions.unlocked")
                : pending?text("Ожидание сервера…","Waiting for server…")
                : !parentUnlocked(focused)?Component.translatable("jem.professions.requires_parent",Component.translatable("jem.professions.node."+focused.parentId()))
                : ready(focused)?text("Нажми навык, чтобы изучить","Click the skill to unlock")
                : Component.translatable("jem.professions.unlock_level",focused.level()));
        int lineY=y+5-detailScroll;
        for (int i=0;i<lines.size();i++) for (var line:font.split(lines.get(i),Math.max(30,graphWidth-34))) {
            g.drawString(font,line,graphLeft+25,lineY,i==0?0xffffe6b9:i==2?0xffb7c794:0xffc5b99f,false);
            lineY+=11;
        }
        g.disableScissor();
    }

    @Override public boolean mouseClicked(double x,double y,int button) {
        if (button!=0) return super.mouseClicked(x,y,button);
        if (x>=left+panelWidth-27 && y>=top+5 && x<left+panelWidth-6 && y<top+32) { onClose(); return true; }
        for (var entry:professions.entrySet()) if (entry.getValue().contains(x,y)) {
            selected=entry.getKey(); lastSelected=selected; focused=null; detailScroll=0; pending=false; buildTree(); return true;
        }
        for (var entry:nodes.entrySet()) if (entry.getValue().contains(x,y)) {
            focused=entry.getKey(); detailScroll=0;
            if (!pending && ready(focused)) { pending=true; ProfessionsNetwork.unlock(selected,focused.id()); }
            return true;
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        if (x>=graphLeft && y>graphTop+graphHeight) { detailScroll=Math.max(0,Math.min(110,detailScroll-(int)(delta*12))); return true; }
        return super.mouseScrolled(x,y,delta);
    }
    @Override public boolean isPauseScreen() { return false; }
    private boolean unlocked(ProfessionNode node) { return status.unlocked().contains(selected.name()+":"+node.id()); }
    private boolean parentUnlocked(ProfessionNode node) { return node.parentId().isBlank()||status.unlocked().contains(selected.name()+":"+node.parentId()); }
    private boolean ready(ProfessionNode node) { return SandboxProfessionPresentation.available(node)&&status.authenticated()&&!unlocked(node)&&status.points()[selected.ordinal()]>0&&status.levels()[selected.ordinal()]>=node.level()&&parentUnlocked(node); }
    private static Component text(String ru,String en) { return Component.literal(net.minecraft.client.Minecraft.getInstance().getLanguageManager().getSelected().startsWith("ru")?ru:en); }
    private static Item item(String id) { return BuiltInRegistries.ITEM.getOptional(new ResourceLocation(SandboxProfessionPresentation.icon(id))).orElse(Items.BOOK); }
    private static Item icon(ProfessionId id) { return switch(id) {
        case ARCHITECT->Items.BRICKS; case FARMER->Items.WHEAT; case FISHER->Items.FISHING_ROD; case MINER->Items.IRON_PICKAXE;
        case EXPLORER->Items.COMPASS; case ENGINEER->item("create:wrench"); case WARRIOR->Items.IRON_SWORD; case MERCHANT->Items.EMERALD;
        case MAYOR->Items.BELL; case MAGE->item("irons_spellbooks:iron_spell_book");
    }; }
    private static String name(ProfessionId id) { return "jem.professions.name."+id.name().toLowerCase(java.util.Locale.ROOT); }
    private record Box(int x,int y,int w,int h) { int cx(){return x+w/2;} int cy(){return y+h/2;} boolean contains(double px,double py){return px>=x&&px<x+w&&py>=y&&py<y+h;} }
}
