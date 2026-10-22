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
            case "scroll" -> {
                String owner = "io.redspace.ironsspellbooks.api.spells.ISpellContainer";
                if (!Boolean.TRUE.equals(BookSignals.call(owner, "isSpellContainer", new Class<?>[]{ItemStack.class}, stack))) yield false;
                Object container = BookSignals.call(owner, "get", new Class<?>[]{ItemStack.class}, stack);
                yield container != null && !Boolean.TRUE.equals(BookSignals.call(container, "isEmpty"));
            }
            case "filled_flask" -> stack.hasTag() && !stack.getTag().getList("CustomPotionEffects", net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty();
            default -> throw new IllegalArgumentException("Unknown native stack proof " + proof);
        };
    }

    static ItemStack prepareReward(ItemStack source) {
        ItemStack stack = source.copy();
        if (QuestSignals.loaded("irons_spellbooks")) {
            try {
                Class<?> nativeDefault = Class.forName("io.redspace.ironsspellbooks.api.backwards_compat.IBackwardsCompatDefaultNbtItem");
                if (nativeDefault.isInstance(stack.getItem())) nativeDefault.getMethod("setupItem", ItemStack.class).invoke(stack.getItem(), stack);
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Native reward ItemStack initializer changed", failure); }
        }
        return stack;
    }
}
