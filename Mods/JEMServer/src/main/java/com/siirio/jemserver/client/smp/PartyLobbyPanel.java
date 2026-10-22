package com.siirio.jemserver.client.smp;

import static com.siirio.jemserver.client.ui.JemPalette.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Lobby presentation, selection and scrolling, independent of screen routing and networking. */
final class PartyLobbyPanel {
    private static final int GAP = 4;
    record Layout(int x, int y, int width, int bottom) {}
    @FunctionalInterface interface Button { void draw(GuiGraphics g, int x, int y, int width, String action, int mx, int my); }
    @FunctionalInterface interface Location { void draw(GuiGraphics g, CompoundTag detail, int x, int y, int width, int height, int mx, int my); }
    record Callbacks(Consumer<SmpHit> hits, Consumer<ItemStack> hover, Runnable invite,
                     Runnable restoreClip, Button button, Location location) {}
    private UUID participant;
    private int memberScroll;
    private int memberMaxScroll;
    private int memberListX;
    private int memberListY;
    private int memberListWidth;
    private int memberListHeight;
    private int partyRewardScroll;
    private int partyRewardMaxScroll;
    private int partyRewardX;
    private int partyRewardY;
    private int partyRewardWidth;
    void render(GuiGraphics g, Font font, CompoundTag detail, Layout layout, int mx, int my,
                EntityPreview bossPreview, Callbacks callbacks) {
        int bodyX = layout.x(), bodyY = layout.y(), bodyWidth = layout.width(), bodyBottom = layout.bottom();
        var actions = detail.getList("actions", Tag.TAG_STRING);
        var members = detail.getCompound("members");
        text(g, font, Component.literal(detail.getString("title")), bodyX + 4, bodyY + 6, CREAM, bodyWidth - 8);
        int contentTop = bodyY + 25;
        if (!detail.getString("state").equals("ACTIVE") && detail.getBoolean("leader") && !detail.getBoolean("canStart") && !detail.getString("startReason").isEmpty()) {
            var reason = font.split(Component.translatable("jem.smp.error." + detail.getString("startReason")), bodyWidth - 12);
            for (var line : reason) {
                g.drawString(font, line, bodyX + 5, contentTop, MUTED, false);
                contentTop += 11;
            }
            contentTop += 4;
        }
        int contentBottom = bodyBottom;
        var loot = detail.getList("reward", Tag.TAG_COMPOUND);
        int rewardHeight = loot.isEmpty() ? 0 : 75;
        int upperBottom = contentBottom - rewardHeight - (rewardHeight > 0 ? 7 : 0);
        int leftWidth = Math.max(104, Math.min(172, bodyWidth / 3));
        int memberX = bodyX + leftWidth + 8;
        int memberWidth = bodyWidth - leftWidth - 8;
        int previewHeight = Math.max(72, upperBottom - contentTop);
        String activity = detail.getString("eventActivity");
        if (activity.isEmpty()) activity = detail.getString("activity");
        g.fill(bodyX, contentTop, bodyX + leftWidth, contentTop + previewHeight, INSET);
        if (detail.getString("bossType").isEmpty()) SmpGuiAssets.cover(g, activity, bodyX + 3, contentTop + 3, leftWidth - 6, previewHeight - 6);
        else SmpGuiAssets.coverBackground(g, activity, bodyX + 3, contentTop + 3, leftWidth - 6, previewHeight - 6);
        SmpGuiAssets.frame(g, bodyX, contentTop, leftWidth, previewHeight, SmpGuiAssets.accent(activity));
        if (!detail.getString("bossType").isEmpty()) {
            bossPreview.render(g, detail.getString("bossType"), bodyX + 4, contentTop + 4, leftWidth - 8, previewHeight - 8, mx, my);
            callbacks.restoreClip().run();
        }
        else if (!Set.of("BOSS", "BOSS_RAID", "BLOOD_MOON", "RESOURCE_RUSH", "FISHING", "COOKING_SHOW").contains(activity)) SmpIcons.draw(g, activity, bodyX + leftWidth / 2 - 24, contentTop + previewHeight / 2 - 24, 48);
        callbacks.restoreClip().run();
        text(g, font, Component.translatable("jem.smp.participants"), memberX + 4, contentTop + 4, PEACH, memberWidth - 8);
        memberListX = memberX;
        memberListY = contentTop + 22;
        memberListWidth = memberWidth;
        int locationHeight = detail.getBoolean("meaningfulLocation") ? 42 : 0;
        CompoundTag selectedMember=participant!=null&&members.contains(participant.toString())?members.getCompound(participant.toString()):null;
        boolean manage=detail.getBoolean("leader")&&selectedMember!=null
                &&(selectedMember.getBoolean("canApprove")||selectedMember.getBoolean("canRemove")||selectedMember.getBoolean("canTransfer"));
        int managementHeight=manage?31:0;
        memberListHeight = Math.max(36, upperBottom - memberListY - (locationHeight > 0 ? locationHeight + 6 : 0) - (managementHeight > 0 ? managementHeight + 6 : 0));
        int memberY = memberListY - memberScroll;
        var memberIds = new ArrayList<>(members.getAllKeys());
        memberIds.sort(Comparator.comparing((String id) -> !detail.hasUUID("owner") || !detail.getUUID("owner").toString().equals(id)).thenComparing(id -> members.getCompound(id).getString("name")));
        g.enableScissor(memberListX, memberListY, memberListX + memberListWidth, memberListY + memberListHeight);
        try {
            for (String raw : memberIds) {
                var member = members.getCompound(raw);
                UUID id;
                try { id = UUID.fromString(raw); } catch (IllegalArgumentException invalid) { continue; }
                if (memberY + 40 >= memberListY && memberY < memberListY + memberListHeight) {
                    frame(g, memberX, memberY, memberWidth, 40, id.equals(participant)?HOVER:ROW);
                    CombatHud.head(g, id, memberX + 7, memberY + 9, 22);
                    String name = member.getString("name");
                    text(g, font, Component.literal(name.isBlank() ? raw : name), memberX + 36, memberY + 7, CREAM, memberWidth - 42);
                    String state = !member.getBoolean("online") ? "offline" : !member.getBoolean("accepted") ? "awaiting_approval" : member.getBoolean("ready") ? "ready" : "not_ready";
                    Component status = Component.translatable("jem.smp." + state);
                    if (detail.hasUUID("owner") && detail.getUUID("owner").equals(id)) status = Component.translatable("jem.smp.leader").append(" · ").append(status);
                    text(g, font, status, memberX + 36, memberY + 23, member.getBoolean("ready") ? GREEN : MUTED, memberWidth - 42);
                    hitWithin(callbacks.hits(), memberX, memberY, memberWidth, 40, memberListX, memberListY, memberListWidth, memberListHeight, () -> participant=id);
                }
                memberY += 44;
            }
            if (hasAction(actions, "invite")) {
                frame(g, memberX, memberY, memberWidth, 40, mx >= memberX && mx < memberX + memberWidth && my >= memberY && my < memberY + 40 ? HOVER : ROW);
                text(g, font, Component.literal("+"), memberX + 12, memberY + 11, PEACH, 18);
                text(g, font, Component.translatable("jem.smp.add_player"), memberX + 36, memberY + 15, CREAM, memberWidth - 42);
                hitWithin(callbacks.hits(), memberX, memberY, memberWidth, 40, memberListX, memberListY, memberListWidth, memberListHeight, callbacks.invite());
                memberY += 44;
            }
        } finally {
            g.disableScissor();
            callbacks.restoreClip().run();
        }
        memberMaxScroll = Math.max(0, memberY + memberScroll - memberListY - memberListHeight);
        memberScroll = Math.max(0, Math.min(memberScroll, memberMaxScroll));
        if(manage) {
            var management=new ArrayList<String>();
            if(selectedMember.getBoolean("canApprove")) management.add("approve");
            if(selectedMember.getBoolean("canRemove")) management.add("remove");
            if(selectedMember.getBoolean("canTransfer")) management.add("transfer");
            int managementY=memberListY+memberListHeight+5;
            int managementWidth=(memberWidth-(management.size()-1)*GAP)/management.size();
            for(int index=0;index<management.size();index++) {
                String action=management.get(index);
                callbacks.button().draw(g, memberX+index*(managementWidth+GAP), managementY, managementWidth, action, mx, my);
            }
        }
        if (locationHeight > 0) callbacks.location().draw(g, detail, memberX, upperBottom - locationHeight, memberWidth, locationHeight, mx, my);
        if (rewardHeight > 0) {
            int rewardY = contentBottom - rewardHeight;
            frame(g, bodyX, rewardY, bodyWidth, rewardHeight, ROW);
            text(g, font, Component.translatable("jem.smp.possible_rewards"), bodyX + 8, rewardY + 7, PEACH, bodyWidth - 16);
            partyRewardX = bodyX + 8;
            partyRewardY = rewardY + 20;
            partyRewardWidth = bodyWidth - 16;
            partyRewardMaxScroll = Math.max(0, ItemRewardStrip.twoRowWidth(loot) - partyRewardWidth);
            partyRewardScroll = Math.max(0, Math.min(partyRewardScroll, partyRewardMaxScroll));
            ItemRewardStrip.renderTwoRows(g, loot, partyRewardX, partyRewardY, partyRewardWidth, partyRewardScroll, mx, my, callbacks.hover());
            callbacks.restoreClip().run();
        } else {
            partyRewardScroll = 0;
            partyRewardMaxScroll = 0;
            partyRewardWidth = 0;
        }
    }

