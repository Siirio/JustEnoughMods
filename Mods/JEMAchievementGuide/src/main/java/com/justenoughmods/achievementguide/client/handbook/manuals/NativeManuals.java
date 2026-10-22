package com.justenoughmods.achievementguide.client.handbook.manuals;

import com.justenoughmods.achievementguide.handbook.manuals.CommonManuals;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.common.book.BookRegistry;

import java.util.ArrayList;
import java.util.List;

/** Read-only entry points verified against the installed native manuals. */
public final class NativeManuals {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation IRON_BOOK = new ResourceLocation("irons_spellbooks", "iss_guide_book");

    private NativeManuals() {}

    public static List<Manual> available() {
        List<Manual> result = new ArrayList<>();
        add(result, "tide", "tide:fishing_journal", "item.tide.fishing_journal",
                "tide fishing journal рыбалка журнал рыб", "com.li64.tide.client.gui.screens.journal.",
                () -> callStatic("com.li64.tide.client.TideClientHelper", "openJournalScreen"), "discoveries");
        add(result, "alexscaves", "alexscaves:cave_book", "item.alexscaves.cave_book",
                "alex caves cave compendium пещеры компендиум", "com.github.alexmodguy.alexscaves.client.gui.book.CaveBookScreen",
                () -> openScreen("com.github.alexmodguy.alexscaves.client.gui.book.CaveBookScreen"), "caves");
        add(result, "alexsmobs", "alexsmobs:animal_dictionary", "item.alexsmobs.animal_dictionary",
                "alex mobs animal dictionary животные словарь", "com.github.alexthe666.alexsmobs.client.gui.GUIAnimalDictionary",
                () -> openStackScreen("com.github.alexthe666.alexsmobs.client.gui.GUIAnimalDictionary", stack("alexsmobs:animal_dictionary")), "read");
        add(result, "blastfromthepast", "blastfromthepast:glacial_guidebook", "item.blastfromthepast.glacial_guidebook",
                "blast past glacial guidebook ледник ледниковый справочник", "team.recrafted.blastfromthepast.client.screen.GlacialGuidebookScreen",
                () -> callStatic("team.recrafted.blastfromthepast.util.ClientUtils", "openGlacialGuidebookScreen"), "read");
        add(result, "cnc", "cnc:field_guide", "item.cnc.field_guide",
                "critters crawlers field guide полевой справочник животные", "net.imasillylittleguy.cnc.client.gui.FieldGuide",
                () -> CommonManuals.open(CommonManuals.Route.CNC), "discoveries");
        add(result, "luminous_beasts", "luminous_beasts:ultimate_bestiary", "item.luminous_beasts.ultimate_bestiary",
                "luminous beasts ultimate bestiary бестиарий звери", "net.mcreator.luminousbeasts.client.gui.BeastBook",
                () -> CommonManuals.open(CommonManuals.Route.LUMINOUS), "read");
        add(result, "unearthed_journey", "unearthed_journey:journey_guide", "item.unearthed_journey.journey_guide",
                "unearthed journey guide путешествие путеводитель", "net.mcreator.unearthedjourney.client.gui.",
                () -> CommonManuals.open(CommonManuals.Route.UNEARTHED), "read");
        if (ModList.get().isLoaded("irons_spellbooks") && ModList.get().isLoaded("patchouli")
                && BookRegistry.INSTANCE.books.containsKey(IRON_BOOK)) {
            ItemStack icon = PatchouliAPI.get().getBookStack(IRON_BOOK);
            if (!icon.isEmpty()) result.add(new Manual("irons_spellbooks", Component.translatable("jem.handbook.manual.irons_spellbooks"),
                    icon, "iron spells spellbooks guide magic магия заклинания книга", "vazkii.patchouli.client.book.gui.",
                    () -> {
                        if (!BookRegistry.INSTANCE.books.containsKey(IRON_BOOK)) {
                            throw new ReflectiveOperationException("Native Patchouli book is no longer registered: " + IRON_BOOK);
                        }
                        PatchouliAPI.get().openBookGUI(IRON_BOOK);
                    }, Component.translatable("jem.handbook.manual.status.read"), true));
        }
        // Aquamirae stores the actual topic and content on each acquired logbook.
        // A blank stack would invent access to a document the player has not found.
        if (ModList.get().isLoaded("aquamirae") && ForgeRegistries.ITEMS.containsKey(new ResourceLocation("aquamirae:logbook"))) {
            List<ItemStack> logbooks = findLogbooks();
            if (logbooks.isEmpty()) {
                result.add(new Manual("aquamirae", Component.translatable("item.aquamirae.logbook"), stack("aquamirae:logbook"),
                        "aquamirae logbook captain журнал капитан", "dev.obscuria.aquamirae.client.screen.LogBookViewScreen",
                        () -> {}, Component.translatable("jem.handbook.manual.status.logbook_missing"), false));
            }
            for (int i = 0; i < logbooks.size(); i++) {
                ItemStack found = logbooks.get(i);
                Component topic = logbookTopic(found);
                Component title = Component.translatable("item.aquamirae.logbook").append(": ").append(topic);
                result.add(new Manual(i == 0 ? "aquamirae" : "aquamirae_" + i, title, found.copy(),
                        "aquamirae logbook captain журнал капитан " + topic.getString(), "dev.obscuria.aquamirae.client.screen.LogBookViewScreen",
                        () -> Class.forName("dev.obscuria.aquamirae.client.AquamiraeClientHelper")
                                .getMethod("openLogbookScreen", ItemStack.class).invoke(null, found.copy()),
                        Component.translatable("jem.handbook.manual.status.logbook"), true));
            }
        }
        return List.copyOf(result);
    }

