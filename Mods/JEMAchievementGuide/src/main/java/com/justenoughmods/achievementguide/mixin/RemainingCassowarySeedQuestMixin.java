package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.food.NativeHarvestProof;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.primal.entity.ai.behavior.cassowary.CassowaryPickFruit", remap = false)
public abstract class RemainingCassowarySeedQuestMixin {
    @Shadow public ItemEntity pendingPickup;
    @Shadow private int timer;
    @Unique private NativeHarvestProof.Run jem$seeds;
    @Unique private ItemStack jem$fruit;
    @Unique private int jem$count;
    @Inject(method = "tick", at = @At("HEAD"))
    private void jem$begin(ServerLevel level, @Coerce LivingEntity cassowary, long time, CallbackInfo callback) {
        jem$seeds = null; jem$fruit = null;
        if (timer != 29 || pendingPickup == null || pendingPickup.isRemoved()
                || !RemainingFoodActors.item(pendingPickup.getItem()).equals("primal:petrified_fruit")) return;
        var thrower = ((RemainingFoodThrowerAccess)pendingPickup).jem$nativeThrower();
        var actor = thrower == null ? null : level.getServer().getPlayerList().getPlayer(thrower);
        if (RemainingFoodActors.real(actor) == null || actor.level() != level) return;
        jem$fruit = pendingPickup.getItem(); jem$count = jem$fruit.getCount();
        jem$seeds = NativeHarvestProof.begin(actor, cassowary.blockPosition(), Blocks.AIR.defaultBlockState(), "cassowary_seed");
    }
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/behavior/BehaviorUtils;m_22613_(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)V"))
    private void jem$nativeSeeds(LivingEntity cassowary, ItemStack seeds, Vec3 target) {
        var run = jem$seeds;
        if (run != null) NativeHarvestProof.expect(run.level, run.pos, seeds);
        try { BehaviorUtils.throwItem(cassowary, seeds, target); }
        finally { NativeHarvestProof.expectedEnd(seeds); }
    }
    @Inject(method = "tick", at = @At("RETURN"))
    private void jem$end(ServerLevel level, @Coerce LivingEntity cassowary, long time, CallbackInfo callback) {
        var run = jem$seeds; jem$seeds = null; NativeHarvestProof.end(run);
        if (run != null && jem$fruit != null && jem$count - jem$fruit.getCount() == 1) {
            for (ItemEntity output : run.drops) {
                if (Set.of("primal:litchi_seeds", "primal:kiwano_seeds", "primal:starfruit_seeds").contains(RemainingFoodActors.item(output.getItem()))) {
                    NativeFoodSources.get(level).certify(level, run.actor.getUUID(), "cassowary_seed", output.getItem(), output.getItem().getCount());
                }
            }
        }
        jem$fruit = null;
    }
}
