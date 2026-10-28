package com.justenoughmods.achievementguide.integration.photo;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.home.HomeInteractionProof;
import io.github.mortuusars.exposure.world.block.entity.LightroomBlockEntity;
import io.github.mortuusars.exposure.world.camera.ColorChannel;
import io.github.mortuusars.exposure.world.camera.ExposureType;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.inventory.LightroomMenu;
import io.github.mortuusars.exposure.world.item.ChromaticSheetItem;
import io.github.mortuusars.exposure.world.item.DevelopedFilmItem;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.StackedPhotographsItem;
import io.github.mortuusars.exposure.world.lightroom.PrintingMode;
import io.github.mortuusars.exposure.world.lightroom.PrintingProcess;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PhotoPrintingProof {
    private static final String REQUEST = "jem_photo_print_request_v4";
    private record Menu(LightroomMenu menu, ServerPlayer player) {}
    private static final class Print {
        final LightroomBlockEntity tile;
        final Frame frame;
        final PrintingProcess process;
        final ServerPlayer player;
        final List<ItemStack> before;
        ItemStack nativeResult;
        UUID composer;
        String compositeId;
        Print(LightroomBlockEntity tile, Frame frame, PrintingProcess process, ServerPlayer player) {
            this.tile = tile; this.frame = frame; this.process = process; this.player = player;
            before = new ArrayList<>();
            for (int slot = 0; slot < 7; slot++) before.add(tile.getItem(slot).copy());
        }
    }
    private static final ThreadLocal<Deque<Menu>> MENUS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<Print>> PRINTS = ThreadLocal.withInitial(ArrayDeque::new);

    public static void menuStart(LightroomMenu menu, Player player, int button) {
        if (MENUS.get().size() >= 16) MENUS.get().clear();
        ServerPlayer actor = button == 0 && player.containerMenu == menu ? HomeInteractionProof.actor(player) : null;
        MENUS.get().push(new Menu(menu, actor));
    }
    public static void menuEnd() { MENUS.get().poll(); }
    public static void started(LightroomBlockEntity tile) {
        Menu menu = MENUS.get().peek();
        tile.getPersistentData().remove(REQUEST);
        if (menu == null || menu.player == null || menu.menu.getBlockEntity() != tile || tile.getLevel() != menu.player.level()
                || ((PhotoLightroomState)tile).jem$photoPrintTime() <= 0 || tile.getSelectedFrame() == null) return;
        Frame frame = tile.getSelectedFrame();
        String id = id(frame);
        if (id.isEmpty()) return;
        CompoundTag request = new CompoundTag();
        request.putUUID("actor", menu.player.getUUID());
        request.putString("frame", id);
        request.putString("process", ((PhotoLightroomState)tile).jem$photoProcess(frame).name());
        tile.getPersistentData().put(REQUEST, request);
        tile.setChanged();
    }
    public static void stopped(LightroomBlockEntity tile) { tile.getPersistentData().remove(REQUEST); }

    public static void before(LightroomBlockEntity tile, Frame frame, PrintingProcess process, boolean consume) {
        if (PRINTS.get().size() >= 16) PRINTS.get().clear();
        CompoundTag request = tile.getPersistentData().getCompound(REQUEST);
        ServerPlayer player = consume && tile.getLevel() instanceof ServerLevel level && request.hasUUID("actor")
                ? level.getServer().getPlayerList().getPlayer(request.getUUID("actor")) : null;
        if (HomeInteractionProof.actor(player) == null || player.level() != tile.getLevel()
                || !request.getString("frame").equals(id(frame)) || !request.getString("process").equals(process.name())
                || !(tile.getItem(0).getItem() instanceof DevelopedFilmItem film) || !film.getStoredFrames(tile.getItem(0)).contains(frame)) player = null;
        PRINTS.get().push(new Print(tile, frame, process, player));
    }
    public static void nativeResult(LightroomBlockEntity tile, Frame frame, PrintingProcess process, ItemStack result) {
        Print print = PRINTS.get().peek();
        if (print != null && print.tile == tile && print.frame.equals(frame) && print.process == process) print.nativeResult = result;
    }
    public static void composite(ServerPlayer composer, ItemStack result) {
        Print print = PRINTS.get().peek();
        if (print != null && print.process == PrintingProcess.CHROMATIC_B && result.getItem() instanceof PhotographItem photo) {
            print.composer = composer.getUUID(); print.compositeId = id(photo.getFrame(result));
        }
    }

    public static void after(LightroomBlockEntity tile) {
        Print print = PRINTS.get().poll();
        if (print == null || print.tile != tile || print.player == null || print.nativeResult == null
                || HomeInteractionProof.actor(print.player) == null || tile.isRemoved() || tile.getLevel() != print.player.level()
                || tile.getLevel().getBlockEntity(tile.getBlockPos()) != tile || !ingredientsPaid(print)) return;
        if (print.process.isRegular()) {
            if (!(print.nativeResult.getItem() instanceof PhotographItem photo) || !photo.getFrame(print.nativeResult).equals(print.frame)
                    || countPhoto(tile.getItem(6), print.frame) != countPhoto(print.before.get(6), print.frame) + 1
                    || !PhotoDevelopmentOrigins.get(print.player.serverLevel()).owns(print.player.getUUID(), print.frame)) return;
            if (print.process == PrintingProcess.BLACK_AND_WHITE)
                PhotoImages.output(print.player, id(print.frame), print.frame.type(), null, false, "v3_home_real_print");
            else if (print.process == PrintingProcess.COLOR && print.frame.isTakenBy(print.player))
                PhotoImages.output(print.player, id(print.frame), print.frame.type(), null, false, "v3_home_color_print");
            return;
        }
        if (tile.getActualPrintingMode() != PrintingMode.CHROMATIC || !print.before.get(6).isEmpty()) return;
        ItemStack input = print.before.get(1);
        List<Frame> previous = input.getItem() instanceof ChromaticSheetItem sheet ? sheet.getLayers(input) : List.of();
        int step = print.process == PrintingProcess.CHROMATIC_R ? 0 : print.process == PrintingProcess.CHROMATIC_G ? 1 : 2;
        List<Frame> layers = new ArrayList<>(previous); layers.add(print.frame);
        if (previous.size() != step || layers.size() > 3 || !rgb(layers)) return;
        var sources = NativeFoodSources.get(print.player.serverLevel());
        if (step > 0 && !sources.consume(print.player.getUUID(), NativeFoodSources.spent(input, 1), step == 1 ? "photo_rgb_r" : "photo_rgb_rg", 1)) return;
        ItemStack output = tile.getItem(6);
        if (step < 2) {
            if (output == print.nativeResult && output.getItem() instanceof ChromaticSheetItem sheet && sheet.getLayers(output).equals(layers)) {
                NativeFoodSources.claims(output, List.of());
                sources.certify(print.player.serverLevel(), print.player.getUUID(), step == 0 ? "photo_rgb_r" : "photo_rgb_rg", output, 1);
            }
        } else if (print.composer != null && print.compositeId != null && !print.compositeId.isEmpty()
                && output == print.nativeResult && output.getItem() instanceof PhotographItem photo
                && photo.getFrame(output).isChromatic() && id(photo.getFrame(output)).equals(print.compositeId))
            PhotoImages.output(print.player, print.compositeId, ExposureType.COLOR, print.composer, true, "v3_home_chromatic");
    }

    private static boolean rgb(List<Frame> layers) {
        ColorChannel[] channels = {ColorChannel.RED, ColorChannel.GREEN, ColorChannel.BLUE};
        java.util.Set<String> identities = new java.util.HashSet<>();
        for (int index = 0; index < layers.size(); index++) {
            Frame frame = layers.get(index);
            if (frame.getColorChannel().orElse(null) != channels[index] || !identities.add(id(frame))
                    || !PhotoImages.valid(id(frame), frame.type(), false)) return false;
        }
        return true;
    }
    private static boolean ingredientsPaid(Print print) {
        if (!debited(print.before.get(1), print.tile.getItem(1), 1)) return false;
        Map<Integer, Integer> dyes = new HashMap<>();
        for (var dye : print.process.getRequiredDyes()) {
            Integer slot = LightroomBlockEntity.DYE_SLOTS.get(dye);
            if (slot == null) return false;
            dyes.merge(slot, 1, Integer::sum);
        }
        for (var dye : dyes.entrySet()) if (!debited(print.before.get(dye.getKey()), print.tile.getItem(dye.getKey()), dye.getValue())) return false;
        return true;
    }
    private static boolean debited(ItemStack before, ItemStack after, int count) {
        return !before.isEmpty() && (after.isEmpty() ? before.getCount() == count
                : ItemStack.isSameItemSameTags(before, after) && before.getCount() - after.getCount() == count);
    }
    private static int countPhoto(ItemStack stack, Frame frame) {
        if (stack.getItem() instanceof PhotographItem photograph) return photograph.getFrame(stack).equals(frame) ? 1 : 0;
        if (stack.getItem() instanceof StackedPhotographsItem photos) {
            var entries = photos.getPhotographs(stack);
            if (entries.size() > 256) return 0;
            return (int)entries.stream().filter(photo -> photo.getItem().getFrame(photo.getItemStack()).equals(frame)).count();
        }
        return 0;
    }
    private static String id(Frame frame) { return frame.identifier().getId().filter(id -> !id.isBlank()).orElse(""); }
    public static void clear() { MENUS.remove(); PRINTS.remove(); }
    private PhotoPrintingProof() {}
}
