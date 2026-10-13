package com.siirio.jemsettlements.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class TownDashboardScreen extends Screen {
    private static final List<String> TABS = List.of("overview", "people", "supplies", "buildings", "treasury", "workforce", "defense", "projects");
    private final CompoundTag view;
    private String tab = "overview";
    private EditBox treasuryAmount;
    private Button treasuryDeposit;
    private Button treasuryWithdraw;
    public TownDashboardScreen(CompoundTag view) { super(Component.literal(view.getString("Name"))); this.view = view.copy(); }
    @Override protected void init() {
        int widthPer = 86, total = widthPer * 4, start = (width - total) / 2;
        for (int i = 0; i < TABS.size(); i++) {
            String id = TABS.get(i); int row = i / 4, col = i % 4;
            addRenderableWidget(Button.builder(Component.translatable("jem.settlements.tab." + id), button -> { tab = id; updateTreasuryControls(); })
                    .bounds(start + col * widthPer, 34 + row * 22, widthPer - 2, 20).build());
        }
        treasuryAmount = addRenderableWidget(new EditBox(font, width / 2 - 108, 132, 216, 20, Component.translatable("jem.settlements.treasury.amount")));
        treasuryAmount.setHint(Component.translatable("jem.settlements.treasury.amount"));
        treasuryAmount.setFilter(value -> value.isEmpty() || value.length() <= 10 && value.chars().allMatch(Character::isDigit));
        treasuryDeposit = addRenderableWidget(Button.builder(Component.translatable("jem.settlements.treasury.deposit"), button -> treasury(true))
                .bounds(width / 2 - 108, 158, 106, 20).build());
        treasuryWithdraw = addRenderableWidget(Button.builder(Component.translatable("jem.settlements.treasury.withdraw"), button -> treasury(false))
                .bounds(width / 2 + 2, 158, 106, 20).build());
        updateTreasuryControls();
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); int x = width / 2 - 172, y = 86;
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFE3BB);
        graphics.fill(x, y, x + 344, height - 28, 0xE82C211B); graphics.fill(x, y, x + 3, height - 28, 0xFFC27A3B);
        int line = y + 12;
        if (tab.equals("overview")) {
            draw(graphics, x, line, "jem.settlements.view.happiness", view.getInt("Happiness")); line += 14;
            draw(graphics, x, line, "jem.settlements.view.center", BlockPosText()); line += 14;
            draw(graphics, x, line, "jem.settlements.view.mode", view.getInt("LoadedStorage") > 0 ? Component.translatable("jem.settlements.mode.active") : Component.translatable("jem.settlements.mode.waiting"));
            line += 18; draw(graphics, x, line, "jem.settlements.view.market_demand", Component.translatable("jem.economy.demand." + view.getString("MarketPrimary")), Component.translatable("jem.economy.demand." + view.getString("MarketSecondary"))); line += 14;
            draw(graphics, x, line, "jem.settlements.view.market_limits", view.getLong("MarketAccountRemaining"), view.getLong("MarketGlobalRemaining"));
        } else if (tab.equals("supplies")) {
            draw(graphics, x, line, "jem.settlements.view.storage", view.getInt("LoadedStorage"), view.getInt("StorageLinks")); line += 16;
            draw(graphics, x, line, "jem.settlements.view.food", view.getLong("FoodUnits")); line += 18;
            ListTag stock = view.getList("Stock", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(7, stock.size()); i++) {
                CompoundTag row = stock.getCompound(i); ResourceLocation id = ResourceLocation.tryParse(row.getString("Item"));
                if (id == null) continue; ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
                graphics.renderItem(stack, x + 14, line - 4); graphics.drawString(font, stack.getHoverName().copy().append(" ×" + row.getLong("Count")), x + 36, line, 0xFFE0CEB2, false); line += 20;
            }
        } else if (tab.equals("workforce") || tab.equals("people") || tab.equals("defense")) {
            CompoundTag workforce = view.getCompound("Workforce");
            for (String key : workforce.getAllKeys()) {
                if (tab.equals("defense") && !key.equals("RECRUIT")) continue;
                graphics.drawString(font, Component.translatable("jem.settlements.worker." + key.toLowerCase()).append(" ×" + workforce.getInt(key)), x + 14, line, 0xFFE0CEB2, false); line += 14;
            }
            if (line == y + 12) graphics.drawString(font, Component.translatable("jem.settlements.view.no_workers"), x + 14, line, 0xFFAA9984, false);
        } else if (tab.equals("treasury")) {
            draw(graphics, x, line, "jem.settlements.view.treasury", view.getLong("Treasury")); line += 18;
            draw(graphics, x, line, "jem.settlements.view.tax", view.getInt("Tax")); line += 76;
            graphics.drawWordWrap(font, Component.translatable("jem.settlements.view.treasury_help"), x + 14, line, 316, 0xFFC5B399);
        } else if (tab.equals("buildings")) {
            graphics.drawWordWrap(font, Component.translatable("jem.settlements.view.buildings_help"), x + 14, line, 316, 0xFFC5B399);
        } else if (tab.equals("projects")) {
            graphics.drawWordWrap(font, Component.translatable("jem.settlements.view.projects_help"), x + 14, line, 316, 0xFFC5B399);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    private void draw(GuiGraphics graphics, int x, int y, String key, Object... values) { graphics.drawString(font, Component.translatable(key, values), x + 14, y, 0xFFE0CEB2, false); }
    private String BlockPosText() { var pos = net.minecraft.core.BlockPos.of(view.getLong("Center")); return pos.getX() + ", " + pos.getY() + ", " + pos.getZ(); }
    private void treasury(boolean deposit) {
        try {
            long amount = Long.parseLong(treasuryAmount.getValue());
            if (amount > 0L && amount <= 1_000_000_000L) com.siirio.jemsettlements.network.SettlementNetwork.treasury(deposit, amount);
        } catch (NumberFormatException ignored) { }
    }
    private void updateTreasuryControls() {
        boolean visible = tab.equals("treasury");
        if (treasuryAmount != null) treasuryAmount.visible = visible;
        if (treasuryDeposit != null) treasuryDeposit.visible = visible;
        if (treasuryWithdraw != null) treasuryWithdraw.visible = visible;
    }
}