    private static void add(List<Manual> result, String mod, String item, String title, String aliases,
                            String screenPrefix, OpenAction open, String status) {
        if (ModList.get().isLoaded(mod) && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(item))) {
            result.add(new Manual(mod, Component.translatable(title), stack(item), aliases, screenPrefix, open,
                    Component.translatable("jem.handbook.manual.status." + status), true));
        }
    }

    public static boolean isManualScreen(Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        return name.startsWith("com.li64.tide.client.gui.screens.journal.")
                || name.equals("com.li64.tide.client.gui.screens.FishyNoteScreen")
                || name.equals("com.github.alexmodguy.alexscaves.client.gui.book.CaveBookScreen")
                || name.startsWith("com.github.alexthe666.alexsmobs.client.gui.GUIAnimalDictionary")
                || name.equals("team.recrafted.blastfromthepast.client.screen.GlacialGuidebookScreen")
                || name.startsWith("net.imasillylittleguy.cnc.client.gui.FieldGuide")
                || name.startsWith("net.mcreator.luminousbeasts.client.gui.BeastBook")
                || isUnearthedGuideScreen(name)
                || name.equals("dev.obscuria.aquamirae.client.screen.LogBookViewScreen")
                || (screen instanceof GuiBook book && IRON_BOOK.equals(book.book.id));
    }

    public static boolean isScreenFor(Manual manual, Screen screen) {
        if (!isManualScreen(screen)) return false;
        String name = screen.getClass().getName();
        if (manual.id().equals("unearthed_journey")) return isUnearthedGuideScreen(name);
        if (manual.id().equals("tide")) return name.startsWith(manual.screenPrefix()) || name.equals("com.li64.tide.client.gui.screens.FishyNoteScreen");
        return name.startsWith(manual.screenPrefix());
    }

    private static boolean isUnearthedGuideScreen(String name) {
        String prefix = "net.mcreator.unearthedjourney.client.gui.";
        if (!name.startsWith(prefix)) return false;
        String simple = name.substring(prefix.length());
        return simple.matches("(?:JourneyGuide|GettingStarted|Environs|ToolsNItems|UniqueAnimals)Page[1-5]Screen");
    }

    private static List<ItemStack> findLogbooks() {
        List<ItemStack> result = new ArrayList<>();
        var player = Minecraft.getInstance().player;
        if (player == null) return result;
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("aquamirae:logbook"));
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack value = player.getInventory().getItem(slot);
            if (value.is(item) && value.hasTag() && value.getTag().contains("content", 8)
                    && result.stream().noneMatch(existing -> ItemStack.isSameItemSameTags(existing, value))) result.add(value.copy());
        }
        return result;
    }

    private static Component logbookTopic(ItemStack stack) {
        Component topic = Component.Serializer.fromJson(stack.getTag().getString("topic"));
        return topic == null ? Component.translatable("item.aquamirae.logbook") : topic;
    }

    private static ItemStack stack(String id) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));
    }

    private static void callStatic(String owner, String method) throws ReflectiveOperationException {
        Class.forName(owner).getMethod(method).invoke(null);
    }

    private static void openScreen(String owner) throws ReflectiveOperationException {
        Minecraft.getInstance().setScreen((Screen) Class.forName(owner).getConstructor().newInstance());
    }

    private static void openStackScreen(String owner, ItemStack stack) throws ReflectiveOperationException {
        Minecraft.getInstance().setScreen((Screen) Class.forName(owner).getConstructor(ItemStack.class).newInstance(stack));
    }

    @FunctionalInterface
    public interface OpenAction {
        void run() throws ReflectiveOperationException;
    }

    public record Manual(String id, Component title, ItemStack icon, String aliases, String screenPrefix,
                         OpenAction action, Component status, boolean enabled) {
        public boolean open() {
            if (!enabled || Minecraft.getInstance().player == null) return false;
            try {
                action.run();
                return true;
            } catch (ReflectiveOperationException | LinkageError error) {
                LOGGER.error("Cannot open native handbook manual {}", id, error);
                Minecraft.getInstance().player.displayClientMessage(Component.translatable("jem.handbook.manual.failed", title), false);
                return false;
            }
        }
    }
}
