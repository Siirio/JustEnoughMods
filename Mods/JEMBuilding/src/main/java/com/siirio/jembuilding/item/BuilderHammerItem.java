package com.siirio.jembuilding.item;

import com.siirio.jembuilding.construction.ConstructionPlane;
import com.siirio.jembuilding.network.BuildingNetwork;
import com.siirio.jembuilding.registry.BuildingContent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.nbt.Tag;
import java.util.List;
import java.util.Optional;

public final class BuilderHammerItem extends Item {
    private static final String SELECTION = "JemBuildingSelection";

    public BuilderHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos()).is(BuildingContent.CONSTRUCTION_PLACEHOLDER.get())) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            clear(stack);
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        BlockPos start = context.getClickedPos().relative(context.getClickedFace());
        setStart(stack, start, ConstructionPlane.fromFace(context.getClickedFace()));
        if (context.getPlayer() != null && !context.getLevel().isClientSide) {
            context.getPlayer().displayClientMessage(Component.translatable("message.jem_building.first_position", start.getX(), start.getY(), start.getZ()), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            clear(stack);
            if (level.isClientSide) BuildingNetwork.cancelSelection();
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (level.isClientSide && start(stack).isPresent()) BuildingNetwork.confirmClientSelection(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.jem_building.builder_hammer.select").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.jem_building.builder_hammer.cancel").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static Optional<BlockPos> start(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(SELECTION, Tag.TAG_COMPOUND)) return Optional.empty();
        CompoundTag selection = root.getCompound(SELECTION);
        return Optional.of(new BlockPos(selection.getInt("X"), selection.getInt("Y"), selection.getInt("Z")));
    }

    public static Optional<ConstructionPlane> plane(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(SELECTION, Tag.TAG_COMPOUND)) return Optional.empty();
        String value = root.getCompound(SELECTION).getString("Plane");
        try { return value.isEmpty() ? Optional.empty() : Optional.of(ConstructionPlane.valueOf(value)); }
        catch (IllegalArgumentException ignored) { return Optional.empty(); }
    }

    public static void setStart(ItemStack stack, BlockPos start, ConstructionPlane plane) {
        CompoundTag selection = new CompoundTag();
        selection.putInt("X", start.getX());
        selection.putInt("Y", start.getY());
        selection.putInt("Z", start.getZ());
        selection.putString("Plane", plane.name());
        stack.getOrCreateTag().put(SELECTION, selection);
    }

    public static void clear(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root != null) root.remove(SELECTION);
    }
}
