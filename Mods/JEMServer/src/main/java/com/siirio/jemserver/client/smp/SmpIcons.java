package com.siirio.jemserver.client.smp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class SmpIcons {
    private static final Map<Item, ItemStack> STACKS = new HashMap<>();

    static Item item(String key) {
        return switch (key) {
            case "crest" -> Items.SHIELD;
            case "search" -> Items.SPYGLASS;
            case "filter" -> Items.HOPPER;
            case "invite", "create_party" -> Items.PLAYER_HEAD;
            case "open", "event_details", "all_rewards" -> Items.BOOK;
            case "solo" -> Items.IRON_SWORD;
            case "profile", "profiles", "seller_profile", "players" -> Items.PLAYER_HEAD;
            case "claims", "BUILDING" -> Items.FILLED_MAP;
            case "map", "navigate", "waypoint", "EXPLORATION" -> Items.FILLED_MAP;
            case "events" -> Items.FIREWORK_ROCKET;
            case "BOSS", "BOSS_RAID" -> Items.DIAMOND_SWORD;
            case "COOKING_SHOW" -> Items.CAKE;
            case "buy" -> Items.EMERALD;
            case "FISHING", "fishingEvents", "fishingMilestones" -> Items.FISHING_ROD;
            case "RESOURCE_RUSH" -> Items.DIAMOND_PICKAXE;
            case "BLOOD_MOON", "bloodMoonClears" -> Items.ZOMBIE_HEAD;
            case "shops", "TRADING" -> Items.EMERALD;
            case "parties", "partiesHosted", "partiesJoined" -> Items.TOTEM_OF_UNDYING;
            case "back", "previous", "next" -> Items.ARROW;
            case "create", "submit", "publish", "join", "accept" -> Items.WRITABLE_BOOK;
            case "ready", "true", "approve" -> Items.EMERALD;
            case "start" -> Items.DIAMOND_SWORD;
            case "leave" -> Items.OAK_DOOR;
            case "false", "cancel", "remove", "close" -> Items.BARRIER;
            case "prizes", "claim_rewards", "claim", "claim_all" -> Items.ENDER_CHEST;
            case "favorite" -> Items.GOLD_INGOT;
            case "playTicks", "planned", "started", "ends", "calendar" -> Items.CLOCK;
            case "mobsKilled" -> Items.IRON_SWORD;
            case "bossesHosted", "bossAssists", "raidsCompleted" -> Items.WITHER_SKELETON_SKULL;
            case "biomesExplored" -> Items.COMPASS;
            case "animalsTamed", "tameSpecies" -> Items.BONE;
            default -> Items.AIR;
        };
    }

    static void draw(GuiGraphics g, String key, int x, int y, int size) {
        if (SmpGuiAssets.icon(g, key, x, y, size)) return;
        Item item = item(key);
        if (item == Items.AIR) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(size / 16f, size / 16f, 1);
        g.renderItem(STACKS.computeIfAbsent(item, ItemStack::new), 0, 0);
        g.pose().popPose();
    }

    static void face(GuiGraphics g, UUID id, int x, int y, int size) {
        var connection = Minecraft.getInstance().getConnection();
        var info = connection == null ? null : connection.getPlayerInfo(id);
        PlayerFaceRenderer.draw(g, info == null ? DefaultPlayerSkin.getDefaultSkin(id) : info.getSkinLocation(), x, y, size);
    }

    private SmpIcons() {}
}
