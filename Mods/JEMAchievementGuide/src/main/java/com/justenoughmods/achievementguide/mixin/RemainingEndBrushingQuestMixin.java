package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodDroppedProof;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrushableBlockEntity.class)
public abstract class RemainingEndBrushingQuestMixin {
    @Unique private ServerPlayer jem$brusher;
    @Unique private final List<ItemEntity> jem$nativeLoot = new ArrayList<>();

    @Inject(method = "brush", at = @At("HEAD"))
    private void jem$start(long time, Player player, Direction face, CallbackInfoReturnable<Boolean> callback) {
        jem$nativeLoot.clear();
        jem$brusher = null;
        BlockEntity owner = (BlockEntity)(Object)this;
        String id = String.valueOf(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(owner.getBlockState().getBlock()));
        if (id.equals("kaleidoscope_end:suspicious_end_stone") || id.equals("kaleidoscope_end:suspicious_dragon_egg")) {
            jem$brusher = RemainingFoodActors.real(player);
        }
    }
    @Redirect(method = "dropContent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$nativeOutput(Level level, Entity entity) {
        boolean accepted = level.addFreshEntity(entity);
        if (accepted && jem$brusher != null && entity instanceof ItemEntity drop && jem$nativeLoot.size() < 2
                && (RemainingFoodActors.item(drop.getItem()).equals("kaleidoscope_end:void_conch")
                || RemainingFoodActors.item(drop.getItem()).equals("kaleidoscope_end:dragon_dust"))) jem$nativeLoot.add(drop);
        return accepted;
    }
    @Inject(method = "brush", at = @At("RETURN"))
    private void jem$committed(long time, Player player, Direction face, CallbackInfoReturnable<Boolean> callback) {
        BlockEntity owner = (BlockEntity)(Object)this;
        if (jem$brusher != null && callback.getReturnValue() && owner.getLevel() != null
                && owner.getLevel().getBlockEntity(owner.getBlockPos()) != owner) {
            for (ItemEntity drop : jem$nativeLoot) {
                FoodDroppedProof.update(drop, "v3_food_end_brushing", drop.getItem().getCount(), jem$brusher.getUUID());
            }
        }
        jem$brusher = null;
        jem$nativeLoot.clear();
    }
}
