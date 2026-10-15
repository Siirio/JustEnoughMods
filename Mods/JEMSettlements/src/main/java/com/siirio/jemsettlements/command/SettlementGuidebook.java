package com.siirio.jemsettlements.command;

import java.util.List;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class SettlementGuidebook {
    private static final String MARKER = "JemSettlementsGuide";

    static void give(ServerPlayer player) {
        boolean russian = player.getLanguage().toLowerCase(java.util.Locale.ROOT).startsWith("ru_");
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        var tag = book.getOrCreateTag();
        tag.putBoolean(MARKER, true);
        tag.putString("title", russian ? "Жизнь поселения" : "Settlement Life");
        tag.putString("author", "JEM");
        tag.putBoolean("resolved", true);
        List<String> text = russian ? List.of(
                "Жильё\n\nДай каждому жителю кровать, крышу и спокойный путь домой. Уютное жильё поднимает настроение.",
                "Еда\n\nЖители ждут блюда из новых земель и кухонь. Обычная еда утолит только привычный голод. Повторяющееся блюдо быстро надоедает.",
                "Безопасность\n\nБереги жителей от любой боли. Падение не пугает их, но раны от врагов, огня и других бед снижают счастье.",
                "Работа\n\nРабочее место даёт жителю дело и смысл. Убедись, что путь к нему свободен и само место пригодно для труда.",
                "Община\n\nСчастливые соседи поддерживают друг друга. Еда, жильё, работа и защита вместе делают всё поселение дружнее.",
                "Уют\n\nСвет, украшения и приятные мелочи рядом с домом и работой радуют жителей. Не обязательно строить дворец — важна забота.")
                : List.of(
                "Housing\n\nGive every resident a bed, a roof, and a safe path home. A comfortable home lifts their spirits.",
                "Food\n\nResidents want dishes from new lands and kitchens. Ordinary food only meets ordinary needs. The same dish soon becomes tiresome.",
                "Safety\n\nProtect residents from harm. Falling does not frighten them, but wounds from enemies, fire, and other dangers lower happiness.",
                "Work\n\nA workplace gives a resident purpose. Keep the path clear and make sure the place is suitable for their craft.",
                "Community\n\nHappy neighbours support one another. Food, housing, work, and safety together make the whole settlement stronger.",
                "Comfort\n\nLight, decorations, and small comforts near home and work cheer residents. Care matters more than a grand palace.");
        ListTag pages = new ListTag();
        text.forEach(page -> pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(page)))));
        tag.put("pages", pages);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            if (current.is(Items.WRITTEN_BOOK) && current.getOrCreateTag().getBoolean(MARKER)) {
                player.getInventory().setItem(slot, book);
                player.containerMenu.broadcastChanges();
                return;
            }
        }
        if (!player.getInventory().add(book)) player.drop(book, false);
        player.containerMenu.broadcastChanges();
    }

    private SettlementGuidebook() {}
}
