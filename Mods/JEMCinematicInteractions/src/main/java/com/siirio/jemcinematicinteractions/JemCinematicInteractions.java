package com.siirio.jemcinematicinteractions;

import com.siirio.jemcinematicinteractions.mixin.SceneScreenAccess;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SceneScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
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
            if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT || !Screen.hasShiftDown()
                    || !(event.getScreen() instanceof SceneScreen)) {
                return;
            }

            quickMoveGesture = true;
            QUICK_MOVED_SLOTS.clear();
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void continueQuickMove(ScreenEvent.MouseDragged.Pre event) {
            if (!quickMoveGesture || event.getMouseButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                    || !Screen.hasShiftDown()
                    || !(event.getScreen() instanceof SceneScreen screen)) {
                return;
            }

            quickMove(screen, event.getMouseX(), event.getMouseY());
            event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void finishQuickMove(ScreenEvent.MouseButtonReleased.Pre event) {
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                resetQuickMove();
            }
        }

        @SubscribeEvent
        public static void closeScreen(ScreenEvent.Closing event) {
            resetQuickMove();
        }

        private static void quickMove(SceneScreen screen, double mouseX, double mouseY) {
            SceneScreenAccess access = (SceneScreenAccess) (Object) screen;
            if (access.jemCinematicInteractions$overPanel(mouseX, mouseY)) {
                int slot = access.jemCinematicInteractions$slotAt(mouseX, mouseY);
                if (slot >= 0 && QUICK_MOVED_SLOTS.add(slot)) {
                    access.jemCinematicInteractions$panelClick(slot, GLFW.GLFW_MOUSE_BUTTON_LEFT, true);
                }
                return;
            }

            if (Scene.station != null) {
                Scene.station.click(GLFW.GLFW_MOUSE_BUTTON_LEFT, true);
            }
        }

        private static void resetQuickMove() {
            quickMoveGesture = false;
            QUICK_MOVED_SLOTS.clear();
        }
    }
}
