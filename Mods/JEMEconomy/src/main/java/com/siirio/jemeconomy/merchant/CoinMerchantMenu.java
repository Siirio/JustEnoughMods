package com.siirio.jemeconomy.merchant;

import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.ledger.SystemAccount;
import com.siirio.jemeconomy.physical.CoinFormatter;
import com.siirio.jemeconomy.policy.ItemPolicyRegistry;
import com.siirio.jemeconomy.registry.EconomyMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

public final class CoinMerchantMenu extends AbstractContainerMenu {
    private static final int MAX_OFFERS = 18;
    private final AbstractVillager merchant;
    private final SimpleContainer display = new SimpleContainer(MAX_OFFERS);
    private final List<AdaptedOffer> offers = new ArrayList<>();

    CoinMerchantMenu(int id, Inventory inventory, AbstractVillager merchant) {
        super(EconomyMenus.COIN_MERCHANT.get(), id); this.merchant = merchant;
        buildOffers(inventory.player);
        for (int i = 0; i < MAX_OFFERS; i++) addSlot(new Slot(display, i, 8 + (i % 9) * 18, 22 + (i / 9) * 36) { @Override public boolean mayPlace(ItemStack stack) { return false; } @Override public boolean mayPickup(Player player) { return false; } });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 104 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 162));
    }

    public static CoinMerchantMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        Entity entity = inventory.player.level().getEntity(buffer.readVarInt());
        if (!(entity instanceof AbstractVillager merchant)) throw new IllegalStateException("Merchant is unavailable");
        return new CoinMerchantMenu(id, inventory, merchant);
    }

    private void buildOffers(Player viewer) {
        for (MerchantOffer offer : merchant.getOffers()) {
            if (offers.size() >= MAX_OFFERS) break;
            AdaptedOffer adapted = adapt(viewer, offer); offers.add(adapted);
            ItemStack icon = adapted.icon.copy();
            icon.setHoverName(Component.translatable(adapted.supported ? "jem.economy.merchant.offer" : "jem.economy.merchant.unsupported",
                    icon.getHoverName(), CoinFormatter.format(adapted.price)));
            display.setItem(offers.size() - 1, icon);
        }
    }

    private static AdaptedOffer adapt(Player viewer, MerchantOffer offer) {
        boolean resultEmerald = offer.getResult().is(Items.EMERALD);
        boolean costsEmerald = offer.getCostA().is(Items.EMERALD) || offer.getCostB().is(Items.EMERALD);
        if (resultEmerald && offer.getCostB().isEmpty() && ItemPolicyRegistry.policy(offer.getCostA()).map(policy -> policy.sellable()).orElse(false)) {
            long unit = ItemPolicyRegistry.policy(offer.getCostA()).orElseThrow().sellCopper();
            return new AdaptedOffer(offer, Direction.PLAYER_SELLS, offer.getCostA(), Math.multiplyExact(unit, offer.getCostA().getCount()), true);
        }
        if (costsEmerald && (offer.getCostA().isEmpty() || offer.getCostA().is(Items.EMERALD))
                && (offer.getCostB().isEmpty() || offer.getCostB().is(Items.EMERALD))
                && ItemPolicyRegistry.policy(offer.getResult()).map(policy -> policy.buyable()).orElse(false)) {
            long unit = ItemPolicyRegistry.policy(offer.getResult()).orElseThrow().buyCopper();
            long base = Math.multiplyExact(unit, offer.getResult().getCount());
            int nativeBase = Math.max(1, offer.getBaseCostA().getCount() + offer.getCostB().getCount());
            int nativeNow = Math.max(1, offer.getCostA().getCount() + offer.getCostB().getCount());
            long adjusted = Math.floorDiv(Math.addExact(Math.multiplyExact(base, nativeNow), nativeBase - 1L), nativeBase);
            long floor = Math.floorDiv(Math.addExact(base, 3L), 4L);
            return new AdaptedOffer(offer, Direction.PLAYER_BUYS, offer.getResult(), Math.max(floor, adjusted), true);
        }
        ItemStack icon = offer.getResult().isEmpty() ? offer.getCostA() : offer.getResult();
        return new AdaptedOffer(offer, Direction.NONE, icon, 0L, false);
    }

    @Override public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < offers.size() && clickType == ClickType.PICKUP && player instanceof ServerPlayer serverPlayer) { execute(serverPlayer, offers.get(slotId)); return; }
        super.clicked(slotId, button, clickType, player);
    }

    private void execute(ServerPlayer player, AdaptedOffer adapted) {
        if (!adapted.supported || adapted.offer.isOutOfStock()) { player.displayClientMessage(Component.translatable("jem.economy.merchant.unsupported_action"), true); return; }
        UUID request = UUID.randomUUID();
        if (adapted.direction == Direction.PLAYER_SELLS) {
            var result = EconomyApi.processNpcSale(player, adapted.offer.getCostA(), adapted.offer.getCostA().getCount(), request);
            if (result.status() != EconomyApi.NpcSaleStatus.COMMITTED) { player.displayClientMessage(Component.translatable("jem.economy.merchant.sale_failed", result.status().name()), true); return; }
        } else if (adapted.direction == Direction.PLAYER_BUYS) {
            ItemStack delivery = adapted.offer.getResult().copy();
            if (!canFit(player, delivery)) { player.displayClientMessage(Component.translatable("jem.economy.no_space"), true); return; }
            var reserved = EconomyApi.reserveMarketPayment(player, adapted.price, request);
            if (reserved.status() != EconomyApi.MarketStatus.RESERVED) { player.displayClientMessage(Component.translatable("jem.economy.merchant.purchase_failed", reserved.status().name()), true); return; }
            ItemStack remainder = delivery.copy(); player.getInventory().add(remainder);
            if (!remainder.isEmpty()) { EconomyApi.refundMarketPayment(player, adapted.price, request); return; }
            var settled = EconomyApi.settleMarketPayment(player, SystemAccount.NPC_MARKET.id(), adapted.price, request);
            if (settled.status() != EconomyApi.MarketStatus.COMMITTED) { player.getInventory().clearOrCountMatchingItems(stack -> ItemStack.isSameItemSameTags(stack, delivery), delivery.getCount(), player.inventoryMenu.getCraftSlots()); EconomyApi.refundMarketPayment(player, adapted.price, request); return; }
        }
        adapted.offer.increaseUses(); merchant.notifyTrade(adapted.offer); merchant.setTradingPlayer(player);
        broadcastChanges();
    }

    private static boolean canFit(ServerPlayer player, ItemStack delivery) {
        int remaining = delivery.getCount(); for (ItemStack stack : player.getInventory().items) if (stack.isEmpty()) return true; else if (ItemStack.isSameItemSameTags(stack, delivery)) remaining -= Math.max(0, stack.getMaxStackSize() - stack.getCount()); return remaining <= 0;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return merchant.isAlive() && player.distanceToSqr(merchant) < 64.0D; }
    @Override public void removed(Player player) { super.removed(player); if (merchant.getTradingPlayer() == player) merchant.setTradingPlayer(null); }
    public int offerCount() { return offers.size(); }
    private enum Direction { PLAYER_BUYS, PLAYER_SELLS, NONE }
    private record AdaptedOffer(MerchantOffer offer, Direction direction, ItemStack icon, long price, boolean supported) { }
}
