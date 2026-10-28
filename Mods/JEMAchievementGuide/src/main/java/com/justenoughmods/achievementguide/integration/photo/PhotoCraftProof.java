package com.justenoughmods.achievementguide.integration.photo;

import com.justenoughmods.achievementguide.integration.home.HomeInteractionProof;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.item.DevelopedFilmItem;
import io.github.mortuusars.exposure.world.item.FilmItem;
import io.github.mortuusars.exposure.world.item.crafting.recipe.FilmDevelopingRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

/** Reuses captured native recipe removals, including the Shift-click empty-result presentation. */
public final class PhotoCraftProof {
    private record Developed(Item item, int count, List<Frame> frames, ItemStack nativeOutput) {}
    private record Click(ServerPlayer actor, AbstractContainerMenu menu, List<ItemStack> before, List<Developed> pending, List<ItemEntity> drops) {}
    private static final ThreadLocal<Click> CURRENT = new ThreadLocal<>();

    public static void begin(ServerPlayer actor, AbstractContainerMenu menu) {
        CURRENT.remove();
        if (HomeInteractionProof.actor(actor) != null && actor.containerMenu == menu && menu.slots.size() <= 256
                && menu.slots.stream().anyMatch(slot -> slot instanceof ResultSlot && slot.getItem().getItem() instanceof DevelopedFilmItem))
            CURRENT.set(new Click(actor, menu, held(actor, menu).stream().map(ItemStack::copy).toList(), new ArrayList<>(), new ArrayList<>()));
    }
    public static void crafted(ServerPlayer actor, Recipe<?> recipe, ItemStack output, List<ItemStack> spent) {
        Click click = CURRENT.get();
        if (click == null || click.actor != actor || !(recipe instanceof FilmDevelopingRecipe developing)
                || click.pending.size() >= 64 || actor.level().getRecipeManager().byKey(recipe.getId()).orElse(null) != recipe) return;
        ItemStack result = developing.getResultItem(actor.level().registryAccess());
        if (!(result.getItem() instanceof DevelopedFilmItem) || result.getCount() != 1) return;
        for (ItemStack input : spent) if (input.getCount() == 1 && input.getItem() instanceof FilmItem raw
                && !(input.getItem() instanceof DevelopedFilmItem) && developing.getSourceIngredient().test(input)) {
            List<Frame> frames = raw.getStoredFrames(input);
            if (!frames.isEmpty() && frames.size() <= 256 && frames.stream().allMatch(frame -> frame.identifier().getId().filter(id -> !id.isBlank()).isPresent()))
                click.pending.add(new Developed(result.getItem(), 1, List.copyOf(frames), output));
            return;
        }
    }
    public static void end(ServerPlayer actor, AbstractContainerMenu menu) {
        Click click = CURRENT.get(); CURRENT.remove();
        if (click == null || click.actor != actor || click.menu != menu || actor.containerMenu != menu || HomeInteractionProof.actor(actor) == null) return;
        List<ItemStack> after = held(actor, menu);
        List<Developed> allocated = new ArrayList<>();
        for (Developed result : click.pending) {
            int gain = count(after, result) - count(click.before, result)
                    - allocated.stream().filter(previous -> previous.item == result.item && previous.frames.equals(result.frames)).mapToInt(Developed::count).sum();
            boolean nativeDrop = !result.nativeOutput.isEmpty() && click.drops.stream().anyMatch(drop -> !drop.isRemoved()
                    && drop.getItem() == result.nativeOutput && drop.getItem().getCount() == result.count);
            if (gain < result.count && !nativeDrop) continue;
            for (Frame frame : result.frames) PhotoDevelopmentOrigins.get(actor.serverLevel()).developed(actor.getUUID(), frame);
            allocated.add(result);
        }
    }
    public static void inserted(Entity entity, boolean accepted) {
        Click click = CURRENT.get();
        if (accepted && click != null && entity instanceof ItemEntity drop && drop.level() == click.actor.level()
                && click.drops.size() < 64 && click.pending.stream().anyMatch(result -> drop.getItem() == result.nativeOutput)) click.drops.add(drop);
    }
    private static int count(List<ItemStack> stacks, Developed expected) {
        int total = 0;
        for (ItemStack stack : stacks) if (stack.is(expected.item) && stack.getItem() instanceof DevelopedFilmItem film
                && film.getStoredFrames(stack).equals(expected.frames)) total += stack.getCount();
        return total;
    }
    private static List<ItemStack> held(ServerPlayer player, AbstractContainerMenu menu) {
        List<ItemStack> held = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) held.add(player.getInventory().getItem(slot));
        held.add(menu.getCarried());
        return held;
    }
    public static void clear() { CURRENT.remove(); }
    private PhotoCraftProof() {}
}
