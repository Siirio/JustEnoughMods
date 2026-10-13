package com.siirio.jemsettlements.client;

import com.siirio.jemeconomy.physical.CoinFormatter;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class TownDashboardScreen extends Screen {
    private static final List<String> TABS = List.of("overview", "residents", "supplies", "happiness", "workforce", "defense", "treasury", "projects", "simulation", "guide");
    private final CompoundTag view; private String tab = "overview"; private EditBox treasuryAmount; private Button treasuryDeposit; private Button treasuryWithdraw;
    public TownDashboardScreen(CompoundTag view) { super(Component.literal(view.getString("Name"))); this.view = view.copy(); }
    @Override protected void init() {
        int widthPer = 72, total = widthPer * 5, start = (width - total) / 2;
        for (int i = 0; i < TABS.size(); i++) { String id = TABS.get(i); int row = i / 5, col = i % 5; addRenderableWidget(Button.builder(Component.translatable("jem.settlements.tab." + id), button -> { tab = id; updateTreasuryControls(); }).bounds(start + col * widthPer, 32 + row * 22, widthPer - 2, 20).build()); }
        treasuryAmount = addRenderableWidget(new EditBox(font, width / 2 - 108, 146, 216, 20, Component.translatable("jem.settlements.treasury.amount"))); treasuryAmount.setHint(Component.translatable("jem.settlements.treasury.amount")); treasuryAmount.setFilter(value -> value.isEmpty() || value.length() <= 10 && value.chars().allMatch(Character::isDigit));
        treasuryDeposit = addRenderableWidget(Button.builder(Component.translatable("jem.settlements.treasury.deposit"), button -> treasury(true)).bounds(width / 2 - 108, 172, 106, 20).build());
        treasuryWithdraw = addRenderableWidget(Button.builder(Component.translatable("jem.settlements.treasury.withdraw"), button -> treasury(false)).bounds(width / 2 + 2, 172, 106, 20).build()); updateTreasuryControls();
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); int x = width / 2 - 184, y = 82; graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFE3BB); graphics.fill(x, y, x + 368, height - 20, 0xE82C211B); graphics.fill(x, y, x + 3, height - 20, 0xFFC27A3B); int line = y + 10;
        switch (tab) {
            case "overview" -> { draw(graphics,x,line,"jem.settlements.view.tier", tr("jem.settlements.tier."+view.getString("Tier").toLowerCase())); line+=14; draw(graphics,x,line,"jem.settlements.view.specialization",tr("jem.settlements.specialization."+view.getString("PrimarySpecialization").toLowerCase())); line+=14; draw(graphics,x,line,"jem.settlements.view.happiness",view.getInt("Happiness")); line+=14; draw(graphics,x,line,"jem.settlements.view.population",view.getList("Residents",Tag.TAG_COMPOUND).size()); line+=14; draw(graphics,x,line,"jem.settlements.view.treasury_component",CoinFormatter.format(view.getLong("Treasury"))); line+=14; draw(graphics,x,line,"jem.settlements.view.next_payroll",ticks(view.getLong("NextPayroll"))); line+=14; draw(graphics,x,line,"jem.settlements.view.mode",view.getBoolean("SimulationActive")?tr("jem.settlements.mode.active"):tr("jem.settlements.mode.background")); }
            case "residents" -> { ListTag rows=view.getList("Residents",Tag.TAG_COMPOUND); for(int i=0;i<Math.min(7,rows.size());i++){CompoundTag row=rows.getCompound(i); graphics.drawString(font,Component.literal(row.getString("Name")).append(Component.translatable("jem.settlements.view.resident_row",row.getInt("Mood"),row.getInt("Satiety"),row.getInt("HomeAmenities"),row.getInt("WorkAmenities"))),x+14,line,0xFFE0CEB2,false);line+=18;} if(rows.isEmpty())graphics.drawWordWrap(font,tr("jem.settlements.view.no_residents"),x+14,line,340,0xFFAA9984); }
            case "supplies" -> { draw(graphics,x,line,"jem.settlements.view.storage",view.getInt("LoadedStorage"),view.getInt("StorageLinks"));line+=14;draw(graphics,x,line,"jem.settlements.view.food",view.getLong("FoodUnits"));line+=14;draw(graphics,x,line,"jem.settlements.view.pending",view.getLong("PendingNotSpendable"),view.getLong("FailedQuarantined"));line+=18;ListTag stock=view.getList("Stock",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(5,stock.size());i++){CompoundTag row=stock.getCompound(i);ResourceLocation id=ResourceLocation.tryParse(row.getString("Item"));if(id==null)continue;ItemStack stack=new ItemStack(BuiltInRegistries.ITEM.get(id));graphics.renderItem(stack,x+14,line-4);graphics.drawString(font,stack.getHoverName().copy().append(" ×"+row.getLong("Count")),x+36,line,0xFFE0CEB2,false);line+=20;} }
            case "happiness" -> { for(String key:List.of("Housing","Food","Safety","Jobs","Community","Decoration")){draw(graphics,x,line,"jem.settlements.view.category."+key.toLowerCase(),view.getInt(key));line+=14;} line+=4;graphics.drawWordWrap(font,tr("jem.settlements.view.happiness_help"),x+14,line,340,0xFFC5B399); }
            case "workforce" -> { CompoundTag workforce=view.getCompound("Workforce");for(String key:workforce.getAllKeys()){graphics.drawString(font,tr("jem.settlements.worker."+key.toLowerCase()).copy().append(" ×"+workforce.getInt(key)),x+14,line,0xFFE0CEB2,false);line+=14;}if(workforce.isEmpty())graphics.drawString(font,tr("jem.settlements.view.no_workers"),x+14,line,0xFFAA9984,false); }
            case "defense" -> { draw(graphics,x,line,"jem.settlements.view.payroll_total",CoinFormatter.format(view.getLong("ProjectedPayroll")));line+=14;draw(graphics,x,line,"jem.settlements.view.payroll_due",CoinFormatter.format(view.getLong("UnpaidPayroll")),CoinFormatter.format(view.getLong("PayrollShortfall")));line+=18;ListTag rows=view.getList("Defense",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(6,rows.size());i++){CompoundTag row=rows.getCompound(i);graphics.drawString(font,Component.literal(row.getString("Name")+" · ").append(Component.translatable("jem.settlements.view.defense_row",row.getString("NativeType"),CoinFormatter.format(row.getLong("Wage")),tr(row.getBoolean("Ready")?"jem.settlements.ready":"jem.settlements.not_ready"))),x+14,line,0xFFE0CEB2,false);line+=18;} }
            case "treasury" -> { draw(graphics,x,line,"jem.settlements.view.treasury_component",CoinFormatter.format(view.getLong("Treasury")));line+=18;draw(graphics,x,line,"jem.settlements.view.tax",view.getInt("Tax"));line+=104;graphics.drawWordWrap(font,tr("jem.settlements.view.treasury_help"),x+14,line,340,0xFFC5B399); }
            case "projects" -> graphics.drawWordWrap(font,tr("jem.settlements.view.projects_phase2"),x+14,line,340,0xFFC5B399);
            case "simulation" -> { ListTag rows=view.getList("SimulationJournal",Tag.TAG_COMPOUND);for(int i=Math.max(0,rows.size()-6);i<rows.size();i++){CompoundTag row=rows.getCompound(i);graphics.drawWordWrap(font,Component.translatable("jem.settlements.view.simulation_row",row.getInt("SimulatedDays"),row.getInt("SkippedDays"),row.getString("Result")),x+14,line,340,0xFFE0CEB2);line+=30;}if(rows.isEmpty())graphics.drawWordWrap(font,tr("jem.settlements.view.no_simulation"),x+14,line,340,0xFFAA9984); }
            case "guide" -> graphics.drawWordWrap(font,tr("jem.settlements.view.guide"),x+14,line,340,0xFFC5B399);
        }
        super.render(graphics,mouseX,mouseY,partialTick);
    }
    private Component tr(String key,Object...values){return Component.translatable(key,values);} private void draw(GuiGraphics graphics,int x,int y,String key,Object...values){graphics.drawString(font,tr(key,values),x+14,y,0xFFE0CEB2,false);} private Component ticks(long due){long now=Minecraft.getInstance().level==null?0L:Minecraft.getInstance().level.getGameTime();return due<=0?tr("jem.settlements.payroll.unassigned"):tr("jem.settlements.payroll.ticks",Math.max(0L,due-now));}
    private void treasury(boolean deposit){try{long amount=Long.parseLong(treasuryAmount.getValue());if(amount>0L&&amount<=1_000_000_000L)com.siirio.jemsettlements.network.SettlementNetwork.treasury(deposit,amount);}catch(NumberFormatException ignored){}}
    private void updateTreasuryControls(){boolean visible=tab.equals("treasury");if(treasuryAmount!=null)treasuryAmount.visible=visible;if(treasuryDeposit!=null)treasuryDeposit.visible=visible;if(treasuryWithdraw!=null)treasuryWithdraw.visible=visible;}
}
