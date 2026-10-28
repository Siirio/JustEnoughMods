package com.justenoughmods.achievementguide.integration.wildlife;
import com.github.alexthe666.alexsmobs.entity.EntityCachalotWhale;
import com.justenoughmods.achievementguide.integration.food.FoodDroppedProof;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
public final class WildlifeQuestWorldChanges {
    public record Change(BlockPos pos, BlockState before, BlockState after) {}
    public record Crystal(BlockPos root, BlockPos pos) {}
    public static final class Run {
        public final ServerPlayer actor; public final InteractionHand hand; public final ItemStack input; public final BlockPos broken; public final CompoundTag proof;
        public final List<Change> changes = new ArrayList<>(); public final List<Crystal> crystals = new ArrayList<>(); public final List<ItemEntity> drops = new ArrayList<>();
        public Run(ServerPlayer actor, InteractionHand hand, BlockPos broken) { this.actor = actor; this.hand = hand; input = actor.getItemInHand(hand).copy(); this.broken = broken; proof = broken == null ? new CompoundTag() : WildlifeQuestSetups.get(actor.serverLevel()).at(actor.serverLevel(), broken); }
    }
    private static final ThreadLocal<Deque<Run>> CURRENT = ThreadLocal.withInitial(ArrayDeque::new);
    private WildlifeQuestWorldChanges() {}
    public static Run begin(ServerPlayer actor, InteractionHand hand, BlockPos broken) {
        if (WildlifeQuestProof.real(actor) == null) return null; Run run = new Run(actor, hand, broken); CURRENT.get().push(run); return run;
    }
    public static void changed(ServerLevel level, BlockPos pos, BlockState before, BlockState after) {
        Run run = CURRENT.get().peek(); if (run != null && run.actor.serverLevel() == level && run.changes.size() < 256 && before != after) run.changes.add(new Change(pos.immutable(), before, after));
    }
    public static void inserted(Entity entity, boolean accepted) { Run run = CURRENT.get().peek(); if (run != null && accepted && entity instanceof ItemEntity drop && run.drops.size() < 128) run.drops.add(drop); }
    public static void converted(net.minecraft.world.level.Level world, BlockPos root, BlockPos pos) {
        if (!(world instanceof ServerLevel level)) return; Run run = CURRENT.get().peek();
        var proof = WildlifeQuestSetups.get(level).at(level, root);
        if (proof.hasUUID("actor")) { CompoundTag receipt = proof.copy(); receipt.putLong("slug_source", root.asLong()); receipt.putString("block", "alexsmobs:crystalized_banana_slug_mucus"); WildlifeQuestSetups.get(level).put(level, pos, receipt); }
        else if (run != null && run.actor.serverLevel() == level && WildlifeQuestProof.item(run.input).equals("alexsmobs:banana_slug_slime_block") && run.crystals.size() < 128) run.crystals.add(new Crystal(root.immutable(), pos.immutable()));
    }
    public static void end(Run run, boolean accepted) {
        if (run == null) return; var stack = CURRENT.get(); if (stack.peek() != run) return; stack.pop(); if (stack.isEmpty()) CURRENT.remove();
        if (!accepted || WildlifeQuestProof.real(run.actor) == null) return;
        ServerLevel level = run.actor.serverLevel(); var setups = WildlifeQuestSetups.get(level);
        boolean paid = WildlifeQuestProof.debited(run.input, run.actor.getItemInHand(run.hand), 1);
        for (Change change : run.changes) {
            BlockState actual = level.getBlockState(change.pos()); if (!actual.equals(change.after())) continue;
            String block = WildlifeQuestSetups.block(actual);
            if (paid && change.before().getBlock() != actual.getBlock() && WildlifeQuestProof.item(run.input).equals(block)
                    && Set.of("alexsmobs:banana_slug_slime_block", "alexsmobs:gustmaker", "alexsmobs:sculk_boomer").contains(block)) {
                CompoundTag proof = new CompoundTag(); proof.putUUID("actor", run.actor.getUUID()); proof.putString("block", block); setups.put(level, change.pos(), proof);
            }
            if (block.equals("alexsmobs:sculk_boomer") && actual.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)
                    && change.before().getBlock() == actual.getBlock() && change.before().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED) && !change.before().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED) && actual.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)) {
                var proof = setups.at(level, change.pos());
                if (proof.hasUUID("actor") && run.actor.getUUID().equals(proof.getUUID("actor")) && proof.getBoolean("boomer_effect")) { proof.putUUID("boomer_disabled_actor", run.actor.getUUID()); setups.put(level, change.pos(), proof); }
            }
            boolean water = actual.getFluidState().is(FluidTags.WATER) && !change.before().getFluidState().is(FluidTags.WATER);
            boolean opened = !change.before().getCollisionShape(level, change.pos()).isEmpty() && actual.getCollisionShape(level, change.pos()).isEmpty();
            if (water || opened) for (EntityCachalotWhale whale : level.getEntitiesOfClass(EntityCachalotWhale.class, new AABB(change.pos()).inflate(16), w -> w.isBeached() && w.getBoundingBox().inflate(1).intersects(new AABB(change.pos())))) {
                var proof = WildlifeQuestProof.data(whale);
                if (!WildlifeQuestWhales.attacked(whale, run.actor.getUUID())) { proof.putUUID("rescue_actor", run.actor.getUUID()); proof.putLong("rescue_time", level.getGameTime()); proof.putLong("rescue_change", change.pos().asLong()); proof.put("rescue_state", net.minecraft.nbt.NbtUtils.writeBlockState(actual)); }
            }
        }
        for (Crystal crystal : run.crystals) {
            var source = setups.at(level, crystal.root());
            if (source.hasUUID("actor") && run.actor.getUUID().equals(source.getUUID("actor")) && WildlifeQuestSetups.block(level.getBlockState(crystal.pos())).equals("alexsmobs:crystalized_banana_slug_mucus")) {
                var receipt = source.copy(); receipt.putLong("slug_source", crystal.root().asLong()); receipt.putString("block", "alexsmobs:crystalized_banana_slug_mucus"); setups.put(level, crystal.pos(), receipt);
            }
        }
        if (run.broken != null && run.proof.hasUUID("actor") && run.actor.getUUID().equals(run.proof.getUUID("actor")) && run.proof.contains("slug_source")
                && !WildlifeQuestSetups.block(level.getBlockState(run.broken)).equals("alexsmobs:crystalized_banana_slug_mucus") && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, run.input) > 0) {
            for (ItemEntity drop : run.drops) if (level.getEntity(drop.getUUID()) == drop && WildlifeQuestProof.item(drop.getItem()).equals("alexsmobs:crystalized_banana_slug_mucus")) FoodDroppedProof.update(drop, "v3_fauna_crystal_water", drop.getItem().getCount(), run.actor.getUUID());
        }
    }
}
