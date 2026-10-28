package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import com.justenoughmods.achievementguide.integration.food.GrapePotReceipt;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import net.satisfy.vinery.core.block.GrapevinePotBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GrapevinePotBlock.class, remap = false)
public abstract class GrapePotFoodQuestMixin {
    @Shadow @Final private static IntegerProperty STAGE;
    @Shadow @Final private static IntegerProperty STORAGE;
    @Unique private static final Set<String> jem$juice = Set.of("vinery:red_grapejuice", "vinery:white_grapejuice",
            "vinery:red_jungle_grapejuice", "vinery:white_jungle_grapejuice", "vinery:red_savanna_grapejuice",
            "vinery:white_savanna_grapejuice", "vinery:red_taiga_grapejuice", "vinery:white_taiga_grapejuice");
    @Redirect(method = "m_142072_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean jem$completed(Level level, BlockPos pos, BlockState after, int flags,
                                  Level nativeLevel, BlockState before, BlockPos nativePos, Entity actor, float distance) {
        boolean changed = level.setBlock(pos, after, flags);
        if (changed && actor instanceof ServerPlayer player && before.getValue(STAGE) == 5 && after.getValue(STAGE) == 6
                && before.getValue(STORAGE).equals(after.getValue(STORAGE)) && level.getBlockState(pos) == after)
            GrapePotReceipt.completed(level, pos, player, after, after.getValue(STORAGE) / 3);
        return changed;
    }
    @Redirect(method = "m_6227_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean jem$bottle(Level level, BlockPos pos, BlockState after, int flags,
                              BlockState before, Level nativeLevel, BlockPos nativePos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean bottle = before.getValue(STAGE) == 6 && before.getValue(STORAGE) >= 3
                && after.getValue(STORAGE) == before.getValue(STORAGE) - 3
                && after.getValue(STAGE) == (after.getValue(STORAGE) == 0 ? 0 : 6) && level.getBlockState(pos) == before;
        GrapePotReceipt.Batch batch = bottle ? GrapePotReceipt.beginBottle(level, pos, before) : null;
        boolean changed = level.setBlock(pos, after, flags);
        if (bottle && changed && level.getBlockState(pos) == after)
            GrapePotReceipt.bottled(level, pos, player, batch, before, after, after.getValue(STORAGE) / 3);
        return changed;
    }
    @Redirect(method = "m_6227_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;m_36054_(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean jem$received(Inventory inventory, ItemStack output, BlockState before, Level level, BlockPos pos,
                                Player actor, InteractionHand hand, BlockHitResult hit) {
        boolean certified = actor instanceof ServerPlayer && output.getCount() == 1
                && jem$juice.contains(String.valueOf(ForgeRegistries.ITEMS.getKey(output.getItem())))
                && GrapePotReceipt.consumeDraw(level, pos, actor, before);
        ItemStack expected = certified ? output.copy() : ItemStack.EMPTY;
        int held = certified ? FoodOutputProof.held((ServerPlayer)actor, actor.containerMenu, expected) : 0;
        boolean received = inventory.add(output);
        if (received && certified && FoodOutputProof.held((ServerPlayer)actor, actor.containerMenu, expected) > held)
            QuestActionTrigger.INSTANCE.emit((ServerPlayer)actor, "food/grape_stomped");
        return received;
    }
}
