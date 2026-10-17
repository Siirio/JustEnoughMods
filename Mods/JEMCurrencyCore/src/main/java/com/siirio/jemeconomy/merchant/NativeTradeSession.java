package com.siirio.jemeconomy.merchant;

import com.siirio.jemeconomy.api.CoinTradeContext;
import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemcurrency.NativeMerchantTags;
import com.siirio.jemeconomy.market.MarketSavedData;
import com.siirio.jemeconomy.network.EconomyNetwork;
import com.siirio.jemeconomy.network.NativeTradePacket;
import com.siirio.jemcurrency.CurrencyItems;
import com.siirio.jemcurrency.CurrencyAuthority;
import com.siirio.jemidentity.api.IdentityPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

/** Per-native-menu transaction context, not a second merchant offer collection. */
public final class NativeTradeSession {
    private final MerchantMenu menu;
    private final MerchantContainer inputs;
    private final AbstractVillager merchant;
    private final ServerPlayer player;
    private final boolean buyer;
    private final boolean preview;
    private final UUID sessionId = UUID.randomUUID();
    private long sequence;
    private long buyerCycle;
    private int selected;
    private boolean closed;
    private boolean refreshing;
    private String status = "";
    private Slot nativeResult;
    private Slot guardedResult;
    private NativeTradePacket lastSnapshot;
    private int lastRefreshTick = Integer.MIN_VALUE;
    private int revision;
    private final java.util.LinkedHashSet<UUID> handledRequests = new java.util.LinkedHashSet<>();
    private List<net.minecraft.nbt.CompoundTag> lastTerms = List.of();

