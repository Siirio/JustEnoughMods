package com.siirio.jemserver.smp.events.calendar;

import com.siirio.jemidentity.api.IdentityPolicy;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_server", value = Dist.DEDICATED_SERVER)
public final class HolidayActivities {
    private static final String MOB_TAG = "jem_holiday_owner_";

    public static boolean join(ServerPlayer player, String path) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return false;
        CalendarEventData data = CalendarEventData.get(player.getServer());
        var active = data.activeEvent();
        if (active.isEmpty() || (!path.equals("calm") && !path.equals("power"))) return false;
        if (data.joined(principal.get().accountId(), active.get(), path)) return false;
        if (path.equals("power") && !spawnEncounter(player, active.get())) return false;
        data.join(principal.get().accountId(), active.get(), path);
        if (path.equals("power")) return true;
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                active.get() == CalendarEvent.HALLOWEEN ? "jem.holiday.halloween.calm" : "jem.holiday.new_year.calm"));
        return true;
    }

    public static UUID offerGift(ServerPlayer sender, ServerPlayer target) {
        var senderPrincipal = IdentityPolicy.resolve(sender);
        var targetPrincipal = IdentityPolicy.resolve(target);
        CalendarEventData data = CalendarEventData.get(sender.getServer());
        if (senderPrincipal.isEmpty() || targetPrincipal.isEmpty() || sender == target
                || data.activeEvent().orElse(null) != CalendarEvent.NEW_YEAR) return null;
        ItemStack held = sender.getMainHandItem();
        if (held.isEmpty() || held.getTag() != null && held.getTag().sizeInBytes() > 16_384L) return null;
        ItemStack escrowed = held.copy();
        sender.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        UUID id = data.escrowGift(senderPrincipal.get().accountId(), sender.getGameProfile().getName(),
                targetPrincipal.get().accountId(), target.getGameProfile().getName(), escrowed);
        sender.getInventory().setChanged(); sender.containerMenu.broadcastChanges();
        target.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.holiday.gift_offer",
                sender.getDisplayName(), escrowed.getHoverName(), escrowed.getCount(), id));
        return id;
    }

    public static boolean acceptGift(ServerPlayer target, UUID id) {
        var principal = IdentityPolicy.resolve(target);
        CalendarEventData data = CalendarEventData.get(target.getServer());
        var gift = data.gift(id);
        if (principal.isEmpty() || gift.isEmpty() || !gift.get().getUUID("Target").equals(principal.get().accountId())) return false;
        ItemStack stack = ItemStack.of(gift.get().getCompound("Item"));
        if (stack.isEmpty() || !canAccept(target, stack)) return false;
        ItemStack remaining = stack.copy();
        if (!target.getInventory().add(remaining) || !remaining.isEmpty()) return false;
        data.removeGift(id);
        target.getInventory().setChanged(); target.containerMenu.broadcastChanges();
        target.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.holiday.gift_accepted",
                gift.get().getString("SenderName"), stack.getHoverName(), stack.getCount()));
        return true;
    }

    public static boolean cancelGift(ServerPlayer sender, UUID id) {
        var principal = IdentityPolicy.resolve(sender);
        CalendarEventData data = CalendarEventData.get(sender.getServer());
        var gift = data.gift(id);
        if (principal.isEmpty() || gift.isEmpty() || !gift.get().getUUID("Sender").equals(principal.get().accountId())) return false;
        ItemStack stack = ItemStack.of(gift.get().getCompound("Item"));
        if (stack.isEmpty() || !canAccept(sender, stack)) return false;
        ItemStack remaining = stack.copy();
        if (!sender.getInventory().add(remaining) || !remaining.isEmpty()) return false;
        data.removeGift(id);
        sender.getInventory().setChanged(); sender.containerMenu.broadcastChanges();
        return true;
    }

    private static boolean spawnEncounter(ServerPlayer player, CalendarEvent event) {
        var data = CalendarEventData.get(player.getServer());
        UUID account = IdentityPolicy.resolve(player).orElseThrow().accountId();
        data.progress(account, event, "combat_started", 1, 1);
        int spawned = 0;
        for (int i = 0; i < 12 && spawned < 5; i++) {
            double angle = Math.PI * 2.0D * i / 12.0D;
            BlockPos pos = BlockPos.containing(player.getX() + Math.cos(angle) * 7.0D, player.getY(), player.getZ() + Math.sin(angle) * 7.0D);
            if (!player.serverLevel().hasChunkAt(pos)) continue;
            while (pos.getY() > player.serverLevel().getMinBuildHeight() + 2 && player.serverLevel().getBlockState(pos.below()).isAir()) pos = pos.below();
            EntityType<? extends Mob> type = event == CalendarEvent.HALLOWEEN ? EntityType.ZOMBIE : EntityType.STRAY;
            Mob mob = type.create(player.serverLevel());
            if (mob == null) continue;
            mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), 0.0F);
            if (!player.serverLevel().noCollision(mob)) { mob.discard(); continue; }
            mob.addTag(MOB_TAG + account);
            mob.setTarget(player);
            mob.setPersistenceRequired();
            player.serverLevel().addFreshEntity(mob);
            spawned++;
        }
        if (spawned == 0) return false;
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.holiday.combat_started", spawned));
        return true;
    }

    @SubscribeEvent
    public static void pickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CalendarEventData data = CalendarEventData.get(player.getServer());
        var active = data.activeEvent();
        var principal = IdentityPolicy.resolve(player);
        if (active.isEmpty() || principal.isEmpty() || !data.joined(principal.get().accountId(), active.get(), "calm")) return;
        ItemStack stack = event.getItem().getItem();
        if (active.get() == CalendarEvent.HALLOWEEN && (stack.is(Items.PUMPKIN) || stack.is(Items.CARVED_PUMPKIN))) {
            complete(player, active.get(), "pumpkins", Math.min(stack.getCount(), 4), 4, new ItemStack(Items.CANDLE, 4));
        }
    }

    @SubscribeEvent
    public static void fished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CalendarEventData data = CalendarEventData.get(player.getServer());
        var active = data.activeEvent();
        var principal = IdentityPolicy.resolve(player);
        if (active.isEmpty() || principal.isEmpty() || !data.joined(principal.get().accountId(), active.get(), "calm")) return;
        int ordinary = event.getDrops().stream().filter(stack -> stack.is(net.minecraft.tags.ItemTags.FISHES)).mapToInt(ItemStack::getCount).sum();
        if (ordinary <= 0) return;
        String objective = active.get() == CalendarEvent.HALLOWEEN ? "seasonal_fish" : "winter_fish";
        ItemStack reward = active.get() == CalendarEvent.HALLOWEEN ? new ItemStack(Items.PUMPKIN_PIE, 2) : new ItemStack(Items.SNOWBALL, 8);
        complete(player, active.get(), objective, ordinary, 3, reward);
    }

    @SubscribeEvent
    public static void killed(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty() || !event.getEntity().getTags().contains(MOB_TAG + principal.get().accountId())) return;
        CalendarEventData data = CalendarEventData.get(player.getServer());
        var active = data.activeEvent();
        if (active.isEmpty()) return;
        complete(player, active.get(), "combat", 1, 5,
                active.get() == CalendarEvent.HALLOWEEN ? new ItemStack(Items.GOLDEN_CARROT, 2) : new ItemStack(Items.FIREWORK_ROCKET, 4));
    }

    private static void complete(ServerPlayer player, CalendarEvent event, String objective, int amount, int cap, ItemStack reward) {
        UUID account = IdentityPolicy.resolve(player).orElseThrow().accountId();
        CalendarEventData data = CalendarEventData.get(player.getServer());
        int progress = data.progress(account, event, objective, amount, cap);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.holiday.progress", progress, cap), true);
        if (progress < cap || !data.rewardOnce(account, event, objective)) return;
        ItemStack remaining = reward.copy();
        if (!player.getInventory().add(remaining) || !remaining.isEmpty()) {
            player.serverLevel().addFreshEntity(new ItemEntity(player.serverLevel(), player.getX(), player.getY(), player.getZ(), remaining));
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.holiday.completed", reward.getHoverName()));
    }

    private static boolean canAccept(ServerPlayer player, ItemStack offered) {
        int remaining = offered.getCount();
        for (ItemStack current : player.getInventory().items) {
            if (current.isEmpty()) return true;
            if (ItemStack.isSameItemSameTags(current, offered)) {
                remaining -= Math.max(0, current.getMaxStackSize() - current.getCount());
                if (remaining <= 0) return true;
            }
        }
        return false;
    }

    static void cleanup(net.minecraft.server.MinecraftServer server) {
        for (var level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity.getTags().stream().anyMatch(tag -> tag.startsWith(MOB_TAG))) entity.discard();
            }
        }
    }

    private HolidayActivities() { }
}
