package com.siirio.jembuilding.item;

import com.siirio.jembuilding.blueprint.BlueprintMode;
import com.siirio.jembuilding.network.BuildingNetwork;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class BlueprintRollItem extends Item {
    private static final String FIRST = "JemBlueprintFirst";
    private static final String FIRST_DIMENSION = "JemBlueprintFirstDimension";
    private static final String BLUEPRINT_ID = "JemBlueprintId";
    private static final String NUMBER = "JemBlueprintNumber";
    private static final String MODE = "JemBlueprintMode";
    private static final String WIDTH = "JemBlueprintWidth";
    private static final String HEIGHT = "JemBlueprintHeight";
    private static final String DEPTH = "JemBlueprintDepth";

    public BlueprintRollItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new com.siirio.jembuilding.client.BlueprintItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            clearFirst(stack);
            if (context.getLevel().isClientSide) BuildingNetwork.clearBlueprintSelection(context.getHand());
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (context.getLevel().isClientSide) {
            BlockPos second = context.getClickedPos();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.siirio.jembuilding.client.BlueprintClientHooks.openCapture(second, context.getHand()));
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide && player.isShiftKeyDown()) {
            clearFirst(stack);
            BuildingNetwork.clearBlueprintSelection(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        Optional<UUID> id = blueprintId(stack);
        if (id.isEmpty()) {
            lines.add(Component.translatable("tooltip.jem_building.blueprint_roll.select").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.jem_building.blueprint_roll.cancel").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        BlueprintMode mode = mode(stack).orElse(BlueprintMode.COPY);
        lines.add(Component.translatable("tooltip.jem_building.blueprint_roll.saved",
                Component.translatable("screen.jem_building.blueprint.mode." + mode.name().toLowerCase()),
                stack.getOrCreateTag().getInt(WIDTH), stack.getOrCreateTag().getInt(HEIGHT), stack.getOrCreateTag().getInt(DEPTH))
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.jem_building.blueprint_roll.controls").withStyle(ChatFormatting.GRAY));
    }

    public static Optional<BlockPos> first(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(FIRST, Tag.TAG_COMPOUND)) return Optional.empty();
        CompoundTag first = root.getCompound(FIRST);
        return Optional.of(new BlockPos(first.getInt("X"), first.getInt("Y"), first.getInt("Z")));
    }

    public static void setFirst(ItemStack stack, BlockPos pos) {
        CompoundTag value = new CompoundTag();
        value.putInt("X", pos.getX()); value.putInt("Y", pos.getY()); value.putInt("Z", pos.getZ());
        stack.getOrCreateTag().put(FIRST, value);
    }

    public static void setFirstDimension(ItemStack stack, String dimension) {
        stack.getOrCreateTag().putString(FIRST_DIMENSION, dimension);
    }

    public static Optional<String> firstDimension(ItemStack stack) {
        CompoundTag root = stack.getTag();
        return root != null && root.contains(FIRST_DIMENSION, Tag.TAG_STRING)
                ? Optional.of(root.getString(FIRST_DIMENSION)) : Optional.empty();
    }

    public static void clearFirst(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root != null) {
            root.remove(FIRST);
            root.remove(FIRST_DIMENSION);
        }
    }

    public static Optional<UUID> blueprintId(ItemStack stack) {
        CompoundTag root = stack.getTag();
        return root != null && root.hasUUID(BLUEPRINT_ID) ? Optional.of(root.getUUID(BLUEPRINT_ID)) : Optional.empty();
    }

    public static Optional<BlueprintMode> mode(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(MODE, Tag.TAG_STRING)) return Optional.empty();
        try { return Optional.of(BlueprintMode.valueOf(root.getString(MODE))); }
        catch (IllegalArgumentException ignored) { return Optional.empty(); }
    }

    public static void bind(ItemStack stack, UUID id, int number, String ownerName, BlueprintMode mode, int width, int height, int depth) {
        CompoundTag root = stack.getOrCreateTag();
        root.remove(FIRST);
        root.remove(FIRST_DIMENSION);
        root.putUUID(BLUEPRINT_ID, id);
        root.putInt(NUMBER, number);
        root.putString(MODE, mode.name());
        root.putInt(WIDTH, width); root.putInt(HEIGHT, height); root.putInt(DEPTH, depth);
        stack.setHoverName(Component.translatable("item.jem_building.structure_blueprint.named", ownerName, number));
    }
}
