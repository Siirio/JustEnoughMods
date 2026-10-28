package com.justenoughmods.achievementguide.integration.magiclast;
import java.util.ArrayDeque;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
public final class MagicLastWorldProof {
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);
    private static final class State { final ArrayDeque<Block> blocks = new ArrayDeque<>(); final ArrayDeque<Move> moves = new ArrayDeque<>(); int blockOverflow, moveOverflow; }
    private record Block(Level level, BlockPos pos, BlockState before) {}
    private record Move(Entity actor, Level level, Vec3 before) {}
    private MagicLastWorldProof() {}
    public static void blockBegin(Level level, BlockPos pos) { State state = STATE.get(); if (state.blockOverflow > 0 || state.blocks.size() >= MagicLastProof.LIMIT) state.blockOverflow++; else state.blocks.push(new Block(level, pos.immutable(), level.hasChunkAt(pos) ? level.getBlockState(pos) : null)); }
    public static void blockEnd(Level level, BlockPos pos, boolean accepted) { State state = STATE.get(); if (state.blockOverflow > 0) { state.blockOverflow--; return; } Block run = state.blocks.isEmpty() ? null : state.blocks.pop(); clean(state); if (accepted && run != null && run.level == level && run.pos.equals(pos) && run.before != null && level.hasChunkAt(pos) && !run.before.equals(level.getBlockState(pos))) { MagicLastBloodProof.changed(level, pos); MagicLastSpellProof.blockOutcome(level); } }
    public static void moveBegin(Entity actor) { State state = STATE.get(); if (state.moveOverflow > 0 || state.moves.size() >= MagicLastProof.LIMIT) state.moveOverflow++; else state.moves.push(new Move(actor, actor.level(), actor.position())); }
    public static void moveEnd(Entity actor) { State state = STATE.get(); if (state.moveOverflow > 0) { state.moveOverflow--; return; } Move run = state.moves.isEmpty() ? null : state.moves.pop(); clean(state); if (run != null && run.actor == actor && (run.level != actor.level() || actor.position().distanceToSqr(run.before) > 0.000001)) MagicLastSpellProof.outcome(actor); }
    public static void clear() { STATE.remove(); }
    private static void clean(State state) { if (state.blocks.isEmpty() && state.moves.isEmpty()) STATE.remove(); }
}
