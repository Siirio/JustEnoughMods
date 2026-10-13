package com.siirio.jemroles.trait;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemroles.api.RaceApi;
import com.siirio.jemroles.race.RaceId;
import com.siirio.jemroles.race.RaceSavedData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class RaceTraitRuntime {
    private static final UUID ELF_SPEED = UUID.fromString("0cc57d67-c799-45f9-8313-d0c56cb0be9e");
    private static final UUID DWARF_KNOCKBACK = UUID.fromString("43b459d2-9746-48d6-acce-5fd66d10f50d");
    private static final Map<UUID, Position> LAST_POSITION = new ConcurrentHashMap<>();

    private RaceTraitRuntime() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        RaceId race = RaceApi.effectiveRace(player.getServer(), player.getUUID());
        updateModifiers(player, race);
        IdentityPolicy.resolve(player).ifPresent(principal -> {
            Position current = new Position(player.getX(), player.getY(), player.getZ());
            Position prior = LAST_POSITION.put(player.getUUID(), current);
            if (prior != null && prior.distanceSquared(current) > 0.0025D && !player.isSpectator()) {
                RaceSavedData data = RaceSavedData.get(player.getServer());
                data.find(principal.accountId()).ifPresent(record -> { record.addActiveTicks(20L); data.changed(); });
            }
        });
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_POSITION.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (RaceApi.effectiveRace(player.getServer(), player.getUUID()) != RaceId.DWARF) return;
        if (!event.getState().is(BlockTags.MINEABLE_WITH_PICKAXE) || !player.hasCorrectToolForDrops(event.getState())) return;
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
                && RaceApi.effectiveRace(attacker.getServer(), attacker.getUUID()) == RaceId.ELF) {
            event.setAmount(event.getAmount() + 1.0F);
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
}
