package com.justenoughmods.achievementguide.compat.ftb;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

/** Native data-bearing item checks, from the exact installed implementations. */
final class BookStackProofs {
    private BookStackProofs() {}

    static boolean topic(ItemStack stack, String expected) {
        if (stack.getTag() == null) return false;
        Component component = Component.Serializer.fromJson(stack.getTag().getString("topic"));
        return component != null && component.getContents() instanceof TranslatableContents translated
                && translated.getKey().equals("logbook.aquamirae." + expected + ".topic");
    }

    static boolean valid(ItemStack stack, String proof) {
        return switch (proof) {
            case "photograph" -> {
                Object identifier = BookSignals.call(stack.getItem(), "getIdentifier", new Class<?>[]{ItemStack.class}, stack);
                yield !Boolean.TRUE.equals(BookSignals.call(identifier, "isEmpty"));
            }
            case "filled_flask" -> stack.hasTag() && !stack.getTag().getList("CustomPotionEffects", net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty();
            default -> throw new IllegalArgumentException("Unknown native stack proof " + proof);
        };
    }

    static ItemStack prepareReward(ItemStack source) {
        return source.copy();
    }
}
