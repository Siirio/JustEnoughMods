package com.siirio.jemprofessions.client;

import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.tree.ProfessionNode;
import com.siirio.jemprofessions.sandbox.SandboxProfessionRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

/** Real action forms: human labels, authoritative selectable rows and bounded typed values. */
public final class SandboxActionScreen extends Screen {
    private final Screen back;
    private final ProfessionId profession;
    private final ProfessionNode skill;
    private CompoundTag state=new CompoundTag();
    private final List<Field> fields=new ArrayList<>();
    private String chosen="",town="";
    private int rowScroll,townIndex,modeIndex,schoolIndex,left,top,w,h,formX,formW,listWidth;
    private int fishIndex;
    private Button modeButton,townButton,schoolButton;
    private boolean customer;
    private final List<String> modes;
    private static final String[] SCHOOLS={"fire","ice","lightning","holy","ender","blood","evocation","nature","eldritch"};
    private static final String[] SCHOOLS_RU={"Огонь","Лёд","Молния","Свет","Край","Кровь","Сотворение","Природа","Тайная магия"};
    private static final Map<String,String> LABELS=Map.ofEntries(
        Map.entry("create","Разместить"),Map.entry("fulfill","Сдать товары"),Map.entry("bid","Сделать ставку"),Map.entry("buy","Купить"),
        Map.entry("cancel","Отменить"),Map.entry("claim","Забрать"),Map.entry("advertise","Показать на доске"),Map.entry("sign","Привязать вывеску"),
        Map.entry("invite","Пригласить"),Map.entry("watch","Следить за запасом"),Map.entry("clear","Очистить выбор"),Map.entry("move","Переселить"),
        Map.entry("tax","Установить налог"),Map.entry("audit","Обновить историю"),Map.entry("reserve","Отложить деньги"),Map.entry("return","Вернуть в казну"),
        Map.entry("spend","Потратить на цель"),Map.entry("select","Выбрать бойца"),Map.entry("rally","Созвать"),Map.entry("add_point","Добавить точку"),
        Map.entry("start","Начать патруль"),Map.entry("captain","Назначить командира"),Map.entry("deliver_food","Отдать еду"),
        Map.entry("first","Первая точка"),Map.entry("save","Сохранить"),Map.entry("write","Записать в предмет"),Map.entry("use","Применить"),
        Map.entry("preview","Предпросмотр"),Map.entry("confirm","Подтвердить"),Map.entry("set","Выбрать"),Map.entry("fuel","Заправить"),
        Map.entry("remove","Убрать теплицу"),Map.entry("bait","Включить / убрать наживку"));

