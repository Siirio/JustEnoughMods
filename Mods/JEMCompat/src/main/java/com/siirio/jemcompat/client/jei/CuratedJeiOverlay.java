package com.siirio.jemcompat.client.jei;

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
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class CuratedJeiOverlay {
    private static final int CELL = 20;
    private static final int BUTTON = 22;
    private static final int MARGIN = 8;
    private static final int SEARCH_HEIGHT = 18;
    private static final int NAVIGATION_HEIGHT = 24;
    private static CatalogCategory selected = CatalogCategory.WEAPONS;
    private static CatalogCategory building = CatalogCategory.BUILDING_BLOCKS;
    private static int page;
    private static int categoryRailOffset;
    private static int buildingRailOffset;
    private static Screen owner;
    private static EditBox categorySearch;
    private static int lastMouseX;
    private static int lastMouseY;

    private CuratedJeiOverlay() {
    }

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || JEMJeiPlugin.runtime() == null) return;
        owner = screen;
        CatalogLayout layout = layout(screen);
        categorySearch = searchBox(layout.searchX(), layout.searchY(), layout.searchWidth(), "gui.jemcompat.jei.search_category");
    }

    @SubscribeEvent
    public static void closing(ScreenEvent.Closing event) {
        if (event.getScreen() == owner) {
            clear();
        }
    }

    static void clear() {
        owner = null;
        categorySearch = null;
    }

    static boolean hasSearchFocus() {
        return owner == Minecraft.getInstance().screen && categorySearch != null && categorySearch.isFocused()
                && !JEMJeiPlugin.isNativeOverlayVisible();
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || JEMJeiPlugin.runtime() == null) return;
        ensureSearch(screen);
        CatalogLayout layout = layout(screen);
        lastMouseX = event.getMouseX();
        lastMouseY = event.getMouseY();
        GuiGraphics graphics = event.getGuiGraphics();
        renderNativeToggle(graphics, layout, event.getMouseX(), event.getMouseY());
        if (JEMJeiPlugin.isNativeOverlayVisible()) return;
        renderCategories(graphics, layout, event.getMouseX(), event.getMouseY());
        positionSearch(layout);
        categorySearch.render(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
        renderGrid(graphics, layout, event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || JEMJeiPlugin.runtime() == null) return;
        ensureSearch(screen);
        CatalogLayout layout = layout(screen);
        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();
        if (inside(mouseX, mouseY, layout.toggleX(), layout.top(), BUTTON, BUTTON)) {
            JEMJeiPlugin.setNativeOverlayVisible(!JEMJeiPlugin.isNativeOverlayVisible());
            event.setCanceled(true);
            return;
        }
        if (JEMJeiPlugin.isNativeOverlayVisible()) return;
        positionSearch(layout);
        if (categorySearch.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
            return;
        }
        categorySearch.setFocused(false);
        List<CatalogCategory> categories = categories();
        int categoryStart = railStart(categories, layout, false);
        for (int index = categoryStart; index < Math.min(categories.size(), categoryStart + layout.railSlots()); index++) {
            if (inside(mouseX, mouseY, layout.railX(), layout.top() + (index - categoryStart) * BUTTON, BUTTON, BUTTON)) {
                select(categories.get(index));
                event.setCanceled(true);
                return;
            }
        }
        if (selected == CatalogCategory.BUILDING) {
            List<CatalogCategory> children = CuratedJeiCatalog.building();
            int childStart = railStart(children, layout, true);
            for (int index = childStart; index < Math.min(children.size(), childStart + layout.railSlots()); index++) {
                if (inside(mouseX, mouseY, layout.subRailX(), layout.top() + (index - childStart) * BUTTON, BUTTON, BUTTON)) {
                    building = children.get(index);
                    categorySearch.setValue("");
                    page = 0;
                    event.setCanceled(true);
                    return;
                }
            }
        }
        if (lastPage(layout) > 0) {
            int navigationY = layout.navigationY();
            int navigationButtonWidth = navigationButtonWidth(layout);
            if (inside(mouseX, mouseY, layout.gridX(), navigationY, navigationButtonWidth, BUTTON)) {
                movePage(layout,-1);
                event.setCanceled(true);
                return;
            }
            int nextX = layout.gridX() + layout.columns() * CELL - navigationButtonWidth;
            if (inside(mouseX, mouseY, nextX, navigationY, navigationButtonWidth, BUTTON)) {
                movePage(layout,1);
                event.setCanceled(true);
                return;
            }
        }
        if (!inside(mouseX, mouseY, layout.gridX(), layout.contentTop(), layout.columns() * CELL, layout.rows() * CELL)) return;
        int column = (mouseX - layout.gridX()) / CELL;
        int row = (mouseY - layout.contentTop()) / CELL;
        int stackIndex = page * layout.capacity() + row * layout.columns() + column;
        List<ItemStack> stacks = visibleStacks();
        if (stackIndex >= stacks.size()) return;
        ItemStack stack = stacks.get(stackIndex);
        RecipeIngredientRole role = event.getButton() == 1 ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
        showItemHelp(stack, role);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() != owner || JEMJeiPlugin.runtime() == null || JEMJeiPlugin.isNativeOverlayVisible()) return;
        if (categorySearch != null && categorySearch.isFocused()) {
            if (event.getKeyCode() == 256) {
                categorySearch.setFocused(false);
            } else {
                categorySearch.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
            }
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
        showItemHelp(hovered, role);
        event.setCanceled(true);
    }

    private static void showItemHelp(ItemStack stack, RecipeIngredientRole role) {
        var runtime = JEMJeiPlugin.runtime();
        var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack);
        boolean hasNativeRoute = runtime.getRecipeManager().createRecipeCategoryLookup().limitFocus(List.of(focus)).get().findAny().isPresent();
        if (!hasNativeRoute && net.minecraftforge.fml.ModList.get().isLoaded("jem_advancements")
                && net.minecraftforge.fml.ModList.get().isLoaded("patchouli")) {
            var entry = com.justenoughmods.achievementguide.client.handbook.HandbookNavigation.entryFor(stack);
            if (entry != null && com.justenoughmods.achievementguide.client.handbook.HandbookNavigation.openEntry(entry)) return;
        }
        runtime.getRecipesGui().show(focus);
    }

    @SubscribeEvent
    public static void characterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (event.getScreen() != owner || JEMJeiPlugin.isNativeOverlayVisible()) return;
        if (categorySearch != null && categorySearch.isFocused()) {
            categorySearch.charTyped(event.getCodePoint(), event.getModifiers());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || JEMJeiPlugin.runtime() == null
                || JEMJeiPlugin.isNativeOverlayVisible()) return;
        CatalogLayout layout = layout(screen);
        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();
        int railHeight = layout.railSlots() * BUTTON;
        if (inside(mouseX, mouseY, layout.railX(), layout.top(), BUTTON, railHeight)) {
            if (event.getScrollDelta() != 0) {
                categoryRailOffset = scrollRail(categoryRailOffset, categories().size(), layout,
                        event.getScrollDelta() < 0 ? 1 : -1);
                event.setCanceled(true);
            }
            return;
        }
        if (selected == CatalogCategory.BUILDING
                && inside(mouseX, mouseY, layout.subRailX(), layout.top(), BUTTON, railHeight)) {
            if (event.getScrollDelta() != 0) {
                buildingRailOffset = scrollRail(buildingRailOffset, CuratedJeiCatalog.building().size(), layout,
                        event.getScrollDelta() < 0 ? 1 : -1);
                event.setCanceled(true);
            }
            return;
        }
        if (!inside((int) event.getMouseX(), (int) event.getMouseY(), layout.gridX(), layout.contentTop(), layout.columns() * CELL, layout.rows() * CELL)) return;
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
        if (owner != screen || categorySearch == null) {
            owner = screen;
            CatalogLayout layout = layout(screen);
            categorySearch = searchBox(layout.searchX(), layout.searchY(), layout.searchWidth(), "gui.jemcompat.jei.search_category");
        }
    }

    private static void positionSearch(CatalogLayout layout) {
        categorySearch.setX(layout.searchX());
        categorySearch.setY(layout.searchY());
        categorySearch.setWidth(layout.searchWidth());
    }

    private static void renderNativeToggle(GuiGraphics graphics, CatalogLayout layout, int mouseX, int mouseY) {
        renderButton(graphics, new ItemStack(Items.COMPASS), layout.toggleX(), layout.top(), JEMJeiPlugin.isNativeOverlayVisible());
        renderLabel(graphics, "gui.jemcompat.jei.vanilla", layout.toggleX(), layout.top(), mouseX, mouseY);
    }

    private static void renderCategories(GuiGraphics graphics, CatalogLayout layout, int mouseX, int mouseY) {
        List<CatalogCategory> categories = categories();
        int categoryStart = railStart(categories, layout, false);
        for (int index = categoryStart; index < Math.min(categories.size(), categoryStart + layout.railSlots()); index++) {
            CatalogCategory category = categories.get(index);
            int y = layout.top() + (index - categoryStart) * BUTTON;
            renderButton(graphics, category.icon(), layout.railX(), y, category == selected);
            renderLabel(graphics, category.translationKey(), layout.railX(), y, mouseX, mouseY);
        }
        if (selected == CatalogCategory.BUILDING) {
            List<CatalogCategory> children = CuratedJeiCatalog.building();
            int childStart = railStart(children, layout, true);
            for (int index = childStart; index < Math.min(children.size(), childStart + layout.railSlots()); index++) {
                CatalogCategory category = children.get(index);
                int y = layout.top() + (index - childStart) * BUTTON;
                renderButton(graphics, category.icon(), layout.subRailX(), y, category == building);
                renderLabel(graphics, category.translationKey(), layout.subRailX(), y, mouseX, mouseY);
            }
        }
    }

    private static int railStart(List<CatalogCategory> categories, CatalogLayout layout, boolean children) {
        int offset = children ? buildingRailOffset : categoryRailOffset;
        int clamped = Math.min(offset, Math.max(0, categories.size() - layout.railSlots()));
        if (children) buildingRailOffset = clamped;
        else categoryRailOffset = clamped;
        return clamped;
    }

    private static int scrollRail(int offset, int count, CatalogLayout layout, int delta) {
        return Math.max(0, Math.min(Math.max(0, count - layout.railSlots()), offset + delta));
    }

    private static void renderGrid(GuiGraphics graphics, CatalogLayout layout, int mouseX, int mouseY) {
        if (layout.rows() == 0) return;
        int width = layout.columns() * CELL;
        int height = layout.rows() * CELL;
        graphics.fill(layout.gridX() - 3, layout.contentTop() - 3, layout.gridX() + width + 3, layout.contentTop() + height + 3, 0xB0101010);
        List<ItemStack> stacks = visibleStacks();
        page = Math.min(page, Math.max(0, (stacks.size() - 1) / layout.capacity()));
        int start = page * layout.capacity();
        int end = Math.min(stacks.size(), start + layout.capacity());
        for (int index = start; index < end; index++) {
            int visible = index - start;
            int x = layout.gridX() + visible % layout.columns() * CELL;
            int y = layout.contentTop() + visible / layout.columns() * CELL;
            ItemStack stack = stacks.get(index);
            if (inside(mouseX, mouseY, x, y, CELL, CELL)) graphics.fill(x, y, x + CELL, y + CELL, 0x80FFFFFF);
            graphics.renderItem(stack, x + 2, y + 2);
            if (inside(mouseX, mouseY, x, y, CELL, CELL)) graphics.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
        }
        renderNavigation(graphics, layout, mouseX, mouseY, stacks.size());
    }

    private static void renderNavigation(GuiGraphics graphics, CatalogLayout layout, int mouseX, int mouseY, int stackCount) {
        int pages = Math.max(1, (stackCount + layout.capacity() - 1) / layout.capacity());
        int y = layout.navigationY();
        int width = layout.columns() * CELL;
        if (width < 80) {
            if (width >= 40 || pages <= 1) {
                graphics.drawCenteredString(Minecraft.getInstance().font, Integer.toString(page + 1),
                        layout.gridX() + width / 2, y + 7, 0xFFFFFF);
            }
            if (pages > 1) {
                graphics.drawString(Minecraft.getInstance().font, "<", layout.gridX() + 2, y + 7, 0xFFFFFF);
                graphics.drawString(Minecraft.getInstance().font, ">", layout.gridX() + width - 7, y + 7, 0xFFFFFF);
            }
            if (inside(mouseX, mouseY, layout.gridX(), y, width, BUTTON)) {
                graphics.renderTooltip(Minecraft.getInstance().font,
                        Component.translatable("gui.jemcompat.jei.page", page + 1, pages), mouseX, mouseY);
            }
            return;
        }
        graphics.drawCenteredString(Minecraft.getInstance().font,
                Component.translatable("gui.jemcompat.jei.page", page + 1, pages), layout.gridX() + width / 2, y + 7, 0xFFFFFF);
        if (pages <= 1) return;
        renderButton(graphics, new ItemStack(Items.ARROW), layout.gridX(), y, true);
        int nextX = layout.gridX() + width - BUTTON;
        renderButton(graphics, new ItemStack(Items.ARROW), nextX, y, true);
        graphics.drawCenteredString(Minecraft.getInstance().font, "<", layout.gridX() + BUTTON / 2, y + 7, 0xFFFFFF);
        graphics.drawCenteredString(Minecraft.getInstance().font, ">", nextX + BUTTON / 2, y + 7, 0xFFFFFF);
        if (inside(mouseX, mouseY, layout.gridX(), y, BUTTON, BUTTON))
            graphics.renderTooltip(Minecraft.getInstance().font, Component.translatable("gui.jemcompat.jei.previous_page"), mouseX, mouseY);
        if (inside(mouseX, mouseY, nextX, y, BUTTON, BUTTON))
            graphics.renderTooltip(Minecraft.getInstance().font, Component.translatable("gui.jemcompat.jei.next_page"), mouseX, mouseY);
    }

    private static int lastPage(CatalogLayout layout) {
        return Math.max(0, (visibleStacks().size() - 1) / layout.capacity());
    }

    private static int navigationButtonWidth(CatalogLayout layout) {
        return layout.columns() * CELL >= 80 ? BUTTON : Math.min(BUTTON, Math.max(8, layout.columns() * CELL / 3));
    }

    private static void movePage(CatalogLayout layout,int delta) {
        page=Math.floorMod(page+delta,lastPage(layout)+1);
    }

    private static List<ItemStack> visibleStacks() {
        List<ItemStack> source = activeStacks();
        String query = categorySearch == null ? "" : categorySearch.getValue().trim();
        if (query.isEmpty()) return source;
        String needle = query.toLowerCase(Locale.ROOT);
        return source.stream().filter(stack -> matches(stack, needle)).toList();
    }

    private static boolean matches(ItemStack stack, String needle) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle)
                || id != null && id.toString().toLowerCase(Locale.ROOT).contains(needle);
    }

    private static ItemStack hoveredStack(CatalogLayout layout, int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY, layout.gridX(), layout.contentTop(), layout.columns() * CELL, layout.rows() * CELL)) return ItemStack.EMPTY;
        int index = page * layout.capacity() + (mouseY - layout.contentTop()) / CELL * layout.columns() + (mouseX - layout.gridX()) / CELL;
        List<ItemStack> stacks = visibleStacks();
        return index < stacks.size() ? stacks.get(index) : ItemStack.EMPTY;
    }

    private static void renderButton(GuiGraphics graphics, ItemStack icon, int x, int y, boolean selectedButton) {
        graphics.fill(x, y, x + BUTTON, y + BUTTON, selectedButton ? 0xE07A4C24 : 0xC0181818);
        graphics.fill(x + 1, y + 1, x + BUTTON - 1, y + BUTTON - 1, selectedButton ? 0xE0B06A32 : 0xC0383838);
        graphics.renderItem(icon, x + 3, y + 3);
    }

    private static void renderLabel(GuiGraphics graphics, String key, int x, int y, int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, x, y, BUTTON, BUTTON)) graphics.renderTooltip(Minecraft.getInstance().font, Component.translatable(key), mouseX, mouseY);
    }

    private static List<CatalogCategory> categories() {
        var player = Minecraft.getInstance().player;
        return CuratedJeiCatalog.topLevel(player != null && player.getAbilities().instabuild);
    }

    private static List<ItemStack> activeStacks() {
        return CuratedJeiCatalog.stacks(selected == CatalogCategory.BUILDING ? building : selected);
    }

    private static void select(CatalogCategory category) {
        if (selected != category) {
            selected = category;
            categorySearch.setValue("");
            page = 0;
        }
    }

    private static CatalogLayout layout(AbstractContainerScreen<?> screen) {
        int railX = screen.width - BUTTON - MARGIN;
        int subRailX = railX - BUTTON - 4;
        int top = MARGIN;
        int right = selected == CatalogCategory.BUILDING ? subRailX - MARGIN : railX - MARGIN;
        int rightGridX = screen.getGuiLeft() + screen.getXSize() + MARGIN;
        int leftAvailable = screen.getGuiLeft() - MARGIN * 2;
        int rightAvailable = right - rightGridX;
        int gridX = rightAvailable >= CELL ? rightGridX : MARGIN;
        int available = Math.max(CELL, gridX == rightGridX ? rightAvailable : leftAvailable);
        int toggleX = gridX;
        int columns = Math.max(1, available / CELL);
        int searchX = gridX;
        int searchWidth = columns * CELL;
        int searchY = Math.max(MARGIN, screen.height - MARGIN - SEARCH_HEIGHT);
        int navigationY = searchY - NAVIGATION_HEIGHT;
        int contentTop = top + BUTTON + 4;
        int rows = Math.max(0, Math.min(12, (navigationY - contentTop - 4) / CELL));
        int railSlots = Math.max(1, (screen.height - MARGIN * 2) / BUTTON);
        return new CatalogLayout(gridX, railX, subRailX, top, contentTop, columns, rows,
                toggleX, searchX, searchY, searchWidth, navigationY, railSlots);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

}
