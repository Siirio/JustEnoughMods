package com.justenoughmods.achievementguide.integration.magiclast;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.redspace.ironsspellbooks.api.backwards_compat.FluidHelper;
import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.BrewAlchemistCauldronRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
/** Original native input/payment and fluid conservation through genuine native bottling. */
public final class MagicLastCauldronProof {
    private static final String ROOT = "JemQuestAlchemyV4";
    private static final Map<Object, AlchemistCauldronTile> OWNERS = new IdentityHashMap<>();
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);
    private static final class State { final ArrayDeque<Use> uses = new ArrayDeque<>(); final ArrayDeque<Melt> melts = new ArrayDeque<>(); int useOverflow, meltOverflow; }
    private static final class Use {
        final AlchemistCauldronTile owner; final ServerPlayer actor; final ItemStack source, before; final List<ItemStack> inputs = new ArrayList<>(); final List<FluidStack> fluids; final ListTag proof;
        final List<ItemEntity> drops = new ArrayList<>(); ItemStack output = ItemStack.EMPTY; int held;
        Use(AlchemistCauldronTile owner, Player user, InteractionHand hand) { this.owner = owner; actor = MagicLastProof.real(user) && user.level() == owner.getLevel() ? (ServerPlayer)user : null; source = user.getItemInHand(hand); before = source.copy(); for (ItemStack input : owner.inputItems) inputs.add(input.copy()); fluids = fluids(owner); proof = owner.getPersistentData().getCompound(ROOT).getList("fluids", Tag.TAG_COMPOUND).copy(); }
    }
    private static final class Melt {
        final AlchemistCauldronTile owner; final ItemStack source, before; final List<FluidStack> fluids; final ListTag proof; final UUID actor; final List<BrewAlchemistCauldronRecipe> recipes = new ArrayList<>();
        Melt(AlchemistCauldronTile owner, ItemStack input) { this.owner = owner; source = input; before = input.copy(); fluids = fluids(owner); proof = owner.getPersistentData().getCompound(ROOT).getList("fluids", Tag.TAG_COMPOUND).copy(); UUID user = null; for (int slot = 0; slot < Math.min(4, owner.inputItems.size()); slot++) if (owner.inputItems.get(slot) == input) { CompoundTag row = owner.getPersistentData().getCompound(ROOT).getCompound("input" + slot); if (row.hasUUID("actor") && input.save(new CompoundTag()).equals(row.getCompound("item"))) user = row.getUUID("actor"); } actor = user; }
    }
    private MagicLastCauldronProof() {}
    public static void register(AlchemistCauldronTile owner) { if (OWNERS.size() >= 256 && !OWNERS.containsKey(owner.fluidInventory)) { for (AlchemistCauldronTile old : OWNERS.values()) old.getPersistentData().getCompound(ROOT).remove("fluids"); OWNERS.clear(); } OWNERS.put(owner.fluidInventory, owner); }
    private static List<FluidStack> fluids(AlchemistCauldronTile owner) { List<FluidStack> result = new ArrayList<>(); for (FluidStack fluid : owner.fluidInventory.fluids()) { if (result.size() >= 4) return List.of(); result.add(fluid.copy()); } return result; }
    private static boolean same(FluidStack a, FluidStack b) { return FluidHelper.isSameFluidSameComponents(a, b); }
    private static int amount(List<FluidStack> fluids, FluidStack matching) { int result = 0; for (FluidStack fluid : fluids) if (same(fluid, matching)) result += fluid.getAmount(); return result; }
    private static boolean elixir(FluidStack fluid) { var id = BuiltInRegistries.FLUID.getKey(fluid.getFluid()); return id.getNamespace().equals("irons_spellbooks") && id.getPath().endsWith("_elixir"); }
    private static ListTag adjusted(ListTag rows, List<FluidStack> before, List<FluidStack> after) {
        ListTag result = new ListTag();
        for (int index = 0; index < Math.min(4, rows.size()); index++) { CompoundTag row = rows.getCompound(index).copy(); FluidStack fluid = FluidStack.loadFluidStackFromNBT(row.getCompound("fluid")); int old = amount(before, fluid), actual = amount(after, fluid), known = row.getInt("known"); if (old != row.getInt("total") || known <= 0 || known > old || actual <= 0) continue; row.putInt("known", Math.min(known, actual)); row.putInt("total", actual); result.add(row); }
        return result;
    }
    private static void save(AlchemistCauldronTile owner, ListTag rows) { CompoundTag root = owner.getPersistentData().getCompound(ROOT); root.put("fluids", rows); owner.getPersistentData().put(ROOT, root); owner.setChanged(); }
    public static void useBegin(AlchemistCauldronTile owner, Player actor, InteractionHand hand) { register(owner); State state = STATE.get(); if (state.useOverflow > 0 || state.uses.size() >= MagicLastProof.LIMIT) state.useOverflow++; else state.uses.push(new Use(owner, actor, hand)); }
    public static void result(ItemStack output) { State state = STATE.get(); if (state.useOverflow > 0 || state.uses.isEmpty()) return; Use run = state.uses.peek(); run.output = output.copy(); run.held = run.actor == null ? 0 : MagicLastProof.held(run.actor, output); }
    public static void spawned(Entity entity, boolean accepted) { State state = STATE.get(); if (!accepted || !(entity instanceof ItemEntity drop) || state.useOverflow > 0 || state.uses.isEmpty()) return; Use run = state.uses.peek(); if (!run.output.isEmpty() && ItemStack.isSameItemSameTags(run.output, drop.getItem()) && run.drops.size() < MagicLastProof.LIMIT) run.drops.add(drop); }
    public static void useEnd(boolean accepted) {
        State state = STATE.get(); if (state.useOverflow > 0) { state.useOverflow--; return; } Use run = state.uses.isEmpty() ? null : state.uses.pop(); clean(state); if (run == null || run.actor == null || !accepted || !MagicLastProof.real(run.actor)) return;
        List<FluidStack> after = fluids(run.owner); ListTag retained = adjusted(run.proof, run.fluids, after); save(run.owner, retained);
        if (run.before.getCount() - run.source.getCount() == 1) for (int slot = 0; slot < Math.min(4, run.inputs.size()); slot++) if (run.inputs.get(slot).isEmpty() && run.owner.inputItems.get(slot).getCount() == 1 && ItemStack.isSameItemSameTags(run.before, run.owner.inputItems.get(slot))) { CompoundTag row = new CompoundTag(); row.putUUID("actor", run.actor.getUUID()); row.put("item", run.owner.inputItems.get(slot).save(new CompoundTag())); CompoundTag root = run.owner.getPersistentData().getCompound(ROOT); root.put("input" + slot, row); run.owner.getPersistentData().put(ROOT, root); run.owner.setChanged(); }
        if (!run.before.is(Items.GLASS_BOTTLE) || run.before.getCount() - run.source.getCount() != 1 || run.output.getCount() != 1 || !MagicLastProof.item(run.output).endsWith("_elixir")) return;
        boolean certified = false;
        for (int index = 0; index < Math.min(4, run.proof.size()); index++) { CompoundTag row = run.proof.getCompound(index); FluidStack fluid = FluidStack.loadFluidStackFromNBT(row.getCompound("fluid")); int old = amount(run.fluids, fluid), used = old - amount(after, fluid); if (elixir(fluid) && row.hasUUID("actor") && row.getUUID("actor").equals(run.actor.getUUID()) && old == row.getInt("total") && used > 0 && used <= row.getInt("known") && old == row.getInt("known")) certified = true; }
        if (!certified) return;
        if (MagicLastProof.held(run.actor, run.output) - run.held == 1) { MagicLastProof.emit(run.actor, "spell_cauldron_native_brew"); MagicLastProof.emit(run.actor, "spell_cauldron_brew"); }
        else if (run.drops.size() == 1 && run.drops.get(0).getItem().getCount() == 1) MagicLastOutputProof.certify(run.drops.get(0), run.actor, "spell_cauldron_brew");
    }
    public static void meltBegin(AlchemistCauldronTile owner, ItemStack input) { register(owner); State state = STATE.get(); if (state.meltOverflow > 0 || state.melts.size() >= MagicLastProof.LIMIT) state.meltOverflow++; else state.melts.push(new Melt(owner, input)); }
    public static void selected(Object recipe) { State state = STATE.get(); if (state.meltOverflow == 0 && !state.melts.isEmpty() && recipe instanceof BrewAlchemistCauldronRecipe nativeRecipe && state.melts.peek().recipes.size() < 4) state.melts.peek().recipes.add(nativeRecipe); }
    public static void meltEnd() {
        State state = STATE.get(); if (state.meltOverflow > 0) { state.meltOverflow--; return; } Melt run = state.melts.isEmpty() ? null : state.melts.pop(); clean(state); if (run == null) return;
        List<FluidStack> after = fluids(run.owner); ListTag rows = adjusted(run.proof, run.fluids, after);
        if (run.actor != null && run.before.getCount() - run.source.getCount() == 1 && run.owner.isBoiling(run.owner.getBlockState())) for (FluidStack fluid : after) {
            int added = fluid.getAmount() - amount(run.fluids, fluid); boolean nativeOutput = false;
            for (BrewAlchemistCauldronRecipe recipe : run.recipes) for (FluidStack result : recipe.results()) if (same(fluid, result) && result.getAmount() > 0) nativeOutput = true;
            if (!elixir(fluid) || !nativeOutput || added <= 0) continue;
            int known = added;
            for (int index = rows.size() - 1; index >= 0; index--) { CompoundTag old = rows.getCompound(index); if (same(fluid, FluidStack.loadFluidStackFromNBT(old.getCompound("fluid")))) { if (old.hasUUID("actor") && old.getUUID("actor").equals(run.actor)) known += old.getInt("known"); rows.remove(index); } }
            if (run.owner.getLevel() != null && run.owner.getLevel().getServer() != null) { ServerPlayer actor = run.owner.getLevel().getServer().getPlayerList().getPlayer(run.actor); if (actor != null) MagicLastProof.emit(actor, "spell_cauldron_native_brew"); }
            if (rows.size() < 4) { CompoundTag row = new CompoundTag(); row.putUUID("actor", run.actor); row.put("fluid", fluid.writeToNBT(new CompoundTag())); row.putInt("known", Math.min(known, fluid.getAmount())); row.putInt("total", fluid.getAmount()); rows.add(row); }
        }
        save(run.owner, rows);
    }
    public static void changed(Object handler, boolean committed) { if (!committed) return; AlchemistCauldronTile owner = OWNERS.get(handler); if (owner == null) return; State state = STATE.get(); if (state.useOverflow == 0 && !state.uses.isEmpty() && state.uses.peek().owner == owner || state.meltOverflow == 0 && !state.melts.isEmpty() && state.melts.peek().owner == owner) return; if (owner.getPersistentData().getCompound(ROOT).contains("fluids")) { owner.getPersistentData().getCompound(ROOT).remove("fluids"); owner.setChanged(); } }
    public static void inputChanged(AlchemistCauldronTile owner, int slot) { owner.getPersistentData().getCompound(ROOT).remove("input" + slot); }
    public static void clear() { STATE.remove(); OWNERS.clear(); }
    private static void clean(State state) { if (state.uses.isEmpty() && state.melts.isEmpty()) STATE.remove(); }
}
