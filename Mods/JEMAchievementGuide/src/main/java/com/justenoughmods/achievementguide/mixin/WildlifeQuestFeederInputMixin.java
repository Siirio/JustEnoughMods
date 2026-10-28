package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestSetups;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.block.BlockHummingbirdFeeder", remap = false)
public abstract class WildlifeQuestFeederInputMixin {
    @Unique private final ThreadLocal<ItemStack> jem$input = new ThreadLocal<>();
    @Inject(method = "m_6227_", at = @At("HEAD"))
    private void jem$before(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) { jem$input.set(player.getItemInHand(hand).copy()); }
    @Inject(method = "m_6227_", at = @At("RETURN"))
    private void jem$accepted(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        ItemStack input = jem$input.get(); jem$input.remove(); var actor = WildlifeQuestProof.real(player);
        if (actor == null || !(level instanceof ServerLevel server) || input == null || !callback.getReturnValue().consumesAction()
                || !WildlifeQuestProof.debited(input, player.getItemInHand(hand), 1)) return;
        int before = WildlifeQuestSetups.property(state, "contents"), after = WildlifeQuestSetups.property(level.getBlockState(pos), "contents");
        if (before < 0 || before == after || after < 1 || after > 3 || !level.getBlockState(pos).is(state.getBlock())) return;
        var store = WildlifeQuestSetups.get(server); var proof = store.at(server, pos);
        if (!proof.hasUUID("actor") || !proof.getUUID("actor").equals(actor.getUUID()) || before == 0) proof = new CompoundTag();
        int gained = after & ~before;
        proof.putUUID("actor", actor.getUUID()); proof.putInt("paid_contents", proof.getInt("paid_contents") | gained); proof.putString("kind", "feeder");
        store.put(server, pos, proof);
    }
}
