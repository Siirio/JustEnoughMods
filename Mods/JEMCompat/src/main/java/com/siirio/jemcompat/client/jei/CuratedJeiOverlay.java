package com.siirio.jemcompat.client.jei;

import com.siirio.jemcompat.JEMCompat;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = JEMCompat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CuratedJeiOverlay {
    private static final int CELL = 20;
    private static final int BUTTON = 22;
    private static final int MARGIN = 8;
    private static final int SEARCH_HEIGHT = 18;
    private static final int NAVIGATION_HEIGHT = 24;
    private static CuratedJeiCatalog.Category selected = CuratedJeiCatalog.Category.WEAPONS;
    private static CuratedJeiCatalog.Category building = CuratedJeiCatalog.Category.FURNITURE;
    private static int page;
    private static Screen owner;
    private static EditBox globalSearch;
    private static EditBox categorySearch;
    private static ResourceLocation highlighted;
    private static long highlightUntil;
    private static int lastMouseX;
    private static int lastMouseY;

    private CuratedJeiOverlay() {
    }

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || JEMJeiPlugin.runtime() == null) return;
        owner = screen;
        Layout layout = layout(screen);
        globalSearch = searchBox(layout.searchX, layout.top, layout.searchWidth, "gui.jemcompat.jei.search_all");
        categorySearch = searchBox(layout.searchX, layout.top + SEARCH_HEIGHT + 3, layout.searchWidth, "gui.jemcompat.jei.search_category");
    }

    @SubscribeEvent
    public static void closing(ScreenEvent.Closing event) {
        if (event.getScreen() == owner) {
            owner = null;
            globalSearch = null;
            categorySearch = null;
        }
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || JEMJeiPlugin.runtime() == null) return;
        ensureSearch(screen);
        Layout layout = layout(screen);
        lastMouseX = event.getMouseX();
        lastMouseY = event.getMouseY();
        GuiGraphics graphics = event.getGuiGraphics();
        renderNativeToggle(graphics, layout, event.getMouseX(), event.getMouseY());
        if (JEMJeiPlugin.isNativeOverlayVisible()) return;
        renderCategories(graphics, layout, event.getMouseX(), event.getMouseY());
        positionSearch(layout);
        globalSearch.render(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
        categorySearch.render(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
        renderGrid(graphics, layout, event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || JEMJeiPlugin.runtime() == null) return;
        ensureSearch(screen);
        Layout layout = layout(screen);
        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();
        if (inside(mouseX, mouseY, layout.toggleX, layout.top, BUTTON, BUTTON)) {
            JEMJeiPlugin.setNativeOverlayVisible(!JEMJeiPlugin.isNativeOverlayVisible());
            event.setCanceled(true);
            return;
        }
        if (JEMJeiPlugin.isNativeOverlayVisible()) return;
        positionSearch(layout);
        if (globalSearch.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())
                || categorySearch.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
            return;
        }
        globalSearch.setFocused(false);
        categorySearch.setFocused(false);
        List<CuratedJeiCatalog.Category> categories = categories();
        for (int index = 0; index < categories.size(); index++) {
            if (inside(mouseX, mouseY, layout.railX, layout.top + index * BUTTON, BUTTON, BUTTON)) {
                select(categories.get(index));
                event.setCanceled(true);
                return;
            }
        }
        if (selected == CuratedJeiCatalog.Category.BUILDING) {
            List<CuratedJeiCatalog.Category> children = CuratedJeiCatalog.building();
            for (int index = 0; index < children.size(); index++) {
                if (inside(mouseX, mouseY, layout.subRailX, layout.top + index * BUTTON, BUTTON, BUTTON)) {
                    building = children.get(index);
                    categorySearch.setValue("");
                    page = 0;
                    event.setCanceled(true);
                    return;
                }
            }
        }
        if (lastPage(layout) > 0) {
            int navigationY = navigationY(layout);
            if (inside(mouseX, mouseY, layout.gridX, navigationY, BUTTON, BUTTON)) {
                movePage(layout,-1);
                event.setCanceled(true);
                return;
            }
            int nextX = layout.gridX + layout.columns * CELL - BUTTON;
            if (inside(mouseX, mouseY, nextX, navigationY, BUTTON, BUTTON)) {
                movePage(layout,1);
                event.setCanceled(true);
                return;
            }
        }
        if (!inside(mouseX, mouseY, layout.gridX, layout.contentTop, layout.columns * CELL, layout.rows * CELL)) return;
        int column = (mouseX - layout.gridX) / CELL;
        int row = (mouseY - layout.contentTop) / CELL;
        int stackIndex = page * layout.capacity() + row * layout.columns + column;
        List<ItemStack> stacks = visibleStacks();
        if (stackIndex >= stacks.size()) return;
        ItemStack stack = stacks.get(stackIndex);
        if (!globalSearch.getValue().isBlank()) {
            openSearchResult(stack, layout);
            event.setCanceled(true);
            return;
        }
        RecipeIngredientRole role = event.getButton() == 1 ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
        var focus = JEMJeiPlugin.runtime().getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack);
        JEMJeiPlugin.runtime().getRecipesGui().show(focus);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() != owner || JEMJeiPlugin.runtime() == null || JEMJeiPlugin.isNativeOverlayVisible()) return;
        boolean handled = globalSearch != null && globalSearch.isFocused()
                && globalSearch.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
        handled |= categorySearch != null && categorySearch.isFocused()
                && categorySearch.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
        if (handled) {
            page = 0;
            event.setCanceled(true);
            return;
        }
        if (!(owner instanceof AbstractContainerScreen<?> screen)) return;
        ItemStack hovered = hoveredStack(layout(screen), lastMouseX, lastMouseY);
        if (hovered.isEmpty()) return;
        InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
        var mappings = JEMJeiPlugin.runtime().getKeyMappings();
        RecipeIngredientRole role = mappings.getShowRecipe().isActiveAndMatches(key) ? RecipeIngredientRole.OUTPUT
                : mappings.getShowUses().isActiveAndMatches(key) ? RecipeIngredientRole.INPUT : null;
        if (role == null) return;
        var focus = JEMJeiPlugin.runtime().getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, hovered);
        JEMJeiPlugin.runtime().getRecipesGui().show(focus);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void characterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (event.getScreen() != owner || JEMJeiPlugin.isNativeOverlayVisible()) return;
        boolean handled = globalSearch != null && globalSearch.isFocused()
                && globalSearch.charTyped(event.getCodePoint(), event.getModifiers());
        handled |= categorySearch != null && categorySearch.isFocused()
                && categorySearch.charTyped(event.getCodePoint(), event.getModifiers());
        if (handled) {
            page = 0;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || JEMJeiPlugin.runtime() == null
                || JEMJeiPlugin.isNativeOverlayVisible()) return;
        Layout layout = layout(screen);
        if (!inside((int) event.getMouseX(), (int) event.getMouseY(), layout.gridX, layout.contentTop, layout.columns * CELL, layout.rows * CELL)) return;
        if(event.getScrollDelta()==0) return;
        movePage(layout,event.getScrollDelta() < 0 ? 1 : -1);
        event.setCanceled(true);
    }

    private static EditBox searchBox(int x, int y, int width, String hint) {
        EditBox box = new EditBox(Minecraft.getInstance().font, x, y, width, SEARCH_HEIGHT, Component.translatable(hint));
        box.setHint(Component.translatable(hint));
        box.setMaxLength(80);
        box.setResponder(value -> page = 0);
        return box;
    }

    private static void ensureSearch(AbstractContainerScreen<?> screen) {
        if (owner != screen || globalSearch == null || categorySearch == null) {
            owner = screen;
            Layout layout = layout(screen);
            globalSearch = searchBox(layout.searchX, layout.top, layout.searchWidth, "gui.jemcompat.jei.search_all");
            categorySearch = searchBox(layout.searchX, layout.top + SEARCH_HEIGHT + 3, layout.searchWidth, "gui.jemcompat.jei.search_category");
        }
    }

    private static void positionSearch(Layout layout) {
        globalSearch.setX(layout.searchX);
        globalSearch.setY(layout.top);
        globalSearch.setWidth(layout.searchWidth);
        categorySearch.setX(layout.searchX);
        categorySearch.setY(layout.top + SEARCH_HEIGHT + 3);
        categorySearch.setWidth(layout.searchWidth);
    }

    private static void renderNativeToggle(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        renderButton(graphics, new ItemStack(Items.COMPASS), layout.toggleX, layout.top, JEMJeiPlugin.isNativeOverlayVisible());
        renderLabel(graphics, "gui.jemcompat.jei.vanilla", layout.toggleX, layout.top, mouseX, mouseY);
    }

    private static void renderCategories(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        List<CuratedJeiCatalog.Category> categories = categories();
        for (int index = 0; index < categories.size(); index++) {
            CuratedJeiCatalog.Category category = categories.get(index);
            renderButton(graphics, category.icon(), layout.railX, layout.top + index * BUTTON, category == selected);
            renderLabel(graphics, category.translationKey(), layout.railX, layout.top + index * BUTTON, mouseX, mouseY);
        }
        if (selected == CuratedJeiCatalog.Category.BUILDING) {
            List<CuratedJeiCatalog.Category> children = CuratedJeiCatalog.building();
            for (int index = 0; index < children.size(); index++) {
                CuratedJeiCatalog.Category category = children.get(index);
                renderButton(graphics, category.icon(), layout.subRailX, layout.top + index * BUTTON, category == building);
                renderLabel(graphics, category.translationKey(), layout.subRailX, layout.top + index * BUTTON, mouseX, mouseY);
            }
        }
    }

    private static void renderGrid(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        int width = layout.columns * CELL;
        int height = layout.rows * CELL;
        graphics.fill(layout.gridX - 3, layout.contentTop - 3, layout.gridX + width + 3, layout.contentTop + height + 3, 0xB0101010);
        List<ItemStack> stacks = visibleStacks();
        page = Math.min(page, Math.max(0, (stacks.size() - 1) / layout.capacity()));
        int start = page * layout.capacity();
        int end = Math.min(stacks.size(), start + layout.capacity());
        for (int index = start; index < end; index++) {
            int visible = index - start;
            int x = layout.gridX + visible % layout.columns * CELL;
            int y = layout.contentTop + visible / layout.columns * CELL;
            ItemStack stack = stacks.get(index);
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (inside(mouseX, mouseY, x, y, CELL, CELL)) graphics.fill(x, y, x + CELL, y + CELL, 0x80FFFFFF);
            if (id != null && id.equals(highlighted) && System.currentTimeMillis() < highlightUntil
                    && System.currentTimeMillis() / 180 % 2 == 0) graphics.fill(x, y, x + CELL, y + CELL, 0xC0FF7A00);
            graphics.renderItem(stack, x + 2, y + 2);
            if (inside(mouseX, mouseY, x, y, CELL, CELL)) graphics.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
        }
        renderNavigation(graphics, layout, mouseX, mouseY, stacks.size());
    }

    private static void renderNavigation(GuiGraphics graphics, Layout layout, int mouseX, int mouseY, int stackCount) {
        int pages = Math.max(1, (stackCount + layout.capacity() - 1) / layout.capacity());
        int y = navigationY(layout);
        int width = layout.columns * CELL;
        graphics.drawCenteredString(Minecraft.getInstance().font,
                Component.translatable("gui.jemcompat.jei.page", page + 1, pages), layout.gridX + width / 2, y + 7, 0xFFFFFF);
        if (pages <= 1) return;
        renderButton(graphics, new ItemStack(Items.ARROW), layout.gridX, y, true);
        int nextX = layout.gridX + width - BUTTON;
        renderButton(graphics, new ItemStack(Items.ARROW), nextX, y, true);
        graphics.drawCenteredString(Minecraft.getInstance().font, "<", layout.gridX + BUTTON / 2, y + 7, 0xFFFFFF);
        graphics.drawCenteredString(Minecraft.getInstance().font, ">", nextX + BUTTON / 2, y + 7, 0xFFFFFF);
        if (inside(mouseX, mouseY, layout.gridX, y, BUTTON, BUTTON))
            graphics.renderTooltip(Minecraft.getInstance().font, Component.translatable("gui.jemcompat.jei.previous_page"), mouseX, mouseY);
        if (inside(mouseX, mouseY, nextX, y, BUTTON, BUTTON))
            graphics.renderTooltip(Minecraft.getInstance().font, Component.translatable("gui.jemcompat.jei.next_page"), mouseX, mouseY);
    }

    private static int navigationY(Layout layout) {
        return layout.contentTop + layout.rows * CELL + 4;
    }

    private static int lastPage(Layout layout) {
        return Math.max(0, (visibleStacks().size() - 1) / layout.capacity());
    }

    private static void movePage(Layout layout,int delta) {
        page=Math.floorMod(page+delta,lastPage(layout)+1);
    }

    private static List<ItemStack> visibleStacks() {
        String global = globalSearch == null ? "" : globalSearch.getValue().trim();
        List<ItemStack> source = global.isEmpty() ? activeStacks() : CuratedJeiCatalog.allStacks();
        String query = global.isEmpty() && categorySearch != null ? categorySearch.getValue().trim() : global;
        if (query.isEmpty()) return source;
        String needle = query.toLowerCase(Locale.ROOT);
        return source.stream().filter(stack -> matches(stack, needle)).toList();
    }

    private static boolean matches(ItemStack stack, String needle) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle)
                || id != null && id.toString().toLowerCase(Locale.ROOT).contains(needle);
    }

    private static ItemStack hoveredStack(Layout layout, int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY, layout.gridX, layout.contentTop, layout.columns * CELL, layout.rows * CELL)) return ItemStack.EMPTY;
        int index = page * layout.capacity() + (mouseY - layout.contentTop) / CELL * layout.columns + (mouseX - layout.gridX) / CELL;
        List<ItemStack> stacks = visibleStacks();
        return index < stacks.size() ? stacks.get(index) : ItemStack.EMPTY;
    }

    private static void openSearchResult(ItemStack stack, Layout layout) {
        CuratedJeiCatalog.Category category = CuratedJeiCatalog.category(stack);
        if (CuratedJeiCatalog.building().contains(category)) {
            selected = CuratedJeiCatalog.Category.BUILDING;
            building = category;
        } else selected = category;
        globalSearch.setValue("");
        categorySearch.setValue("");
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        List<ItemStack> stacks = activeStacks();
        int index = 0;
        for (; index < stacks.size(); index++) if (ItemStack.isSameItemSameTags(stacks.get(index), stack)) break;
        page = index >= stacks.size() ? 0 : index / layout.capacity();
        highlighted = id;
        highlightUntil = System.currentTimeMillis() + 3000;
    }

    private static void renderButton(GuiGraphics graphics, ItemStack icon, int x, int y, boolean selectedButton) {
        graphics.fill(x, y, x + BUTTON, y + BUTTON, selectedButton ? 0xE07A4C24 : 0xC0181818);
        graphics.fill(x + 1, y + 1, x + BUTTON - 1, y + BUTTON - 1, selectedButton ? 0xE0B06A32 : 0xC0383838);
        graphics.renderItem(icon, x + 3, y + 3);
    }

    private static void renderLabel(GuiGraphics graphics, String key, int x, int y, int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, x, y, BUTTON, BUTTON)) graphics.renderTooltip(Minecraft.getInstance().font, Component.translatable(key), mouseX, mouseY);
    }

    private static List<CuratedJeiCatalog.Category> categories() {
        var player = Minecraft.getInstance().player;
        return CuratedJeiCatalog.topLevel(player != null && player.getAbilities().instabuild);
    }

    private static List<ItemStack> activeStacks() {
        return CuratedJeiCatalog.stacks(selected == CuratedJeiCatalog.Category.BUILDING ? building : selected);
    }

    private static void select(CuratedJeiCatalog.Category category) {
        if (selected != category) {
            selected = category;
            categorySearch.setValue("");
            page = 0;
        }
    }

    private static Layout layout(AbstractContainerScreen<?> screen) {
        int railX = screen.width - BUTTON - MARGIN;
        int subRailX = railX - BUTTON - 4;
        int top = MARGIN;
        int right = selected == CuratedJeiCatalog.Category.BUILDING ? subRailX - MARGIN : railX - MARGIN;
        int gridX = screen.getGuiLeft() + screen.getXSize() + MARGIN;
        int toggleX = gridX;
        int searchX = toggleX + BUTTON + 4;
        int available = Math.max(CELL * 4, right - gridX);
        int columns = Math.max(4, available / CELL);
        int searchWidth = Math.max(60, right - searchX);
        int contentTop = top + SEARCH_HEIGHT * 2 + 8;
        int rows = Math.max(4, Math.min(12, (screen.height - contentTop - MARGIN - NAVIGATION_HEIGHT) / CELL));
        return new Layout(gridX, railX, subRailX, top, contentTop, columns, rows, toggleX, searchX, searchWidth);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Layout(int gridX, int railX, int subRailX, int top, int contentTop, int columns, int rows,
                          int toggleX, int searchX, int searchWidth) {
        private int capacity() {
            return columns * rows;
        }
    }
}
