package com.siirio.jemvisualsandbox.workbench;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Passive adapter of JEMRoles RaceTraitRuntime; authored sandbox abilities retain their owners. */
@Mod.EventBusSubscriber(modid = "jem_visual_sandbox")
public final class SandboxRaceTraits {
    private static final UUID VOID_HEALTH = UUID.fromString("65cc4bf4-fc6c-43be-9d9b-23b7bb8f51d6");
    private static final UUID SCULK_HEALTH = UUID.fromString("5f85aeb5-778c-4a22-a108-cde26538ff58");
    private static final UUID SCULK_ATTACK = UUID.fromString("0e3e3509-88db-437c-81dd-5b62c4124950");
    private static final UUID BLOOM_BARREN = UUID.fromString("2e2f5209-0a0e-4ccb-aee4-7a92c93bf26b");
    private static final UUID SCULK_SUN = UUID.fromString("593bd676-c3dd-4dd3-856c-734c380013fb");
    private static final UUID NETHER_FROZEN = UUID.fromString("e17185a7-99e8-427b-9710-02973629d149");
    private static final UUID WATER_STIFFNESS = UUID.fromString("25a887ba-2e0c-4b4f-9b3f-149137cbab7f");
    private record Discount(UUID merchant, List<Integer> prices) {}
    private static final Map<UUID, Discount> DISCOUNTS = new HashMap<>();
    private SandboxRaceTraits() {}

