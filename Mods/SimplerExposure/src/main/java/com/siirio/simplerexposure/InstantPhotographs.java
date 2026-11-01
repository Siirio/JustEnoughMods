package com.siirio.simplerexposure;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.forge.api.event.FrameAddedEvent;
import io.github.mortuusars.exposure.world.item.FilmItem;
import io.github.mortuusars.exposure.world.item.camera.Attachment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class InstantPhotographs {
    private InstantPhotographs() { }

    public static void onFrameAdded(FrameAddedEvent event) {
        ServerPlayer player = event.getCameraHolder().getServerPlayerExecutingExposure().orElse(null);
        if (player == null) return;

        ItemStack film = Attachment.FILM.get(event.getCamera()).getCopy();
        if (!(film.getItem() instanceof FilmItem filmItem) || !isRegularFilm(film)) return;

        ItemStack photograph = new ItemStack(Exposure.Items.PHOTOGRAPH.get());
        Exposure.DataComponents.setPhotographFrame(photograph, event.getFrame());
        Exposure.DataComponents.setPhotographType(photograph, event.getFrame().type());
        if (!player.getInventory().add(photograph)) player.drop(photograph, false);

        if (filmItem.getStoredFramesCount(film) >= SimplerExposureConfig.FILM_CAPACITY.get()) {
            Attachment.FILM.set(event.getCamera(), ItemStack.EMPTY);
        }
    }

    public static boolean isRegularFilm(ItemStack stack) {
        return stack.is(Exposure.Items.BLACK_AND_WHITE_FILM.get()) || stack.is(Exposure.Items.COLOR_FILM.get());
    }
}
