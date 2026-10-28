package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareEggs;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareLineage;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareNest.Place;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.primal.block_entity.NestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.primal.block.NestBlock", remap = false)
public abstract class CreatureCareNestEggMixin {
    @Unique private static final ThreadLocal<CreatureCareEggs.Harvest> jem$harvest = new ThreadLocal<>();
    @Unique private static final ThreadLocal<Integer> jem$oldCount = new ThreadLocal<>();
    @Unique private static final ThreadLocal<List<CreatureCareLineage.Egg>> jem$siblings = new ThreadLocal<>();
    @Unique private static final ThreadLocal<Place> jem$place = new ThreadLocal<>();
    @Inject(method = "removeEgg", at = @At("HEAD"))
    private static void jem$removeBefore(LivingEntity entity, Level level, BlockPos pos, BlockState state, boolean pop, CallbackInfoReturnable<Boolean> callback) {
        jem$harvest.remove(); jem$oldCount.remove(); jem$siblings.remove();
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof NestBlockEntity nest
                && CreatureCareProof.item(nest.getEgg()).equals("primal:eagle_egg")) {
            jem$oldCount.set(nest.getEggsAmount());
            jem$siblings.set(CreatureCareLineage.get(server).eggs(server, pos, nest.getEggsAmount()));
            if (entity instanceof Player player && pop) {
                var actor = CreatureCareProof.real(player);
                if (actor != null) jem$harvest.set(CreatureCareEggs.harvest(actor, pos));
            }
        }
    }
    @Inject(method = "removeEgg", at = @At("RETURN"))
    private static void jem$removeAfter(LivingEntity entity, Level level, BlockPos pos, BlockState state, boolean pop, CallbackInfoReturnable<Boolean> callback) {
        var run = jem$harvest.get(); Integer before = jem$oldCount.get(); var siblings = jem$siblings.get();
        jem$harvest.remove(); jem$oldCount.remove(); jem$siblings.remove();
        boolean committed = before != null && callback.getReturnValue() && level.getBlockEntity(pos) instanceof NestBlockEntity nest
                && before - nest.getEggsAmount() == 1;
        CreatureCareEggs.harvested(run, committed);
        if (committed && level instanceof ServerLevel server && siblings != null && !siblings.isEmpty()) {
            CreatureCareLineage.get(server).eggs(server, pos, siblings.subList(1, siblings.size()));
        }
    }
    @Inject(method = "tryPlaceEgg", at = @At("HEAD"))
    private static void jem$placeBefore(LivingEntity entity, Level level, BlockPos pos, BlockState state, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        jem$place.remove();
        if (entity instanceof Player player && level instanceof ServerLevel server && CreatureCareProof.item(stack).equals("primal:eagle_egg")
                && level.getBlockEntity(pos) instanceof NestBlockEntity nest) {
            var actor = CreatureCareProof.real(player);
            if (actor != null) jem$place.set(new Place(actor, stack, nest.getEggsAmount(), CreatureCareLineage.get(server).eggs(server, pos, nest.getEggsAmount())));
        }
    }
    @Inject(method = "tryPlaceEgg", at = @At("RETURN"))
    private static void jem$placeAfter(LivingEntity entity, Level level, BlockPos pos, BlockState state, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        Place place = jem$place.get(); jem$place.remove();
        if (place == null || !callback.getReturnValue() || !CreatureCareProof.debited(place.before, stack)
                || !(level.getBlockEntity(pos) instanceof NestBlockEntity nest) || nest.getEggsAmount() != place.count + 1
                || !CreatureCareProof.item(nest.getEgg()).equals("primal:eagle_egg") || place.siblings.size() != place.count) return;
        var physical = NativeFoodSources.get(place.actor.serverLevel()); ItemStack spent = NativeFoodSources.spent(place.before, 1);
        var claims = NativeFoodSources.claims(spent);
        if (!place.actor.getUUID().equals(physical.owner(spent, CreatureCareEggs.KIND)) || claims.size() != 1
                || !physical.consume(place.actor.getUUID(), spent, CreatureCareEggs.KIND, 1)) return;
        var siblings = new ArrayList<>(place.siblings);
        siblings.add(new CreatureCareLineage.Egg(place.actor.getUUID(), claims.get(0).batch()));
        CreatureCareLineage.get(place.actor.serverLevel()).eggs(place.actor.serverLevel(), pos, siblings);
    }
}
