package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityUnderminer", remap = false)
public abstract class WildlifeQuestUnderminerMixin {
    @Shadow private ItemStack lastGivenStack;
    @Shadow public abstract BlockPos getMiningPos();
    @Unique private UUID jem$thrower;
    @Unique private ItemStack jem$sample = ItemStack.EMPTY;
    @Inject(method = "m_7581_", at = @At("HEAD"))
    private void jem$before(ItemEntity drop, CallbackInfo callback) {
        var actor = drop.getOwner() instanceof ServerPlayer player ? WildlifeQuestProof.real(player) : null;
        jem$thrower = actor == null ? null : actor.getUUID(); jem$sample = drop.getItem().copy();
    }
    @Inject(method = "m_7581_", at = @At("RETURN"))
    private void jem$accepted(ItemEntity drop, CallbackInfo callback) {
        Entity self = (Entity)(Object)this;
        if (jem$thrower != null && drop.isRemoved() && lastGivenStack != null && ItemStack.isSameItemSameTags(jem$sample, lastGivenStack)) {
            WildlifeQuestProof.mark(self, "ore_sample_actor", jem$thrower);
            WildlifeQuestProof.data(self).put("ore_sample", lastGivenStack.copyWithCount(1).save(new CompoundTag()));
            WildlifeQuestProof.data(self).put("ore_candidates", new ListTag());
        }
        jem$thrower = null; jem$sample = ItemStack.EMPTY;
    }
    @Inject(method = "getNearbyObscuredOres", at = @At("HEAD"))
    private void jem$search(int range, int maximum, CallbackInfoReturnable<java.util.List<BlockPos>> callback) {
        WildlifeQuestProof.data((Entity)(Object)this).put("ore_candidates", new ListTag());
    }
    @Inject(method = "getObscuringBlockOf", at = @At("RETURN"))
    private void jem$nativeTarget(BlockPos ore, CallbackInfoReturnable<BlockPos> callback) {
        Entity self = (Entity)(Object)this; BlockPos surface = callback.getReturnValue();
        if (surface == null || lastGivenStack == null || self.level().getBlockState(ore).getBlock().asItem() != lastGivenStack.getItem()) return;
        var proof = WildlifeQuestProof.data(self); ListTag candidates = proof.getList("ore_candidates", 10);
        if (candidates.size() < 8) { CompoundTag row = new CompoundTag(); row.putLong("ore", ore.asLong()); row.putLong("surface", surface.asLong()); candidates.add(row); proof.put("ore_candidates", candidates); }
    }
    @Inject(method = "setMiningProgress", at = @At("RETURN"))
    private void jem$revealed(float progress, CallbackInfo callback) {
        Entity self = (Entity)(Object)this; var actor = WildlifeQuestProof.actor(self, "ore_sample_actor"); var proof = WildlifeQuestProof.data(self);
        if (actor == null || progress <= 0 || getMiningPos() == null || lastGivenStack == null
                || !ItemStack.isSameItemSameTags(lastGivenStack, ItemStack.of(proof.getCompound("ore_sample")))) return;
        ListTag candidates = proof.getList("ore_candidates", 10);
        for (int i = 0; i < candidates.size(); i++) {
            CompoundTag row = candidates.getCompound(i);
            if (row.getLong("surface") == getMiningPos().asLong() && self.level().getBlockState(BlockPos.of(row.getLong("ore"))).getBlock().asItem() == lastGivenStack.getItem()) {
                WildlifeQuestProof.emit(actor, "v3_fauna_prospecting"); break;
            }
        }
    }
}