    UUID participant() { return participant; }
    void reset() { memberScroll = 0; partyRewardScroll = 0; participant = null; }
    void resetRewards() { partyRewardScroll = 0; }
    void resetMembers() { memberScroll = 0; }
    void clamp() { memberScroll = Math.max(0, Math.min(memberScroll, memberMaxScroll)); }

    boolean scroll(double x, double y, double delta) {
        if (x >= partyRewardX && x < partyRewardX + partyRewardWidth
                && y >= partyRewardY && y < partyRewardY + ItemRewardStrip.CELL * 2) {
            partyRewardScroll = Math.max(0, Math.min(partyRewardMaxScroll, partyRewardScroll - (int) (delta * ItemRewardStrip.CELL)));
            return true;
        }
        if (x >= memberListX && x < memberListX + memberListWidth
                && y >= memberListY && y < memberListY + memberListHeight) {
            memberScroll = Math.max(0, Math.min(memberMaxScroll, memberScroll - (int) (delta * 22)));
            return true;
        }
        return false;
    }

    private static boolean hasAction(ListTag actions, String action) {
        for (Tag value : actions) if (value.getAsString().equals(action)) return true;
        return false;
    }
    private static void frame(GuiGraphics g, int x, int y, int w, int h, int fill) {
        SmpGuiAssets.panel(g, x, y, w, h, fill);
    }
    private static void text(GuiGraphics g, Font font, Component text, int x, int y, int color, int max) {
        String value = text.getString();
        if (font.width(value) > max) value = font.plainSubstrByWidth(value, Math.max(0, max - 8)) + "\u2026";
        g.drawString(font, value, x, y, color, false);
    }
    private static void hitWithin(Consumer<SmpHit> hits, int x, int y, int w, int h,
                                  int clipX, int clipY, int clipWidth, int clipHeight, Runnable action) {
        int right = Math.min(x + w, clipX + clipWidth), bottom = Math.min(y + h, clipY + clipHeight);
        x = Math.max(x, clipX);
        y = Math.max(y, clipY);
        if (right > x && bottom > y) hits.accept(new SmpHit(x, y, right - x, bottom - y, action));
    }
}
