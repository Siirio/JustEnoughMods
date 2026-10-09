package com.siirio.jempackcore.postend;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public final class PostEndLocatorItem extends Item {
    private final PostEndTarget target;
    private final boolean mystery;

    public PostEndLocatorItem(PostEndTarget target, boolean mystery) {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        this.target = target;
        this.mystery = mystery;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.sidedSuccess(stack, true);
        if (serverPlayer.level().dimension() != Level.END) {
            serverPlayer.displayClientMessage(Component.translatable("message.jemcompat.post_end.requires_end").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        PostEndMapService.locate(serverPlayer.server, target).whenCompleteAsync((result, error) -> {
            if (error != null || result.isEmpty()) {
                serverPlayer.displayClientMessage(Component.translatable("message.jemcompat.map_not_found").withStyle(ChatFormatting.RED), true);
                return;
            }
            var pos = result.get();
            serverPlayer.displayClientMessage(Component.literal("X: " + pos.getX() + "  Z: " + pos.getZ()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }, serverPlayer.server);
        return InteractionResultHolder.success(stack);
    }

    @Override public Component getName(ItemStack stack) { return mystery ? Component.literal("???") : Component.translatable("item.jemcompat.post_end_locator." + target.key()); }
    @Override public boolean isFoil(ItemStack stack) { return true; }
}