    public SandboxActionScreen(Screen back,ProfessionId profession,ProfessionNode skill){super(SandboxSkillText.name(skill.id()));this.back=back;this.profession=profession;this.skill=skill;this.modes=!SandboxClientState.unlocked(skill.id())&&(profession==ProfessionId.MERCHANT||skill.id().equals("cargo_contract"))?List.of("claim","cancel"):modes(skill.id());}
    public SandboxActionScreen consumer(String listing){customer=true;chosen=listing;return this;}
    public void accept(String node,CompoundTag value){if(skill.id().equals(node)){state=value.copy();updateTown();}}
    @Override protected void init(){
        w=Math.min(710,width-16);h=Math.min(410,height-16);left=(width-w)/2;top=(height-h)/2;
        listWidth=Math.max(96,w/2-14);formX=left+listWidth+20;formW=Math.max(110,w-listWidth-32);fields.clear();
        addRenderableWidget(Button.builder(t("Назад","Back"),b->onClose()).bounds(left+8,top+h-24,68,18).build());
        townButton=addRenderableWidget(Button.builder(t("Город не выбран","No town selected"),b->{townIndex++;updateTown();}).bounds(left+8,top+35,listWidth,18).build());
        townButton.visible=profession==ProfessionId.MAYOR;
        int y=top+51;
        if(profession==ProfessionId.MERCHANT||skill.id().equals("cargo_contract")){
            field("Quantity",t("Количество","Quantity"),"1",64,y);y+=29;
            field("Price",t("Цена, медных монет","Price, copper coins"),"1",1000000,y);y+=29;
            if(!skill.id().equals("trading_post")&&!skill.id().equals("stall_sign")&&!skill.id().equals("market_network")){field("Hours",t("Срок, реальных часов","Duration, real hours"),"1",skill.id().equals("auction")?24:72,y);y+=29;}
        }else if(skill.id().equals("civic_fund")){field("Price",t("Сумма, медных монет","Amount, copper coins"),"1",1000000,y);y+=29;}
        else if(skill.id().equals("supply_notice")){field("Value",t("Номер запаса: 1–3","Supply slot: 1–3"),"1",3,y);y+=29;field("Quantity",t("Предупредить, если меньше","Warn when below"),"16",4096,y);y+=29;}
        else if(skill.id().equals("taxes")){field("Value",t("Налог: 0, 5, 10 или 15%","Tax: 0, 5, 10 or 15%"),"0",15,y);y+=29;}
        else if(skill.id().equals("shaft_builder")||skill.id().equals("track_run")){field("Value",t("Длина линии","Line length"),"8",skill.id().equals("shaft_builder")?16:12,y);y+=29;}
        else if(skill.id().equals("factory_copy")||skill.id().equals("schedule_templates")){field("Value",t("Ячейка шаблона: 1–3","Template slot: 1–3"),"1",3,y);y+=29;}
        if(skill.id().equals("target_fishing")||skill.id().equals("rare_bait")||skill.id().equals("trophy_catch")){
            String[] fish={"Треска","Лосось","Иглобрюх","Тропическая рыба"};
            addRenderableWidget(Button.builder(t("Рыба: "+fish[fishIndex],"Fish: cod"),b->{fishIndex=(fishIndex+1)%4;b.setMessage(t("Рыба: "+fish[fishIndex],"Fish: "+new String[]{"cod","salmon","pufferfish","tropical fish"}[fishIndex]));}).bounds(formX,y,formW,20).build());y+=29;
        }
        if(skill.branch().equals("affinity")){
            schoolButton=addRenderableWidget(Button.builder(Component.empty(),b->{schoolIndex=(schoolIndex+1)%SCHOOLS.length;schoolButton.setMessage(schoolName());}).bounds(formX,y,formW,20).build());
            schoolButton.setMessage(schoolName());y+=29;
        }
        modeButton=addRenderableWidget(Button.builder(Component.empty(),b->{modeIndex=(modeIndex+1)%modes.size();updateMode();}).bounds(formX,top+h-69,formW,20).build());
        addRenderableWidget(Button.builder(t("Выполнить","Execute"),b->execute()).bounds(formX,top+h-44,formW,20).build());
        updateMode();state=SandboxClientState.extra(skill.id());updateTown();
        SandboxProfessionRuntime.request(profession,skill.id(),"view",new CompoundTag());
    }
    private Component schoolName(){return t("Школа: "+SCHOOLS_RU[schoolIndex],"School: "+SCHOOLS[schoolIndex]);}
    private void field(String key,Component label,String initial,int max,int y){
        EditBox box=new EditBox(font,formX,y+11,formW,16,label);box.setMaxLength(7);box.setFilter(s->s.isEmpty()||s.matches("[0-9]{1,7}"));box.setValue(initial);addRenderableWidget(box);fields.add(new Field(key,label,box,max,y));
    }
    private void updateMode(){String mode=customer?"buy":modes.get(modeIndex);String label=LABELS.getOrDefault(mode,"Применить");
        if(mode.equals("start")&&skill.id().equals("ledge_catch"))label="Повиснуть на уступе";
        if(mode.equals("select")&&skill.branch().equals("greenhouse"))label="Выбрать теплицу";
        if(mode.equals("select")&&skill.branch().equals("targeting"))label="Выбрать рыбу";
        modeButton.setMessage(t("Действие: "+label,"Action: "+mode.replace('_',' ')));}
    private void updateTown(){
        if(townButton==null)return;ListTag towns=state.getList("Towns",10);if(towns.isEmpty())return;
        townIndex=Math.floorMod(townIndex,towns.size());CompoundTag row=towns.getCompound(townIndex);town=row.getString("Id");townButton.setMessage(Component.literal(font.plainSubstrByWidth(row.getString("Name"),listWidth-8)));
    }
    private void execute(){
        CompoundTag input=new CompoundTag();input.putInt("Hours",1);input.putString("Mode",customer?"buy":modes.get(modeIndex));input.putString("Id",chosen);input.putString("Town",town);
        for(Field field:fields){try{int value=Integer.parseInt(field.box.getValue());if(value<0||value>field.max)return;if(field.key.equals("Value")&&skill.id().equals("supply_notice"))value--;if(field.key.equals("Price"))input.putLong(field.key,value);else input.putInt(field.key,value);}catch(NumberFormatException invalid){return;}}
        if(skill.branch().equals("affinity"))input.putString("Option",SCHOOLS[schoolIndex]);
        if(skill.id().equals("target_fishing")||skill.id().equals("rare_bait")||skill.id().equals("trophy_catch"))input.putString("Option","minecraft:"+new String[]{"cod","salmon","pufferfish","tropical_fish"}[fishIndex]);
        if(minecraft.player!=null){String item=BuiltInRegistries.ITEM.getKey(minecraft.player.getMainHandItem().getItem()).toString();input.putString("Purpose",item);}
        String mode=input.getString("Mode");
        boolean recovery=(profession==ProfessionId.MERCHANT||skill.id().equals("cargo_contract"))&&(mode.equals("claim")||mode.equals("cancel"));
        SandboxProfessionRuntime.request(profession,skill.id(),customer?"customer":recovery?"recovery":"activate",input);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);g.fill(left,top,left+w,top+h,0xf51a1511);g.drawString(font,title,left+9,top+10,0xffffdfad,false);
        int textY=top+26;g.drawString(font,t("Выбери строку слева, затем действие справа.","Select a row, then choose an action."),left+9,textY,0xffc9bda9,false);
        int listTop=top+(townButton.visible?60:43),listBottom=top+h-31;
        g.fill(left+8,listTop,left+8+listWidth,listBottom,0xff100e0c);g.enableScissor(left+9,listTop+1,left+7+listWidth,listBottom-1);
        ListTag rows=state.getList("Rows",10);
        for(int i=rowScroll;i<rows.size();i++){
            int y=listTop+3+(i-rowScroll)*31;if(y>=listBottom)break;CompoundTag row=rows.getCompound(i);boolean chosenRow=chosen.equals(row.getString("Id"));
            if(chosenRow)g.fill(left+10,y,left+listWidth+5,y+29,0xff40341f);
            g.drawString(font,font.plainSubstrByWidth(row.getString("Name"),listWidth-10),left+13,y+2,0xffffdfad,false);
            g.drawString(font,font.plainSubstrByWidth(row.getString("Detail"),listWidth-10),left+13,y+14,0xffb8b5aa,false);
        }
        if(rows.isEmpty()){int y=listTop+5;for(var line:font.split(t("Здесь появятся настоящие записи. Для заказа держи нужный товар в руке.","Real entries appear here. Hold the requested goods when posting an order."),listWidth-12)){g.drawString(font,line,left+13,y,0xffaaa291,false);y+=11;}}
        g.disableScissor();for(Field field:fields)g.drawString(font,field.label,formX,field.y,0xffe3d9c7,false);
        int y=fields.isEmpty()?top+55:fields.get(fields.size()-1).y+33;
        g.enableScissor(formX,y,formX+formW,top+h-74);
        Component help=help();for(var line:font.split(help,formW)){g.drawString(font,line,formX,y,0xffc7c0ad,false);y+=11;}
        g.disableScissor();super.render(g,mx,my,partial);
    }
    private Component help(){
        return switch(skill.id()){
            case "gear_swap"->t("Смотри на шестерню Create. Держи замену во второй руке.","Aim at a Create cog; hold its replacement in your offhand.");
            case "factory_copy"->t("Первая точка → посмотри на второй угол → Сохранить. Записать: держи пустой чертёж Create.","First corner, aim at the opposite corner, Save. Write requires a real empty Create schematic.");
            case "schedule_templates"->t("Держи настоящее расписание Create: Сохранить или Записать.","Hold a real Create schedule to Save or Write.");
            case "resident_move"->t("Выбери жителя и смотри на подходящую кровать.","Select a resident and aim at a suitable bed.");
            case "civic_fund"->t("Держи предмет цели в руке. Для покупки выбери предложение нужного товара.","Hold the goal item. Select the matching shop offer to spend the reserved funds.");
            case "stall_sign"->t("Выбери свою лавку и смотри на настоящую вывеску.","Select your shop and aim at a real sign.");
            default->SandboxSkillText.field(skill.id(),"effect");
        };
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        int listTop=top+(townButton.visible?60:43);if(button==0&&x>=left+8&&x<left+8+listWidth&&y>=listTop&&y<top+h-31){int i=rowScroll+(int)(y-listTop-3)/31;ListTag rows=state.getList("Rows",10);if(i>=0&&i<rows.size()){CompoundTag row=rows.getCompound(i);chosen=row.getString("Id");if(!row.getString("Town").isBlank())town=row.getString("Town");return true;}}
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta){if(x<formX){rowScroll=Math.max(0,Math.min(Math.max(0,state.getList("Rows",10).size()-1),rowScroll-(int)delta));return true;}return super.mouseScrolled(x,y,delta);}
    @Override public void onClose(){minecraft.setScreen(back);}
    @Override public boolean isPauseScreen(){return false;}
    private static Component t(String ru,String en){return SandboxSkillText.text(ru,en);}
    private static List<String> modes(String node){return switch(node){
        case "buy_orders","commission_escrow","cargo_contract"->List.of("create","fulfill","cancel","claim");
        case "auction"->List.of("create","bid","cancel","claim");case "trading_post"->List.of("create","buy","cancel","claim");
        case "stall_sign"->List.of("create","sign","cancel","claim");case "market_network"->List.of("create","advertise","buy","fulfill","bid","claim","cancel");
        case "invite_residents"->List.of("invite","deliver_food");case "supply_notice"->List.of("watch","clear","deliver_food");
        case "resident_move"->List.of("move");case "taxes"->List.of("tax");case "treasury_audit"->List.of("audit");case "civic_fund"->List.of("reserve","spend","return");
        case "rally"->List.of("select","rally","clear");case "patrol_route"->List.of("select","add_point","start","clear");case "captain_order"->List.of("select","captain","clear");
        case "factory_copy"->List.of("first","save","write");case "schedule_templates"->List.of("save","write");
        case "greenhouse_master","greenhouse_keeper","greenhouse_abundance"->List.of("select","fuel","remove");
        case "target_fishing","rare_bait","trophy_catch"->List.of("select","bait");
        case "portable_camp"->List.of("preview","confirm","cancel");case "ledge_catch"->List.of("start","cancel");
        default->List.of("use");};}
    private record Field(String key,Component label,EditBox box,int max,int y){}
}
