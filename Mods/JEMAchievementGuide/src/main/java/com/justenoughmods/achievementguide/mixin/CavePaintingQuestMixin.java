package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayDeque;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.block.SmoothLimestoneBlock", remap = false)
public abstract class CavePaintingQuestMixin {
    @Unique private static final TagKey<Block> jem$surfaces = TagKey.create(Registries.BLOCK, new ResourceLocation("alexscaves", "turns_into_cave_paintings"));
    @Unique private static final TagKey<Block> jem$paintings = TagKey.create(Registries.BLOCK, new ResourceLocation("alexscaves", "cave_paintings"));
    @Unique private static final ThreadLocal<ArrayDeque<Map.Entry<BlockState, Integer>>> jem$calls = ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "m_6227_", at = @At("HEAD"))
    private void jem$beforePainting(BlockState state, Level level, BlockPos pos, Player player,
                                   InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (player instanceof ServerPlayer) {
            var held = player.getItemInHand(hand);
            jem$calls.get().push(Map.entry(level.getBlockState(pos), held.is(Items.CHARCOAL) ? held.getCount() : 0));
        }
    }

    @Inject(method = "m_6227_", at = @At("RETURN"))
    private void jem$afterPainting(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (!(player instanceof ServerPlayer actor)) return;
        var calls = jem$calls.get();
        Map.Entry<BlockState, Integer> attempt = calls.isEmpty() ? null : calls.pop();
        if (calls.isEmpty()) jem$calls.remove();
        if (attempt == null || attempt.getValue() <= 0 || !attempt.getKey().is(jem$surfaces)) return;
        var held = actor.getItemInHand(hand);
        BlockState after = level.getBlockState(pos);
        if (callback.getReturnValue().consumesAction() && attempt.getKey() != after && after.is(jem$paintings)
                && (held.isEmpty() || held.is(Items.CHARCOAL)) && attempt.getValue() - held.getCount() == 1) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_cave_cave_painting");
        }
    }
}