    private static boolean owner(ServerPlayer p) {
        return RaceExperience.allowed(p);
    }
    public static boolean hasRace(ServerPlayer p, int race) {
        return owner(p) && RaceExperience.selected(p) && RaceExperience.race(p) == race;
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || !owner(p)) return;
        int race = RaceExperience.selected(p) ? RaceExperience.race(p) : -1;
        if (race != 0) restoreMerchantDiscount(p);
        if (race == 1 && p.isAlive() && !p.isSpectator()) voidState(p);
        if (race == 4 && p.isAlive() && !p.isSpectator()) lavaMovement(p);
        if (p.tickCount % 20 == 0) refresh(p);
    }
    static void refresh(ServerPlayer p) {
        if(!owner(p))return;
        int race=RaceExperience.selected(p)?RaceExperience.race(p):-1;
        clearAttributes(p);
        if (!p.isAlive() || p.isSpectator()) return;
        ServerLevel level = p.serverLevel(); BlockPos pos = p.blockPosition();
        if (race == 1) add(p, Attributes.MAX_HEALTH, VOID_HEALTH, -6, AttributeModifier.Operation.ADDITION);
        if (race == 2) {
            if (!SandboxEnvironment.green(level, pos)) add(p, Attributes.MAX_HEALTH, BLOOM_BARREN, -8, AttributeModifier.Operation.ADDITION);
            else if (SandboxEnvironment.naturalDaylight(level, pos)) {
                p.heal(.5F);
                p.getFoodData().setExhaustion(Math.max(0, p.getFoodData().getExhaustionLevel() - 1));
            }
        }
        if (race == 3) {
            if (SandboxEnvironment.underground(level, pos)) {
                add(p, Attributes.MAX_HEALTH, SCULK_HEALTH, 10, AttributeModifier.Operation.ADDITION);
                add(p, Attributes.ATTACK_DAMAGE, SCULK_ATTACK, .15, AttributeModifier.Operation.MULTIPLY_TOTAL);
            }
            if (SandboxEnvironment.naturalDaylight(level, pos)) add(p, Attributes.MAX_HEALTH, SCULK_SUN, -8, AttributeModifier.Operation.ADDITION);
        }
        if (race == 4) {
            if (level.getBiome(pos).value().coldEnoughToSnow(pos)) add(p, Attributes.MAX_HEALTH, NETHER_FROZEN, -8, AttributeModifier.Operation.ADDITION);
            int old = p.getPersistentData().getInt("JemSandboxWaterExposure");
            int exposure = p.isInWater() && !p.isPassenger() ? Math.min(140, old + 20) : Math.max(0, old - 10);
            p.getPersistentData().putInt("JemSandboxWaterExposure", exposure);
            if (p.isInWater()) add(p, Attributes.MOVEMENT_SPEED, WATER_STIFFNESS, -.70 * exposure / 140.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
        if (race == 0) for (Raider raider : level.getEntitiesOfClass(Raider.class, p.getBoundingBox().inflate(48), Entity::isAlive))
            if (raider.getTarget() == null || raider.distanceToSqr(p) < raider.distanceToSqr(raider.getTarget())) raider.setTarget(p);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }
    private static void voidState(ServerPlayer p) {
        ServerLevel level = p.serverLevel(); var tag = p.getPersistentData();
        String key = "JemSandboxSafe_" + level.dimension().location().toString().replace(':', '_');
        if (SandboxEnvironment.safe(p, level, p.blockPosition()) && !p.isInWater()) tag.putLong(key, p.blockPosition().asLong());
        BlockPos saved = tag.contains(key) ? BlockPos.of(tag.getLong(key)) : null;
        if (p.getY() < level.getMinBuildHeight() + 1 && saved != null && SandboxEnvironment.safe(p, level, saved)) teleport(p, saved);
        boolean exposed = p.isInWater() && !waterProtected(p);
        if (exposed && !tag.getBoolean("JemSandboxWaterContact")) {
            p.hurt(p.damageSources().drown(), 1);
            BlockPos dry = saved;
            if (dry == null || !dry.closerToCenterThan(p.position(), 100) || !SandboxEnvironment.safe(p, level, dry))
                dry = SandboxEnvironment.nearestDrySurface(p, level, p.blockPosition(), 32)
                        .or(() -> SandboxEnvironment.nearestSafe(p, level, p.blockPosition(), 16, true)).orElse(null);
            if (dry != null) {
                teleport(p, dry);
                level.playSound(null, dry, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
            }
        }
        tag.putBoolean("JemSandboxWaterContact", exposed);
    }
    private static void teleport(ServerPlayer p, BlockPos pos) {
        p.teleportTo(p.serverLevel(), pos.getX() + .5, pos.getY(), pos.getZ() + .5, p.getYRot(), p.getXRot());
    }
    private static boolean waterProtected(ServerPlayer p) {
        if (p.isPassenger() || p.hasEffect(MobEffects.WATER_BREATHING) || p.hasEffect(MobEffects.CONDUIT_POWER)) return true;
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.FROST_WALKER, p) > 0
                || net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.RESPIRATION, p) > 0;
    }
    private static void lavaMovement(ServerPlayer p) {
        p.clearFire();
        if (!p.isInLava()) { if (p.isSwimming() && !p.isInWater()) p.setSwimming(false); return; }
        p.setSwimming(true); Vec3 look = p.getLookAngle(); double speed = p.isSprinting() ? .085 : .055;
        Vec3 motion = p.getDeltaMovement().scale(.82).add(look.x * speed, look.y * speed + .025, look.z * speed);
        p.setDeltaMovement(motion.x, Math.max(-.25, Math.min(.32, motion.y)), motion.z); p.hurtMarked = true;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void attack(LivingAttackEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && hasRace(p, 4) && e.getSource().is(DamageTypeTags.IS_FIRE)) { e.setCanceled(true); p.clearFire(); }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void hurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !owner(p) || !RaceExperience.selected(p) || !e.getSource().is(DamageTypeTags.IS_FIRE)) return;
        switch (RaceExperience.race(p)) { case 2 -> e.setAmount(e.getAmount() * 2); case 3 -> e.setAmount(e.getAmount() * 1.25F); case 4 -> e.setCanceled(true); default -> { } }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void death(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && hasRace(p, 2) && SandboxEnvironment.green(p.serverLevel(), p.blockPosition())
                && p.getY() > p.serverLevel().getMinBuildHeight() && RaceExperience.ready(p, "second_blossom")) {
            e.setCanceled(true); p.setHealth(p.getMaxHealth() * .5F);
            RaceOwnedEffects.grant(p,new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
            RaceExperience.cooldown(p, "second_blossom", 24000);
        }
        if (e.getSource().getEntity() instanceof ServerPlayer p && hasRace(p, 0)
                && (e.getEntity() instanceof Animal || e.getEntity() instanceof AbstractVillager)) RaceExperience.cooldown(p, "guardian", 24000);
    }
    @SubscribeEvent public static void experience(LivingExperienceDropEvent e) {
        if (e.getAttackingPlayer() instanceof ServerPlayer p && hasRace(p, 3)) e.setDroppedExperience((int)Math.floor(e.getDroppedExperience() * 1.15));
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void target(LivingChangeTargetEvent e) {
        if (!(e.getNewTarget() instanceof ServerPlayer p) || !owner(p) || !RaceExperience.selected(p)) return;
        var actor = e.getEntity(); int race = RaceExperience.race(p);
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(actor.getType()).toString();
        boolean endKin = List.of("minecraft:enderman", "minecraft:endermite", "minecraft:shulker").contains(id);
        boolean endBoss = actor instanceof EnderDragon || id.equals("minecraft:wither");
        if (race == 1 && endKin && !endBoss || race == 2 && SandboxEnvironment.green(p.serverLevel(), p.blockPosition()) && !(actor instanceof EnderMan)
                || race == 4 && actor instanceof AbstractPiglin) e.setCanceled(true);
    }
    @SubscribeEvent public static void containerOpen(PlayerContainerEvent.Open e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !hasRace(p, 0) || !(e.getContainer() instanceof MerchantMenu menu)) return;
        restoreMerchantDiscount(p);
        AbstractVillager merchant = p.serverLevel().getEntitiesOfClass(AbstractVillager.class, p.getBoundingBox().inflate(8), v -> v.getTradingPlayer() == p)
                .stream().min(java.util.Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        if (merchant == null) return;
        var prices = new java.util.ArrayList<Integer>();
        for (var offer : merchant.getOffers()) {
            prices.add(offer.getSpecialPriceDiff()); var cost = offer.getCostA();
            if (cost.is(Items.EMERALD) && !offer.getResult().is(Items.EMERALD)) offer.addToSpecialPriceDiff(Math.max(1, (int)Math.ceil(cost.getCount() * .8)) - cost.getCount());
        }
        DISCOUNTS.put(p.getUUID(), new Discount(merchant.getUUID(), prices));
        p.sendMerchantOffers(menu.containerId, merchant.getOffers(), menu.getTraderLevel(), merchant.getVillagerXp(), merchant.showProgressBar(), merchant.canRestock());
    }
    @SubscribeEvent public static void containerClose(PlayerContainerEvent.Close e) { if (e.getEntity() instanceof ServerPlayer p && owner(p)) restoreMerchantDiscount(p); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p && owner(p)) clear(p); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { if (RaceExperience.serverActive(e.getServer())) DISCOUNTS.clear(); }
    public static void clear(ServerPlayer p) {
        if (!owner(p)) return;
        restoreMerchantDiscount(p); clearAttributes(p);
        if(p.isSwimming()&&!p.isInWater())p.setSwimming(false);
        p.getPersistentData().remove("JemSandboxWaterExposure"); p.getPersistentData().remove("JemSandboxWaterContact");
    }
    private static void restoreMerchantDiscount(ServerPlayer p) {
        Discount discount = DISCOUNTS.remove(p.getUUID()); if (discount == null) return;
        for (ServerLevel level : p.server.getAllLevels()) {
            if (!(level.getEntity(discount.merchant()) instanceof AbstractVillager merchant)) continue;
            for (int i = 0; i < merchant.getOffers().size() && i < discount.prices().size(); i++) {
                var offer = merchant.getOffers().get(i); offer.resetSpecialPriceDiff(); offer.addToSpecialPriceDiff(discount.prices().get(i));
            }
            break;
        }
    }
    private static void clearAttributes(ServerPlayer p) {
        for (UUID id : List.of(VOID_HEALTH, SCULK_HEALTH, BLOOM_BARREN, SCULK_SUN, NETHER_FROZEN)) remove(p, Attributes.MAX_HEALTH, id);
        remove(p, Attributes.ATTACK_DAMAGE, SCULK_ATTACK); remove(p, Attributes.MOVEMENT_SPEED, WATER_STIFFNESS);
    }
    private static void remove(ServerPlayer p, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id) {
        var instance = p.getAttribute(attribute); if (instance != null) instance.removeModifier(id);
    }
    private static void add(ServerPlayer p, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id, double amount, AttributeModifier.Operation operation) {
        var instance = p.getAttribute(attribute); if (instance != null) instance.addTransientModifier(new AttributeModifier(id, "JEM sandbox race", amount, operation));
    }
}
