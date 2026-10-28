package com.justenoughmods.achievementguide.integration.magiclast;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
/** Exact upstream heated BloodCauldron production and later native blood-vial receipt. */
public final class MagicLastBloodProof {
    private static final Map<ServerLevel, LinkedHashMap<Long, Stock>> STOCK = new WeakHashMap<>();
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);
    private static final class State { final ArrayDeque<Cook> cooks = new ArrayDeque<>(); final ArrayDeque<Take> takes = new ArrayDeque<>(); int cookOverflow, takeOverflow; }
    private record Stock(BlockState state, int known) {}
    private record Cook(Level level, BlockPos pos, Entity source, float vitality, BlockState before, Stock stock) {}
    private static final class Take {
        final ServerPlayer actor; final Level level; final BlockPos pos; final ItemStack input; final int bottles, held, before; final Stock stock; final List<ItemEntity> drops = new ArrayList<>();
        Take(Level level, BlockPos pos, Player actor, ItemStack input) { this.level = level; this.pos = pos.immutable(); this.actor = MagicLastProof.real(actor) && actor.level() == level && input.is(Items.GLASS_BOTTLE) ? (ServerPlayer)actor : null; this.input = input; bottles = input.getCount(); held = this.actor == null ? 0 : MagicLastProof.held(this.actor, new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.BLOOD_VIAL.get())); before = amount(level.getBlockState(pos)); Stock row = get(level, pos); stock = row != null && row.state.equals(level.getBlockState(pos)) ? row : null; }
    }
    private MagicLastBloodProof() {}
    private static int amount(BlockState state) { return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals("irons_spellbooks:blood_cauldron") && state.hasProperty(LayeredCauldronBlock.LEVEL) ? state.getValue(LayeredCauldronBlock.LEVEL) : 0; }
    private static Stock get(Level level, BlockPos pos) { return level instanceof ServerLevel server && STOCK.containsKey(server) ? STOCK.get(server).get(pos.asLong()) : null; }
    private static void put(Level world, BlockPos pos, Stock proof) { if (!(world instanceof ServerLevel level)) return; LinkedHashMap<Long, Stock> cells = STOCK.computeIfAbsent(level, ignored -> new LinkedHashMap<>()); if (cells.size() >= 256 && !cells.containsKey(pos.asLong())) cells.remove(cells.keySet().iterator().next()); if (proof.known > 0) cells.put(pos.asLong(), proof); else cells.remove(pos.asLong()); }
    public static void cookBegin(Level level, BlockPos pos, Entity source) { State state = STATE.get(); if (state.cookOverflow > 0 || state.cooks.size() >= MagicLastProof.LIMIT) state.cookOverflow++; else state.cooks.push(new Cook(level, pos.immutable(), source, source instanceof LivingEntity living ? living.getHealth() + living.getAbsorptionAmount() : Float.NaN, level.getBlockState(pos), get(level, pos))); }
    public static void cookEnd() { State state = STATE.get(); if (state.cookOverflow > 0) { state.cookOverflow--; return; } Cook run = state.cooks.isEmpty() ? null : state.cooks.pop(); clean(state); if (run == null || !(run.level instanceof ServerLevel) || !(run.source instanceof LivingEntity living) || !(living.getHealth() + living.getAbsorptionAmount() < run.vitality)) return; BlockState after = run.level.getBlockState(run.pos); int gain = amount(after) - amount(run.before); if (gain == 1) put(run.level, run.pos, new Stock(after, Math.min(amount(after), 1 + (run.stock != null && run.stock.state.equals(run.before) ? run.stock.known : 0)))); }
    public static void takeBegin(Level level, BlockPos pos, Player actor, ItemStack input) { State state = STATE.get(); if (state.takeOverflow > 0 || state.takes.size() >= MagicLastProof.LIMIT) state.takeOverflow++; else state.takes.push(new Take(level, pos, actor, input)); }
    public static void spawned(Entity entity, boolean accepted) { State state = STATE.get(); if (!accepted || !(entity instanceof ItemEntity drop) || state.takeOverflow > 0 || state.takes.isEmpty()) return; Take run = state.takes.peek(); if (drop.level() == run.level && MagicLastProof.item(drop.getItem()).equals("irons_spellbooks:blood_vial") && run.drops.size() < MagicLastProof.LIMIT) run.drops.add(drop); }
    public static void takeEnd(boolean accepted) {
        State state = STATE.get(); if (state.takeOverflow > 0) { state.takeOverflow--; return; } Take run = state.takes.isEmpty() ? null : state.takes.pop(); clean(state);
        if (run == null || run.actor == null || !accepted || run.stock == null || run.stock.known <= 0 || run.before - amount(run.level.getBlockState(run.pos)) != 1 || run.bottles - run.input.getCount() != 1) return;
        int certified = Math.max(0, 1 - (run.before - run.stock.known)); put(run.level, run.pos, new Stock(run.level.getBlockState(run.pos), run.stock.known - certified)); if (certified <= 0) return;
        ItemStack output = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.BLOOD_VIAL.get());
        if (MagicLastProof.held(run.actor, output) - run.held == 1) MagicLastProof.emit(run.actor, "v3_magic_focus_blood"); else if (run.drops.size() == 1 && run.drops.get(0).getItem().getCount() == 1) MagicLastOutputProof.certify(run.drops.get(0), run.actor, "v3_magic_focus_blood");
    }
    public static void changed(Level level, BlockPos pos) { State state = STATE.get(); if (state.cookOverflow == 0 && !state.cooks.isEmpty() && state.cooks.peek().level == level && state.cooks.peek().pos.equals(pos) || state.takeOverflow == 0 && !state.takes.isEmpty() && state.takes.peek().level == level && state.takes.peek().pos.equals(pos)) return; if (level instanceof ServerLevel server && STOCK.containsKey(server)) STOCK.get(server).remove(pos.asLong()); }
    public static void clear() { STOCK.clear(); STATE.remove(); }
    private static void clean(State state) { if (state.cooks.isEmpty() && state.takes.isEmpty()) STATE.remove(); }
}
