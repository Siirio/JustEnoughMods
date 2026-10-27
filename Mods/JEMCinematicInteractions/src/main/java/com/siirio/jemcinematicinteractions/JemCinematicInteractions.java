package com.siirio.jemcinematicinteractions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

@Mod(JemCinematicInteractions.MOD_ID)
public final class JemCinematicInteractions {
    public static final String MOD_ID = "jem_cinematic_interactions";

    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private static final Set<Integer> QUICK_MOVED_SLOTS = new HashSet<>();
        private static boolean quickMoveGesture;

        private ClientEvents() {
        }

        @SubscribeEvent
        public static void hideVanillaRecipeBook(ScreenEvent.Init.Post event) {
            if (!(event.getScreen() instanceof RecipeUpdateListener recipeScreen)) {
                return;
            }

            var recipeButtons = event.getListenersList().stream()
                    .filter(ImageButton.class::isInstance)
                    .map(ImageButton.class::cast)
                    .filter(button -> button.getWidth() == 20 && button.getHeight() == 18)
                    .toList();
            if (recipeScreen.getRecipeBookComponent().isVisible() && !recipeButtons.isEmpty()) {
                recipeButtons.get(0).onPress();
            }
            recipeButtons.forEach(event::removeListener);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void beginQuickMove(ScreenEvent.MouseButtonPressed.Pre event) {
            if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT || !Screen.hasShiftDown()
                    || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
                return;
            }

            quickMoveGesture = true;
            QUICK_MOVED_SLOTS.clear();
            quickMove(screen, event.getMouseX(), event.getMouseY());
            event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void continueQuickMove(ScreenEvent.MouseDragged.Pre event) {
            if (!quickMoveGesture || event.getMouseButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT
                    || !Screen.hasShiftDown()
                    || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
                return;
            }

            quickMove(screen, event.getMouseX(), event.getMouseY());
            event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void finishQuickMove(ScreenEvent.MouseButtonReleased.Pre event) {
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                resetQuickMove();
            }
        }

        @SubscribeEvent
        public static void closeScreen(ScreenEvent.Closing event) {
            resetQuickMove();
        }

        private static void quickMove(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gameMode == null || minecraft.player == null) {
                return;
            }

            Slot slot = findSlot(screen, mouseX, mouseY);
            if (slot == null || !slot.hasItem() || !QUICK_MOVED_SLOTS.add(slot.index)) {
                return;
            }

            minecraft.gameMode.handleInventoryMouseClick(
                    screen.getMenu().containerId,
                    slot.index,
                    0,
                    ClickType.QUICK_MOVE,
                    minecraft.player
            );
        }

        private static Slot findSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
            int left = screen.getGuiLeft();
            int top = screen.getGuiTop();
            for (Slot slot : screen.getMenu().slots) {
                if (slot.isActive()
                        && mouseX >= left + slot.x && mouseX < left + slot.x + 16
                        && mouseY >= top + slot.y && mouseY < top + slot.y + 16) {
                    return slot;
                }
            }
            return null;
        }

        private static void resetQuickMove() {
            quickMoveGesture = false;
            QUICK_MOVED_SLOTS.clear();
        }
    }
}
