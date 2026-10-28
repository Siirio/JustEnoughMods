package com.justenoughmods.achievementguide.mixin;
import com.github.alexthe666.alexsmobs.entity.EntityMungus;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityMungus", remap = false)
public abstract class WildlifeQuestMungusBiomeMixin {
    @Unique private ItemStack jem$food;
    @Unique private int jem$count;
    @Unique private List<Holder<Biome>> jem$biomes;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$beforeFeed(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) { jem$food = player.getItemInHand(hand).copy(); jem$count = ((EntityMungus)(Object)this).getMushroomCount(); }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$fed(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        EntityMungus self = (EntityMungus)(Object)this; var actor = WildlifeQuestProof.real(player); var proof = WildlifeQuestProof.data(self);
        if (actor == null || !callback.getReturnValue().consumesAction() || self.getMushroomCount() != jem$count + 1 || self.getMushroomState() == null || !WildlifeQuestProof.debited(jem$food, player.getItemInHand(hand), 1)) return;
        if (jem$count == 0) { proof.putUUID("mungus_feeder", actor.getUUID()); proof.putInt("mungus_paid", 0); proof.put("mungus_mushroom", NbtUtils.writeBlockState(self.getMushroomState())); }
        if (WildlifeQuestProof.matches(self, "mungus_feeder", actor.getUUID()) && proof.getInt("mungus_paid") == jem$count && proof.getCompound("mungus_mushroom").equals(NbtUtils.writeBlockState(self.getMushroomState()))) proof.putInt("mungus_paid", jem$count + 1);
        else { proof.remove("mungus_feeder"); proof.remove("mungus_paid"); }
    }
    @Inject(method = "transformBiome", at = @At("HEAD"))
    private void jem$beforeBiome(BlockPos pos, Holder<Biome> biome, CallbackInfo callback) {
        EntityMungus self = (EntityMungus)(Object)this;
        jem$biomes = !self.isAlive() && !self.isBaby() && !self.isReverting() && self.getMushroomCount() == 5 && WildlifeQuestProof.data(self).getInt("mungus_paid") == 5 ? WildlifeQuestBiome.snapshot(self) : List.of();
    }
    @Inject(method = "transformBiome", at = @At("RETURN"))
    private void jem$changed(BlockPos pos, Holder<Biome> biome, CallbackInfo callback) {
        EntityMungus self = (EntityMungus)(Object)this; var actor = WildlifeQuestProof.actor(self, "mungus_feeder"); if (actor == null || jem$biomes == null || jem$biomes.size() != 64 || biome == null) return;
        var after = WildlifeQuestBiome.snapshot(self);
        if (!after.equals(jem$biomes) && after.size() == 64 && after.stream().allMatch(biome::equals)) WildlifeQuestProof.emit(actor, "v3_fauna_fungal_landscape");
    }
}
