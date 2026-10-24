package com.siirio.jemserver;

import com.siirio.jemserver.menus.VanillaMenus;

import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EntityType;

public final class BossLeaderboard {
    private static final int PAGE_SIZE = 45;

    private BossLeaderboard() {}

    public static void open(ServerPlayer player, int requestedPage) {
        if (!ServerConfig.LEADERBOARD_ENABLED.get()) return;
        List<ServerData.Hunter> hunters = ServerData.get(player.server).hunters().stream()
                .sorted(Comparator.comparingLong(ServerData.Hunter::total).reversed()
                        .thenComparing(Comparator.comparingInt(ServerData.Hunter::unique).reversed())
                        .thenComparing(ServerData.Hunter::name, String.CASE_INSENSITIVE_ORDER).thenComparing(ServerData.Hunter::id)).toList();
        int page = Math.max(0, Math.min(requestedPage, Math.max(0, (hunters.size() - 1) / PAGE_SIZE)));
        VanillaMenus.chest(player, JemServer.MOD_ID, "bosses", Component.translatable("jem.bosses.title"), menu -> {
            for (int index = page * PAGE_SIZE; index < Math.min(hunters.size(), (page + 1) * PAGE_SIZE); index++) {
                var hunter = hunters.get(index);
                var head = VanillaMenus.icon(Items.PLAYER_HEAD, Component.literal((index + 1) + ". " + hunter.name()), Component.translatable("jem.bosses.total", hunter.total()), Component.translatable("jem.bosses.unique", hunter.unique()));
                head.getOrCreateTag().putString("SkullOwner", hunter.name());
                menu.button(index % PAGE_SIZE, head, () -> details(player, hunter.id(), 0));
            }
            if (hunters.isEmpty()) menu.button(22, VanillaMenus.icon(Items.BOOK, Component.translatable("jem.bosses.empty")), null);
            if (page > 0) menu.button(45, VanillaMenus.icon(Items.ARROW, Component.translatable("jem.ui.back")), () -> open(player, page - 1));
            if ((page + 1) * PAGE_SIZE < hunters.size()) menu.button(53, VanillaMenus.icon(Items.ARROW, Component.translatable("jem.ui.next")), () -> open(player, page + 1));
        });
    }

    private static void details(ServerPlayer player, UUID id, int requestedPage) {
        if (!ServerConfig.LEADERBOARD_ENABLED.get()) return;
        ServerData.Hunter hunter = ServerData.get(player.server).hunters().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
        if (hunter == null) {
            open(player, 0);
            return;
        }
        var entries = hunter.kills().entrySet().stream().filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<ResourceLocation, Long>comparingByValue().reversed().thenComparing(entry -> entry.getKey().toString())).toList();
        int page = Math.max(0, Math.min(requestedPage, Math.max(0, (entries.size() - 1) / PAGE_SIZE)));
        VanillaMenus.chest(player, JemServer.MOD_ID, "boss_details", Component.literal(hunter.name()), menu -> {
            for (int index = page * PAGE_SIZE; index < Math.min(entries.size(), (page + 1) * PAGE_SIZE); index++) {
                var entry = entries.get(index);
                menu.button(index % PAGE_SIZE, VanillaMenus.icon(Items.IRON_SWORD, bossName(entry.getKey()), Component.translatable("jem.bosses.victories", entry.getValue())), null);
            }
            if (page > 0) menu.button(45, VanillaMenus.icon(Items.ARROW, Component.translatable("jem.ui.back")), () -> details(player, id, page - 1));
            menu.button(49, VanillaMenus.icon(Items.BOOK, Component.translatable("jem.bosses.overall")), () -> open(player, 0));
            if ((page + 1) * PAGE_SIZE < entries.size()) menu.button(53, VanillaMenus.icon(Items.ARROW, Component.translatable("jem.ui.next")), () -> details(player, id, page + 1));
        });
    }

    private static Component bossName(ResourceLocation key) {
        var profile = BalanceRegistry.bossByKey(key).orElse(null);
        if (profile == null) return Component.literal(key.getPath());
        for (var id : profile.entityIds()) {
            var type = EntityType.byString(id.toString());
            if (type.isPresent()) return Component.translatable(type.get().getDescriptionId());
        }
        return Component.literal(profile.displayName());
    }
}
