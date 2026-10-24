package com.siirio.jemfood;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = JEMFood.ID)
public final class FoodEvents {
    private static final ThreadLocal<Boolean> POISON = ThreadLocal.withInitial(() -> false);
    private FoodEvents() {}
    @SubscribeEvent
    public static void consumed(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && !PlacedConsumption.active(player) && edible(event.getItem(), player))
            FoodState.get(player).consume(player, ForgeRegistries.ITEMS.getKey(event.getItem().getItem()));
    }
    private static boolean edible(ItemStack stack, Player player) {
        return stack.getFoodProperties(player) != null;
    }
    @SubscribeEvent
    public static void quickbite(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player && edible(event.getItem(), player))
            event.setDuration(Math.max(1, event.getDuration() - (int) FoodState.get(player).value(player, "quickbite")));
    }
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.isAlive()) FoodState.get(player).pulse(player);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0 || event.getEntity().level().isClientSide) return;
        var source = event.getSource();
        boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE) && source.getDirectEntity() instanceof Projectile && !source.is(DamageTypeTags.IS_EXPLOSION);
        boolean melee = source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof ServerPlayer;
        if (!(event.getEntity() instanceof Player) && source.getEntity() instanceof ServerPlayer attacker && (projectile || melee)) {
            var state = FoodState.get(attacker);
            double bonus = 0;
            if (melee) {
                bonus += state.value(attacker, "keenedge");
                if (attacker.isSprinting()) bonus += state.value(attacker, "onrush");
                if (attacker.getHealth() < attacker.getMaxHealth() * 0.5F) bonus += state.value(attacker, "resolve");
            } else bonus += state.value(attacker, "marksman");
            if (ordinary(event.getEntity()) && event.getEntity() instanceof Enemy) bonus += state.value(attacker, "beastbane");
            event.setAmount(event.getAmount() * (float) (1 + Math.min(0.07, bonus)));
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            var state = FoodState.get(player);
            state.lastDamage = FoodState.now(player);
            if (source.is(DamageTypeTags.BYPASSES_EFFECTS) || source.is(DamageTypeTags.BYPASSES_RESISTANCE)) return;
            boolean fire = source.is(DamageTypeTags.IS_FIRE);
            boolean freeze = source.is(DamageTypes.FREEZE);
            boolean poison = POISON.get();
            boolean wither = source.is(DamageTypes.WITHER);
            boolean hazard = fire || freeze || poison || wither;
            double reduction = 0;
            if (source.is(DamageTypes.FALL)) reduction += state.value(player, "surefoot");
            if (projectile) reduction += state.value(player, "bulwark");
            if (fire) reduction += state.value(player, "cinderward");
            if (freeze) reduction += state.value(player, "frostward");
            if (poison || wither) reduction += state.value(player, "purity");
            // Fall has its explicitly authored 10/16/22% budget, distinct from combat physical damage.
            double cap = source.is(DamageTypes.FALL) ? 0.30 : hazard ? 0.30 : 0.10;
            event.setAmount(event.getAmount() * (float) (1 - Math.min(cap, reduction)));
        }
    }
    private static boolean ordinary(net.minecraft.world.entity.LivingEntity entity) {
        return entity instanceof Mob && entity.getType().is(Tags.EntityTypes.BOSSES) == false
                && !entity.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                new net.minecraft.resources.ResourceLocation(JEMFood.ID, "bosses")));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void xp(LivingExperienceDropEvent event) {
        ServerPlayer player = event.getAttackingPlayer() instanceof ServerPlayer server ? server : null;
        if (event.isCanceled() || player == null || !ordinary(event.getEntity()) || event.getDroppedExperience() <= 0) return;
        double bonus = FoodState.get(player).value(player, "sage");
        event.setDroppedExperience(event.getDroppedExperience() + Math.min(2, (int) Math.floor(event.getDroppedExperience() * bonus)));
    }
    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.isCanceled() || event.getEntity().isCreative()) return;
        Player player = event.getEntity();
        ItemStack tool = player.getMainHandItem();
        BlockState block = event.getState();
        if (!tool.isCorrectToolForDrops(block)) return;
        double bonus = 0;
        if (tool.is(ItemTags.PICKAXES) && block.is(BlockTags.MINEABLE_WITH_PICKAXE)) bonus = traitValue(player, "miner");
        if (tool.is(ItemTags.SHOVELS) && block.is(BlockTags.MINEABLE_WITH_SHOVEL)) bonus = traitValue(player, "digger");
        if (tool.is(ItemTags.AXES) && block.is(BlockTags.LOGS)) bonus = traitValue(player, "woodcutter");
        event.setNewSpeed(event.getNewSpeed() * (float) (1 + bonus));
    }
    private static double traitValue(Player player, String name) {
        if (player instanceof ServerPlayer server) return FoodState.get(server).value(server, name);
        return net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.siirio.jemfood.client.FoodTooltips.value(name));
    }
    public static boolean poisonDamage(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount, boolean poison) {
        boolean previous = POISON.get();
        POISON.set(poison);
        try { return entity.hurt(source, amount); }
        finally { if (previous) POISON.set(true); else POISON.remove(); }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void removed(MobEffectEvent.Remove event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player && event.getEffect() == JEMFood.BLESSING.get()) {
            var state = FoodState.get(player);
            if (!state.internal) { state.expires = 0; state.save(player); }
        }
    }
    /** Called only after native destroyBlock returned true for the captured mature crop. */
    public static void harvested(ServerPlayer player, BlockState before) {
        if (player.isCreative() || !(before.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(before)
                || !before.is(BlockTags.CROPS)) return;
        var state = FoodState.get(player);
        int threshold = (int) state.value(player, "harvester");
        long now = FoodState.now(player);
        if (threshold > 0 && state.ready("harvester", now) && state.threshold("harvester", threshold)) {
            player.giveExperiencePoints(1); state.cooldown("harvester", now, 200); state.save(player);
        }
    }
    public static void fished(ServerPlayer player) {
        if (player.isCreative()) return;
        var state = FoodState.get(player);
        int threshold = (int) state.value(player, "angler");
        long now = FoodState.now(player);
        if (threshold > 0 && state.ready("angler", now) && state.threshold("angler", threshold)) {
            player.giveExperiencePoints(1); state.cooldown("angler", now, 600); state.save(player);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player) {
            var state = FoodState.get(player); state.clearBundle(player); state.save(player);
        }
    }
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getOriginal() instanceof ServerPlayer original) {
            var state = FoodState.get(original); state.clearBundle(original); state.save(original);
        }
        player.getPersistentData().put(FoodState.KEY, event.getOriginal().getPersistentData().getCompound(FoodState.KEY).copy());
        player.getPersistentData().put("JemFoodQualityV1", event.getOriginal().getPersistentData().getCompound("JemFoodQualityV1").copy());
        FoodState.forget(player);
        FoodState.get(player).clearBundle(player); FoodState.get(player).save(player);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var state = FoodState.get(player); state.clearBundle(player); state.pulse(player); state.save(player);
        }
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var state = FoodState.get(player); state.clearBundle(player); state.pulse(player); state.save(player);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var state = FoodState.get(player); state.clearBundle(player); FoodState.logout(player);
        }
    }
    @SubscribeEvent public static void stopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            var state = FoodState.get(player); state.clearBundle(player); state.save(player);
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { FoodState.stop(); }
}
