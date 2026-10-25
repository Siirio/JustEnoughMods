package com.justenoughmods.achievementguide.client.handbook;

import com.justenoughmods.achievementguide.client.handbook.enchantment.EnchantmentHelp;
import com.justenoughmods.achievementguide.client.handbook.manuals.NativeManuals;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookCategory;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.gui.GuiBookIndex;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

/** Presentation-only navigation. Native books retain their own history and progression. */
public final class HandbookNavigation {
    public static final ResourceLocation BOOK = new ResourceLocation("jem_advancements", "just_enough_guide");
    public static final KeyMapping HELP_KEY = new KeyMapping("key.jem_advancements.handbook", KeyConflictContext.UNIVERSAL,
            KeyModifier.ALT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.jem_advancements");
    private static ItemStack hovered = ItemStack.EMPTY;
    private static Screen hoveredScreen;
    private static ReturnSession returning;

    private HandbookNavigation() { }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(HandbookNavigation.class);
        MinecraftForge.EVENT_BUS.register(HandbookWorldVisibility.class);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(HandbookNavigation::keys);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(HandbookSearch::reloadListeners);
        EnchantmentHelp.register();
        HandbookItemHelp.register();
        PatchouliAPI.get().registerFunction("jemmanual", (id, style) -> NativeManuals.available().stream()
                .filter(manual -> manual.id().equals(id)).findFirst().map(manual -> manual.status().getString())
                .orElseGet(() -> Component.translatable("jem_advancements.handbook.manual_unavailable").getString()));
        if (net.minecraftforge.fml.ModList.get().isLoaded("jem_drill_lifecycle")) {
            PatchouliAPI.get().registerFunction("jemdrill", (name, style) -> HandbookText.drillPolicy(name));
        }
        PatchouliAPI.get().registerFunction("jemkey", (name, style) -> {
            for (KeyMapping key : Minecraft.getInstance().options.keyMappings) {
                if (key.getName().equals(name)) return key.getTranslatedKeyMessage().getString();
            }
            return Component.translatable("key.keyboard.unknown").getString();
        });
    }

    private static void keys(RegisterKeyMappingsEvent event) {
        event.register(HELP_KEY);
    }

    public static Book book() {
        return BookRegistry.INSTANCE.books.get(BOOK);
    }

    public static boolean openEntry(ResourceLocation id) {
        Book guide = book();
        if (guide == null || guide.getContents().isErrored()) return false;
        var entry = guide.getContents().entries.get(id);
        if (entry == null || entry.isLocked() || entry.shouldHide()) return false;
        PatchouliAPI.get().openBookEntry(BOOK, id, 0);
        return true;
    }

