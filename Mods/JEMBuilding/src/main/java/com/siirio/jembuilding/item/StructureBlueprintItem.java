package com.siirio.jembuilding.item;

import com.siirio.jembuilding.blueprint.BlueprintMode;
import com.siirio.jembuilding.network.BuildingNetwork;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class StructureBlueprintItem extends Item {
    public StructureBlueprintItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) BuildingNetwork.cancelBlueprint(context.getHand());
            else BuildingNetwork.confirmBlueprint(context.getHand());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            if (player.isShiftKeyDown()) BuildingNetwork.cancelBlueprint(hand);
            else BuildingNetwork.confirmBlueprint(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        BlueprintMode mode = BlueprintRollItem.mode(stack).orElse(BlueprintMode.COPY);
        lines.add(Component.translatable("tooltip.jem_building.blueprint_roll.saved",
                Component.translatable("screen.jem_building.blueprint.mode." + mode.name().toLowerCase()),
                stack.getOrCreateTag().getInt("JemBlueprintWidth"), stack.getOrCreateTag().getInt("JemBlueprintHeight"),
                stack.getOrCreateTag().getInt("JemBlueprintDepth")).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.jem_building.blueprint_roll.controls").withStyle(ChatFormatting.GRAY));
    }
}