    public NativeTradeSession(MerchantMenu menu, MerchantContainer inputs, AbstractVillager merchant, ServerPlayer player) {
        this.menu = menu; this.inputs = inputs; this.merchant = merchant; this.player = player;
        buyer = NativeMerchantTags.isBuyer(merchant);
        preview = NativeMerchantTags.isPreview(merchant);
        buyerCycle = buyer ? MarketSavedData.get(player.getServer()).cycle(player.getServer()) : 0L;
    }
    public void keepNativeResult(Slot nativeResult, Slot guardedResult) {
        this.nativeResult = nativeResult; this.guardedResult = guardedResult;
        guardedResult.index = nativeResult.index;
        menu.slots.set(2, guardedResult);
    }
    public boolean valid() {
        return !closed && CurrencyAuthority.active(player) && player.isAlive() && merchant.isAlive()
                && merchant.getTradingPlayer() == player && (preview || player.distanceToSqr(merchant) < 64.0D);
    }
    public boolean select(int index) {
        if (index < 0 || index >= merchant.getOffers().size()) return false;
        selected = index; status = ""; lastSnapshot = null; return true;
    }
    private MerchantOffer offer() {
        return selected >= 0 && selected < merchant.getOffers().size() ? merchant.getOffers().get(selected) : null;
    }
    private NativeTradeQuote quote() {
        var offer = offer();
        return offer == null ? new NativeTradeQuote(selected, NativeTradeQuote.Direction.UNPRICED, 0, false, "jem.economy.trade.stock")
                : NativeTradePricing.quote(player, offer, selected, buyer);
    }
    public boolean nativeBarter() { return offer() != null && !com.siirio.jemeconomy.policy.TradeRestrictions.offerProhibited(offer()) && !NativeTradePricing.monetary(offer()) && IdentityPolicy.resolve(player).isPresent(); }
    public boolean updateResult() {
        if (closed) return false;
        if (nativeResult != null) menu.slots.set(2, nativeBarter() ? nativeResult : guardedResult);
        if (nativeBarter()) {
            var barter = offer();
            boolean ready = !barter.isOutOfStock() && (barter.satisfiedBy(inputs.getItem(0), inputs.getItem(1))
                    || barter.satisfiedBy(inputs.getItem(1), inputs.getItem(0)));
            ((NativeTradeContainer) inputs).jem$projection(ready ? barter : null, ready ? barter.assemble() : ItemStack.EMPTY,
                    ready ? barter.getXp() : 0);
            return true;
        }
        var offer = offer(); var quote = quote();
        boolean ready = !preview && offer != null && quote.available() && matchesInputs(offer);
        ItemStack result = ready ? quote.direction() == NativeTradeQuote.Direction.PLAYER_SELLS
                ? new ItemStack(CurrencyItems.COPPER_COIN.get()) : offer.getResult().copy() : ItemStack.EMPTY;
        if (ready && quote.direction() == NativeTradeQuote.Direction.PLAYER_SELLS)
            result.setHoverName(net.minecraft.network.chat.Component.translatable("jem.economy.trade.wallet_credit",
                    com.siirio.jemeconomy.physical.CoinFormatter.format(quote.copper())));
        ((NativeTradeContainer) inputs).jem$projection(ready ? offer : null, result, ready ? offer.getXp() : 0);
        return true;
    }
    private boolean matchesInputs(MerchantOffer offer) {
        return inputOrder(offer) >= 0;
    }
    private int inputOrder(MerchantOffer offer) {
        if (matchesOrder(offer, inputs.getItem(0), inputs.getItem(1))) return 0;
        return matchesOrder(offer, inputs.getItem(1), inputs.getItem(0)) ? 1 : -1;
    }
    private boolean matchesOrder(MerchantOffer offer, ItemStack first, ItemStack second) {
        if (offer.getCostA().is(Items.EMERALD)) {
            if (!first.isEmpty()) return false;
            first = offer.getCostA();
        }
        if (offer.getCostB().is(Items.EMERALD)) {
            if (!second.isEmpty()) return false;
            second = offer.getCostB();
        }
        if (NativeItemPricing.usesWineAging(offer.getCostA()) && !NativeItemPricing.matchesGoods(offer.getCostA(), first)) return false;
        if (NativeItemPricing.usesWineAging(offer.getCostB()) && !NativeItemPricing.matchesGoods(offer.getCostB(), second)) return false;
        return offer.satisfiedBy(first, second);
    }
    public void autoFill(int index) {
        if (index != selected || offer() == null) return;
        // Return real input stacks before filling the newly selected original item legs.
        for (int slot = 0; slot < 2; slot++) {
            var held = inputs.getItem(slot);
            if (held.isEmpty()) continue;
            ItemStack remainder = held.copy(); player.getInventory().add(remainder);
            ((NativeTradeContainer) inputs).jem$input(slot, remainder);
            if (!remainder.isEmpty()) { updateResult(); return; }
        }
        for (int slot = 0; slot < 2; slot++) {
            var required = NativeTradePricing.input(offer(), slot);
            if (required.isEmpty()) continue;
            ItemStack moved = required.copy(); moved.setCount(0);
            for (ItemStack stack : player.getInventory().items) {
                if (!NativeItemPricing.matchesGoods(required, stack)) continue;
                // Native wine aging is accepted, but each moved stack retains its actual vintage/NBT.
                if (moved.isEmpty()) { moved = stack.copy(); moved.setCount(0); }
                else if (!ItemStack.isSameItemSameTags(moved, stack)) continue;
                int take = Math.min(stack.getCount(), required.getMaxStackSize() - moved.getCount());
                moved.grow(take); stack.shrink(take);
                if (moved.getCount() >= required.getMaxStackSize()) break;
            }
            ((NativeTradeContainer) inputs).jem$input(slot, moved);
        }
        player.getInventory().setChanged(); inputs.setChanged(); menu.broadcastChanges();
    }
    public void refresh() {
        if (closed || refreshing || (lastSnapshot != null && player.tickCount - lastRefreshTick < 20)) return;
        refreshing = true;
        lastRefreshTick = player.tickCount;
        try {
            if (buyer) {
                long cycle = MarketSavedData.get(player.getServer()).cycle(player.getServer());
                if (cycle != buyerCycle) {
                    buyerCycle = cycle; BuyerTradeOffers.refresh(merchant, player); selected = 0;
                    inputs.setSelectionHint(0); syncNativeOffers(); status = "jem.economy.trade.demand_changed";
                }
            }
            if (!buyer) ProfessionTradePools.extend(merchant);
            selected = Math.max(0, Math.min(selected, Math.max(0, merchant.getOffers().size() - 1)));
            updateResult();
            List<NativeTradeQuote> rows = new ArrayList<>();
            var offers = merchant.getOffers();
            var terms = offers.stream().map(MerchantOffer::createTag).toList();
            for (int i = 0; i < offers.size(); i++) {
                var quote = NativeTradePricing.quote(player, offers.get(i), i, buyer);
                rows.add(preview ? new NativeTradeQuote(i, quote.direction(), quote.copper(), false, "jem.economy.trade.preview") : quote);
            }
            var bulletin = EconomyApi.marketBulletin(player);
            var snapshot = new NativeTradePacket(menu.containerId, preview ? -1 : merchant.getId(), selected, revision, List.copyOf(rows),
                    EconomyApi.balance(player), bulletin.accountRemaining(), bulletin.ticksUntilUpdate(), status);
            if (!snapshot.equals(lastSnapshot) || !terms.equals(lastTerms)) {
                if (!terms.equals(lastTerms) && player.containerMenu == menu) syncNativeOffers();
                lastTerms = terms;
                revision++;
                lastSnapshot = new NativeTradePacket(snapshot.containerId(), snapshot.merchantId(), snapshot.selected(), revision,
                        snapshot.quotes(), snapshot.wallet(), snapshot.remaining(), snapshot.updateTicks(), snapshot.status());
                EconomyNetwork.sendTrade(player, lastSnapshot);
            }
        } finally { refreshing = false; }
    }
    public void takeQuoted(com.siirio.jemeconomy.network.NativeTradeActionPacket action) {
        if (closed || action.containerId() != menu.containerId || player.containerMenu != menu || handledRequests.contains(action.requestId())) return;
        if (lastSnapshot == null || action.revision() != revision || action.selected() != selected) {
            status = "jem.economy.trade.price_changed"; lastSnapshot = null; refresh(); menu.sendAllDataToRemote(); return;
        }
        var displayed = lastSnapshot.quotes().isEmpty() ? null : lastSnapshot.quotes().get(selected);
        var current = quote();
        if (displayed == null || displayed.copper() != current.copper() || displayed.direction() != current.direction()
                || offer() == null || selected >= lastTerms.size() || !offer().createTag().equals(lastTerms.get(selected))) {
            status = "jem.economy.trade.price_changed"; lastSnapshot = null; refresh(); menu.sendAllDataToRemote(); return;
        }
        handledRequests.add(action.requestId());
        while (handledRequests.size() > 256) handledRequests.remove(handledRequests.iterator().next());
        take(action.button(), action.type(), displayed); menu.sendAllDataToRemote();
    }
    public void rejectUnquotedTake() { lastSnapshot = null; refresh(); }
    private void take(int button, ClickType type, NativeTradeQuote acknowledged) {
        if (preview) { status = "jem.economy.trade.preview"; lastSnapshot = null; refresh(); return; }
        if (!valid() || player.containerMenu != menu || IdentityPolicy.resolve(player).isEmpty()) { status = "jem.economy.trade.identity"; return; }
        if (type != ClickType.PICKUP && type != ClickType.QUICK_MOVE && type != ClickType.SWAP && type != ClickType.THROW) return;
        if ((type == ClickType.PICKUP || type == ClickType.THROW || type == ClickType.QUICK_MOVE) && (button < 0 || button > 1)) return;
        if (type == ClickType.SWAP && !((button >= 0 && button < 9) || button == 40)) return;
        int repetitions = type == ClickType.QUICK_MOVE ? 64 : 1;
        boolean committed = false;
        MerchantOffer acknowledgedOffer = offer();
        ItemStack originalA = acknowledgedOffer == null ? ItemStack.EMPTY : NativeTradePricing.input(acknowledgedOffer, 0).copy();
        ItemStack originalB = acknowledgedOffer == null ? ItemStack.EMPTY : NativeTradePricing.input(acknowledgedOffer, 1).copy();
        ItemStack originalResult = acknowledgedOffer == null ? ItemStack.EMPTY : acknowledgedOffer.getResult().copy();
        for (int repeat = 0; repeat < repetitions; repeat++) {
            var offer = offer(); var quote = quote();
            if (offer == null || offer != acknowledgedOffer || quote.copper() != acknowledged.copper() || quote.direction() != acknowledged.direction()
                    || !sameTerms(originalA, NativeTradePricing.input(offer, 0))
                    || !sameTerms(originalB, NativeTradePricing.input(offer, 1)) || !sameTerms(originalResult, offer.getResult())) {
                if (!committed) status = "jem.economy.trade.price_changed";
                break;
            }
            if (offer == null || !quote.available() || !matchesInputs(offer)) {
                if (!committed) status = quote.available() ? "jem.economy.trade.inputs" : quote.reasonKey();
                break;
            }
            boolean sale = quote.direction() == NativeTradeQuote.Direction.PLAYER_SELLS;
            ItemStack delivery = sale ? ItemStack.EMPTY : offer.getResult().copy();
            if (!sale && !canDeliver(delivery, type, button)) { if (!committed) status = "jem.economy.no_space"; break; }
            // One server-generated operation, synchronously funded before any native output leaves.
            UUID operation = UUID.nameUUIDFromBytes((sessionId + ":" + sequence++).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            int order = inputOrder(offer);
            String failure = NativeTradeSettlement.commit(player, inputs, offer, quote, order, operation);
            if (!failure.isEmpty()) { if (!committed) status = failure; break; }
            for (int slot = 0; slot < 2; slot++) {
                var expected = NativeTradePricing.input(offer, slot);
                if (!expected.isEmpty()) inputs.getItem(order == 0 ? slot : 1 - slot).shrink(expected.getCount());
            }
            if (!sale) deliver(delivery, type, button);
            merchant.setTradingPlayer(player);
            try (var context = CoinTradeContext.enter(player)) { merchant.notifyTrade(offer); }
            merchant.overrideXp(merchant.getVillagerXp() + offer.getXp());
            player.awardStat(Stats.TRADED_WITH_VILLAGER);
            if (!sale) delivery.onCraftedBy(player.level(), player, delivery.getCount());
            player.level().playSound(null, merchant.getX(), merchant.getY(), merchant.getZ(), merchant.getNotifyTradeSound(),
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
            committed = true; status = "jem.economy.trade.complete";
            player.getInventory().setChanged();
        }
        updateResult(); syncNativeOffers(); lastSnapshot = null; refresh(); menu.broadcastChanges();
    }
    private static boolean sameTerms(ItemStack a, ItemStack b) {
        return a.isEmpty() ? b.isEmpty() : !b.isEmpty() && ItemStack.isSameItemSameTags(a, b) && a.getCount() == b.getCount();
    }
    private boolean canDeliver(ItemStack delivery, ClickType type, int button) {
        if (type == ClickType.THROW) return true;
        if (type == ClickType.PICKUP) return fits(menu.getCarried(), delivery);
        if (type == ClickType.SWAP) return fits(player.getInventory().getItem(button), delivery);
        int space = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) space += delivery.getMaxStackSize();
            else if (ItemStack.isSameItemSameTags(stack, delivery)) space += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            if (space >= delivery.getCount()) return true;
        }
        return false;
    }
    private static boolean fits(ItemStack existing, ItemStack delivery) {
        return existing.isEmpty() ? delivery.getCount() <= delivery.getMaxStackSize()
                : ItemStack.isSameItemSameTags(existing, delivery) && existing.getCount() + delivery.getCount() <= existing.getMaxStackSize();
    }
    private void deliver(ItemStack delivery, ClickType type, int button) {
        if (type == ClickType.THROW) { player.drop(delivery, true); return; }
        if (type == ClickType.QUICK_MOVE) {
            ItemStack remainder = delivery.copy(); player.getInventory().add(remainder);
            if (!remainder.isEmpty()) throw new IllegalStateException("Native trade delivery capacity changed on server thread");
            return;
        }
        ItemStack target = type == ClickType.SWAP ? player.getInventory().getItem(button) : menu.getCarried();
        ItemStack combined = target.isEmpty() ? delivery.copy() : target.copy();
        if (!target.isEmpty()) combined.grow(delivery.getCount());
        if (type == ClickType.SWAP) player.getInventory().setItem(button, combined); else menu.setCarried(combined);
    }
    private void syncNativeOffers() {
        player.sendMerchantOffers(menu.containerId, merchant.getOffers(), menu.getTraderLevel(), merchant.getVillagerXp(),
                merchant.showProgressBar(), merchant.canRestock());
    }
    public void close() {
        closed = true; ((NativeTradeContainer) inputs).jem$projection(null, ItemStack.EMPTY, 0);
        lastSnapshot = null; lastTerms = List.of(); handledRequests.clear();
    }
}
