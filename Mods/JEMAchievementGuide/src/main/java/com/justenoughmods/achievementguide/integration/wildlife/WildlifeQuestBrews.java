package com.justenoughmods.achievementguide.integration.wildlife;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.registries.ForgeRegistries;
public final class WildlifeQuestBrews {
    public record Deposit(ServerPlayer actor, AbstractContainerMenu menu, BrewingStandBlockEntity stand, ItemStack ingredient, List<ItemStack> held) {}
    public record Tick(BrewingStandBlockEntity stand, int time, int fuel, ItemStack powder) {}
    public record Brew(ServerPlayer actor, BrewingStandBlockEntity stand, ItemStack ingredient, List<ItemStack> before, List<ItemStack> expected) {}
    private static final String KEY = "JemWildlifeBrewV4";
    private WildlifeQuestBrews() {}
    public static Deposit begin(ServerPlayer actor, AbstractContainerMenu menu) {
        if (WildlifeQuestProof.real(actor) == null || !(menu instanceof BrewingStandMenu) || menu.slots.isEmpty() || !(menu.slots.get(0).container instanceof BrewingStandBlockEntity stand)) return null;
        return new Deposit(actor, menu, stand, stand.getItem(3).copy(), WildlifeQuestCrafts.held(actor, menu).stream().map(ItemStack::copy).toList());
    }
    private static int count(List<ItemStack> stacks, ItemStack variant) { return stacks.stream().filter(s -> ItemStack.isSameItemSameTags(s, variant)).mapToInt(ItemStack::getCount).sum(); }
    public static void deposited(Deposit run) {
        if (run == null || run.actor.containerMenu != run.menu) return; ItemStack actual = run.stand.getItem(3); if (actual.isEmpty()) return;
        int gain = actual.getCount() - (ItemStack.isSameItemSameTags(run.ingredient, actual) ? run.ingredient.getCount() : 0);
        int loss = count(run.held, actual) - count(WildlifeQuestCrafts.held(run.actor, run.menu), actual);
        if (gain > 0 && gain == loss) NativeFoodSources.get(run.actor.serverLevel()).certify(run.actor.serverLevel(), run.actor.getUUID(), "wildlife_brew_input", actual, gain);
    }
    public static Tick tick(BrewingStandBlockEntity stand) { var tag = stand.saveWithoutMetadata(); return new Tick(stand, tag.getShort("BrewTime"), tag.getByte("Fuel"), stand.getItem(4).copy()); }
    public static void ticked(Tick run) {
        if (run == null || !(run.stand.getLevel() instanceof ServerLevel level)) return; var tag = run.stand.saveWithoutMetadata(); int fuel = tag.getByte("Fuel");
        boolean charged = run.fuel > 0 && fuel == run.fuel - 1 || run.fuel <= 0 && fuel == 19 && run.powder.is(Items.BLAZE_POWDER) && WildlifeQuestProof.debited(run.powder, run.stand.getItem(4), 1);
        if (run.time != 0 || tag.getShort("BrewTime") != 400 || !charged) return;
        var ingredient = run.stand.getItem(3); var sources = NativeFoodSources.get(level); var actor = sources.owner(ingredient, "wildlife_brew_input");
        var proof = new CompoundTag(); if (actor != null) { proof.putUUID("actor", actor); var claims = NativeFoodSources.claims(ingredient); if (!claims.isEmpty()) proof.putUUID("batch", claims.get(0).batch()); }
        run.stand.getPersistentData().put(KEY, proof); run.stand.setChanged();
    }
    public static Brew before(ServerLevel level, BlockPos pos, NonNullList<ItemStack> items) {
        if (!(level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand)) return null; var proof = stand.getPersistentData().getCompound(KEY);
        if (!proof.hasUUID("actor") || !proof.hasUUID("batch")) return null; var actor = WildlifeQuestProof.real(level.getServer().getPlayerList().getPlayer(proof.getUUID("actor")));
        ItemStack ingredient = items.get(3); if (actor == null || !actor.getUUID().equals(NativeFoodSources.get(level).owner(ingredient, "wildlife_brew_input")) || NativeFoodSources.claims(ingredient).stream().noneMatch(c -> c.batch().equals(proof.getUUID("batch")))) return null;
        List<ItemStack> before = new ArrayList<>(), expected = new ArrayList<>(); for (int i = 0; i < 3; i++) { before.add(items.get(i).copy()); expected.add(BrewingRecipeRegistry.getOutput(items.get(i), ingredient).copy()); }
        return new Brew(actor, stand, ingredient.copy(), before, expected);
    }
    public static boolean draught(ItemStack stack) {
        String potion = String.valueOf(ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(stack)));
        return stack.is(Items.POTION) && Set.of("alexsmobs:poison_resistance", "alexsmobs:long_poison_resistance", "alexsmobs:bug_pheromones", "alexsmobs:long_bug_pheromones", "alexsmobs:soulsteal", "alexsmobs:long_soulsteal", "alexsmobs:strong_soulsteal").contains(potion);
    }
    public static void brewed(Brew run, NonNullList<ItemStack> items) {
        if (run == null || !WildlifeQuestProof.debited(run.ingredient, items.get(3), 1)) return;
        List<ItemStack> outputs = new ArrayList<>();
        for (int i = 0; i < 3; i++) if (draught(items.get(i)) && !ItemStack.isSameItemSameTags(items.get(i), run.before.get(i)) && ItemStack.matches(items.get(i), run.expected.get(i))) outputs.add(items.get(i));
        if (outputs.isEmpty() || !NativeFoodSources.get(run.actor.serverLevel()).consume(run.actor.getUUID(), NativeFoodSources.spent(run.ingredient, 1), "wildlife_brew_input", 1)) return;
        for (ItemStack output : outputs) NativeFoodSources.get(run.actor.serverLevel()).certify(run.actor.serverLevel(), run.actor.getUUID(), "wildlife_draught", output, output.getCount());
        run.stand.getPersistentData().remove(KEY); run.stand.setChanged();
    }
}
