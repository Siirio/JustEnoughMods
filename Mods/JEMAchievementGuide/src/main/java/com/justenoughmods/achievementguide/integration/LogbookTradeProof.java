package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.mixin.MerchantQuestAccess;
import io.redspace.ironsspellbooks.entity.mobs.wizards.priest.PriestEntity;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

/** Native translation followed by a native Priest exchange of the same authenticated book. */
public final class LogbookTradeProof {
    private static final String ACTOR = "JemQuestLogbookTranslatorV4";
    private static final ThreadLocal<Run> CURRENT = new ThreadLocal<>();
    public static final class Run {
        final MerchantMenu menu;
        final ServerPlayer actor;
        final Merchant merchant;
        final MerchantOffer offer;
        final int uses;
        final ItemStack result;
        final int held;
        final Map<ItemStack, Integer> originals;
        boolean committed;
        Run(MerchantMenu menu, ServerPlayer actor, Merchant merchant, MerchantOffer offer) {
            this.menu = menu;
            this.actor = actor;
            this.merchant = merchant;
            this.offer = offer;
            this.uses = offer.getUses();
            this.result = offer.getResult().copy();
            this.held = count(actor, menu, this.result);
            originals = new IdentityHashMap<>();
            for (ItemStack stack : held(actor, menu)) originals.put(stack, stack.getCount());
        }
    }
    private LogbookTradeProof() {}

    public static Run begin(MerchantMenu menu, ServerPlayer actor) {
        CURRENT.remove();
        if (actor.containerMenu != menu || actor.isSpectator() || actor.getAbilities().instabuild
                || actor instanceof net.minecraftforge.common.util.FakePlayer) return null;
        var access = (MerchantQuestAccess) menu;
        var merchant = access.jem$nativeMerchant();
        var offer = access.jem$nativeTradeContainer().getActiveOffer();
        if (offer == null || merchant.getTradingPlayer() != actor || !menu.stillValid(actor)) return null;
        boolean translation = merchant instanceof WanderingTrader trader && trader.getSpawnType() == MobSpawnType.STRUCTURE
                && offer.getBaseCostA().is(ItemRegistry.UNTRANSLATED_ARCHEVOKER_LOGBOOK.get())
                && offer.getBaseCostA().getCount() == 1 && offer.getCostB().is(Items.EMERALD)
                && offer.getCostB().getCount() == 64 && offer.getResult().is(ItemRegistry.TRANSLATED_ARCHEVOKER_LOGBOOK.get());
        boolean bible = merchant instanceof PriestEntity priest && priest.getSpawnType() == MobSpawnType.STRUCTURE
                && offer.getBaseCostA().is(ItemRegistry.TRANSLATED_ARCHEVOKER_LOGBOOK.get())
                && offer.getBaseCostA().getCount() == 1 && offer.getCostB().isEmpty()
                && offer.getResult().is(ItemRegistry.VILLAGER_SPELL_BOOK.get());
        if ((!translation && !bible) || offer.getResult().getCount() != 1) return null;
        var run = new Run(menu, actor, merchant, offer);
        CURRENT.set(run);
        return run;
    }

    public static void consumed(Merchant merchant, MerchantOffer offer, ItemStack a, ItemStack b,
                                ItemStack beforeA, ItemStack beforeB, boolean accepted) {
        Run run = CURRENT.get();
        if (run == null || !accepted || run.merchant != merchant || merchant.getTradingPlayer() != run.actor
                || run.actor.containerMenu != run.menu || !merchant.getOffers().contains(offer)
                || !ItemStack.matches(run.result, offer.getResult()) || !offer.satisfiedBy(beforeA, beforeB)
                || beforeA.getCount() - a.getCount() != offer.getCostA().getCount()
                || beforeB.getCount() - b.getCount() != offer.getCostB().getCount()) return;
        if (run.result.is(ItemRegistry.VILLAGER_SPELL_BOOK.get())) {
            var tag = beforeA.getTag();
            if (tag == null || !tag.hasUUID(ACTOR) || !run.actor.getUUID().equals(tag.getUUID(ACTOR))) return;
        }
        run.committed = true;
    }

    public static void finish(Run run) {
        CURRENT.remove();
        if (run == null || !run.committed || run.offer.getUses() != run.uses + 1 || run.actor.containerMenu != run.menu
                || run.merchant.getTradingPlayer() != run.actor || !run.menu.stillValid(run.actor)
                || count(run.actor, run.menu, run.result) != run.held + 1) return;
        var fresh = held(run.actor, run.menu).stream().filter(stack -> ItemStack.isSameItemSameTags(stack, run.result)
                && stack.getCount() - run.originals.getOrDefault(stack, 0) == 1).toList();
        if (fresh.size() != 1) return;
        if (run.result.is(ItemRegistry.TRANSLATED_ARCHEVOKER_LOGBOOK.get())) {
            fresh.get(0).getOrCreateTag().putUUID(ACTOR, run.actor.getUUID());
        } else QuestActionTrigger.INSTANCE.emit(run.actor, "v3_magic_book_translation");
    }

    private static ArrayList<ItemStack> held(ServerPlayer actor, MerchantMenu menu) {
        var stacks = new ArrayList<ItemStack>();
        for (int slot = 0; slot < actor.getInventory().getContainerSize(); slot++) stacks.add(actor.getInventory().getItem(slot));
        stacks.add(menu.getCarried());
        return stacks;
    }
    private static int count(ServerPlayer actor, MerchantMenu menu, ItemStack result) {
        return held(actor, menu).stream().filter(stack -> ItemStack.isSameItemSameTags(stack, result)).mapToInt(ItemStack::getCount).sum();
    }
}
