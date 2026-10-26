package com.siirio.jemguisandbox;

import com.siirio.jemguisandbox.mirror.jemserver.client.smp.*;
import com.siirio.jemguisandbox.mirror.jemserver.client.ui.JemMenuScreen;
import com.siirio.jemguisandbox.mirror.jemserver.smp.*;
import com.siirio.jemguisandbox.mirror.jemserver.smp.events.EventPrompt;
import com.siirio.jemguisandbox.mirror.jemsettlements.client.*;
import com.siirio.jemguisandbox.mirror.jemeconomy.api.*;
import com.siirio.jemguisandbox.mirror.jemeconomy.client.MarketScreen;
import com.siirio.jemguisandbox.mirror.jemeconomy.network.MarketActionPacket;
import com.siirio.jemguisandbox.mirror.jemeconomy.policy.ItemPolicy.MarketCategory;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Ephemeral view state only. No world writes, real inventories, commands or packets. */
public final class Demo {
    private static final UUID TOWN=id("town"), PARTY=id("party"), RESULT=id("result");
    private static final Map<String,List<CompoundTag>> TABLES=new LinkedHashMap<>();
    private static CompoundTag town;
    private static SmpQuery destination=new SmpQuery("events","","",0,null);
    private static long wallet=12345, revision=1;
    private static boolean ready;
    private static int selectedRace;
    private static final Set<UUID> claimed=new HashSet<>();
    private static final Map<UUID,String> eventStates=new HashMap<>();
    private static final Map<UUID,String> partyStates=new HashMap<>();
    private static final ArrayDeque<Runnable> replies=new ArrayDeque<>();

    /** Preserve the native request/response boundary, including chat command closure. */
    public static void defer(Runnable reply) { replies.addLast(reply); }
    public static void drainReplies() {
        int count=Math.min(64,replies.size());
        for(int i=0;i<count&&!replies.isEmpty();i++)replies.removeFirst().run();
    }

