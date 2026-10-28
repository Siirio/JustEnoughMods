package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockState;
import net.satisfy.farm_and_charm.core.block.CraftingBowlBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftingBowlBlock.class, remap = false)
public abstract class BowlCollectionQuestMixin {
    @Redirect(method = "m_6227_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;m_36054_(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean jem$receivedDough(Inventory inventory, ItemStack stack, BlockState state, Level level, BlockPos pos,
                                      Player player, InteractionHand hand, BlockHitResult hit) {
        var owner = level.getBlockEntity(pos);
        var proof = owner == null ? new net.minecraft.nbt.CompoundTag() : owner.getPersistentData().getCompound(FoodOutputProof.KEY).copy();
        ItemStack before = stack.copy();
        boolean certified = FoodOutputProof.matches(proof, before) && proof.getInt("slot") == 4 && proof.getString("action").equals("food_bowl_dough");
        int held = player instanceof ServerPlayer actor ? FoodOutputProof.held(actor, actor.containerMenu, before) : 0;
        boolean added = inventory.add(stack);
        if (owner != null) FoodOutputProof.replaced(owner, 4);
        if (added && certified && player instanceof ServerPlayer actor && FoodOutputProof.held(actor, actor.containerMenu, before) > held)
            QuestActionTrigger.INSTANCE.emit(actor, "food_bowl_dough");
        return added;
    }
}
