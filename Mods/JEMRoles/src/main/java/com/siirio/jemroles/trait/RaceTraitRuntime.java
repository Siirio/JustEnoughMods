package com.siirio.jemroles.trait;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemroles.api.RaceApi;
import com.siirio.jemroles.race.RaceId;
import com.siirio.jemroles.race.RaceSavedData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;

public final class RaceTraitRuntime {
    private static final UUID ELF_SPEED = UUID.fromString("0cc57d67-c799-45f9-8313-d0c56cb0be9e");
    private static final UUID DWARF_KNOCKBACK = UUID.fromString("43b459d2-9746-48d6-acce-5fd66d10f50d");
    private static final Map<UUID, Position> LAST_POSITION = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ACTION_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> COUNTED_ACTION_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingDurability> PENDING_DURABILITY = new ConcurrentHashMap<>();
    private static final TagKey<net.minecraft.world.level.block.Block> DWARF_MINING = TagKey.create(Registries.BLOCK,
            new ResourceLocation("jem_roles", "dwarf_mining"));

    private RaceTraitRuntime() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        RaceId race = RaceApi.effectiveRace(player.getServer(), player.getUUID());
        updateModifiers(player, race);
        applyDurabilityPrevention(player, race);
        IdentityPolicy.resolve(player).ifPresent(principal -> {
            Position current = new Position(player.getX(), player.getY(), player.getZ());
            Position prior = LAST_POSITION.put(player.getUUID(), current);
            int action = ACTION_TICK.getOrDefault(player.getUUID(), Integer.MIN_VALUE);
            int counted = COUNTED_ACTION_TICK.getOrDefault(player.getUUID(), Integer.MIN_VALUE);
            boolean moved = prior != null && prior.distanceSquared(current) > 0.0025D;
            boolean acted = action > counted;
            if ((moved || acted) && !player.isSpectator() && player.isAlive()) {
                RaceSavedData data = RaceSavedData.get(player.getServer());
                data.find(principal.accountId()).ifPresent(record -> { record.addActiveTicks(20L); data.changed(); });
                if (acted) COUNTED_ACTION_TICK.put(player.getUUID(), action);
            }
        });
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_POSITION.remove(event.getEntity().getUUID());
        ACTION_TICK.remove(event.getEntity().getUUID());
        COUNTED_ACTION_TICK.remove(event.getEntity().getUUID());
        PENDING_DURABILITY.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent public static void attack(AttackEntityEvent event) { markAction(event.getEntity()); }
    @SubscribeEvent public static void interact(PlayerInteractEvent event) { markAction(event.getEntity()); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void harvest(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player instanceof FakePlayer || event.isCanceled()) return;
        if (RaceApi.effectiveRace(player.getServer(), player.getUUID()) != RaceId.DWARF) return;
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !stack.isDamageableItem() || !player.hasCorrectToolForDrops(event.getState())) return;
        markAction(player);
        PENDING_DURABILITY.put(player.getUUID(), new PendingDurability(stack, stack.getDamageValue()));
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (RaceApi.effectiveRace(player.getServer(), player.getUUID()) != RaceId.DWARF) return;
        if (player instanceof FakePlayer || !event.getState().is(DWARF_MINING) || !event.getState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                || !player.hasCorrectToolForDrops(event.getState())) return;
        event.setNewSpeed(event.getOriginalSpeed() * 1.10F);
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim
                && RaceApi.effectiveRace(victim.getServer(), victim.getUUID()) == RaceId.ELF
                && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            event.setAmount(event.getAmount() * 0.90F);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && !(event.getEntity() instanceof net.minecraft.world.entity.player.Player)
                && event.getSource().getDirectEntity() instanceof AbstractArrow
                && !(event.getSource().getDirectEntity() instanceof ThrownTrident)
                && RaceApi.effectiveRace(attacker.getServer(), attacker.getUUID()) == RaceId.ELF
                && !event.getSource().getDirectEntity().getPersistentData().getBoolean("JemRolesElfHitApplied")) {
            event.getSource().getDirectEntity().getPersistentData().putBoolean("JemRolesElfHitApplied", true);
            event.setAmount(event.getAmount() + 1.0F);
        }
    }

    @SubscribeEvent
    public static void trade(net.minecraftforge.event.entity.player.TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (RaceApi.effectiveRace(player.getServer(), player.getUUID()) != RaceId.HUMAN) return;
        var offer = event.getMerchantOffer();
        ItemStack cost = offer.getCostA();
        if (!cost.is(Items.EMERALD) || offer.getResult().is(Items.EMERALD)) return;
        int discounted = Math.max(1, (int) Math.ceil(cost.getCount() * 0.90D));
        int refund = cost.getCount() - discounted;
        if (refund <= 0) return;
        ItemStack emeralds = new ItemStack(Items.EMERALD, refund);
        if (!player.getInventory().add(emeralds)) player.drop(emeralds, false);
    }

    private static void markAction(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer) ACTION_TICK.put(player.getUUID(), player.tickCount);
    }

    private static void applyDurabilityPrevention(ServerPlayer player, RaceId race) {
        PendingDurability pending = PENDING_DURABILITY.remove(player.getUUID());
        if (race != RaceId.DWARF || pending == null) return;
        ItemStack current = player.getMainHandItem();
        if (current == pending.stack && current.getDamageValue() > pending.damage && player.getRandom().nextFloat() < 0.10F) {
            current.setDamageValue(Math.max(pending.damage, current.getDamageValue() - 1));
        }
    }

    private static void updateModifiers(ServerPlayer player, RaceId race) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(ELF_SPEED);
            if (race == RaceId.ELF && player.onGround() && !player.isPassenger() && !player.isSwimming() && !player.isFallFlying()) {
                speed.addTransientModifier(new AttributeModifier(ELF_SPEED, "JEM elf on-foot speed", 0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
        var knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.removeModifier(DWARF_KNOCKBACK);
            if (race == RaceId.DWARF) {
                knockback.addTransientModifier(new AttributeModifier(DWARF_KNOCKBACK, "JEM dwarf knockback resistance", 0.10D, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    private record Position(double x, double y, double z) {
        double distanceSquared(Position other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    private record PendingDurability(ItemStack stack, int damage) {}
}