    public static UUID id(String value) { return UUID.nameUUIDFromBytes(("jem-gui-demo:"+value).getBytes(StandardCharsets.UTF_8)); }
    public static UUID viewer() { var p=Minecraft.getInstance().player; return p==null?id("viewer"):p.getUUID(); }
    public static CompoundTag tag(Object... pairs) {
        var t=new CompoundTag();
        for(int i=0;i<pairs.length;i+=2) {
            String k=(String)pairs[i];Object v=pairs[i+1];
            if(v instanceof String s)t.putString(k,s);else if(v instanceof Integer n)t.putInt(k,n);
            else if(v instanceof Long n)t.putLong(k,n);else if(v instanceof Double n)t.putDouble(k,n);
            else if(v instanceof Float n)t.putFloat(k,n);else if(v instanceof Boolean b)t.putBoolean(k,b);
            else if(v instanceof UUID u)t.putUUID(k,u);else if(v instanceof Tag n)t.put(k,n);
            else throw new IllegalArgumentException("Unsupported view value: "+k);
        }
        return t;
    }
    public static ListTag list(Tag... values) { var list=new ListTag(); Collections.addAll(list,values);return list; }
    public static ListTag strings(String... values) { var l=new ListTag();for(String s:values)l.add(StringTag.valueOf(s));return l; }
    public static CompoundTag stack(Item item,int count) { return new ItemStack(item,count).save(new CompoundTag()); }
    public static void reset() {
        TABLES.clear();claimed.clear();eventStates.clear();partyStates.clear();replies.clear();wallet=12345;revision=1;ready=false;selectedRace=0;
        var players=new ArrayList<CompoundTag>();
        for(int i=0;i<18;i++) players.add(tag("id",i==0?viewer():id("player"+i),"name",i==0?Minecraft.getInstance().getUser().getName():"Sandbox player "+i,
                "online",i%3!=0,"race",new String[]{"ancestors","born_in_bloom","infected_by_sculk","void_walkers","nether_hearteds"}[i%5],"playTicks",96000L,
                "mobsKilled",156L+i,"recruitCount",3L,"settlement","Oakbridge","settlementTier","village",
                "actions",strings("message","invite","home"),"professions",new ListTag(),"eventStats",list(tag("type","BLOOD_MOON","count",4L))));
        TABLES.put("profiles",players);
        var events=new ArrayList<CompoundTag>();
        String[] types={"BLOOD_MOON","BOSS","BOSS_RAID","RESOURCE_RUSH","FISHING","COOKING_SHOW"};
        for(int i=0;i<types.length;i++)events.add(tag("id",id("event"+i),"type",types[i],"activity",types[i],"title","Sandbox "+types[i],
                "titleKey","jem.smp."+types[i],"state",i==0?"ACTIVE":"SCHEDULED","lifecycle","READY","bossType","minecraft:ravager",
                "dimension","minecraft:overworld","position",new BlockPos(128,64,96).asLong(),"meaningfulLocation",true,
                "planned",System.currentTimeMillis()+120000L,"ends",System.currentTimeMillis()+1200000L,"duration",24000L,
                "participants",3,"maxPlayers",8,"revision",1,"rules",list(tag("key","jem.event.group_hint","args",strings())),
                "reward",list(tag("item","minecraft:diamond","count",3,"minCount",1,"maxCount",5)),
                "actions",strings("join","arrive","leave","create_party"),"organizerName","Sandbox host"));
        TABLES.put("events",events);
        var parties=new ArrayList<CompoundTag>();
        for(int i=0;i<6;i++) {
            var members=new CompoundTag();
            for(int m=0;m<3;m++) {var player=players.get(m);members.put(player.getUUID("id").toString(),tag("id",player.getUUID("id"),"name",player.getString("name"),"online",true,"ready",m!=0,"accepted",true,"canRemove",m!=0,"canApprove",true,"canTransfer",true));}
            parties.add(tag("id",i==0?PARTY:id("party"+i),"owner",viewer(),"name","Sandbox party "+(i+1),"title","Sandbox party "+(i+1),
                    "activity",i==0?"BOSS_RAID":"BLOOD_MOON","state","PREPARING","revision",1,"leader",true,"canStart",true,
                    "description","Presentation demo. Actions affect this screen only.","language","en","slots",8,"members",members,
                    "bossType","minecraft:ravager","organizerName",players.get(0).getString("name"),"meaningfulLocation",true,
                    "dimension","minecraft:overworld","position",new BlockPos(128,64,96).asLong(),
                    "reward",list(tag("item","minecraft:diamond","count",3)),"inviteCandidates",new ListTag(),
                    "actions",strings("join","arrive","ready","leave","dissolve","start","cancel","invite")));
        }
        TABLES.put("parties",parties);
        var prizes=new ArrayList<CompoundTag>();
        for(int i=0;i<5;i++)prizes.add(tag("id",id("prize"+i),"titleKey","jem.smp.BOSS_RAID","createdAt",System.currentTimeMillis(),
                "items",list(stack(Items.DIAMOND,3),stack(Items.EMERALD,24),stack(Items.GOLD_INGOT,8)),"groups",new ListTag()));
        TABLES.put("prizes",prizes);
        town=makeTown();
    }
    private static void ensure() { if(town==null)reset(); }
    public static void open(String route) {
        ensure();
        var mc=Minecraft.getInstance();
        if(route.equals("hub")){mc.setScreen(new GalleryScreen());return;}
        if(route.equals("reset")){reset();mc.setScreen(new GalleryScreen());return;}
        if(route.equals("settlements")){mc.setScreen(new SettlementScreen(settlementView()));return;}
        if(route.equals("settlement-directory")){var v=settlementView();v.remove("Selected");mc.setScreen(new SettlementScreen(v));return;}
        if(route.equals("settlement-guide")){mc.setScreen(new SettlementScreen(settlementView()));((SettlementScreen)mc.screen).guide();return;}
        if(route.equals("founding")){var v=settlementView();v.putBoolean("Founding",true);v.remove("Selected");mc.setScreen(new SettlementScreen(v));return;}
        if(route.equals("market")){market(null,"","blocks",0);return;}
        if(route.equals("inventory")){if(mc.player!=null)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));return;}
        if(route.equals("races")||route.equals("race-progression")) {
            mc.setScreen(new com.siirio.jemguisandbox.mirror.jemvisualsandbox.client.races.RaceExperienceScreen(raceStatus(),false,route.equals("race-progression")));return;
        }
        if(route.equals("empty-events")){destination=new SmpQuery("events","","no-demo-results",0,null);mc.setScreen(new SmpScreen(new SmpUpdate(view(destination),true,null,"")));return;}
        if(route.equals("result")||route.equals("defeat")){mc.setScreen(new ResultScreen(result(route.equals("result"))));return;}
        if(route.equals("encounter")||route.equals("busy-arena")){
            EventEntryScreen.open(new EventPrompt(id("encounter"),"BOSS_STRUCTURE","Sandbox boss encounter","minecraft:ravager",true,
                    route.equals("busy-arena")?"ACTIVE":"FREE",route.equals("busy-arena")?List.of("Sandbox party"):List.of()));return;
        }
        if(Set.of("death","deaths","bosses","landmarks","homes").contains(route)){menu(route.equals("death")?"deaths":route);return;}
        String tab=route.equals("smp")?"events":Set.of("profile","players").contains(route)?"profiles":route;
        destination=new SmpQuery(tab,"","",0,route.equals("profile")?viewer():null);
        mc.setScreen(new SmpScreen(new SmpUpdate(view(destination),true,null,"")));
    }
    public static boolean openExternal(String route) {
        if(Set.of("market","settlements","roles","professions").contains(route)){
            if(route.equals("roles")){open("races");return true;}
            if(route.equals("professions")) { notice("Professions is absent from test jem.");return true; }
            open(route);return true;
        }
        return false;
    }
    public static void query(SmpQuery q) {
        if(q.tab().isBlank())return;
        if(q.tab().equals("settlements")){defer(()->open("settlements"));return;}
        destination=q;
        var origin=Minecraft.getInstance().screen;
        defer(()->{if(Minecraft.getInstance().screen==origin&&origin instanceof SmpScreen s)s.accept(new SmpUpdate(view(q),true,null,""));});
    }
    private static CompoundTag view(SmpQuery q) {
        ensure();
        var v=tag("tab",q.tab(),"filter",q.filter(),"querySearch",q.search(),"page",q.page(),"viewer",viewer(),"coins",true,
                "features",tag("economy",true,"settlements",true,"events",true,"prizes",true,"roles",true,"professions",false),
                "message","", "canCreate",true);
        var rows=new ListTag();
        var all=TABLES.getOrDefault(q.tab(),List.of());
        String search=q.search().toLowerCase(Locale.ROOT);
        var filtered=all.stream().filter(r->!claimed.contains(r.hasUUID("id")?r.getUUID("id"):id("none")))
                .filter(r->search.isBlank()||(r.getString("name")+r.getString("title")).toLowerCase(Locale.ROOT).contains(search)).toList();
        int page=Math.min(Math.max(0,q.page()),Math.max(0,(filtered.size()-1)/8));
        for(var row:filtered.subList(Math.min(filtered.size(),page*8),Math.min(filtered.size(),page*8+8)))rows.add(project(row));
        v.put("rows",rows);v.putInt("page",page);v.putInt("pages",Math.max(1,(filtered.size()+7)/8));v.putInt("total",filtered.size());v.putInt("totalItems",filtered.size()*3);
        if(q.selected()!=null) {
            var selected=all.stream().filter(r->q.selected()==null||r.getUUID("id").equals(q.selected())).findFirst();
            selected.ifPresent(row->v.put("detail",project(row)));
        }
        return v;
    }
    private static CompoundTag project(CompoundTag original) {
        var row=original.copy();
        if(row.hasUUID("id")) {
            UUID id=row.getUUID("id");
            if(eventStates.containsKey(id))row.putString("state",eventStates.get(id));
            if(partyStates.containsKey(id))row.putString("state",partyStates.get(id));
            if(TABLES.get("parties").stream().anyMatch(p->p.getUUID("id").equals(id))) {
                row.getCompound("members").getCompound(viewer().toString()).putBoolean("ready",ready);
                var candidates=new ListTag();for(var p:TABLES.get("profiles"))candidates.add(p.copy());row.put("inviteCandidates",candidates);
            }
        }
        return row;
    }
    public static void action(SmpAction a) {
        defer(()->{
            String key=a.kind().key();
            if(key.equals("ready"))ready=!ready;
            if(key.equals("claim")&&a.id()!=null)claimed.add(a.id());
            if(key.equals("claim_all"))for(var prize:TABLES.get("prizes"))claimed.add(prize.getUUID("id"));
            if(key.equals("start")&&a.id()!=null)partyStates.put(a.id(),"ACTIVE");
            if(key.equals("cancel")&&a.id()!=null)partyStates.put(a.id(),"PREPARING");
            if(key.equals("create")){openParty(a.fields().has("activity")?a.fields().get("activity").getAsString():"BOSS_RAID",false);return;}
            if(key.equals("create_party")||key.equals("solo")) {
                var event=TABLES.get("events").stream().filter(row->row.getUUID("id").equals(a.id())).findFirst();
                if(event.isPresent()){openParty(event.get().getString("activity"),key.equals("solo"));return;}
            }
            var v=view(destination);notice("Demo: "+key);
            if(Minecraft.getInstance().screen instanceof SmpScreen s)s.accept(new SmpUpdate(v,false,a.request(),""));
        });
    }
    public static void openParty(String mode,boolean solo) {
        ensure();TABLES.get("parties").get(0).putString("activity",mode);
        destination=new SmpQuery("parties","","",0,PARTY);
        defer(()->Minecraft.getInstance().setScreen(new SmpScreen(new SmpUpdate(view(destination),true,null,""))));
    }
    private static CompoundTag makeTown() {
        var residents=new ListTag();
        for(int i=0;i<12;i++)residents.add(tag("Id",id("resident"+i),"Name","Resident "+(i+1),"Type","villager","Profession",i%2==0?"farmer":"librarian",
                "Living",true,"Resident",true,"Loaded",true,"Child",false,"Mood",72+i,"Satiety",64+i,"HasBed",true,"Roofed",true,
                "HasHome",true,"HomePos",new BlockPos(i*3,64,8).asLong(),"Position",new BlockPos(i*2,64,4).asLong(),"Dimension","minecraft:overworld",
                "Health",20f,"MaxHealth",20f,"RequestedDish","minecraft:bread","EmploymentReason","working"));
        var defense=list(tag("Id",id("recruit"),"Name","Sandbox guard","Type","RECRUIT","Loyalty","LOYAL","Loaded",true,"Living",true,
                "Position",new BlockPos(12,64,12).asLong(),"Dimension","minecraft:overworld","Wage",12L,"NextPayroll",24000L,"Health",20f,"MaxHealth",20f,"Mood",80,"Satiety",80));
        var storage=tag("Number",1,"Position",new BlockPos(4,64,4).asLong(),"Loaded",true,"InventoryKnown",true,"SlotsTotal",27,"SlotOffset",0,
                "MealOrders",3,"Name","Food storage","Slots",list(tag("Slot",0,"Stack",stack(Items.BREAD,64)),tag("Slot",1,"Stack",stack(Items.CARROT,32)),tag("Slot",2,"Stack",stack(Items.APPLE,16))));
        var stock=list(tag("Item","minecraft:bread","Count",128L,"Demand",12,"StockKnown",true,"Classified",true,"Mood",8,"Satiety",24),tag("Item","minecraft:carrot","Count",64L,"Demand",8,"StockKnown",true,"Classified",true,"Mood",4,"Satiety",12));
        return tag("Id",TOWN,"Name","Oakbridge","Dimension","minecraft:overworld","Center",BlockPos.ZERO.above(64).asLong(),
                "Owner",viewer(),"Owned",true,"SameDimension",true,"CoinCurrency",true,"Residents",residents,"Defense",defense,"WorkforceRows",new ListTag(),
                "Population",12,"Happiness",78,"Food",84,"Housing",90,"Safety",82,"Jobs",75,"Community",80,"Decoration",68,"UnhappyThreshold",30,
                "Treasury",12345L,"Tax",5,"LastDailyTax",120L,"LastResidentTax",10L,"ProjectedPayroll",144L,"UnpaidPayroll",24L,
                "TerritoryMinX",-16,"TerritoryMaxX",16,"TerritoryMinZ",-16,"TerritoryMaxZ",16,"TerritoryArea",1089L,
                "ExpansionCostPerBlock",2L,"ExpansionMinimumCost",10L,"PublicTerritories",new ListTag(),"FoodUnits",256L,
                "Storage",list(storage),"Storages",list(storage.copy()),"Chests",list(storage.copy()),"Stock",stock,"FoodStock",stock.copy(),
                "SelectedStorage",storage.copy(),"LoadedStorage",1,"StorageLinks",1,"FoodOffset",0,"FoodTotal",2,"FoodCatalogue",stock.copy(),
                "Metrics",tag("RequiredResidents",16,"HousingExact",90.0,"FoodExact",84.0,"SafetyExact",82.0,"JobsExact",75.0,"CommunityExact",80.0,"ComfortExact",68.0,
                    "ComfortCategories",tag("Path",18.0,"Animals",16.0,"Lighting",20.0,"Greenery",14.0)));
    }
    public static CompoundTag settlementView() {
        ensure();return tag("Selected",town.copy(),"Directory",list(tag("Id",TOWN,"Name",town.getString("Name"),"Dimension","minecraft:overworld","Residents",12,"Happiness",78)),
                "DirectoryTotal",1,"DirectoryOffset",0,"CoinCurrency",true,"GameTime",24000L,"Bell",new BlockPos(0,64,0).asLong(),
                "FoundingDimension","minecraft:overworld","FoundingCost",100L,"InitialRadius",16,"Result","ACCEPTED");
    }
    public static void settlementAction(String action,UUID selected,CompoundTag input) {
        var payload=input.copy();
        defer(()->{
            ensure();
            if(action.equals("close"))return;
            if(action.equals("rename")||action.equals("create")) {String name=payload.getString("Name");if(!name.isBlank())town.putString("Name",name);}
            if(action.equals("tax"))town.putInt("Tax",payload.getInt("Tax"));
            if(action.equals("payroll"))town.putLong("UnpaidPayroll",0);
            if((action.equals("deposit")||action.equals("withdraw"))&&payload.getLong("Amount")>0) {
                long amount=Math.min(1000000L,payload.getLong("Amount"));
                town.putLong("Treasury",Math.max(0,town.getLong("Treasury")+(action.equals("deposit")?amount:-amount)));
            }
            if(action.equals("remove")&&payload.hasUUID("Entity")) {
                for(String group:List.of("Residents","Defense"))town.getList(group,Tag.TAG_COMPOUND).removeIf(r->((CompoundTag)r).getUUID("Id").equals(payload.getUUID("Entity")));
            }
            var v=settlementView();
            if(action.equals("delete")){v.remove("Selected");v.put("Directory",new ListTag());v.putInt("DirectoryTotal",0);}
            if(action.equals("directory")||action.equals("open"))v.remove("Selected");
            if(Minecraft.getInstance().screen instanceof SettlementScreen s)s.accept(v,action);
            else if(Minecraft.getInstance().screen instanceof CityInspectorScreen)open("settlements");
            notice("Demo: "+action);
        });
    }
    public static void market(Screen parent,String search,String category,int page) {
        ensure();Minecraft.getInstance().setScreen(new MarketScreen(parent,catalog(search,category,page)));
    }
    private static MarketCatalogSnapshot catalog(String search,String category,int page) {
        String base=category.split("_")[0]; if(base.startsWith("building"))base="building";
        MarketCategory kind;try{kind=MarketCategory.valueOf(base.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException e){kind=MarketCategory.BLOCKS;}
        Item[] goods=switch(kind){case FOOD->new Item[]{Items.BREAD,Items.APPLE,Items.CARROT,Items.COOKED_BEEF,Items.POTATO,Items.PUMPKIN_PIE};
            case PLANTS->new Item[]{Items.OAK_SAPLING,Items.DANDELION,Items.POPPY,Items.WHEAT_SEEDS,Items.CACTUS,Items.BAMBOO};
            case RESOURCES->new Item[]{Items.IRON_INGOT,Items.GOLD_INGOT,Items.DIAMOND,Items.COAL,Items.REDSTONE,Items.COPPER_INGOT};
            case DROPS->new Item[]{Items.BONE,Items.STRING,Items.GUNPOWDER,Items.LEATHER,Items.FEATHER,Items.SLIME_BALL};
            default->new Item[]{Items.OAK_LOG,Items.STONE,Items.BRICKS,Items.GLASS,Items.SPRUCE_PLANKS,Items.COBBLESTONE,Items.LANTERN,Items.CHEST};};
        var items=new ArrayList<MarketItemSnapshot>();
        for(int i=0;i<goods.length;i++)if(search.isBlank()||new ItemStack(goods[i]).getHoverName().getString().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
            items.add(new MarketItemSnapshot(BuiltInRegistries.ITEM.getKey(goods[i]),new ItemStack(goods[i]),10L*(i+1),5L*(i+1),kind,256,4096,true,true));
        return new MarketCatalogSnapshot(revision,0,1,search,category,wallet,new int[]{64,32,16,8,2},24000L,items,"");
    }
    public static void marketQuery(String search,String category,int page) {
        defer(()->{if(Minecraft.getInstance().screen instanceof MarketScreen s)s.accept(catalog(search,category,page));});
    }
    public static void marketAction(MarketActionPacket.Action action,ResourceLocation item,int count,String search,String category,int page) {
        revision++;marketQuery(search,category,page);notice("Demo: "+action+" "+count+" x "+item);
    }
    public static CompoundTag result(boolean success) {
        ensure();var rows=new ListTag();
        for(int i=0;i<6;i++)rows.add(tag("id",i==0?viewer():id("player"+i),"name","Sandbox player "+(i+1),"damage",800.0-i*80,"kills",15-i,"revives",i%2,"waves",6));
        return tag("id",RESULT,"success",success,"blood",false,"title","Sandbox raid","rows",rows,
                "bundles",claimed.contains(RESULT)?new ListTag():list(tag("titleKey","jem.smp.BOSS_RAID","items",list(stack(Items.DIAMOND,3),stack(Items.GOLD_INGOT,12)))));
    }
    public static void claimResult() {
        claimed.add(RESULT);if(Minecraft.getInstance().screen instanceof ResultScreen s)s.update(result(true));notice("Demo rewards claimed; real inventory unchanged");
    }
    public static void notice(String message) {
        Minecraft.getInstance().getToasts().addToast(new net.minecraft.client.gui.components.toasts.SystemToast(
                net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,Component.literal("JEM GUI Sandbox"),Component.literal(message)));
    }
    public static void itemPreview(ItemStack item) { Minecraft.getInstance().setScreen(new PreviewScreen(Minecraft.getInstance().screen,item)); }
    public static void mapPreview(ResourceLocation dim,BlockPos pos) { Minecraft.getInstance().setScreen(new PreviewScreen(Minecraft.getInstance().screen,dim,pos)); }
    public static void menu(String kind) {
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        var inventory=new net.minecraft.world.entity.player.Inventory(mc.player);
        var container=new SimpleContainer(54);
        for(int i=0;i<12;i++) {
            var item=new ItemStack(kind.equals("deaths")?Items.RECOVERY_COMPASS:kind.equals("bosses")?Items.DRAGON_HEAD:Items.MAP);
            item.setHoverName(Component.literal(kind.equals("deaths")?"Death "+(i+1):kind+" "+(i+1)));
            var n=item.getOrCreateTag();n.putString("jem_ui_role","entry");n.putBoolean("jem_ui_action",true);
            n.putString("jem_ui_dimension","minecraft:overworld");n.putLong("jem_ui_position",new BlockPos(128+i*16,64,96).asLong());
            n.putString("jem_ui_kind",kind);n.putString("jem_ui_title",kind.equals("deaths")?"Death history":kind);
            n.put("display",tag("Name",Component.Serializer.toJson(item.getHoverName()),"Lore",strings(Component.Serializer.toJson(Component.literal("Overworld | 128, 64, 96")),Component.Serializer.toJson(Component.literal("Demo destination")))));
            container.setItem(i,item);
        }
        var menu=new ChestMenu(MenuType.GENERIC_9x6,0,inventory,container,6);
        mc.setScreen(new JemMenuScreen(menu,inventory,Component.literal(kind.equals("deaths")?"Death history":kind),kind));
    }
    public static void menuAction(AbstractContainerMenu menu,int slot) {
        if(slot<0){Minecraft.getInstance().setScreen(null);return;}
        if(slot>=menu.slots.size())return;
        var item=menu.getSlot(slot).getItem();
        if(item.isEmpty())return;
        var parent=Minecraft.getInstance().screen;
        Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(accepted->{
            Minecraft.getInstance().setScreen(parent);if(accepted)notice("Demo travel selected; player position unchanged");
        },item.getHoverName(),Component.literal("Preview this destination? No teleport or payment is performed.")));
    }
    public static com.siirio.jemguisandbox.mirror.jemvisualsandbox.workbench.RaceExperience.Status raceStatus() {
        return new com.siirio.jemguisandbox.mirror.jemvisualsandbox.workbench.RaceExperience.Status(selectedRace,true,new int[]{3,3,3,3,3},new int[]{30,30,30,30,30},0,0,true,3,0,true,UUID.randomUUID(),false);
    }
    public static void selectRace(int race) {
        selectedRace=Math.max(0,Math.min(4,race));
        defer(()->{if(Minecraft.getInstance().screen instanceof com.siirio.jemguisandbox.mirror.jemvisualsandbox.client.races.RaceExperienceScreen s)s.receive(raceStatus());});
    }
    private Demo() { }
}
