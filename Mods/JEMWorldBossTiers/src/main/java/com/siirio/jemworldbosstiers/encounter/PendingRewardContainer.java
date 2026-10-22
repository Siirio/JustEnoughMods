package com.siirio.jemworldbosstiers.encounter;

import java.util.*;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Compatibility adapter; JEMRewards is the sole item-storage owner. */
public final class PendingRewardContainer {
    private PendingRewardContainer() {}
    public static void enqueue(MinecraftServer s, UUID p, String source, String title, List<ItemStack> items) { com.siirio.jemrewards.PendingRewardContainer.enqueue(s,p,source,title,items); }
    public static void enqueueGrouped(MinecraftServer s, UUID p, String source, String title, List<RewardGroup> groups) { com.siirio.jemrewards.PendingRewardContainer.enqueueGrouped(s,p,source,title,groups.stream().map(g -> new com.siirio.jemrewards.RewardGroup(g.titleKey(),g.items())).toList()); }
    public static void importLegacy(ServerPlayer p, UUID id, ListTag items) { com.siirio.jemrewards.PendingRewardContainer.importLegacy(p,id,items); }
    public static void finishLegacyImport(ServerPlayer p, UUID id) { com.siirio.jemrewards.PendingRewardContainer.finishLegacyImport(p,id); }
    public static ListTag bundles(ServerPlayer p, int start, int size) { return com.siirio.jemrewards.PendingRewardContainer.bundles(p,start,size); }
    public static ListTag bundles(ServerPlayer p, String source) { return com.siirio.jemrewards.PendingRewardContainer.bundles(p,source); }
    public static int count(ServerPlayer p) { return com.siirio.jemrewards.PendingRewardContainer.count(p); }
    public static int totalItems(ServerPlayer p) { return com.siirio.jemrewards.PendingRewardContainer.totalItems(p); }
    public static void discardBySource(MinecraftServer s, Collection<UUID> p, String source) { com.siirio.jemrewards.PendingRewardContainer.discardBySource(s,p,source); }
    public static boolean claim(ServerPlayer p, UUID id) { return com.siirio.jemrewards.PendingRewardContainer.claim(p,id); }
    public static boolean claimBundles(ServerPlayer p, Collection<UUID> ids) { return com.siirio.jemrewards.PendingRewardContainer.claimBundles(p,ids); }
}