    public static ResourceLocation entryFor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation enchanted = EnchantmentHelp.entryFor(stack);
        if (enchanted != null) return enchanted;
        Book guide = book();
        if (guide == null || guide.getContents().isErrored()) return null;
        var target = guide.getContents().getEntryForStack(stack);
        if (target == null || target.getFirst().isLocked() || target.getFirst().shouldHide()) return null;
        return target.getFirst().getId();
    }

    public static ResourceLocation entryForEntity(net.minecraft.world.entity.Entity entity) {
        ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) return null;
        ResourceLocation entry = new ResourceLocation("jem_advancements", "handbook/bosses/" + id.getNamespace() + "/" + id.getPath());
        Book guide = book();
        return guide != null && guide.getContents().entries.containsKey(entry) ? entry : null;
    }

    @SubscribeEvent
    public static void worldHelp(net.minecraftforge.client.event.InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS || minecraft.screen != null || minecraft.player == null) return;
        if (HELP_KEY.isActiveAndMatches(InputConstants.getKey(event.getKey(), event.getScanCode()))
                && minecraft.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit) {
            ResourceLocation entry = entryForEntity(hit.getEntity());
            if (entry != null) openEntry(entry);
        }
    }

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof GuiBook screen && BOOK.equals(screen.book.id)) {
            String section = section(screen);
            for (int i = 0; i < HandbookSection.values().length; i++) {
                HandbookSection tab = HandbookSection.values()[i];
                event.addListener(new HandbookRibbon(screen, tab, i, tab.path.equals(section)));
            }
            if (screen instanceof GuiBookEntry page) {
                String path = page.getEntry().getId().getPath();
                if (path.startsWith("handbook/manuals/")) {
                    String manualId = path.substring("handbook/manuals/".length());
                    NativeManuals.available().stream().filter(m -> m.id().equals(manualId)).findFirst().ifPresent(manual -> {
                        Button button = Button.builder(Component.translatable("jem_advancements.handbook.open_manual"),
                                b -> openManual(screen, manual)).bounds(screen.bookLeft + 155, screen.bookTop + 143, 90, 20).build();
                        button.active = manual.enabled();
                        event.addListener(button);
                    });
                }
            }
        }
        if (returning != null && returning.accepts(event.getScreen())) {
            Screen screen = event.getScreen();
            Button button = Button.builder(Component.translatable("jem_advancements.handbook.return"), b -> returnToGuide())
                    .bounds(4, 4, Math.min(156, screen.width - 8), 20).build();
            if (screen instanceof GuiBook patchouli) {
                button.setPosition(patchouli.bookLeft + 70, Math.max(0, patchouli.bookTop - 23));
                button.setWidth(132);
            }
            event.addListener(button);
        }
    }

    static void select(GuiBook source, HandbookSection section) {
        if (section == HandbookSection.START) {
            source.displayLexiconGui(new GuiBookIndex(source.book), true);
        } else if (section == HandbookSection.MANUALS) {
            source.displayLexiconGui(new HandbookManualScreen(source.book), true);
        } else {
            var category = source.book.getContents().categories.get(new ResourceLocation("jem_advancements", "handbook/" + section.path));
            if (category != null && !category.isLocked() && !category.shouldHide()) {
                source.displayLexiconGui(new GuiBookCategory(source.book, category), true);
            }
        }
    }

    private static String section(GuiBook screen) {
        if (screen instanceof HandbookManualScreen) return "manuals";
        if (screen instanceof GuiBookCategory) {
            var category = ((com.justenoughmods.achievementguide.mixin.PatchouliHandbookCategoryAccess) screen).jem$category();
            while (category.getParentCategory() != null) category = category.getParentCategory();
            return category.getId().getPath().replace("handbook/", "");
        }
        if (screen instanceof GuiBookEntry page) {
            var category = page.getEntry().getCategory();
            while (category != null && category.getParentCategory() != null) category = category.getParentCategory();
            if (category != null) return category.getId().getPath().replace("handbook/", "");
        }
        return "start";
    }

    public static void openManual(GuiBook origin, NativeManuals.Manual manual) {
        if (!manual.enabled()) return;
        returning = new ReturnSession(origin, manual, System.nanoTime());
        if (!manual.open()) returning = null;
    }

    private static void returnToGuide() {
        ReturnSession session = returning;
        returning = null;
        if (session == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        if (minecraft.player.containerMenu != minecraft.player.inventoryMenu) minecraft.player.closeContainer();
        minecraft.setScreen(session.origin);
    }

    @SubscribeEvent
    public static void renderStart(ScreenEvent.Render.Pre event) {
        hovered = ItemStack.EMPTY;
        hoveredScreen = null;
        if (returning != null && !returning.accepts(event.getScreen())
                && (returning.opened || System.nanoTime() - returning.started > 5_000_000_000L)) returning = null;
    }

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) {
        if (returning != null && returning.opened && !returning.accepts(event.getNewScreen())) returning = null;
    }

    @SubscribeEvent
    public static void tooltipShown(RenderTooltipEvent.Pre event) {
        hovered = event.getItemStack();
        hoveredScreen = Minecraft.getInstance().screen;
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (entryFor(event.getItemStack()) != null) {
            event.getToolTip().add(Component.translatable("jem_advancements.handbook.details", HELP_KEY.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @SubscribeEvent
    public static void key(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen().getFocused() instanceof EditBox || hoveredScreen != event.getScreen()) return;
        if (HELP_KEY.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) {
            ResourceLocation entry = entryFor(hovered);
            if (entry != null && openEntry(entry)) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        returning = null;
        hovered = ItemStack.EMPTY;
        hoveredScreen = null;
    }

    private static final class ReturnSession {
        final GuiBook origin;
        final NativeManuals.Manual manual;
        final long started;
        boolean opened;
        ReturnSession(GuiBook origin, NativeManuals.Manual manual, long started) {
            this.origin = origin;
            this.manual = manual;
            this.started = started;
        }
        boolean accepts(Screen screen) {
            if (NativeManuals.isScreenFor(manual, screen)) {
                opened = true;
                return true;
            }
            return false;
        }
    }
}
