package com.siirio.mobhome.client.shelter;

import com.siirio.mobhome.feature.shelter.ShelterContent;
import com.siirio.mobhome.feature.shelter.ShelterMenu;
import com.siirio.mobhome.feature.shelter.ShelterNetwork;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Compact native parchment + native controls. No private pet-management inventory or client authority. */
@Mod.EventBusSubscriber(modid = "mobhome", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ShelterScreen extends AbstractContainerScreen<ShelterMenu> {
    private static final ResourceLocation PANEL = new ResourceLocation("minecraft", "textures/gui/demo_background.png");
    private static final int ROWS = 3, ROW_HEIGHT = 24, LIST_TOP = 43;
    private List<ShelterMenu.Row> rows = List.of(), filtered = List.of();
    private UUID selected;
    private int scroll, revision;
    private boolean occupied;
    private EditBox search;
    private Button bind, unbind;

    public ShelterScreen(ShelterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 248; imageHeight = 166;
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) { event.enqueueWork(() -> MenuScreens.register(ShelterContent.MENU.get(), ShelterScreen::new)); }
    public static void receive(ShelterNetwork.Snapshot snapshot) {
        if (Minecraft.getInstance().screen instanceof ShelterScreen screen && screen.menu.containerId == snapshot.menu() && screen.menu.nonce.equals(snapshot.nonce())) {
            screen.rows = snapshot.rows(); screen.revision = snapshot.revision(); screen.occupied = snapshot.occupied(); screen.filter();
        }
    }
    @Override protected void init() {
        super.init();
        search = addRenderableWidget(new EditBox(font, leftPos + 10, topPos + 23, 228, 16, Component.translatable("mobhome.shelter.search")));
        search.setHint(Component.translatable("mobhome.shelter.search")); search.setMaxLength(64); search.setResponder(ignored -> { scroll = 0; filter(); });
        bind = addRenderableWidget(Button.builder(Component.translatable("mobhome.shelter.bind"), ignored -> send(true)).bounds(leftPos + 10, topPos + 135, 110, 20).build());
        unbind = addRenderableWidget(Button.builder(Component.translatable("mobhome.shelter.unbind"), ignored -> send(false)).bounds(leftPos + 128, topPos + 135, 110, 20).build());
        filter();
    }
    private void filter() {
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
        filtered = rows.stream().filter(row -> (display(row).getString() + " " + species(row).getString()).toLowerCase(Locale.ROOT).contains(query)).toList();
        scroll = Math.min(scroll, Math.max(0, filtered.size() - ROWS)); updateButtons();
    }
    private ShelterMenu.Row selected() { return rows.stream().filter(row -> row.id().equals(selected)).findFirst().orElse(null); }
    private boolean has(ShelterMenu.Row row, int flag) { return row != null && (row.flags() & flag) != 0; }
    private void updateButtons() {
        if (bind == null || unbind == null) return;
        ShelterMenu.Row row = selected();
        bind.active = row != null && has(row, ShelterMenu.SUPPORTED) && has(row, ShelterMenu.BIND_READY) && !has(row, ShelterMenu.HERE) && !occupied;
        unbind.active = has(row, ShelterMenu.HERE);
        String reason = row == null ? "select_pet" : !has(row, ShelterMenu.SUPPORTED) ? "unsupported_reason" : has(row, ShelterMenu.HERE) ? "already_bound" : occupied ? "occupied_reason" : "bind_help";
        bind.setTooltip(Tooltip.create(row != null && !has(row, ShelterMenu.BIND_READY) && !row.blocked().isEmpty()
                ? Component.translatable("mobhome.shelter.blocked." + row.blocked()) : Component.translatable("mobhome.shelter." + reason)));
        unbind.setTooltip(Tooltip.create(Component.translatable("mobhome.shelter." + (unbind.active ? "unbind_help" : "not_bound_here"))));
    }
    private void send(boolean bindAction) {
        if (selected == null) return;
        ShelterNetwork.action(new ShelterNetwork.Action(menu.containerId, menu.nonce, revision, selected, bindAction));
        bind.active = false; unbind.active = false;
    }
    private Component species(ShelterMenu.Row row) {
        ResourceLocation id = ResourceLocation.tryParse(row.species());
        return id == null ? Component.literal(row.species()) : BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(type -> type.getDescription()).orElse(Component.literal(row.species()));
    }
    private Component display(ShelterMenu.Row row) { return row.name().isEmpty() ? species(row) : Component.literal(row.name()); }
    private Component state(ShelterMenu.Row row) {
        if (!has(row, ShelterMenu.SUPPORTED)) return Component.translatable("mobhome.shelter.unsupported");
        if (!row.blocked().isEmpty()) return Component.translatable("mobhome.shelter.blocked." + row.blocked());
        if (has(row, ShelterMenu.PENDING)) return Component.translatable("mobhome.shelter." + (has(row, ShelterMenu.HERE) || has(row, ShelterMenu.ELSEWHERE) ? "pending" : "needs_home"));
        String key = has(row, ShelterMenu.HERE) ? "bound_here" : has(row, ShelterMenu.ELSEWHERE) ? "bound_elsewhere" : "unbound";
        Component base = Component.translatable("mobhome.shelter." + key);
        return has(row, ShelterMenu.UNLOADED) ? Component.translatable("mobhome.shelter.unloaded_state", base) : base;
    }
    private ItemStack icon(ShelterMenu.Row row) {
        ResourceLocation id = ResourceLocation.tryParse(row.species());
        if (id != null) {
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            SpawnEggItem egg = type == null ? null : SpawnEggItem.byId(type);
            if (egg != null) return new ItemStack(egg);
        }
        return new ItemStack(Items.LEAD);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(PANEL, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, font.plainSubstrByWidth(title.getString(), 225), 10, 9, 0x40352A, false);
        if (filtered.isEmpty()) graphics.drawWordWrap(font, Component.translatable(rows.isEmpty() ? "mobhome.shelter.discovery_help" : "mobhome.shelter.no_match"), 12, 58, 222, 0x51493E);
        for (int i = 0; i < ROWS && scroll + i < filtered.size(); i++) {
            ShelterMenu.Row row = filtered.get(scroll + i); int y = LIST_TOP + i * ROW_HEIGHT;
            graphics.renderItem(icon(row), 14, y + 3);
            if (row.id().equals(selected)) graphics.drawString(font, ">", 6, y + 6, 0x50412F, false);
            graphics.drawString(font, font.plainSubstrByWidth(display(row).getString(), 194), 36, y + 1, 0x302820, false);
            graphics.drawString(font, font.plainSubstrByWidth(state(row).getString(), 194), 36, y + 12, 0x655849, false);
        }
        ShelterMenu.Row occupant = rows.stream().filter(row -> has(row, ShelterMenu.HERE)).findFirst().orElse(null);
        Component assigned = occupant == null ? Component.translatable("mobhome.shelter.empty") : Component.translatable("mobhome.shelter.assigned", display(occupant));
        graphics.drawString(font, font.plainSubstrByWidth(assigned.getString(), 226), 10, 119, 0x40352A, false);
        if (filtered.size() > ROWS) graphics.drawString(font, (scroll + 1) + "/" + filtered.size(), 210, 9, 0x655849, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick);
        int index = rowAt(mouseX, mouseY);
        if (index >= 0) {
            ShelterMenu.Row row = filtered.get(index);
            graphics.renderComponentTooltip(font, List.of(display(row), species(row), state(row), Component.translatable("mobhome.shelter.discovery_note")), mouseX, mouseY);
        }
    }
    private int rowAt(double x, double y) {
        if (x < leftPos + 7 || x >= leftPos + 238 || y < topPos + LIST_TOP || y >= topPos + LIST_TOP + ROWS * ROW_HEIGHT) return -1;
        int row = scroll + (int) (y - topPos - LIST_TOP) / ROW_HEIGHT;
        return row < filtered.size() ? row : -1;
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        int row = rowAt(x, y);
        if (button == 0 && row >= 0) { selected = filtered.get(row).id(); updateButtons(); return true; }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (x >= leftPos && x < leftPos + imageWidth && y >= topPos + LIST_TOP && y < topPos + 116) {
            scroll = Math.max(0, Math.min(Math.max(0, filtered.size() - ROWS), scroll - (int) Math.signum(delta))); return true;
        }
        return super.mouseScrolled(x, y, delta);
    }
}
